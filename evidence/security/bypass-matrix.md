# Matriz de bypass — P9

6 oct 2026. Etiqueta **REAL** salvo donde se dice otra cosa. `[FACT]` sobre lo que Horizon devolvió ese día.

Ningún ataque de esta tabla movió el SAC de prueba fuera de la lista blanca. Eso cubre las rutas de abajo. No cubre lo que figura como no corrido.

## Dos cuentas, dos wasm

Los hashes firmados de esta matriz salen de un deploy nuevo. Las semillas de `evidence/stellar/deployment.md` no estaban en el clon, y la cuenta de la app (`evidence/demo/rehearsal.md`) firma con el Keystore del emulador. No se mezclan.

| | Cuenta de ataques (esta matriz) | Cuenta publicada (`deployment.md`) | Cuenta de la app (`rehearsal.md`) |
| --- | --- | --- | --- |
| account | `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP` | `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` | `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` |
| guardian_hold | `CAZZQTSQUQ5Z2C4FIRG7HUV53S5MNTDUFMFWUH4MS6A5TQ5AJ2ELYEQ2` | ver `deployment.md` | `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI` |
| registry | `CDCABEI4EPTDARZMMLKRLWP3WJDCF2O7U6GNBRXWA3UW3I2CPZFTJZWM` | ver `deployment.md` | `CC5NG54ICWDH7N46RMESSAMXTFAFEL243ZIJC5RNFZ3FFHQ6CHY5NRY2` |
| token | `CAYYSVXHPQWELMONBJCFYKQORJS5IJGTEW7VBJGB7W5OV7QMNRLKSXOX` | SAC de prueba, emisor = dueña | SAC de prueba, emisor = dueña |

El token de las tres cuentas es un SAC de prueba emitido por la dueña. No es el USDC de Circle.

`[FACT]` El wasm de la cuenta de ataques es `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c` (rustc 1.99, `stellar contract build --optimize=false`). El wasm publicado en el ledger de `deployment.md` sigue siendo `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b`. Misma fuente del repo; el binario no es el mismo. La lectura de la cuenta publicada está en `evidence/smart-account/rules-onchain.md`.

Dueña de los ataques: `GDGJQ3RULSDNWDMXT55R6MB6B57SG7KHW3RET5VNWJFI6P5Y4GQ5YRFQ`. Guardián: `GAU7JGHXGN3RE4TXW3AYNDMQFLIJCJ4EGHTAW42B2MOT5A572UWH6PFN`. No es firmante.

## Ataques

Cada fila: precondición, acción, esperado, real, hash, código, propiedad. Los hashes están en Horizon. `no hay tx` significa que no hubo transacción incluida.

