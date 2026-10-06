# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

`docs/` holds the Spanish-language research, design and planning. The only code so far is the **Gradle KMP skeleton** (day 1, Persona 2): root `settings.gradle.kts`, `gradle/libs.versions.toml` (exact versions, aligned with `stellar-sdk` 1.14.0's own build), `app/shared` (P4 shared core: `domain` with the 9 payment states, transitions and lane prediction; `ai` with the `GuardPayAI` interface, `AnalysisParser` and a stub per platform; `signing` interface; `stellar` with `StellarGateway`, `FakeStellarGateway` (commonTest only) and the day-3 `KmpStellarGateway` skeleton over stellar-sdk: SCVal decoders, an auth-entry guard that signs only the exact call the app built, and a send/poll classifier. Contract function names live in `ProvisionalAbi.kt` and addresses in `GuardPayNetwork`, both provisional until INTERFACES.md / Sync 2; and the day-4 P5 `ui` in Compose Multiplatform, driven by `OwnerSession`/`GuardianSession`, with the guardian screens limited to the `GuardianChain` read + cancel surface) and `app/android` (`src/debug` wires the UI to an in-memory `SimulatedStellarGateway`; `src/release` has null sessions until the testnet contract addresses exist). There are no contracts and no CI yet; when they land, record their real commands here.

## Commands

Gradle 8.14.3 via the wrapper, JDK 17, Android SDK path in `local.properties` (gitignored; use forward slashes: `sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk`).

- `./gradlew :app:shared:allTests`: shared tests (Android unit tests plus the test-only `jvm()` target; on Windows the iOS klibs compile but are never linked or run, since there is no Mac). It also runs all four checks below.
- `./gradlew :app:shared:jvmTest`: JVM tests, including `ScreensAt360Test`, which renders the Compose UI headless at 360 dp and writes PNGs to `evidence/demo/screens/` (through the `gp.screens` system property).
- `./gradlew :app:shared:testDebugUnitTest --tests "com.guardpay.shared.domain.LaneTest"`: run a single test class.
- `./gradlew :app:shared:checkCommonMainArchitecture`: fails if `src/commonMain` imports `android.*`, `androidx.*`, `platform.*`, LiteRT-LM or WebAuthn/passkey APIs.
- `./gradlew :app:shared:checkDependencyGraph`: fails if anything in `ai/` references `signing`/`stellar`, if a production file mentions `GuardPayAI` together with `Signer`/`StellarGateway`, or if a `Fake*` type is used outside tests. Also run by `allTests`.
- `./gradlew :app:shared:testDebugUnitTest -PliveTestnet --tests "*LiveTest"`: opt-in tests that hit Stellar **Testnet** and FriendBot (keys generated per run, never printed). Without `-PliveTestnet` every `*LiveTest*` is excluded, so `allTests` stays offline.
- `./gradlew :app:shared:checkUiVocabulary`: fails if a string literal in `ui/` or `app/android/src` (or an Android `strings*.xml`) says seguro/protegido/verificado as a whole word, ignoring case. Comments are ignored.
- `./gradlew :app:shared:checkGuardianSurface`: fails if `ui/guardian/**` mentions `StellarGateway`, `Signer`, `submitTransfer`, `submitQueue` or the `signing` package.
- `./gradlew :app:android:assembleDebug`: debug APK in `app/android/build/outputs/apk/debug/`. `./gradlew :app:android:compileReleaseKotlin` checks the release wiring.

Planned, not set up yet: Rust + `soroban-sdk` 28 targeting `wasm32-unknown-unknown` (`cargo test` for contracts) and `stellar-cli` against **Testnet only**.

## Document authority

The docs contradict each other. When they disagree, this is the order:

1. `docs/GuardPay — Plan de construcción.md`: the current build plan (architecture, phases P0–P10, risks, spikes, kill test).
2. `docs/GuardPay — Prompts de ejecución.md`: one self-contained prompt per phase. Run them in dependency order, and never open P2 until Spike A (P1-A) is green.
3. `docs/GuardPay — Security spike y propuesta web definitiva.md` (Oct 5): authoritative for the **contract design** and the adversarial test suite GH-01…GH-33. Its "web app" platform conclusion is superseded by the plan (KMP), but its contract architecture is adopted unchanged.
4. `docs/GuardPay — Propuesta visual.md`: authoritative for UI (palette, contrast, the 9 payment states, the 4 screens). Do not redesign or add screens.
5. `docs/GuardPay — MVP Proposal.md` (Oct 4): scope and acceptance criteria where not overridden above.
6. `docs/GuardPay — Propuesta para el pitch.md`: **technically obsolete**. `TrustedPayee` as a separate contract, the OZ Spending Limit policy and Stellar Channels were all removed. Do not implement anything from it. Its pitch text is still usable.
7. `docs/GuardPay — Validación Costa Rica y Chile.md`: market evidence only.

## Product thesis (do not change)

AI-assisted payment safety + human guardian veto + deterministic on-chain enforcement. "La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás." The guardian can stop money but never move it.

## Architecture (planned)

**On-chain (Stellar Testnet, Soroban), three contracts:**
- `account`: built from OpenZeppelin smart-account pieces, with **exactly one `Default` context rule** (signer = owner, policy = GuardianHold). It must have **no `execute`, no `upgrade` and no admin functions** (`add_context_rule`, `add_signer`, `add_policy`, `remove_*`). Do not copy OZ's `multisig-smart-account` example: its extra `Default` rule, `execute` and `upgrade` are the three bypasses the security spike found.
- `guardian_hold` (policy): `install` once, plus `enforce`. `enforce` first calls `account.require_auth()` and checks that the owner key is in `authenticated_signers`. It then applies a **strict allowlist**: USDC `transfer` to a trusted contact under the daily cap, or USDC `transfer` matching a matured held record; `HoldRegistry.queue`; `HoldRegistry.cancel`. Everything else is rejected, including calls to the account itself, `approve`, `transfer_from`, other tokens and nested invocations. It never calls a token.
- `hold_registry`: `queue` / `cancel` (guardian only) / `mark_executed` (only callable by guardian_hold). `ready_at` is **computed by the registry, never an input**. Binding is typed field-by-field `(account, token, destination, amount)`. Use persistent storage, not temporary. It has no function that moves tokens.
- There are two contracts (policy + registry) because Soroban forbids re-entrancy. Whether a policy may call the registry inside `__check_auth` within resource limits is **unverified** (risk R6). It is the first thing Spike A tests.
- OZ `stellar-contracts` is pinned to commit `b40c5ea`, a pre-release (stable is v0.7.2). The design relies on `context_rule_ids` being bound into the signed digest.

**Client (Kotlin Multiplatform + Compose Multiplatform):**
- `commonMain` holds all logic and must compile with no Android, iOS, LiteRT-LM or WebAuthn imports. Layers: `domain` (9 payment states as sealed types), `stellar` (`StellarGateway` interface over `com.soneso.stellar:stellar-sdk:1.14.0`), `signing` (`expect Signer`) and `ai` (`expect GuardPayAI`).
- `GuardPayAI` has **one** function, `analyzeMessage(text): Analysis`, whose suggestion is `SuggestHold | NoClearSignals`. It has no tool calling and no access to balances, contacts, policy or keys. It must never draft a payment or fill in the destination or amount. Nothing in `ai/` may depend on, or share a graph with, `Signer` or `StellarGateway`; a test enforces this.
- Android is the demo device. The iOS target is declared but only compiles if a Mac is available.
- **There is no backend**: no server, DB, push (FCM/APNs) or fee sponsor. The guardian is notified by on-device RPC polling plus a local notification.

## Hard constraints

- Testnet only, never mainnet. Pin exact dependency versions and never use `latest`.
- The guardian must never become a signer of the account (that would turn it into a multisig and kill the thesis).
- UI strings must never use "seguro" or "protegido". UI shows "Retenido" only when an on-chain record exists.
- Evidence goes in `evidence/`, labeled REAL / SIMULATED / ILLUSTRATIVE. Never fabricate tx hashes. Record unrun tests as "no corrido".
- Label claims in docs `[FACT]` / `[INFERENCE]` / `[RECOMMENDATION]` / `[UNVERIFIED]`. No claims of "first", "unique" or "no bypass exists".
- Done means implementation + unit + integration + security tests + acceptance criteria + evidence. "Compiles" is not done.

## Open blocking gates (before P1)

- G1: is the submission deadline Oct 12 (Passport) or Oct 5 (Luma)?
- G2: does the OZ pin `b40c5ea` build with soroban-sdk 28?
- G3: is a Mac available for iOS? **Answered Oct 5: no.** iOS klibs compile on Windows, but nothing is linked or run on iOS; say so in the README.
