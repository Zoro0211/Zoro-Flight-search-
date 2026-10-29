plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.skytrackfree"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.skytrackfree"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
