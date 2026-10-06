# P6: firma desde el dispositivo (día 5)

**Veredicto: VERDE en emulador, con matices.** Una clave ed25519 del Keystore de Android firma `queue`, `transfer` y, la del guardián, `cancel`. Las transacciones se confirman en testnet. El ciclo encolar → esperar → enviar se hizo entero desde la app. **No** se corrió en un teléfono físico. GH-01 y GH-24 se reprodujeron con el código del cliente, pero no desde el teléfono (ver abajo).

| | |
| --- | --- |
| Etiqueta | **REAL** (testnet; hashes copiados de Horizon / RPC, todos `successful`) |
| Fecha | 2026-10-06, 14:25–14:50 UTC, ledgers 5054626–5054922 |
| Dispositivo | Emulador Android, AVD `Medium_Phone_API_37.0` (API 37), con PIN de bloqueo de pantalla. **No es un teléfono físico.** |
| Red | Stellar **Testnet**, RPC `https://soroban-testnet.stellar.org` |
| Código | `app/android/src/main/kotlin/com/guardpay/android/signing/` (`KeystoreSigner`, `BiometricSigningPrompt`), `app/android/src/debug/.../Wiring.kt` |

## Cómo está montado

- **Claves.** `KeystoreSigner` crea un par ed25519 dentro del `AndroidKeyStore` (API 33+; `PURPOSE_SIGN`). Hay dos alias: `guardpay-owner` y `guardpay-guardian`. En debug viven las dos en el mismo teléfono, para la demo. En uso real cada persona tiene su teléfono.
- **Autorización por firma.** Cada clave pide `setUserAuthenticationRequired(true)` con timeout `0` y `AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL`. Cada firma abre el diálogo del sistema: "Firmar en Stellar" para la dueña y "Detener el pago" para el guardián. Se libera con la huella o con el bloqueo de pantalla.
- **Solo hashes.** `signHash` acepta exactamente 32 bytes. El digest lo construye `KmpStellarGateway`, que antes pasa la entrada de auth por `AuthEntryGuard`: solo se firma la llamada exacta que la app construyó.
- **Provisión.** La app escribe las direcciones G públicas en `keys.json`. `DeviceProvisioningLiveTest` despliega una cuenta para esas claves y escribe `testnet.json`, que se sube con `adb push`. No hay semilla en ningún paso.

Cuenta del dispositivo:

```
account   CDQBRJVITWKCLX6KJO4DW4L76BKOK6T4YLANZDSQLJ7OSGAJIMPSFDB6
registry  CBUT2MHP7PZPXG7W7W6Q655C2NNPHEILL7CQY4ZDOIDYAYXXIGXS5SFQ
usdc      CBLUFECN26BH3HMIH2CZPPHJ2QBEH3X3B4TSOGYEH4SI7UZEB3MAZRAA   (SAC de prueba, no el USDC de Circle)
owner     GDIFOBI5NW4N5DHBBYMRLJ766X4EWN6VQABCRUJ6SRMJ3SW2AI56U5UD   (clave del Keystore, alias guardpay-owner)
guardian  GCCKC6JFAA3J2TWUOF32S4TERWZFGSF7NDHTDJ7FBZK5KWHV5VGJE4ZI   (clave del Keystore, alias guardpay-guardian)
```

## Transacciones firmadas en el dispositivo

Cada fila pasó por el diálogo del sistema y se liberó con el PIN. El firmante es la clave del Keystore indicada.

