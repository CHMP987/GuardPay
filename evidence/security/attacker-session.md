# Sesión de atacante — P9

6 oct 2026, testnet. Etiqueta **REAL**. `[FACT]` salvo la inferencia marcada.

La ventana de envíos incluidos va de 2026-10-06T21:24:57Z (ledger 5059662) a 2026-10-06T21:47:32Z (ledger 5059933). No se afirma una duración de dos horas. Lo no intentado queda no corrido.

No se encontró una ruta, entre las de abajo, que moviera el SAC de prueba hacia un destino fuera de la lista blanca. Eso no se escribe como "no existe bypass".

## Herramienta, antes del rechazo de contrato

El envío forzado de un rechazo arma el footprint a mano. Las primeras transacciones incluidas leyeron una clave `Hold` que no estaba en ese footprint. Horizon las tiene, `successful=false`, y el evento es `storage exceeded_limit` ("outside of the footprint"), no el error del contrato. Esas filas no se citan como NotAllowed.

| Intento | Hash | Ledger | Qué dice el evento |
| --- | --- | --- | --- |
| Destino distinto, primer envío | `7ef163b113bd70bac6536fa5759da0975676da50594d84261347dc9ce895cf26` | 5059849, 2026-10-06T21:40:32Z | clave Hold 3 fuera del footprint. El hold 1 siguió Retained |
| Replay, primer reenvío | `b9ac81fc99cf1f688eba934c5fc618f35200a14be601729eb39b987dd404076a` | 5059852, 2026-10-06T21:40:47Z | la misma clave Hold 3 |
| Contrato intermedio, primer `pay` | `61573e21ce8a96db2eae5e874351c9ba9e6de26fd45c059fcb927167418ad1f6` | 5059915, 2026-10-06T21:46:02Z | el wasm del contrato intermedio fuera del footprint. La simulación, antes, había sido 3014 (longitud de `context_rule_ids`) |

El monto distinto tuvo un envío anterior (`7803cdf560f3a4a41ea4bd41c54de3a0357086b14b58ed67aa569020825cd7c3`, ledger 5059850, 2026-10-06T21:40:37Z) en la misma ventana de footprint corto. No se usa como prueba de NotAllowed. El que sí tiene el evento de contrato 3 es `a08bc09f…`.

Un envío aún anterior (`980fbd59ff76c6d16bb5e21aa61d2e5484d50ee478296e6ac3c06a652e6fcb58`) salió `TxSorobanInvalid`. Horizon responde 404. No es un hash de rechazo.

## Otras rutas, el mismo día

| Intento | Resultado | Hash | Código |
| --- | --- | --- | --- |
| Monto 0 a un contacto | Rechazo | `603fc49af023b1607d613a0363a9f2fc25a7fa9f6084d34bdad86aaccad85031` | 4 InvalidAmount |
| `transfer` hacia la dirección G de la dueña | Rechazo. No es contacto y no hay hold | `8870df06845f8f5e27ffd44ab188863ab7e8866f74846d4158089422741e4106` | 3 |
| `burn` del SAC de prueba | Rechazo. Catch-all | `901b04dd164ce328b887b4dc34aa0238678b3d8f0793f1ac596fdab645721a4a` | 3 |
| Firmar con `context_rule_ids = [1]` | Rechazo. No apareció una segunda regla | `ef638a7af21d3957e5766fc1a0183dc9b8c6ae8bea62240a4bb56d12e93cce87` | 3000 ContextRuleNotFound |
| `mark_executed` firmado por el atacante | `TxMalformed`. No hay transacción incluida | no hay tx | — |
| `cancel` firmado por la dueña | `TxMalformed`. T-049 no está. `cancel` exige al guardián | no hay tx | — |
| Segunda `install` en esta cuenta | Solo se pidió `--help`. No hay segunda llamada en el ledger | no hay tx | — |
| Segunda `install` en la cuenta publicada | Ya registrada: reentrada en modo enforce, antes de AlreadyInstalled. Sin hash | no hay tx | `rules-onchain.md` |
| `execute`, `upgrade`, `add_context_rule` | Subcomando no reconocido, en esta cuenta y en la publicada | no hay tx | — |
| `enforce` directo | Simulación con contrato 3. `--send=yes` no dejó hash | no hay tx | — |
| Dos transferencias de 30 en el mismo ledger | no corrido. Salieron en 5059703 y 5059704 | ver la matriz | 5 en la segunda |

`[INFERENCE]` El 3016 del guardián ocurre en la cuenta, al ver un firmante que no está en la regla, antes de que `enforce` devuelva OwnerNotAuthenticated (2). El efecto medido es el mismo: el SAC no se mueve. No se reescribió el contrato.

## No corrido en esta sesión

- Otro token, distinto del SAC de la config.
- Monto negativo.
- `transfer` de la cuenta hacia sí misma.
- Clawback del SAC.
- `uninstall` incluido en el ledger.
- GH-29 en un solo ledger.
- GH-30 (`expires_at` a 600 s). No se implementó.
- GH-33 (passkey). No se añadió un firmante.
- Un segundo `install` incluido en el ledger de la cuenta de ataques.

Si una de esas rutas pasara, sería un bypass y habría que parar. No se corrieron, así que no se afirma su resultado.
