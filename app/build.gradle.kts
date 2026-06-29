plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.aiphotoeditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aiphotoeditor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Вот эти две строчки - всё, что нужно для ИИ
    implementation("com.google.ai.client.generativeai:generativeai:0.4.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
}
