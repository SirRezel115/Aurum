plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.aurum.aurum"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aurum.aurum"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // Compose
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.graphics)
    debugImplementation(libs.compose.ui.tooling)

    // Coil
    implementation(libs.coil.compose)
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // ExoPlayer (Media3)
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.datasource)
    implementation(libs.exoplayer.ui)
    implementation(libs.exoplayer.session)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
}
