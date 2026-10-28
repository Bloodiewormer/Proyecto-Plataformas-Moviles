// Proyecto generado con el mismo esquema que Spring Initializr (start.spring.io):
// Gradle con Kotlin DSL, lenguaje Kotlin, la última versión estable de Spring Boot y
// empaquetado Jar. Las dependencias base son las que se ven en clase (Web, Data JPA,
// driver de PostgreSQL y el módulo Kotlin de Jackson); el resto las usa el código de
// seguridad (JWT) que ya traía el proyecto.
plugins {
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.4.10"
    // kotlin-spring: abre (open) las clases anotadas con @Component, @Service,
    // @Configuration, etc., porque Spring necesita poder heredarlas para crear proxies.
    kotlin("plugin.spring") version "2.4.10"
    // kotlin-jpa: genera el constructor sin argumentos que JPA/Hibernate exige en
    // cada @Entity.
    kotlin("plugin.jpa") version "2.4.10"
}

group = "cr.ac.una.glifo"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Las del curso ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // Spring Boot 4 serializa los JSON con Jackson 3 (paquete tools.jackson), así que el
    // módulo Kotlin tiene que ser el de Jackson 3 para que pueda construir data classes.
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- Seguridad (JWT) y validación de requests ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- Validación determinista de LaTeX (issue #39) ---
    // Parser de JLaTeXMath: compila la fórmula que devuelve N1.5 sin red ni API keys.
    // 1.0.7 es la última versión publicada en Maven Central; solo arrastra sus fuentes
    // griega y cirílica (jlatexmath-font-greek/cyrillic), sin otras bibliotecas.
    implementation("org.scilab.forge:jlatexmath:1.0.7")

    // --- Notificaciones push reales con Firebase Cloud Messaging (issue #48) ---
    implementation("com.google.firebase:firebase-admin:9.4.3")

    // Sin Flyway: igual que en clase, el esquema lo genera Hibernate a partir de las
    // entidades (spring.jpa.hibernate.ddl-auto) y import.sql carga los datos (D-27).

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Hibernate necesita poder heredar las entidades para cargar relaciones LAZY (crea un
// proxy como subclase). En Kotlin las clases son final por defecto, así que se abren.
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Spring Boot corre headless en el servidor; las pruebas también, para que JLaTeXMath
    // (AWT) se pruebe igual que en producción y no dependa de tener pantalla.
    systemProperty("java.awt.headless", "true")
}