| Ataque | Precondición | Acción | Esperado | Real | Hash | Código | Propiedad |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pago ya detenido | Hold 0 encolado y cancelado por el guardián | La dueña firma el `transfer` de ese hold | Rechazo | `successful=false`, ledger 5059693, 2026-10-06T21:27:32Z. El hold sigue Stopped | `5776967c045f1751262668c41c22ef7c1613c12329242f040d1e3e836d642eac` | 3 NotAllowed | P2 |
| Guardián transfiere hacia sí | El guardián no está en la regla | `transfer` desde la cuenta, payload Delegated del guardián | Rechazo | `successful=false`, ledger 5059688, 2026-10-06T21:27:07Z | `6c445cd95ca7b98e1ba76aab3406c77965835923c25983d39b3cb073511ebdf1` | 3016 UnauthorizedSigner | P3 |
| Destino de un hold maduro | Hold 1 Retained, 19 unidades, destino el atacante, `ready_at` ya pasó | `transfer` del mismo monto hacia otro destino | Rechazo | `successful=false`, ledger 5059903, 2026-10-06T21:45:02Z. `find_retained` devolvió vacío. El hold 1 sigue Retained | `ba888c47aba69344cdcff79c985f32ab32682fa41d77cc88819b6b1baa86e04e` | 3 NotAllowed | P1 |
| Monto de un hold maduro | Hold 2 Retained, 21 unidades, ya maduro | `transfer` al mismo destino por 20 unidades | Rechazo | `successful=false`, ledger 5059904, 2026-10-06T21:45:07Z. El hold 2 sigue Retained | `a08bc09f0b9e3b54d87782f7d4e84fcc1333da518f2eac98ff4e61b8a3512df6` | 3 NotAllowed | P1 |
| `approve` | La dueña firma | `approve` del SAC de prueba | Rechazo | `successful=false`, ledger 5059853, 2026-10-06T21:40:52Z. `enforce` vio `fn_name: approve` | `e1afaf76d12717f933a9a4b3d13ee025505e78d9fb79b2a20b248352747b1361` | 3 NotAllowed | P4 |
| `transfer_from` | `approve` no dejó allowance | `transfer_from` con spender = atacante | Rechazo | `successful=false`, ledger 5059854, 2026-10-06T21:40:57Z. Falla en el SAC: auth `invalid_action` del spender. No llega a `enforce` | `c856524e520afffd9882a598ffa396e54f4dd4c79d15e708208218336a3a06ca` | sin código de contrato; auth del SAC | P4 |
| Segunda context rule | La cuenta no exporta `add_context_rule` | La CLI invoca `add_context_rule` | Rechazo de la CLI | `unrecognized subcommand 'add_context_rule'` | no hay tx | — | P5 |
| `execute` | El wasm no exporta `execute` | La CLI invoca `execute` | Rechazo de la CLI | `unrecognized subcommand 'execute'` | no hay tx | — | P5 |
| `upgrade` | El wasm no exporta `upgrade` | La CLI invoca `upgrade` | Rechazo de la CLI | `unrecognized subcommand 'upgrade'` | no hay tx | — | P5 |
| `enforce` directo | Un tercero llama la política | `guardian_hold.enforce` con `--send=yes` | Rechazo | La simulación (auth de la CLI, no del ledger) termina en contrato 3. No hubo transacción incluida | no hay tx | 3 en simulación | P4 |
| `queue` sin la dueña | El atacante no firma por la cuenta | `queue` con auth none | Rechazo | `successful=false`, ledger 5059690, 2026-10-06T21:27:17Z. Auth `invalid_action`; `__check_auth` llegó a `UnreachableCodeReached`. No se creó el hold | `62bd41b060d11234903c3516c2439b9b6832fe67a31f3c57bef97832dce7145c` | sin código de `GuardianHold` | P4 |
| Contrato intermedio | `nested.pay` llama al SAC con `from` = la cuenta. Contrato `CBC4LAAGZUDW3A43X2R63QR6S7H6LJEWKXVLHRAISFA3NBWU64BSSQ2X`, no es de GuardPay | La dueña autoriza `pay` y el `transfer` interno (dos contextos, regla 0) | Rechazo | `successful=false`, ledger 5059932, 2026-10-06T21:47:27Z. `enforce` rechazó el contexto `pay` | `8424d8d2cfee001c8c9438e036ea83ec385900d89f7e478ae8be98cab49cc7fe` | 3 NotAllowed | P4 |
| Replay | Hold 3 pasó a Executed en `f310ba3561f83dd3f0af2e7177bfca47f49d474d57e43b08d1d5cd50a3c022ec` | La dueña firma otra vez el mismo `transfer` | Rechazo | `successful=false`, ledger 5059933, 2026-10-06T21:47:32Z. `find_retained` devolvió vacío | `f71a344c8d542cb4e9212423182523ef04b8fb5ade1e064097101dde48f5bc61` | 3 NotAllowed | P2 |
| Destino muxed | El id no convierte a `Address` | `transfer` con destino muxed | Rechazo | `successful=false`, ledger 5059694, 2026-10-06T21:27:37Z | `bdf9bc034409bf39be19a884e36a876b329bcf0cb37f39d7a0ed7194e11d6f50` | 8 InvalidDestination | P1 |
| Envío inmediato a un extraño | No hay hold para ese destino | `transfer` de 7 unidades al extraño | Rechazo | `successful=false`, ledger 5059687, 2026-10-06T21:27:02Z | `e344af8576918c00916505fc1fcb342e648ee65ac9239a39dfa1c6f3e7ed208a` | 3 NotAllowed | P1 |
| Tope 30+20+1 (GH-28) | Tope diario 50. Gasto del día en 0 | Tres `transfer` a un contacto | 30 y 20 pasan; el 1 falla | 30 pasa ledger 5059671. 20 pasa ledger 5059681. El 1 falla ledger 5059683 | `70897ef04b1af554587e49766f3e82de9f7f412f2617f370b52d3422c2240b84` · `5962c6777a28a1c60cfd24cd7acdc73bb14b2056c53137a4745c07f68feec7d0` · `76d2683a4238e4e817d936f77570b16c562d3909237d58569befb98b9ddd9f09` | 5 CapExceeded en el tercero | P4 |

