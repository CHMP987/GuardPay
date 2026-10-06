import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
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

    // Test-only target: renders the Compose UI headless (Skia, no device) for the
    // UI tests and the screenshots in evidence/demo/screens/. There is no desktop app.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.stellar.sdk)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

compose.resources {
    packageOfResClass = "com.guardpay.shared.ui.res"
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
    // DeviceProvisioningLiveTest: the phone's public G addresses (from its keys.json).
    listOf("gp.owner", "gp.guardian").forEach { k -> findProperty(k)?.let { systemProperty(k, it) } }
    systemProperty("gp.out", layout.buildDirectory.file("testnet.json").get().asFile.absolutePath)
}

// The 360 dp UI test saves each screen here (SIMULATED evidence); without the property it only asserts.
tasks.named<Test>("jvmTest") {
    systemProperty("gp.screens", rootProject.file("evidence/demo/screens").absolutePath)
    outputs.dir(rootProject.file("evidence/demo/screens"))
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
            // Compose Multiplatform keeps the androidx.compose namespace on every platform.
            Regex("""^\s*import\s+androidx\.(?!compose\.)"""),
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

// UI vocabulary test: no string a person can read says "seguro", "protegido" or
// "Verificado". Scans string literals (comments stripped) in the shared UI and the
// Android app, plus Android string resources.
val uiTextSources = fileTree("src/commonMain/kotlin/com/guardpay/shared/ui") { include("**/*.kt") } +
    fileTree(rootProject.file("app/android/src")) { include("**/*.kt", "**/res/**/strings*.xml") }
val checkUiVocabulary by tasks.registering {
    group = "verification"
    description = "Fails if a UI string says seguro, protegido or verificado."
    inputs.files(uiTextSources)
    doLast {
        val banned = Regex("""\b(segur[oa]s?|protegid[oa]s?|verificad[oa]s?)\b""", RegexOption.IGNORE_CASE)
        // A triple-quoted string, or a one-line string with escapes.
        val literal = Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(?:[^\"\\\\\\n]|\\\\.)*\"")
        val violations = uiTextSources.files.flatMap { file ->
            val text = file.readText()
            val strings = if (file.extension == "xml") {
                Regex("""<string[^>]*>([\s\S]*?)</string>""").findAll(text).map { it.groupValues[1] }.toList()
            } else {
                val code = text.replace(Regex("""/\*[\s\S]*?\*/"""), "").replace(Regex("""(?m)^\s*//.*$"""), "")
                literal.findAll(code).map { it.value }.toList()
            }
            strings.filter { banned.containsMatchIn(it) }.map { "${file.relativeTo(rootDir).invariantSeparatorsPath}: $it" }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("UI strings must not say seguro, protegido or verificado:\n" + violations.joinToString("\n"))
        }
    }
}

tasks.named("check") { dependsOn(checkUiVocabulary) }
tasks.matching { it.name == "allTests" }.configureEach { dependsOn(checkUiVocabulary) }

// Guardian surface test: the guardian's screens can read holds and stop one, and
// nothing else. They never see the gateway, a signer, a transfer or a queue.
val guardianUiSources = fileTree("src/commonMain/kotlin/com/guardpay/shared/ui/guardian") { include("**/*.kt") }
val checkGuardianSurface by tasks.registering {
    group = "verification"
    description = "Fails if ui/guardian reaches the gateway, a signer, a transfer or a queue."
    inputs.files(guardianUiSources)
    doLast {
        val forbidden = listOf(
            Regex("""\bStellarGateway\b"""),
            Regex("""\bSigner\b"""),
            Regex("""\bsubmitTransfer\b"""),
            Regex("""\bsubmitQueue\b"""),
            Regex("""com\.guardpay\.shared\.signing"""),
        )
        val violations = guardianUiSources.files.flatMap { file ->
            val code = file.readText().replace(Regex("""/\*[\s\S]*?\*/"""), "").replace(Regex("""//.*"""), "")
            forbidden.filter { it.containsMatchIn(code) }.map { "${file.relativeTo(projectDir).invariantSeparatorsPath}: ${it.pattern}" }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("The guardian UI reaches beyond read + stop:\n" + violations.joinToString("\n"))
        }
    }
}

tasks.named("check") { dependsOn(checkGuardianSurface) }
tasks.matching { it.name == "allTests" }.configureEach { dependsOn(checkGuardianSurface) }
