plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        compileSdk { version = release(37) }
        namespace = "com.aurum"
        minSdk = 26
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm("desktop")

    applyDefaultHierarchyTemplate()

    sourceSets {
        // ── Common ─────────────────────────────────────────────────
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(libs.compose.material3.windowSizeClass)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.compose.ui:ui-tooling-preview")

            // Coil 3 (KMP)
            implementation(libs.coil.compose)
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")

            // Navigation
            implementation(libs.compose.navigation)

            // Lifecycle + ViewModel
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // ── Android ────────────────────────────────────────────────
        androidMain.apply {
            dependencies {
                api(compose.preview)
                api(libs.compose.ui.tooling)
                api(libs.compose.ui.tooling.preview)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.exoplayer.core)
                implementation(libs.exoplayer.datasource)
                implementation(libs.exoplayer.session)
                implementation(libs.exoplayer.ui)
            }
        }

        androidUnitTest.apply {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // ── iOS ────────────────────────────────────────────────────
        iosMain.apply {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }

        iosTest.apply {}

        // ── Desktop ────────────────────────────────────────────────
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.sqlite)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
                implementation("javazoom:jlayer:1.0.1")
                implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
                implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")
                implementation("org.jflac:jflac-codec:1.5.2")
                implementation("org.slf4j:slf4j-nop:2.0.16")
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.aurum"
}

compose.desktop {
    application {
        mainClass = "com.aurum.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "Aurum"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    databases {
        create("AurumDatabase") {
            packageName.set("com.aurum.database")
        }
    }
}