Cola del pago detenido: `065654755b5f27c08d4f83e2e6130e7e6656af67ad267d36b21a62d448034329`. Cancel del guardián: `8b39ea26414037c633f4ce5c995dda354f333553da53cc5eb442fdabf6280a8b`. Deploy del contrato intermedio: subida `60bb528a932e7bc4d50d8e30d34bea5d41d66dfbc8d55f7694f6390bded58665`, instancia `e8ee3c689ccf1570615f7f48a36ad53b44c9919737cc367e3e6665be0c6533f9`.

## Rama de `enforce`

Contra `evidence/policies/whitelist-review.md`.

| Ataque | Dónde cae |
| --- | --- |
| Pago detenido, destino distinto, monto distinto, replay, envío a un extraño, transfer a la dueña G | Carril `transfer`: no hay hold Retained con esos campos. NotAllowed (3) |
| Muxed | Carril `transfer`: `to` no convierte a Address. InvalidDestination (8) |
| Tope 30+20+1 | Carril `transfer` a contacto. El tercero cae en CapExceeded (5) |
| Guardián | Antes de la lista: el firmante no está en la regla. En esta corrida el código es 3016 (UnauthorizedSigner de la cuenta), no el 2 de la política |
| `approve`, `burn`, `pay` del contrato intermedio | Catch-all. NotAllowed (3) |
| `transfer_from` | No entra a `enforce`. El SAC rechaza al spender |
| `queue` sin la dueña | La cuenta no autoriza. No es una rama de la lista blanca |
| `enforce` directo | Simulación: la lista blanca (args vacíos → NotAllowed). En el ledger de la cuenta publicada, GH-26 es el test nativo. Esta corrida no dejó hash |
| `execute`, `upgrade`, segunda regla | No hay función. No hay rama |
| Segundo `install` | Citado de `rules-onchain.md`: reentrada, sin hash. GH-32 nativo es verde |

## GH-01…GH-33

Solo tres estados. Lo verde en tests nativos o en `deployment.md` se cita. No se volvió a correr para conseguir otro hash, salvo el ataque de la demo.

