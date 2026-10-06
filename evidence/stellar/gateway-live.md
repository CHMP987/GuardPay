# `KmpStellarGateway` contra testnet (día 3)

**Veredicto: VERDE para transporte y firma** (lecturas, firma de la smart account, firma de una cuenta G clásica y rechazo de un firmante ajeno). **No** prueba nuestros contratos: todavía no existen. Se repite en el día 4 contra las direcciones de Abraham (Sync 2). **Actualización día 5:** la corrida 4, contra los contratos de producción, está al final. Las notas "No corrido" de abajo son las del día 3; la corrida 4 cubre la mayoría.

| | |
| --- | --- |
| Etiqueta | **REAL** (testnet, salida literal de una corrida) |
| Fecha | 2026-10-06, 06:58 UTC, ledgers 5049273–5049278 |
| Red | Stellar **Testnet**, RPC `https://soroban-testnet.stellar.org` |
| Código | `app/shared/src/androidUnitTest/kotlin/com/guardpay/shared/stellar/KmpStellarGatewayLiveTest.kt` |
| Comando | `./gradlew :app:shared:testDebugUnitTest -PliveTestnet --tests "*LiveTest"` |
| Duración | 48 s, 1 test, 0 fallos |

Sin `-PliveTestnet` el test queda excluido, así que `allTests` no toca la red.

## Montaje

- Claves generadas en memoria en cada corrida: pagador de comisiones, dueña, atacante y una cuenta G clásica. Ninguna se imprime.
- La cuenta es el wasm de la demo del SDK (`86b49fe0…6d28`, OZ **v0.7.0**, **sin política**) con firmante `External(verificador ed25519, dueña)`. Es el mismo montaje que el Spike C.
- El SAC nativo de XLM (`CDLZ…CYSC`) hace de "USDC", porque tiene el mismo `balance`/`transfer` SEP-41.
- `holdRegistry`, `guardianHold` y el verificador reciben direcciones de relleno: `transfer` no los toca.
- El montaje (desplegar y fondear con 20 XLM) usa el SDK directamente. Todo lo demás pasa por el gateway.

## Resultados

| Caso | Esperado | Resultado |
| --- | --- | --- |
| `latestLedgerTime()` | tiempo de ledger plausible | OK |
| `readBalance(cuenta)` tras fondear | 20 XLM | OK (200 000 000 unidades) |
| Atacante: `submitTransfer` firmado por una clave que no es firmante | `Rejected` sin hash, nada enviado | `Rejected(txHash=null, code=3002)`; el saldo sigue en 20 XLM |
| Ruta **SmartAccount**: la dueña firma el digest OZ | `Confirmed` | `Confirmed`, tx `f9675eff…ea72`; el saldo baja a 19 XLM |
| Ruta **ClassicAccount**: una cuenta G autoriza su propio `transfer` con credenciales `Address` (la ruta que usará el `cancel` del guardián) | `Confirmed` | `Confirmed`, tx `83899519…a567`; su saldo baja exactamente 1 XLM |

### Hashes (verificados aparte con `getTransaction` en el RPC: los tres `SUCCESS`)

```
fixture fund tx  71bb442d7fb5c25919b9ba51f6e432dba253f9fa03f759f8ffb85962d46e98c9  ledger 5049273
smart account    f9675eff2894211f833950f005e79605f212a649cd4e338209558ddfe6e2ea72  ledger 5049276
classic account  8389951942b086456997774c977c459d3050696dcc2f6aad0aa2d14533faa567  ledger 5049278
smart account C  CB7FLMYZ7RFDFNMFF3PLKZJE5EJYGX5ZTWP24JHVC4UO4HKDYQ6GUU4B
```

## Lectura