| # | Acción en la app | Firmante | Hash | Ledger (cierre UTC) |
| --- | --- | --- | --- | --- |
| 1 | Pagar 150 USDC a un desconocido → **Retenido** (hold 0) | dueña | `40c4956915dfde414a38b0d27e1769fa6cda3e552044d6053db7116faa22f450` | 5054626 (14:25:17) |
| 2 | Pagar 60 USDC a un desconocido → Retenido (hold 1) | dueña | `5d398fb6490bc8dab745b3f8ff30c3fd53780dd243e4603b10e7c0f35ab5ea95` | 5054644 (14:26:47) |
| 3 | Guardián: **Detener** el hold 1 | guardián | `1ee145654c5b6de2baabb025f5c24ecef7d222ed4923d0a3e24670a3aef89ce9` | 5054673 (14:29:12) |
| 4 | Enviar el hold 0 ya madurado (150 USDC) | dueña | `02fd47873f1c9c8cb318144b778da9b7ba20de54a80dd5b9e2498225445e8621` | 5054696 (14:31:07) |
| 5 | Pagar 10 USDC a un contacto de confianza ("Sale ahora") | dueña | `0afd7985a00ebc2743bea4b6fdfa0bcf9394e440dc83f871632cb89fc84dd1f7` | 5054719 (14:33:02) |
| 6 | Retener 20 USDC (hold 2, prueba de aviso P8) | dueña | `b53235d0233764cb2d2fe135e127844696b8b9d43999fc6f161b039fc06430c8` | 5054852 (14:44:07) |
| 7 | Retener 5 USDC (hold 3, prueba de aviso P8) | dueña | `6b1e5d7719d0d0ebb6c2208b7dc5f592b8f9697722b974f681e033e86a422bc8` | 5054881 (14:46:32) |
| 8 | Retener 7 USDC (hold 4, prueba de aviso P8) | dueña | `e89c8a85f1ac423a9bad4f4e3bd3dc7d1521a731e1f8d25129bb9eb7d5ee2a57` | 5054922 (14:49:57) |

Saldos tras la fila 5: el desconocido 150 USDC y el contacto 10 USDC. La app de la dueña muestra 340 USDC (500 − 150 − 10).

Capturas en `screens/`: `p2-retenido` (fila 1), `g1-detalle` y `g2-detenido` (fila 3), `o2-enviado` (fila 4), `o3-contacto` (fila 5) y `o1-home`. El diálogo de firma no sale en ninguna. Su captura (`adb screencap`) da una imagen negra. [INFERENCE] El sistema bloquea las capturas del diálogo biométrico.

## Criterios de aceptación

| Criterio | Estado | Evidencia |
| --- | --- | --- |
| Un transfer firmado desde el teléfono se ejecuta en testnet, con hash | **Cumplido (emulador)** | filas 4 y 5 |
| Un transfer sin la firma de la dueña se rechaza, con hash | **Cumplido con el código del cliente, no desde el teléfono** | GH-24 abajo |
| Ciclo queue → esperar → enviar desde el teléfono | **Cumplido (emulador)** | filas 1 → 4 |
| La clave nunca sale del Keystore ni aparece en logs | **Cumplido** en lo que se probó | `thePrivateKeyCannotBeExported`; ver "Revisión de seguridad" |
| El guardián nunca es firmante | **Cumplido** | `signers == [owner]` en la cuenta del dispositivo; dos tests lo fallan si el guardián aparece |

## GH-01 y GH-24 contra testnet

Se corrieron en `KmpStellarGatewayLiveTest`, run 4 (2026-10-06, 13:36 UTC). Ese test usa el mismo `KmpStellarGateway` que la app y los mismos contratos de producción. Las claves son desechables y en memoria, así que la cuenta es otra (`CB4MB57WWSCC4FO6CMSMGPEH45HGH55AMCSYMZG7GNCWWSSI73WMFRU4`).

| Caso | Resultado | Hash |
| --- | --- | --- |
| GH-01: el atacante firma un `transfer` por el gateway | `Rejected(code=3016)` en la simulación; no se envía nada | — |
| GH-01: mapa de firmantes vacío, enviado a la red a mano | **FAILED** en cadena | `2f1631ba22f7f1a3082d6392246c618be99861e4562104f17948ce36e92e4dc8` |
| GH-24: `transfer` **sin** firma de la dueña, enviado a la red | **FAILED** en cadena | `4ea3a7d2ca671b350bf7ede738521b594885cbcdd3bbad5f11d8c9cf99157eb6` |
| GH-24: `transfer` con la firma de un atacante, enviado a la red | **FAILED** en cadena | `ca329ac303d0da8b474c4aa57fb5841bd6194f7170b630a252a09e022c63f6e1` |
| Control: `transfer` del hold madurado, firmado por la dueña | Confirmed | `67e7ed683077d30733b51d18a1a773389ed177824296a1f225a6fdb621b281dc` |

