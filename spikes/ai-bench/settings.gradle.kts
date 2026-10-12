// Throwaway spike (P1-B). Standalone Android app, deliberately outside the MVP's
// Gradle project. Run from the repo root: ./gradlew -p spikes/ai-bench assembleDebug
pluginManagement {
    repositories {
        google()
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

rootProject.name = "ai-bench-spike"