- [FACT] El gateway firma y envía una transferencia autorizada por una smart account OZ v0.7.0 con la regla `[0]`, y una autorizada por una cuenta G clásica. Las dos se confirman en testnet.
- [FACT] Con un firmante ajeno, el rechazo llega en la simulación (`prepareTransaction`) y no se envía nada.
- [INFERENCE] `3002` es un código de error del contrato de cuenta OZ v0.7.0 para un firmante que no está en la regla. No se comprobó en el código fuente de OZ. Nuestros códigos llegan con INTERFACES.md.
- [UNVERIFIED] El `cancel` real del guardián contra `HoldRegistry` todavía no se ha probado: no existe el contrato. Esta corrida solo demuestra la misma ruta de firma clásica sobre otra llamada.
- [UNVERIFIED] Que la regla `0` sea la `Default` de la cuenta de Abraham (pin `b40c5ea`) es una suposición.
- No corrido:
  - `submitQueue`, `submitCancel`, `readAccountRules`, `readTrustedContacts`, `readDailySpent`, `readHolds` (no hay contratos).
  - El rechazo de una entrada de auth alterada por un RPC hostil, contra la red. Está cubierto solo por tests unitarios (`AuthEntryGuardTest`).
  - El tope de comisión.
  - `SubmissionOutcomeUnknownException`.

## Corrida 4: contratos de producción (días 4–5)

| | |
| --- | --- |
| Etiqueta | **REAL** (testnet, salida literal) |
| Fecha | 2026-10-06, 13:36 UTC, 230 s, 1 test, 0 fallos |
| Contratos | wasm de producción (`contracts/`). `guardian_hold` es `CCGIEMIU…BJJI`, el mismo de `deployment.md`. La cuenta y el registro son nuevos, de esta corrida. |
| Montaje | `TestnetProvisioner` (androidUnitTest): claves desechables en memoria; SAC de prueba, trustlines y mint en cada corrida |

```
account   CB4MB57WWSCC4FO6CMSMGPEH45HGH55AMCSYMZG7GNCWWSSI73WMFRU4
registry  CDOYYCUUGHAJBVQNCVUR36XUHGTVJRU4ZH63F3NLGFXXKJN6DWYANDWG
usdc      CC5FNFHLZA5ZWVG5YHM3Y7ZHRXJPAQXPRZTT7EKVNRCWX4ZAQ4TZJM2K   (SAC de prueba)
shape     contextRuleCount=1, signers=[dueña], policies=[guardian_hold]
```

| Caso | Resultado | Hash |
| --- | --- | --- |
| La dueña transfiere 10 a un contacto | Confirmed | `dc2bfbda586090f45866519e0d5703993d3d059dceb2cdd892fce84e4f26c26e` |
| GH-01: el atacante firma por el gateway | `Rejected(code=3016)` en la simulación | — |
| GH-01: mapa de firmantes vacío, a la red | FAILED | `2f1631ba22f7f1a3082d6392246c618be99861e4562104f17948ce36e92e4dc8` |
| La dueña encola 150 (hold 0) | Confirmed | `74f8a785f9cc3f5647ac63017378389792ec5c03c17e5efac0f30871c0562298` |
| La dueña encola 60 (hold 1) | Confirmed | `96fbcb5e8653c734b3c220d36d438f8ecbe8f8fc0da20395d8d88da1efbb95d4` |
| `transfer` del hold 0 antes de madurar | `Rejected(code=7)` en la simulación | — |
| La dueña intenta `cancel` | Rejected en la simulación (el registro desplegado solo deja cancelar al guardián) | — |
| El guardián cancela el hold 1 | Confirmed | `1e86b42994d1a85d5af28b8320e391f08601bb2f01b55181716d45dd9179b9e0` |
| GH-24: `transfer` sin firma de la dueña, a la red | FAILED | `4ea3a7d2ca671b350bf7ede738521b594885cbcdd3bbad5f11d8c9cf99157eb6` |
| GH-24: `transfer` con firma del atacante, a la red | FAILED | `ca329ac303d0da8b474c4aa57fb5841bd6194f7170b630a252a09e022c63f6e1` |
| La dueña envía el hold 0 madurado | Confirmed | `67e7ed683077d30733b51d18a1a773389ed177824296a1f225a6fdb621b281dc` |
| `transfer` del hold 1 ya detenido | `Rejected(code=3)` en la simulación | — |

- [FACT] `submitQueue`, `submitCancel`, `readHolds` y `readAccountShape` funcionan contra los contratos reales.
- [FACT] El guardián no aparece entre los firmantes; el test lo afirma.
- [INFERENCE] Los códigos `3016`, `7` y `3` vienen de los contratos de producción y de OZ. Su significado exacto no se cruzó con INTERFACES.md en este documento.
- La misma ruta, firmada con el Keystore desde la app en el emulador, está en `evidence/smart-account/signing.md`.
- No corrido: el tope de comisión y `SubmissionOutcomeUnknownException` contra la red.
