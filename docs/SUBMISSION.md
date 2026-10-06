# Entrega

Fecha: 12 oct 2026, 17:59, Passport. La fecha está contestada. Esta sesión no envió el formulario.

## Qué se entrega

| Pieza | Dónde | Estado |
| --- | --- | --- |
| Repo | este repositorio | el remoto lo confirma el humano al publicar |
| README | `README.md` | escrito el 6 oct 2026 |
| Tests de contratos | `cargo test -p account -p guardian_hold -p hold_registry` | verdes el 6 oct, citados en `evidence/policies/tests.md` |
| Tests de la app | `./gradlew :app:shared:allTests` | no se volvieron a correr en esta sesión de P9 |
| Evidencia | `evidence/` | REAL / SIMULATED / ILLUSTRATIVE. Huecos en `no corrido`. |
| Direcciones de testnet | `evidence/stellar/deployment.md` y `evidence/demo/rehearsal.md` | dos cuentas. No mezclar hashes. |
| Vídeo de la app | `evidence/demo/rehearsal/` | escenas 1, 2, 3 y 6. El PIN sale en negro. |
| Vídeo de rechazos | `evidence/demo/video.md` y `evidence/security/p9-cli-run.log` | el log tiene el hash visible. El mp4, si se grabó, va al lado. |
| Texto de abajo | este archivo | listo para pegar. Los enlaces de vídeo público no se inventan. |
| Entrevistas | `evidence/interviews.md` | no corrido |

## Campos de Passport

La inscripción y los campos del formulario que nadie ha abierto siguen **sin contestar**. No se inventan. No se envió la entrega.

## Describe your project

GuardPay is a Stellar Testnet wallet. Payments to trusted contacts under a daily cap go through immediately. Any other payment is held for 120 seconds. During that wait, a guardian chosen by the owner can stop it. The guardian cannot move the money and is not an account signer. The contract enforces the rule.

The on-device model is not in this build. It warns only when it exists. The owner signs. The guardian can stop a payment. The contract rejects the other cases we tested.

There are three Soroban contracts and a Kotlin Multiplatform app. There is no backend: the guardian's phone polls the network and raises a local alert. The account has one Default rule, the owner is the signer, and the policy is GuardianHold. It has no execute, no upgrade, and no admin entry points. The hold registry computes ready_at and does not move tokens. The demo asset is a test SAC issued by the owner. It is not Circle's USDC.

The same shape could be built on an EVM with Safe, a Delay modifier, and a guard. On Stellar it is built with less code of our own, because the account comes from OpenZeppelin stellar-contracts at commit b40c5eaefe6a29f0030f00bd2d730b7a91cce330, a pre-release, on soroban-sdk 28.0.0.

Related work we name on purpose: Yandex Pay, OpenZeppelin TimelockController, Argent/Ready, and Monzo.

Testnet only. The contracts are not audited. The guardian cannot be changed. If the guardian does not look, nobody stops the payment. The alert can be late when the app is closed. iOS was not linked or run. The AI benchmark has no numbers from a phone, so the AI is optional and the message step was not part of the demo. We tested the bypass routes listed in evidence/security/bypass-matrix.md. Some failed on the ledger with a hash. Some were not run.

## Hashes que un jurado puede abrir ya

Cuenta CLI, SAC de prueba `CBUSK46Y…`:

- Pago detenido: `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`
- Guardián intenta gastar: `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`
- approve: `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434`
- Antes de la hora: `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed`

Cuenta del emulador `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`, contacto Mamá, guardián Diego. Hashes en `evidence/demo/rehearsal.md`.
