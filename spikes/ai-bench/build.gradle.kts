import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kotlin 2.4.0 rather than the MVP's 2.2.20: litertlm-android 0.17.1 is built
// against kotlin-reflect 2.4.0, whose metadata a 2.2 compiler cannot read.
plugins {
    id("com.android.application") version "8.12.3"
    id("org.jetbrains.kotlin.android") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
}

android {
    namespace = "com.guardpay.aibench"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.guardpay.aibench"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Official LiteRT-LM Kotlin API (B2: >= 0.12.0, no community wrapper). Pinned.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
