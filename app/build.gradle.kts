plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.jamescronin.zeno64"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.jamescronin.zeno64"
        minSdk = 28 // the TV runs Android 9 (API 28)
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        getByName("release") {
            // The fast build for the TV: optimised and shrunk. It's a personal sideload, so it's signed with the
            // local debug key, which lets it update the debug build already on the TV in place.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // the real org.json, since Android's is a stub in unit tests
}
