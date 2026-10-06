# Dependencias

5 oct 2026. Una fila por pin. La palabra de rango flotante no se usa. `[FACT]` es salida de un comando de esta máquina. Lo demás es el pin que ya fija el plan de construcción.

## Toolchain medido

| Pieza | Versión | Etiqueta | Salida |
| --- | --- | --- | --- |
| rustc | 1.92.0 (ded5c06cf 2025-12-08) | `[FACT]` | `rustc --version` |
| cargo | 1.92.0 (344c4567c 2025-10-21) | `[FACT]` | `cargo --version` |
| Target wasm | `wasm32v1-none` | `[FACT]` | `rustup target list --installed` también lista `x86_64-pc-windows-msvc`. `wasm32-unknown-unknown` no está instalado. |
| stellar-cli | 28.1.0 (c0f4d0da891bbf214c08b8c5035ae6db80e9a3bd) | `[FACT]` | Binario en `%USERPROFILE%\.local\bin\stellar.exe`, release `v28.1.0`. `stellar-xdr` 28.0.0. |
| Java | 23.0.1 (2024-10-15) | `[FACT]` | `java -version`. `JAVA_HOME` vacío. |
| Android SDK | no instalado en esta sesión | `[FACT]` | `ANDROID_HOME` vacío. Bloquea la app Android, no el Spike A. |

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

## Pines del plan, no reinstalados aquí

| Dependencia | Pin | Papel | Nota |
| --- | --- | --- | --- |
| OpenZeppelin `stellar-contracts` | commit `b40c5ea` | Cuentas. Pre-release. No es v0.7.2. | Compilación wasm comprobada arriba. Se acepta con el spike. Estable publicado v0.7.2 no se usa. |
| soroban-sdk | 28.0.0 | Contratos | Resuelto por el lock del pin. |
| `com.soneso.stellar:stellar-sdk` | 1.14.0 | Cliente KMP. Comunidad: no hay SDK KMP oficial. | No instalado en esta comprobación. |
| Kotlin | 2.x, el que fije `libs.versions.toml` cuando exista | App | No instalado aquí. |
| Compose Multiplatform | 1.8.0 o superior, versión exacta en el toml | UI | No instalado aquí. |
| LiteRT-LM | 0.12.0 o superior, versión exacta cuando se fije | IA on-device | No instalado aquí. |
| Gemma 4 E2B | el artefacto que fije Spike B | Modelo | No descargado aquí. |
