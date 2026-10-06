# AGENTS.md — Persona 1 (contratos)

GuardPay es una billetera en Stellar Testnet: pago inmediato a contactos de confianza bajo un tope; el resto queda retenido y un guardián puede detenerlo sin mover fondos. La regla la aplica el contrato, no la app. Este archivo es solo para la Persona 1: Spike A, contratos, deploy en testnet y arreglos que P9 pida. Tesis: "La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás."

Autoridad: `docs/WBS.md` (alcance) → `docs/GuardPay — Plan de construcción.md` → `docs/GuardPay — Security spike y propuesta web definitiva.md` (contratos y GH-01…GH-33) → `docs/WBS-AGENTES.md` (la tarea). El pitch está obsoleto: no implementes TrustedPayee, Spending Limit de OZ ni Channels.

## Stack

- Rust + soroban-sdk 28.x, target wasm fijado en `docs/DEPENDENCIES.md` (no asumas `wasm32-unknown-unknown` si G2 registró otro).
- OpenZeppelin `stellar-contracts` commit `b40c5ea`, vendorizado en `vendor/`. No uses v0.7.2. Nunca `latest` ni rangos.
- `stellar-cli` contra **Testnet**. Friendbot para comisiones. Sin mainnet.

## Carpetas de esta persona

```text
spikes/guardian-hold/     P1-A, desechable; no se copia a producción
contracts/account/        una regla Default; sin execute, upgrade ni admin
contracts/guardian_hold/  install (una vez) + enforce
contracts/hold_registry/  queue, cancel, mark_executed, lecturas
vendor/stellar-contracts/
scripts/deploy-testnet.*  scripts/read-rules.*
docs/INTERFACES.md        firmas congeladas en Sync 1
evidence/security/spike-a.md
evidence/policies/        tests.md, whitelist-review.md
evidence/stellar/deployment.md
evidence/smart-account/rules-onchain.md
```

No toques `app/`, `spikes/ai-bench/`, `spikes/kmp-signing/`, `scripts/attacks/` ni el pitch.

## Comandos

Aún no hay workspace. Cuando exista:

```text
cargo test --workspace
cargo test -p guardian_hold gh_01
cargo build --target <target de DEPENDENCIES.md>
```

Deploy y lectura: `scripts/deploy-testnet.ps1` (Windows) y `scripts/read-rules.ps1`. No hay lint de producto. No inventes Gradle ni CI.

## Convenciones

- Tests y evidencias con el ID `GH-xx` (`GH-07b`, `GH-18b`, `GH-20b` son casos aparte).
- `ready_at` lo calcula el registro; no es parámetro. Storage persistente.
- `enforce`: primero `account.require_auth()`, luego la dueña en `authenticated_signers`, luego la lista blanca. Comparación tipada campo a campo. `to` que no convierte a `Address` se rechaza.
- El guardián no es firmante. `guardian_hold` no llama al token. El registro no mueve tokens.
- Pánico en `enforce` revierte la tx: no "registrar y rechazar" en la misma llamada.
- Evidencia: `[FACT]` / `[INFERENCE]` / `[RECOMMENDATION]` / `[UNVERIFIED]`. Datos: REAL / SIMULATED / ILLUSTRATIVE. Test no corrido = `no corrido`.
- UI y docs de producto: nunca "seguro" ni "protegido".

## Testing

- Spike A (T-019…T-028): A0 digest OZ, A1 reentrada/recursos, luego GH-01, 04, 05, 06, 09, 13, 20–23, 24, 25, 26. Rojo en A0, A1, GH-01, 13, 24, 25 o 26 = PARA.
- P2 MUST: GH-01…GH-27 (con 07b, 18b, 20b), GH-31, GH-32, más overflow i128 y lectura tras avanzar ledgers. Cada rama de `enforce` tiene un accept y un reject.
- P3: repetir en testnet GH-02…GH-10 y GH-16, un hash REAL cada uno.
- SHOULD (GH-28, 29, 30, cancel de la dueña) solo si `docs/ESTADO-FASES.md` lo autoriza.
- Salida literal en `evidence/`. No inventes hashes.

## Límites

- Una `T-xxx` por sesión. No abras la siguiente.
- No instales toolchain ni dependencias sin decirlo y sin registrarlo en `docs/DEPENDENCIES.md`.
- No abras T-034 (P2) si CP-1 no está GO. No implementes el fallback 7.A sin aprobación humana.
- No copies `multisig-smart-account`. No añadas `execute`, `upgrade`, `add_*`, `remove_*` ni una segunda regla, ni "solo para tests".
- No cambies firmas en `docs/INTERFACES.md` después del Sync 1 sin OK humano.
- No commitees `.env`, semillas, `*.key`, `*.seed`, `*.jks`.
- Si un MUST solo pasa añadiendo un bypass, paras y lo reportas.

## Flujo

1. Lee la `T-xxx` en `docs/WBS-AGENTES.md` (Origen, archivos, fuera de alcance, verificación).
2. Impleméntala. Máximo ~5 archivos.
3. Ejecuta la verificación de esa tarea.
4. Marca la fila del checklist (`hecha` / `bloqueada` / `omitida`) y el estado en `docs/ESTADO-FASES.md` si la fase cambió.
5. Commit solo de esos archivos: `T-xxx: descripción`.

## Cursor Cloud specific instructions

La imagen ya trae el toolchain. No lo reinstales ni lo anotes otra vez en `docs/DEPENDENCIES.md`.

- JDK 17: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`. `java` y `javac` apuntan ahí.
- Rust 1.92.0 con el target `wasm32v1-none`. `stellar` 28.1.0 está en `/usr/local/bin`.
- Android SDK 35: `ANDROID_HOME=/opt/android-sdk`. Si falta `local.properties` (está en `.gitignore`), escribe `sdk.dir=/opt/android-sdk`. `./gradlew` en git no es ejecutable: `chmod +x ./gradlew` antes de usarlo.
- No hay servidor ni base de datos. Nada tiene que quedar en marcha.
- Contratos, sin red: `cargo test --workspace`.
- App, sin red: `./gradlew :app:shared:jvmTest` (pantallas a 360 dp en `evidence/demo/screens/`) y `./gradlew :app:android:assembleDebug`. `allTests` es el chequeo completo. Los `*LiveTest*` siguen fuera salvo `-PliveTestnet`.
- iOS no se enlaza: no hay Mac (G3).
- Emulador: AVD `guardpay_api_35` (API 35, google_apis, x86_64). Arranca con `-accel off -no-window -gpu swiftshader_indirect`. No uses KVM: en esta VM `kvm_arch_vcpu_create` dispara un kernel BUG. El primer arranque tarda varios minutos. Sin `testnet.json` el APK de debug abre en simulación. No corras Gradle y el emulador a la vez: no caben en la RAM.
