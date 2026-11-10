import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// URL del backend para el APK de release (Render). No va al repo: se lee de local.properties
// (glifo.release.base.url=...), de -PGLIFO_RELEASE_BASE_URL=... o de la variable de entorno
// GLIFO_RELEASE_BASE_URL. Sin ninguna, el release usa la misma URL del emulador.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val debugBaseUrl = "http://10.0.2.2:8080/api/v1/"
val releaseBaseUrl: String = (
    localProps.getProperty("glifo.release.base.url")
        ?: providers.gradleProperty("GLIFO_RELEASE_BASE_URL").orNull
        ?: System.getenv("GLIFO_RELEASE_BASE_URL")
    )?.trim()?.takeIf { it.isNotEmpty() }?.let { it.removeSuffix("/") + "/" } ?: debugBaseUrl
// Las cuentas demo del login se ven en debug; en release solo si se pide (glifo.release.show.demo=true).
val releaseShowDemo: Boolean = localProps.getProperty("glifo.release.show.demo")?.trim()?.toBoolean() ?: false

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
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
            buildConfigField("boolean", "SHOW_DEMO_ACCOUNTS", "true")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
            buildConfigField("boolean", "SHOW_DEMO_ACCOUNTS", releaseShowDemo.toString())
            // APK de prueba para compañeros: se firma con la llave debug para poder instalarlo
            // sin keystore propio. Para la Play Store habría que crear uno.
            signingConfig = signingConfigs.getByName("debug")
            // Solo teléfonos reales (ARM): saca las librerías x86 de OpenCV y ML Kit del APK.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
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