- [FACT] Los tres intentos sin la firma de la dueña llegaron a un ledger y fallaron. El control con su firma pasó.
- [FACT] La app no tiene ninguna pantalla que envíe una transacción sin firma de la dueña, y no se añadió una para esta prueba.
- **No corrido:** GH-01 y GH-24 contra la cuenta del dispositivo, y desde el teléfono. El prompt de P6 pide "desde la app". Lo que se hizo es "con el código de la app, en JVM".

## Tests

| Test | Dónde | Resultado |
| --- | --- | --- |
| `KeystoreSignerTest` (5): la firma verifica contra la dirección G con un verificador ed25519 independiente; el mismo alias da la misma clave; `PrivateKey.encoded == null`; solo firma 32 bytes; una clave con autenticación obligatoria no firma si el diálogo se salta | Instrumentado, emulador API 37 con PIN (la condición `isDeviceSecure` se cumplió) | **OK (5 tests)**, 2026-10-06 |
| `DeviceProvisioningLiveTest`: la cuenta del dispositivo tiene `signers == [owner]` y el guardián no está entre ellos | JVM contra testnet, `-PliveTestnet` | OK al provisionar (día 5). La aserción explícita "el guardián no es firmante" se añadió después. **Corrió contra la red el día 6**, al provisionar la cuenta de la demo `CDWPPTDM…GFV6`: OK (`evidence/demo/rehearsal.md`). |
| `KmpStellarGatewayLiveTest`: el guardián no es firmante | JVM contra testnet, run 4 | OK |
| `:app:shared:allTests` (incluye los checks de arquitectura y de grafo) | JVM | 338 tests, 0 fallos, 2026-10-06 tras los cambios del día 5 |

## Revisión de seguridad

- [FACT] **No hay ruta de exportación.** `KeystoreSigner` solo expone `publicKey` y `signHash`. La clave privada es una referencia del Keystore y su `encoded` es `null` (test). Nada en el código escribe material privado. `keys.json` y `testnet.json` solo contienen direcciones públicas.
- [FACT] **Logs.** `HoldWatchService` registra ids de hold y retrasos. `Wiring` registra el tipo de excepción. Ninguno registra claves, firmas ni hashes de firma. Un `println` temporal de depuración en `KmpStellarGateway` se quitó antes de este commit.
- [FACT] **Cada firma pide a la persona.** El timeout `0` significa autenticación por uso. El test instrumentado muestra que el Keystore rechaza la firma si el diálogo se salta.
- [FACT] Hallazgo de esta sesión: sin `android.permission.USE_BIOMETRIC` en el manifiesto, `BiometricPrompt` lanza `SecurityException` y la app mostraba "Error de red". Se añadió el permiso. [RECOMMENDATION] `OwnerSession.submit` debería distinguir un fallo de firma de uno de red. **Hecho el día 6:** `SubmitResult.SigningFailed` muestra "No se pudo firmar en este teléfono. No se envió nada." (`OwnerSessionTest.aBrokenKeyIsNotShownAsANetworkError`).
- **Firma a ciegas.** El diálogo del sistema muestra un título fijo, no el destino, el monto ni la regla. La clave firma un hash. Esto lo mitigan la retención en cadena y la vista del guardián, no la clave. No resuelve al adversario A (ingeniería social).
- [UNVERIFIED] Que el emulador use un Keystore respaldado por hardware. En el emulador probablemente es software. En un teléfono físico sería TEE o StrongBox, sin probar.

## No corrido

- Teléfono físico (solo emulador).
- GH-01 y GH-24 desde el teléfono y contra la cuenta del dispositivo.
- GH-33 (passkey WebAuthn). Era SHOULD. Se usa Keystore + BiometricPrompt, no una passkey.
- Recuperación de cuenta (fuera de alcance).
- iOS (no hay Mac).
