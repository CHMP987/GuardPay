# Entrega

6 oct 2026. La fecha de entrega es el 12 oct 2026, 17:59, Passport. G1 ya está contestada. La inscripción y los campos del formulario que nadie ha abierto siguen **sin contestar**. No se inventan.

CP-3, CP-7 y CP-9 no están firmados aquí.

## Piezas

| Pieza | Dónde |
| --- | --- |
| Repo | este repositorio. Rama de trabajo de P9/P10: la que abra el PR |
| README | `README.md` |
| Tests de contratos | `cargo test -p account -p guardian_hold -p hold_registry`. Salida citada en `evidence/policies/tests.md` |
| Tests del cliente | `./gradlew :app:shared:allTests` |
| Evidencia | `evidence/` |
| Direcciones de testnet, ensayos CLI | `evidence/stellar/deployment.md` |
| Cuenta de la app (Mamá, Diego) | `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` en `evidence/demo/rehearsal.md` |
| Cuenta de los ataques firmados de P9 | `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP` en `evidence/security/bypass-matrix.md` |
| Vídeo de la app | `evidence/demo/rehearsal/` |
| Vídeo de rechazos de terminal | `evidence/demo/video.md` |
| Texto de entrega | la sección de abajo |
| Entrevistas | `evidence/demo/interviews.md` — no corrido |

El token de las tres cuentas es un SAC de prueba emitido por la dueña. No es el USDC de Circle.

## Fases incompletas

P0 sigue incompleta. P7 no se abrió. Spike B está rojo y la IA queda en NICE. iOS no se corrió. GH-29, GH-30 y GH-33 están no corrido. Las seis escenas no se cronometraron en una sola toma.

## Describe your project

GuardPay is a Stellar Testnet wallet for someone who may be pressured into paying a stranger. Payments to a trusted contact, under a daily cap, go out at once. Anything else is held for 120 seconds, a constant in the registry, not a 600-second expiry. During that wait a guardian chosen by the owner can stop the payment. The guardian cannot move the funds and is not a signer on the account. The contract rejects the rest.

The asset in this demo is a test SAC issued by the owner. It is not Circle USDC.

There are three Soroban contracts and a Kotlin Multiplatform app. There is no backend. The guardian learns about a hold by polling from the phone. If the app is closed, that alert can be late. If the guardian does not look, nothing is stopped.

The account has one Default context rule: the owner is the delegated signer and the policy is `guardian_hold`. The account wasm does not export `execute`, `upgrade`, or a function that adds a second rule. `hold_registry` queues, lets the guardian cancel, and marks a hold executed only when the policy asks. It does not move tokens.

This is testnet. The contracts are not audited. The guardian cannot be changed. iOS was not linked or run. On-device AI is not in the build: Spike B had no phone and no measurements, so that work stays out. The Android and iOS stubs return analysis unavailable. The payment lane does not read a model.

The pattern is already in use elsewhere. Yandex Pay has asked a trusted contact to confirm a payment. OpenZeppelin's TimelockController gives a role that can cancel an operation identified by its parameters. Argent and Ready put a smart account, recovery, and spending rules on a phone. Monzo lets a trusted contact review a large transfer. GuardPay puts the stop in the contract, and the person who can stop is not an owner.

The same shape could be built on EVM with Safe, a Delay modifier, and a guard. On Stellar it is built with less custom code, on top of the OpenZeppelin account pinned at `b40c5eaefe6a29f0030f00bd2d730b7a91cce330` (a pre-release), `soroban-sdk` 28.0.0, and target `wasm32v1-none`.

The pitch draft in `docs/GuardPay — Propuesta para el pitch.md` is older. It still mentions a separate TrustedPayee contract, an OpenZeppelin spending-limit policy, and Stellar channels. Those are not in this repo. This section replaces that technical description. The original file is left as history.

## Campos de Passport

| Campo | Estado |
| --- | --- |
| Fecha límite | 12 oct 2026, 17:59. Contestada el 5 oct |
| Inscripción | sin contestar |
| El resto de campos del formulario | sin contestar |
