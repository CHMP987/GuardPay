plugins {
    kotlin("jvm") version "2.2.20"
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.soneso.stellar:stellar-sdk:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

application {
    mainClass.set("SpikeCKt")
}
