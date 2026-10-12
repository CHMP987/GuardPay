import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

android {
    namespace = "com.guardpay.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.guardpay.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // LiteRT-LM's native libraries are loaded from disk, as Spike B ran them.
    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Same reason as app/shared: LiteRT-LM (P7) brings kotlin-stdlib 2.4.0.
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    implementation(project(":app:shared"))
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Checks Keystore signatures with an independent ed25519 implementation.
    androidTestImplementation(libs.stellar.sdk)
}

// LiteRT-LM 0.17.1 (P7) asks for kotlin-stdlib/reflect 2.4.0. The 2.2.20 compiler crashes
// inlining the 2.4 stdlib, so keep the catalog's Kotlin. Its coroutines 1.11.0 stays:
// LiteRT-LM calls a 1.11 method and crashes on 1.10.2 (seen on the phone, Oct 12).
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && (requested.name.startsWith("kotlin-stdlib") || requested.name == "kotlin-reflect")) {
            useVersion(libs.versions.kotlin.get())
        }
    }
}
