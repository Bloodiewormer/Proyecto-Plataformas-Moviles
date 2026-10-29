import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Firma de release: se lee de gradle.properties del usuario o de variables de entorno, nunca del repo.
val releaseSigning = listOf(
    "GLIFO_RELEASE_STORE_FILE",
    "GLIFO_RELEASE_STORE_PASSWORD",
    "GLIFO_RELEASE_KEY_ALIAS",
    "GLIFO_RELEASE_KEY_PASSWORD",
).associateWith { providers.gradleProperty(it).orElse(providers.environmentVariable(it)).orNull }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "cr.ac.una.glifo"
    compileSdk = 37

    defaultConfig {
        applicationId = "cr.ac.una.glifo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Debug: backend local visto desde el emulador. Release: backend desplegado en Render.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api/v1/\"")
    }

    signingConfigs {
        if (releaseSigning.values.none { it.isNullOrBlank() }) {
            create("release") {
                storeFile = file(releaseSigning.getValue("GLIFO_RELEASE_STORE_FILE")!!)
                storePassword = releaseSigning.getValue("GLIFO_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("GLIFO_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("GLIFO_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "API_BASE_URL", "\"https://glifo-api.onrender.com/api/v1/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material.google)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Icons
    implementation(libs.androidx.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // OpenCV
    implementation(libs.opencv)
    // Firebase Crashlytics, Analytics & Cloud Messaging (#48)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // ML Kit — Pipeline N1: OCR local con el modelo latino empaquetado (sin llamadas de red)
    implementation(libs.mlkit.text.recognition)


    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
