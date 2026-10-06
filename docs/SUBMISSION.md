# Entrega

Fecha: 12 oct 2026, 17:59, Passport. La fecha está contestada. Esta sesión no envió el formulario.

## Qué se entrega

| Pieza | Dónde | Estado |
| --- | --- | --- |
| Repo | este repositorio | el remoto lo confirma el humano al publicar |
| README | `README.md` | escrito el 6 oct 2026, encima de `main` |
| Tests de contratos | `cargo test -p account -p guardian_hold -p hold_registry` | verdes el 6 oct, citados en `evidence/policies/tests.md`. No se volvieron a correr en esta reconciliación |
| Tests de la app | `./gradlew :app:shared:allTests` | no se volvieron a correr en esta reconciliación |
| Evidencia | `evidence/` | REAL / SIMULATED / ILLUSTRATIVE. Huecos en no corrido |
| Tres cuentas | `evidence/security/bypass-matrix.md` | un hash no se copia de una columna a otra |
| Vídeo de la app | `evidence/demo/rehearsal/` | escenas 1, 2, 3 y 6. Cuenta del emulador. El PIN sale en negro |
| Vídeo de rechazos | `evidence/demo/terminal-rejections.mp4` | cuenta de ataques, wasm `06226b37…`. No se grabó otro |
| Texto de abajo | este archivo | listo para pegar en el campo que ya se conoce. Los demás campos no se rellenan |
| Entrevistas | `evidence/interviews.md` | no corrido |

## Campos de Passport

La inscripción y los campos del formulario que nadie ha abierto siguen **sin contestar**. No se inventan. No se envió la entrega.

CP-3, CP-7 y CP-9 no están firmados.

## Fases que siguen incompletas

P0 está incompleta. P7 no se abrió. Spike B está rojo y la IA queda en NICE. Las entrevistas están no corrido. Passport no se envió.

Las seis escenas seguidas, en una sola sentada y por debajo de 5 minutos, no se cronometraron.

## Describe your project

GuardPay is a Stellar Testnet wallet. Payments to trusted contacts under a daily cap go through immediately. Any other payment is held for 120 seconds. During that wait, a guardian chosen by the owner can stop it. The guardian cannot move the money and is not an account signer. The contract enforces the rule.

The on-device model is not in this build. Spike B has no numbers from a phone, so the AI stays optional and P7 was not opened. The owner signs. The guardian can stop a payment. The contract rejects the cases that have a ledger hash. The rest was not run. That list is not a claim that no other route exists.

There are three Soroban contracts and a Kotlin Multiplatform app. There is no backend: the guardian's phone polls the network and raises a local alert. The account has one Default rule, the owner is the signer, and the policy is GuardianHold. It has no execute, no upgrade, and no admin entry points. The hold registry computes ready_at and does not move tokens. The demo asset is a test SAC issued by the owner. It is not Circle's USDC. The SAC admin is the owner: mint and clawback do not go through the policy, and they were not executed.

The same shape could be built on an EVM with Safe, a Delay modifier, and a guard. On Stellar it is built with less code of our own, because the account comes from OpenZeppelin stellar-contracts at commit b40c5eaefe6a29f0030f00bd2d730b7a91cce330, a pre-release, on soroban-sdk 28.0.0.

Related work we name on purpose: Yandex Pay, OpenZeppelin TimelockController, Argent/Ready, and Monzo.

Testnet only. The contracts are not audited. The guardian cannot be changed. If the guardian does not look, nobody stops the payment. The alert can be late when the app is closed. iOS was not linked or run. Interviews were not run.

## Tres cuentas

No mezclar un hash de una columna con otra. El wasm va en la fila.

| | Publicada | Ataques | Emulador |
| --- | --- | --- | --- |
| account | `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` | `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP` | `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` |
| wasm | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` | `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` |

Mañana, cuenta publicada, wasm `986956cc…`:

- Pago detenido: `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`
- Guardián intenta gastar: `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`
- `approve`: `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434`
- Antes de `ready_at`: `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed`

Noche, misma cuenta publicada, wasm `986956cc…`: `enforce` directo `0e46055b18304e54f69a1ec39891bf12fb75ece15e16680ab17393410804678f`. `queue` sin la dueña `20c5b59814acf635c33a92405c7897c398454a31481585b295983630e499b4c7`. `execute`, `upgrade` y una segunda regla: no hay tx. Destino distinto, monto distinto, muxed y contrato intermedio: sin hash propio en esta cuenta.

Noche, cuenta de ataques, wasm `06226b37…`:

- Destino distinto: `ba888c47aba69344cdcff79c985f32ab32682fa41d77cc88819b6b1baa86e04e`
- Monto distinto: `a08bc09f0b9e3b54d87782f7d4e84fcc1333da518f2eac98ff4e61b8a3512df6`
- Muxed: `bdf9bc034409bf39be19a884e36a876b329bcf0cb37f39d7a0ed7194e11d6f50`
- Contrato intermedio: `8424d8d2cfee001c8c9438e036ea83ec385900d89f7e478ae8be98cab49cc7fe`
- GH-28: `70897ef04b1af554587e49766f3e82de9f7f412f2617f370b52d3422c2240b84`, `5962c6777a28a1c60cfd24cd7acdc73bb14b2056c53137a4745c07f68feec7d0`, `76d2683a4238e4e817d936f77570b16c562d3909237d58569befb98b9ddd9f09`

GH-28 queda verde solo para esa cuenta y ese wasm. GH-29, GH-30 y GH-33: no corrido.

Emulador, Mamá y Diego, wasm `986956cc…`: hashes en `evidence/demo/rehearsal.md`. No se firmaron ataques con esa cuenta.
