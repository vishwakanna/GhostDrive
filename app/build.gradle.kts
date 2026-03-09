// App-level build.gradle.kts
// This file is inside app/ folder (Module: app)
// This is where ALL your dependencies go

plugins {
    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace   = "com.example.ghostdrive"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.example.ghostdrive"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core:1.13.1")

    // Compose BOM — controls all Compose versions, never add version to compose libs below
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Material Icons — no version, BOM handles it
    implementation("androidx.compose.material:material-icons-extended:1.6.7")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Retrofit + Gson + OkHttp (networking)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ExoPlayer / Media3 (video streaming)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")
}