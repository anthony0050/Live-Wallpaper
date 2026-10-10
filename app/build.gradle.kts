plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.anthony.redmiwallpaper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anthony.redmiwallpaper"
        minSdk = 29
        targetSdk = 35
        versionCode = 8
        versionName = "0.1.7"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
