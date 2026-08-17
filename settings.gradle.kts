plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "Proyecto-Plataformas-Moviles"

includeBuild("android")
includeBuild("backend")
