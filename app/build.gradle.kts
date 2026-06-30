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
compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
} 

dependencies {
    // Базовая поддержка Compose
    dependencies {
    implementation("com.google.ai.client.generativeai:generativeai:0.4.0")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
}
    
    // Поддержка Activity для Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Core и прочие необходимые библиотеки
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.4.0")
}