| ID | Estado | Dónde |
| --- | --- | --- |
| GH-01 | verde | `evidence/policies/tests.md` |
| GH-02 | verde | `deployment.md` `90fac9d8292ec3c9b22750700159ec31bda505703e6eba17823932bd9dee0701` |
| GH-03 | verde | `deployment.md` `a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492` |
| GH-04 | verde | `deployment.md` `8361eff0c06a1a52bcf264ada0de6c41d0bbfa63e440891d1fef08d24e7b94b6` |
| GH-05 | verde | `deployment.md` `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed` |
| GH-06 | verde | `deployment.md` `35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573` |
| GH-07 | verde | `deployment.md` `09838694770afb1d17d60f3a65f3ee94e92c2b804c4337672f5f99c2bef9e8e8` |
| GH-07b | verde | `tests.md` |
| GH-08 | verde | `deployment.md` `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`. La demo de esta matriz repite el caso en `5776967c…` |
| GH-09 | verde | `deployment.md` `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`. La demo repite el caso en `6c445cd9…` |
| GH-10 | verde | `deployment.md` `89e02fa36233c70729e10adab4ce682f53265e931bc21845b16d5deb2418040d` |
| GH-11 | verde | `tests.md` |
| GH-12 | verde | `tests.md` |
| GH-13 | verde | `tests.md` y `rules-onchain.md` (una regla) |
| GH-14 | verde | `tests.md` |
| GH-15 | verde | `tests.md`. En esta cuenta, `transfer_from` falló en el SAC: `c856524e…` |
| GH-16 | verde | `deployment.md` `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434`. Repetido en `e1afaf76…` |
| GH-17 | verde | `tests.md`. En esta cuenta, `burn` es `901b04dd164ce328b887b4dc34aa0238678b3d8f0793f1ac596fdab645721a4a`, código 3 |
| GH-18 | verde | `tests.md`. En esta cuenta, `8424d8d2…`, código 3 |
| GH-18b | verde | `tests.md` |
| GH-19 | verde | `tests.md`. En esta cuenta, `f71a344c…`, código 3 |
| GH-20 | verde | `tests.md`. En esta cuenta, `ba888c47…`, código 3 |
| GH-20b | verde | `tests.md`. En esta cuenta, `bdf9bc03…`, código 8 |
| GH-21 | verde | `tests.md`. En esta cuenta, `a08bc09f…`, código 3 |
| GH-22 | verde | `tests.md` |
| GH-23 | verde | `tests.md` |
| GH-24 | verde | `tests.md` |
| GH-25 | verde | `tests.md` y el rechazo de la CLI (`execute`, `upgrade`, `add_context_rule`: no hay tx) |
| GH-26 | verde | `tests.md`. La llamada directa de esta corrida no dejó hash |
| GH-27 | verde | `tests.md`. En esta cuenta, `62bd41b0…` |
| GH-28 | verde | Esta matriz. Tres hashes de arriba |
| GH-29 | no corrido | Ver abajo |
| GH-30 | no corrido | No hay `expires_at`. T-049 no está autorizado |
| GH-31 | verde | `tests.md` |
| GH-32 | verde | `tests.md`. El segundo `install` en testnet no tiene hash: `rules-onchain.md` |
| GH-33 | no corrido | No hay firmante passkey |

### GH-29

`[FACT]` En la cuenta `CCPPHXNVQBQEXI5LXJ4YJLPJLMOV7XDORYVQVMECJAECXQC72Y6DOFQ2`, un `transfer` de 30 pasó (`ad8c4142b357bd53d335694c382fa6ad9b42041d7ee8b3aa14fee7e96c838c5c`, ledger 5059703, 2026-10-06T21:28:22Z) y el siguiente de 30 falló con CapExceeded (`6797ea0963487267da0491586d2c9dc634ba023cf4cfda7ee2e04c16395317f3`, ledger 5059704, 2026-10-06T21:28:27Z). Son ledgers consecutivos, no el mismo. GH-29 pide el mismo ledger. Queda no corrido. El primer intento del script comparó dos lecturas vacías y escribió "mismo ledger"; esa fila no se usa.

## Lectura de la cadena

`[FACT]` La cuenta publicada, leída sin firmar, tiene una regla Default, firmante la dueña, política `guardian_hold`. El guardián no es firmante. El spec de la cuenta no exporta `execute` ni `upgrade`. El registro no tiene función de token. Detalle: `evidence/smart-account/rules-onchain.md`.

Los intentos que no están en la lista, y los que fallaron por el footprint antes de repetirse, están en `attacker-session.md`.
