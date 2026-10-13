# Dependencias

5 oct 2026, puesto al día el 13 oct (F-12). Una fila por pin. La palabra de rango flotante no se usa. `[FACT]` es salida de un comando de esta máquina. Lo demás es el pin que ya fija el plan de construcción.

## Toolchain medido

| Pieza | Versión | Etiqueta | Salida |
| --- | --- | --- | --- |
| rustc | 1.92.0 (ded5c06cf 2025-12-08) | `[FACT]` | `rustc --version` |
| cargo | 1.92.0 (344c4567c 2025-10-21) | `[FACT]` | `cargo --version` |
| Target wasm | `wasm32v1-none` | `[FACT]` | `rustup target list --installed` también lista `x86_64-pc-windows-msvc`. `wasm32-unknown-unknown` no está instalado. |
| stellar-cli | 28.1.0 (c0f4d0da891bbf214c08b8c5035ae6db80e9a3bd) | `[FACT]` | Binario en `%USERPROFILE%\.local\bin\stellar.exe`, release `v28.1.0`. `stellar-xdr` 28.0.0. |
| Java | 23.0.1 (2024-10-15) | `[FACT]` | `java -version`. `JAVA_HOME` vacío. |
| Android SDK | no instalado en esta sesión | `[FACT]` | `ANDROID_HOME` vacío. Bloquea la app Android, no el Spike A. |

### Medición del 13 oct 2026

`[FACT]` Salida de los comandos en la misma máquina de Windows:

| Pieza | Versión | Salida |
| --- | --- | --- |
| rustc | 1.98.0 (88d9e12ae 2026-08-18) | `rustc --version` |
| cargo | 1.98.0 (797e8a9bc 2026-08-05) | `cargo --version` |
| Java | OpenJDK 17.0.20.1 (2026-08-18 LTS) | `java -version`. Es el JDK 17 que pide Gradle; el CI usa `java-version: "17"`. |
| Android SDK | Instalado: plataformas `android-34`, `android-35` y `android-36`; build-tools 35.0.0 y 36.0.0 | `sdk.dir` en `local.properties` (no versionado) |
| Gradle | 8.14.3 | `distributionUrl` de `gradle/wrapper/gradle-wrapper.properties` |
| stellar-cli | No está en el `PATH` de esta sesión ni en `%USERPROFILE%\.local\bin` | `stellar --version` falla. La tabla del 5 oct registra 28.1.0; desde el 12 oct el wasm lo compila el job `wasm` del CI. |

La tabla del 5 oct queda como registro histórico.

`[FACT]` 12 oct 2026: el wasm desplegado en testnet el 6 oct declara `rsver` 1.99.0 en su sección `contractmetav0` (`rustc 1.99.0 (b940084d7 2026-09-28)`), leída de la RPC de testnet. Así que se compiló con 1.99.0, no con el 1.92.0 de la tabla, que es la medición del 5 oct. El job `wasm` del CI compila con 1.99.0; el job `contracts` (`cargo test`) usa 1.98.0.

## G2 — pin OpenZeppelin

`[FACT]` 5 oct 2026. Commit `b40c5eaefe6a29f0030f00bd2d730b7a91cce330` (`b40c5ea`, mensaje `chore: bump soroban-sdk to v28 (#866)`). Clonado en `.scratch/oz-g2/`, que no se versiona.

`packages/accounts` (`stellar-accounts` 0.7.1) declara `soroban-sdk = "28.0.0"`. `Cargo.lock` resuelve `soroban-sdk` 28.0.0 desde crates.io.

Comando:

```text
stellar contract build --manifest-path packages/accounts/Cargo.toml
```

La CLI ejecutó:

```text
cargo rustc --manifest-path=packages\accounts\Cargo.toml --crate-type=cdylib --target=wasm32v1-none --release
```

Resultado: `Finished release profile [optimized] target(s) in 1m 42s`. Exit code 0. Wasm: `stellar_accounts.wasm`, 324 bytes optimizado (335 sin optimizar), hash `88b41306c2d0dbe8181fe7851ac507894305419791521db57c717a5e6e039430`. Funciones exportadas: ninguna. El paquete es librería (`crate-type = ["lib", "cdylib"]`), no el contrato de la cuenta.

**G2 = sí.** El pin compila con soroban-sdk 28, target `wasm32v1-none`, stellar-cli 28.1.0.

El test `do_check_auth_rule_selection_downgrade_fails` está en `packages/accounts/src/smart_account/test/context_rules.rs` línea 1293. `[FACT]` de lectura. No se ejecutó. Eso es T-019.

## Pines actuales

| Dependencia | Pin | Papel | Nota |
| --- | --- | --- | --- |
| OpenZeppelin `stellar-contracts` | commit `b40c5ea` | Cuentas. Pre-release. No es v0.7.2. | Compilación wasm comprobada arriba. Se acepta con el spike. Estable publicado v0.7.2 no se usa. |
| soroban-sdk | 28.0.0 | Contratos | Resuelto por el lock del pin. |
| `com.soneso.stellar:stellar-sdk` | 1.14.0 | Cliente KMP. Comunidad: no hay SDK KMP oficial. | `gradle/libs.versions.toml` |
| Kotlin | 2.2.20 | App | `libs.versions.toml`. Igual al que usa stellar-sdk 1.14.0. `spikes/ai-bench/` usa 2.4.0, fuera del build del MVP. |
| Android Gradle Plugin | 8.12.3 | App | `libs.versions.toml` |
| Compose Multiplatform | 1.9.1 | UI | `libs.versions.toml` |
| kotlinx-coroutines | 1.10.2 | App | `libs.versions.toml`. LiteRT-LM necesita 1.11.0 y falla con 1.10.2, así que los módulos de Android no fijan coroutines (ver `CLAUDE.md`). |
| kotlinx-serialization | 1.9.0 | App | `libs.versions.toml` |
| kotlinx-datetime | 0.7.1 | UI | `libs.versions.toml` |
| LiteRT-LM | 0.17.1 | IA en el teléfono | `libs.versions.toml`, la versión que midió Spike B (`evidence/ai/benchmark.md`) |
| Gemma 4 E2B | `gemma-4-E2B-it.litertlm`, commit de Hugging Face `b3ca0d2f…eae1`, 2 588 147 712 bytes, SHA-256 `18193810…a63c` | Modelo | `GemmaModelFile.kt`. Se descarga en la app; nunca va en el APK ni en git. |
| Android SDK | compileSdk 35, targetSdk 35, minSdk 28 | App | `libs.versions.toml` |
| Rust en el CI | 1.98.0 (`cargo test`) y 1.99.0 (job `wasm`) | Contratos | `.github/workflows/ci.yml` |
