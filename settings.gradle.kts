pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "9.5.0"
        id("com.android.library") version "9.5.0"
        id("org.jetbrains.kotlin.android") version "2.4.21"
        id("org.jetbrains.kotlin.multiplatform") version "2.4.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
        id("org.jetbrains.compose") version "1.12.1"
        id("app.cash.sqldelight") version "2.4.1"
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Aurum"
include(":app")
include(":shared")
