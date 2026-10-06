// Throwaway spike (P1-C). Standalone build, deliberately outside the app's
// Gradle project. Run from the repo root: ./gradlew -p spikes/kmp-signing run
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kmp-signing-spike"
