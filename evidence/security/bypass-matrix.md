# Matriz de bypass — P9

6 oct 2026. Etiqueta **REAL** donde hay un hash de Horizon o una lectura del ledger. `[FACT]` sobre lo que esas lecturas devolvieron. Un rechazo sin transacción incluida se escribe `no hay tx`. Lo que ninguna corrida envió sigue **no corrido**.

Esta tabla cubre las filas que tienen hash. No afirma que no exista bypass. CP-9 no está firmado.

El activo de las tres cuentas es un SAC de prueba emitido por la dueña de esa cuenta. No es el USDC de Circle. El admin del SAC es la dueña: `mint` y `clawback` no pasan por la política. No se ejecutaron con esa clave.

Ninguna fila de abajo atribuye un hash de `CC6FFXGG…` a `CDBJMSUI…` ni a la cuenta del emulador. El wasm va en la columna, con el hash del binario.

## Tres cuentas

| | Publicada (`deployment.md`) | Ataques (deploy del 6 oct, noche) | Emulador (`rehearsal.md`) |
| --- | --- | --- | --- |
| account | `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` | `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP` | `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` |
| wasm de `account` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` | `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` |
| Cómo se compiló el wasm | `stellar contract build`, coincide con `account.wasm` de `deployment.md` | rustc 1.99, `stellar contract build --optimize=false`. Misma fuente del repo. El binario no es el de la columna publicada | Misma lectura que la cuenta publicada. `evidence/security/chain-reread.md` |
| guardian_hold | `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI` | `CAZZQTSQUQ5Z2C4FIRG7HUV53S5MNTDUFMFWUH4MS6A5TQ5AJ2ELYEQ2` | `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI` |
| registry | `CBEDM2DZYJ7VEU434J7D44VVQ6ADW4IAG5UHE74PY72KI5GNRTMRU36S` | `CDCABEI4EPTDARZMMLKRLWP3WJDCF2O7U6GNBRXWA3UW3I2CPZFTJZWM` | `CC5NG54ICWDH7N46RMESSAMXTFAFEL243ZIJC5RNFZ3FFHQ6CHY5NRY2` |
| SAC de prueba | `CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R` | `CAYYSVXHPQWELMONBJCFYKQORJS5IJGTEW7VBJGB7W5OV7QMNRLKSXOX` | `CC2K3NDVHI52YKCDORJVNIZYRYNO2TE6MN3LIK2LWDRIZMJDBEG3VDBH` |
| Dueña | `GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y` | `GDGJQ3RULSDNWDMXT55R6MB6B57SG7KHW3RET5VNWJFI6P5Y4GQ5YRFQ` | `GDIFOBI5NW4N5DHBBYMRLJ766X4EWN6VQABCRUJ6SRMJ3SW2AI56U5UD` |
| Guardián | `GA7G4HPJFYRWD6SI5DW7YYXYNAXZD7JXD3Z4L5GFJIMVBSNBNPBF7JZ3`. No es firmante | `GAU7JGHXGN3RE4TXW3AYNDMQFLIJCJ4EGHTAW42B2MOT5A572UWH6PFN`. No es firmante | Diego `GCCKC6JFAA3J2TWUOF32S4TERWZFGSF7NDHTDJ7FBZK5KWHV5VGJE4ZI`. No es firmante |
| Para qué se usa aquí | Ensayos ★ de la mañana y los dos hashes de la noche que sí se incluyeron en esta cuenta | Los ataques que la cuenta publicada dejó no corrido, más las repeticiones de esa noche | Escenas de la app. No se firmaron ataques: la semilla del Keystore no está en el repo |

La política `CCGIEMIU…` es el mismo contrato en la cuenta publicada y en la del emulador. Las cuentas, los registros y los SAC no lo son. Un hash de una no se copia a la otra.

Los `.ps1`, `run.mjs` e `invoke.mjs` leen `scripts/.testnet/state.json` y son los que enviaron las transacciones de `CC6FFXGG…`. Los `.sh` junto a `common.sh` se corrieron contra `CDBJMSUI…` (`evidence/security/p9-cli-run.log`). Sin ese `state.json` citan el hash de la mañana o imprimen `no hay tx`. No imprimen semillas.

## Ataques

Cada celda nombra el wasm de esa columna. Verde solo donde esa cuenta y ese wasm tienen hash de ledger, o donde la CLI rechazó el subcomando y no hubo transacción.

| Ataque | Publicada · wasm `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` | Ataques · wasm `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c` | Emulador · wasm `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` |
| --- | --- | --- | --- |
| Pago ya detenido (GH-08) | Mañana. `successful=false`, ledger 5050733, 2026-10-06T09:00:52Z. Hash `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`. Código 3. Citado de `deployment.md`. No reenviado de noche | Noche. `successful=false`, ledger 5059693, 2026-10-06T21:27:32Z. Hash `5776967c045f1751262668c41c22ef7c1613c12329242f040d1e3e836d642eac`. Código 3. El hold siguió Stopped. Cola `065654755b5f27c08d4f83e2e6130e7e6656af67ad267d36b21a62d448034329`. Cancel `8b39ea26414037c633f4ce5c995dda354f333553da53cc5eb442fdabf6280a8b` | no corrido |
| Guardián transfiere hacia sí (GH-09) | Mañana. `successful=false`, ledger 5050731, 2026-10-06T09:00:42Z. Hash `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`. Código 3016 | Noche. `successful=false`, ledger 5059688, 2026-10-06T21:27:07Z. Hash `6c445cd95ca7b98e1ba76aab3406c77965835923c25983d39b3cb073511ebdf1`. Código 3016 | no corrido |
| Destino distinto de un hold maduro (GH-20) | no corrido. El ★ GH-03 (`a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492`, error 3) es un destino desconocido sin ese hold. No es esta fila | Noche. `successful=false`, ledger 5059903, 2026-10-06T21:45:02Z. Hash `ba888c47aba69344cdcff79c985f32ab32682fa41d77cc88819b6b1baa86e04e`. Código 3. Hold 1 siguió Retained | no corrido |
| Monto distinto de un hold maduro (GH-21) | no corrido. El ★ GH-05 (`0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed`, error 7) es el mismo monto antes de `ready_at`. No es esta fila | Noche. `successful=false`, ledger 5059904, 2026-10-06T21:45:07Z. Hash `a08bc09f0b9e3b54d87782f7d4e84fcc1333da518f2eac98ff4e61b8a3512df6`. Código 3, el que devolvió el ledger. Hold 2 (21 unidades) siguió Retained. El `transfer` era de 20 | no corrido |
| Destino muxed (GH-20b) | no hay tx. La CLI dijo que el alias `MAAA…` no existe. No armó transacción | Noche. `successful=false`, ledger 5059694, 2026-10-06T21:27:37Z. Hash `bdf9bc034409bf39be19a884e36a876b329bcf0cb37f39d7a0ed7194e11d6f50`. Código 8 InvalidDestination | no corrido |
| `approve` (GH-16) | Mañana. `successful=false`, ledger 5050729, 2026-10-06T09:00:32Z. Hash `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434`. Código 3 | Noche. `successful=false`, ledger 5059853, 2026-10-06T21:40:52Z. Hash `e1afaf76d12717f933a9a4b3d13ee025505e78d9fb79b2a20b248352747b1361`. Código 3. `enforce` vio `fn_name: approve` | no corrido |
| `transfer_from` (GH-15) | no hay tx. Simulación: error 9 del SAC, allowance 0. No se envió | Noche. `successful=false`, ledger 5059854, 2026-10-06T21:40:57Z. Hash `c856524e520afffd9882a598ffa396e54f4dd4c79d15e708208218336a3a06ca`. Auth `invalid_action` del SAC. No llegó a `enforce` | no corrido |
| Segunda context rule | no hay tx. `unrecognized subcommand 'add_context_rule'`. Lectura: `Count` = 1, sin regla 1 (`chain-reread.md`) | no hay tx. `unrecognized subcommand 'add_context_rule'` | no hay tx de ataque. Lectura: `Count` = 1, sin regla 1 (`chain-reread.md`) |
| `execute` | no hay tx. `unrecognized subcommand 'execute'` | no hay tx. `unrecognized subcommand 'execute'` | no corrido |
| `upgrade` | no hay tx. `unrecognized subcommand 'upgrade'` | no hay tx. `unrecognized subcommand 'upgrade'` | no corrido |
| `enforce` directo (GH-26) | Noche. `successful=false`, ledger 5060459, 2026-10-06T22:31:22Z. Hash `0e46055b18304e54f69a1ec39891bf12fb75ece15e16680ab17393410804678f`. Auth `invalid_action`. La simulación en modo record había aceptado la lista blanca. `spent_today` siguió en `100000000`. Fee payer `GC7SNM4SEWEZTPDNXSVUWLTDXVOB7DGKKRQ3OTBTQ75OETUULQZFSFKZ` (Friendbot). Semilla fuera del repo | no hay tx. La simulación terminó en contrato 3. `--send=yes` no dejó hash | no corrido |
| `queue` sin la dueña (GH-27) | Noche. `successful=false`, ledger 5060405, 2026-10-06T22:26:52Z. Hash `20c5b59814acf635c33a92405c7897c398454a31481585b295983630e499b4c7`. Auth `invalid_action`. Hold 2 no se creó (`HoldNotFound`) | Noche. `successful=false`, ledger 5059690, 2026-10-06T21:27:17Z. Hash `62bd41b060d11234903c3516c2439b9b6832fe67a31f3c57bef97832dce7145c`. Auth `invalid_action`. `__check_auth` llegó a `UnreachableCodeReached`. No se creó el hold | no corrido |
| Contrato intermedio (GH-18) | no corrido. No se desplegó un contrato intermedio contra esta cuenta | Noche. `successful=false`, ledger 5059932, 2026-10-06T21:47:27Z. Hash `8424d8d2cfee001c8c9438e036ea83ec385900d89f7e478ae8be98cab49cc7fe`. Código 3 sobre el contexto `pay`. Contrato `CBC4LAAGZUDW3A43X2R63QR6S7H6LJEWKXVLHRAISFA3NBWU64BSSQ2X`, no es de GuardPay. Subida `60bb528a932e7bc4d50d8e30d34bea5d41d66dfbc8d55f7694f6390bded58665`. Instancia `e8ee3c689ccf1570615f7f48a36ad53b44c9919737cc367e3e6665be0c6533f9` | no corrido |
| Replay de un transfer ya ejecutado (GH-19) | Reenvío del sobre de GH-06: `TxBadSeq`. No hay tx nueva. El hash que imprime la CLI es el original `35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573`. Firmar otra vez el mismo `transfer`: no corrido | Noche. `successful=false`, ledger 5059933, 2026-10-06T21:47:32Z. Hash `f71a344c8d542cb4e9212423182523ef04b8fb5ade1e064097101dde48f5bc61`. Código 3. El hold 3 había pasado a Executed en `f310ba3561f83dd3f0af2e7177bfca47f49d474d57e43b08d1d5cd50a3c022ec` | no corrido |
| Envío inmediato a un extraño | no corrido como este caso. GH-05 es otra fila | Noche. `successful=false`, ledger 5059687, 2026-10-06T21:27:02Z. Hash `e344af8576918c00916505fc1fcb342e648ee65ac9239a39dfa1c6f3e7ed208a`. Código 3. 7 unidades, sin hold | no corrido |
| Tope 30+20+1 (GH-28) | no corrido | Noche. 30 pasa, ledger 5059671, hash `70897ef04b1af554587e49766f3e82de9f7f412f2617f370b52d3422c2240b84`. 20 pasa, ledger 5059681, hash `5962c6777a28a1c60cfd24cd7acdc73bb14b2056c53137a4745c07f68feec7d0`. El 1 falla, ledger 5059683, hash `76d2683a4238e4e817d936f77570b16c562d3909237d58569befb98b9ddd9f09`, código 5 CapExceeded | no corrido |
| Dos transferencias de 30 en el mismo ledger (GH-29) | no corrido | no corrido | no corrido |
| `expires_at` a 600 s (GH-30) | no corrido. No se implementó. T-049 no está autorizado | no corrido. No se implementó | no corrido. No se implementó |
| Passkey (GH-33) | no corrido. No se añadió un firmante | no corrido. No se añadió un firmante | no corrido. No se añadió un firmante |

## GH-01…GH-33

El verde nativo está en `evidence/policies/tests.md`. No es un hash de estas tres cuentas. Una celda verde de aquí es solo el wasm de esa columna.

| ID | Publicada · `986956cc…` | Ataques · `06226b37…` | Emulador · `986956cc…` |
| --- | --- | --- | --- |
| GH-01 | nativo verde. Sin hash nuevo de esta cuenta | nativo verde. Sin hash de esta cuenta | nativo verde. Sin hash de esta cuenta |
| GH-02 | verde. Mañana `90fac9d8292ec3c9b22750700159ec31bda505703e6eba17823932bd9dee0701` | no corrido | no corrido |
| GH-03 | verde. Mañana `a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492` | no corrido | no corrido |
| GH-04 | verde. Mañana `8361eff0c06a1a52bcf264ada0de6c41d0bbfa63e440891d1fef08d24e7b94b6` | no corrido | no corrido |
| GH-05 | verde. Mañana `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed` | no corrido | no corrido |
| GH-06 | verde. Mañana `35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573` | no corrido | no corrido |
| GH-07 | verde. Mañana `09838694770afb1d17d60f3a65f3ee94e92c2b804c4337672f5f99c2bef9e8e8` | no corrido | no corrido |
| GH-07b | nativo verde. Sin hash de esta cuenta | nativo verde. Sin hash de esta cuenta | nativo verde. Sin hash de esta cuenta |
| GH-08 | verde. Mañana `2175e512…` | verde. Noche `5776967c…`. Solo este wasm | no corrido |
| GH-09 | verde. Mañana `ef32c3bd…` | verde. Noche `6c445cd9…`. Solo este wasm | no corrido |
| GH-10 | verde. Mañana `89e02fa36233c70729e10adab4ce682f53265e931bc21845b16d5deb2418040d` | no corrido | no corrido |
| GH-11 | nativo verde | nativo verde | nativo verde |
| GH-12 | nativo verde | nativo verde | nativo verde |
| GH-13 | verde por lectura: una regla. `chain-reread.md` | no hay tx al pedir una segunda regla. Lectura de `Count` en esta cuenta: no corrido | verde por lectura: una regla. `chain-reread.md` |
| GH-14 | nativo verde | nativo verde | nativo verde |
| GH-15 | nativo verde. En esta cuenta: no hay tx | verde en el ledger de esta cuenta: `c856524e…`. Murió en el SAC | no corrido |
| GH-16 | verde. Mañana `25b91a39…` | verde. Noche `e1afaf76…`. Solo este wasm | no corrido |
| GH-17 | nativo verde. El `burn` con firma válida no se envió en esta cuenta | verde. `901b04dd164ce328b887b4dc34aa0238678b3d8f0793f1ac596fdab645721a4a`, código 3. Solo este wasm | no corrido |
| GH-18 | nativo verde. En esta cuenta: no corrido | verde. `8424d8d2…`. Solo este wasm | no corrido |
| GH-18b | nativo verde | nativo verde | nativo verde |
| GH-19 | nativo verde. Reenvío del sobre: no hay tx nueva. Refirma: no corrido | verde. `f71a344c…`. Solo este wasm | no corrido |
| GH-20 | nativo verde. En esta cuenta: no corrido | verde. `ba888c47…`. Solo este wasm | no corrido |
| GH-20b | nativo verde. En esta cuenta: no hay tx | verde. `bdf9bc03…`, código 8. Solo este wasm | no corrido |
| GH-21 | nativo verde. En esta cuenta: no corrido | verde. `a08bc09f…`, código 3. Solo este wasm | no corrido |
| GH-22 | nativo verde | nativo verde | nativo verde |
| GH-23 | nativo verde | nativo verde | nativo verde |
| GH-24 | nativo verde | nativo verde | nativo verde |
| GH-25 | no hay tx (`execute`, `upgrade`, `add_context_rule`) | no hay tx (`execute`, `upgrade`, `add_context_rule`) | no corrido |
| GH-26 | verde. Noche `0e46055b…`. Solo este wasm | no hay tx | no corrido |
| GH-27 | verde. Noche `20c5b598…`. Solo este wasm | verde. `62bd41b0…`. Solo este wasm | no corrido |
| GH-28 | no corrido | verde. Tres hashes de la fila de arriba. Solo este wasm | no corrido |
| GH-29 | no corrido | no corrido | no corrido |
| GH-30 | no corrido | no corrido | no corrido |
| GH-31 | nativo verde | nativo verde | nativo verde |
| GH-32 | nativo verde. El segundo `install` en testnet no tiene hash (`rules-onchain.md`) | no corrido. Solo se pidió `--help` | no corrido |
| GH-33 | no corrido | no corrido | no corrido |

## Lo que no entra en las tres columnas

`[FACT]` En la cuenta `CCPPHXNVQBQEXI5LXJ4YJLPJLMOV7XDORYVQVMECJAECXQC72Y6DOFQ2`, que no es ninguna de las tres, un `transfer` de 30 pasó (`ad8c4142b357bd53d335694c382fa6ad9b42041d7ee8b3aa14fee7e96c838c5c`, ledger 5059703, 2026-10-06T21:28:22Z) y el siguiente de 30 falló con CapExceeded (`6797ea0963487267da0491586d2c9dc634ba023cf4cfda7ee2e04c16395317f3`, ledger 5059704, 2026-10-06T21:28:27Z). La operación nombra ese contrato, no `CC6FFXGG…`. El `source_account` de Horizon es `GDGJQ3RU…`, la dueña del deploy de ataques: eso paga la comisión y no cambia de cuenta el hash. Son ledgers consecutivos. GH-29 pide el mismo ledger. Sigue no corrido. El primer intento de `same-ledger.ps1` comparó dos lecturas vacías y escribió "mismo ledger". Esa fila no se usa.

En `CC6FFXGG…`, tres envíos incluidos fallaron por footprint corto, antes del error de contrato. No se citan como NotAllowed:

| Intento | Hash | Ledger | Evento |
| --- | --- | --- | --- |
| Destino distinto, primer envío | `7ef163b113bd70bac6536fa5759da0975676da50594d84261347dc9ce895cf26` | 5059849, 2026-10-06T21:40:32Z | clave Hold fuera del footprint. El hold 1 siguió Retained |
| Replay, primer reenvío | `b9ac81fc99cf1f688eba934c5fc618f35200a14be601729eb39b987dd404076a` | 5059852, 2026-10-06T21:40:47Z | la misma clave Hold |
| Contrato intermedio, primer `pay` | `61573e21ce8a96db2eae5e874351c9ba9e6de26fd45c059fcb927167418ad1f6` | 5059915, 2026-10-06T21:46:02Z | wasm del contrato intermedio fuera del footprint |

El monto distinto tuvo un envío anterior (`7803cdf560f3a4a41ea4bd41c54de3a0357086b14b58ed67aa569020825cd7c3`, ledger 5059850, 2026-10-06T21:40:37Z) en esa misma ventana. No se usa como NotAllowed. El que tiene el código 3 es `a08bc09f…`.

`980fbd59ff76c6d16bb5e21aa61d2e5484d50ee478296e6ac3c06a652e6fcb58` salió `TxSorobanInvalid`. Horizon responde 404. No es un hash de rechazo.

Otras filas de esa misma cuenta, la misma noche, que no son las de la tabla principal: monto 0 `603fc49af023b1607d613a0363a9f2fc25a7fa9f6084d34bdad86aaccad85031` (código 4); `transfer` a la G de la dueña `8870df06845f8f5e27ffd44ab188863ab7e8866f74846d4158089422741e4106` (código 3); firma con `context_rule_ids = [1]` `ef638a7af21d3957e5766fc1a0183dc9b8c6ae8bea62240a4bb56d12e93cce87` (3000 ContextRuleNotFound). `mark_executed` firmado por el atacante y `cancel` firmado por la dueña: `TxMalformed`, no hay tx.

En la cuenta publicada, la noche del 6 oct, tres envíos con firma vacía fallaron en `__check_auth` y no sustituyen el caso firmado por la dueña: `transfer` `a5d43e1dc7598aa04f05ee7f621f308ec2bbfffdf9b45426fc74a85d2870f336` (ledger 5060414, 22:27:37Z); `burn` `ea0aa8a03b7e6f95ca53856d8cf8e01e332c0eb642769a88c534d9304360b436` (ledger 5060415, 22:27:42Z); `approve` `b3edc7ccb0d1ed2136e061f9cddb638140f29f3e9c3d31dd0a8c962d0aadadc0` (ledger 5060435, 22:29:22Z). El `approve` con firma de la dueña sigue siendo el de la mañana, `25b91a39…`.

`[INFERENCE]` El 3016 del guardián ocurre en la cuenta, al ver un firmante que no está en la regla, antes de que `enforce` devuelva OwnerNotAuthenticated (2). El SAC no se movió. No se reescribió el contrato.

## Lectura

`[FACT]` Cuenta publicada y cuenta del emulador, leídas sin firmar alrededor del ledger 5060374–5060385: `Count` = 1, firmante la dueña de cada una, política `CCGIEMIU…`, sin regla 1. Wasm de las dos: `986956cc…`. Detalle en `chain-reread.md`. El spec de la cuenta publicada no exporta `execute` ni `upgrade`. El registro publicado no publica `transfer`.
