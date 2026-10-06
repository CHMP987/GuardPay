# `KmpStellarGateway` contra testnet (día 3)

**Veredicto: VERDE para transporte y firma** (lecturas, firma de la smart account, firma de una cuenta G clásica y rechazo de un firmante ajeno). **No** prueba nuestros contratos: todavía no existen. Se repite en el día 4 contra las direcciones de Abraham (Sync 2).

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
