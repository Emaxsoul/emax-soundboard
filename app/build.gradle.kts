plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "it.emax.soundboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.emax.soundboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Firma esplicita anche v1 per migliorare la compatibilità
    // con i programmi di installazione di alcuni dispositivi Xiaomi/MIUI.
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.0")
}
