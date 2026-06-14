plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ai.sarj.agentsim"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.sarj.agentsim"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            // MediaPipe GenAI ships native .so libs; only ship arm64 to keep the APK lean.
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "agentsim"
            keyAlias = "agentsim"
            keyPassword = "agentsim"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false   // keep R8 off — MediaPipe/Compose reflection stays intact
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Pinned to the API-36 line: AGP 8.13's max compileSdk is 36, so newer (API-37/AGP-9.1)
    // library versions are intentionally avoided. compose-bom 2025.11.01 = compose 1.9.5 / material3 1.4.0.
    val composeBom = platform("androidx.compose:compose-bom:2025.11.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // On-device LLM (Gemma 3 1B via MediaPipe LLM Inference). AGP-agnostic; ships arm64 native libs.
    implementation("com.google.mediapipe:tasks-genai:0.10.27")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
