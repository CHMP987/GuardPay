import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Declared but not compiled: nobody on the team has a Mac (gate G3).
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.stellar.sdk)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.guardpay.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Live testnet tests hit the network and FriendBot: opt-in with -PliveTestnet.
val liveTestnet = project.hasProperty("liveTestnet")
tasks.withType<Test>().configureEach {
    inputs.property("liveTestnet", liveTestnet)
    if (!liveTestnet) exclude("**/*LiveTest*")
}

// Architecture test: commonMain must compile without Android, iOS, LiteRT-LM or
// WebAuthn. Fails the build if any of them is imported there.
val commonMainSources = fileTree("src/commonMain") { include("**/*.kt") }
val checkCommonMainArchitecture by tasks.registering {
    group = "verification"
    description = "Fails if commonMain imports a platform, LiteRT-LM or WebAuthn API."
    inputs.files(commonMainSources)
    doLast {
        val forbidden = listOf(
            Regex("""^\s*import\s+android\."""),
            Regex("""^\s*import\s+androidx\."""),
            Regex("""^\s*import\s+platform\."""),
            Regex("""^\s*import\s+.*litert""", RegexOption.IGNORE_CASE),
            Regex("""^\s*import\s+com\.google\.ai\.edge\."""),
            Regex("""^\s*import\s+.*webauthn""", RegexOption.IGNORE_CASE),
            Regex("""^\s*import\s+.*passkey""", RegexOption.IGNORE_CASE),
        )
        val violations = commonMainSources.files.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> forbidden.any { it.containsMatchIn(line) } }
                .map { (i, line) -> "${file.relativeTo(projectDir)}:${i + 1}: ${line.trim()}" }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "commonMain must not depend on platform, LiteRT-LM or WebAuthn APIs:\n" +
                    violations.joinToString("\n")
            )
        }
    }
}

tasks.named("check") { dependsOn(checkCommonMainArchitecture) }
tasks.matching { it.name == "allTests" }.configureEach { dependsOn(checkCommonMainArchitecture) }

// Dependency-graph test: the AI layer never shares a graph with signing or the
// chain, and the in-memory contract model never ships. Comments are stripped
// first so KDoc that names a type does not count as a dependency.
val productionSources = fileTree("src") {
    include("**/*.kt")
    exclude("*Test/**")
}
val checkDependencyGraph by tasks.registering {
    group = "verification"
    description = "Fails if ai/ reaches Signer/StellarGateway, or a fake ships in production."
    inputs.files(productionSources)
    doLast {
        fun code(file: File) = file.readText()
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .replace(Regex("""//[^\n]*"""), "")
        val signingOrChain = listOf(
            Regex("""com\.guardpay\.shared\.signing"""),
            Regex("""com\.guardpay\.shared\.stellar"""),
            Regex("""\bSigner\b"""),
            Regex("""\bStellarGateway\b"""),
        )
        val violations = mutableListOf<String>()
        productionSources.files.forEach { file ->
            val path = file.relativeTo(projectDir).invariantSeparatorsPath
            val src = code(file)
            val usesSigningOrChain = signingOrChain.any { it.containsMatchIn(src) }
            if ("/com/guardpay/shared/ai/" in path && usesSigningOrChain) {
                violations += "$path: ai/ references signing or stellar"
            }
            if (Regex("""\bGuardPayAI\b""").containsMatchIn(src) && usesSigningOrChain) {
                violations += "$path: GuardPayAI shares a file with Signer/StellarGateway"
            }
            if (Regex("""\bFake[A-Z]\w*""").containsMatchIn(src)) {
                violations += "$path: a Fake* type is referenced from production code"
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Dependency graph violations:\n" + violations.joinToString("\n"))
        }
    }
}

tasks.named("check") { dependsOn(checkDependencyGraph) }
tasks.matching { it.name == "allTests" }.configureEach { dependsOn(checkDependencyGraph) }
