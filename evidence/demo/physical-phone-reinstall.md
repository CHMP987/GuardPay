# 12 oct: reinstalación desde cero en el teléfono, más todas las pruebas

**Veredicto:**
- Se borró la app del teléfono y se reinstaló.
- La app descargó el modelo de 2,6 GB desde cero. P7 repitió las mismas 18 salidas.
- Las escenas 1, 2, 3 y 6 corrieron otra vez en el teléfono contra testnet, con la huella de la persona.
- Las escenas de terminal 2, 4, 5 y 6 corrieron con una cuenta de testnet nueva.
- Todas las suites pasaron.

Dueña y guardián estaban en el **mismo** teléfono.

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet + teléfono físico. Hashes copiados de Horizon (app) o de la salida de `DemoTerminal` (terminal). Los mensajes de P7 son **SIMULATED**. |
| Fecha | 2026-10-12, 16:46–17:42 hora local (UTC−6), es decir 22:46–23:42 UTC |
| Dispositivo | El teléfono del día 8, Android 14 (API 34), con huella |
| App | APKs de debug y de androidTest compilados a las 11:16, del árbol que luego se subió en `1c1c1d3` (P7). [INFERENCE] Hasta `e46b604` (`main`), el código solo cambió en comentarios (`add759e`). |
| Quién tocó | Los guiones (`physical-phone-reinstall/phone.py` y `download.log`) tocaron la pantalla. **La huella la puso la persona** en cada diálogo. |

## Reinstalación

1. `adb uninstall com.guardpay.android` y `com.guardpay.android.test`. [FACT] Esto borra también las claves del Keystore. La cuenta del día 8 (`CBGZSY…`) queda huérfana: nadie puede firmar por ella.
2. Se instalaron los dos APKs. La app creó claves nuevas y escribió `keys.json`.
3. `DeviceProvisioningLiveTest` desplegó una cuenta para esas claves. Las transacciones de provisión son de las 22:46 UTC: dueña `69f4672e…` y guardián `b5994043…`. Luego se hizo `adb push` de `testnet.json`.

```
account   CAUANKS6AQDFRIJYX6LGZ6K2EXTM2BD65TADONAT5OPHOTQMZZXC5K7R
registry  CBEQZTKQIKFKVRHAKUTMT4DN3CTUT7IMA37PJMGFIGEHWCFTI3YJ4K5Y
policy    CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI
usdc      CAT5HLTPSDNFJLMAB3DFX5SXUF33REAG6QWYGERRVM6NTGFMP2C3QFTO   (SAC de prueba, no el USDC de Circle)
owner     GABPAWVGD5QFDNUGG5P5BETPUTBSKYQCQ35AGZGCTVDP4H6QKKOXGRRW   (Keystore del teléfono)
guardian  GCVKKO5OBPZY63AYJNCGW7YWQKA33NZNGPG7253REEALHL7JWDELMJK5   (Keystore del teléfono)
contacto  GDLLUME644YL5O3UL4DDDYSZJ5BQC2OWRJWZ5XFZSBVZ7IQALC2QIOU7   ("Mamá")
stranger  GALVMKSSOCH5QOOQHCFI53LBQUIZ2HDED3KYN7MCAAJL7ZQFITADTAXK
```

Saldo inicial de 500 USDC y tope diario de 50. No hay semilla: las claves no salen del Keystore.

## Descarga del modelo en la app, desde cero

| Paso | Valor |
| --- | --- |
| Antes | En la carpeta de la app solo estaban `keys.json` y `testnet.json` |
| Toque en "Descargar el modelo" | 16:53:54 |
| Progreso | 1 % a los 22 s (41 MB), 2 % a los 39 s (78 MB) |
| Archivo final | `gemma-4-E2B-it.litertlm`, **2 588 147 712 bytes**, fecha 17:02 |
| Tiempo total | unos 8–9 min, por la hora del archivo final (no se midió el segundo exacto) |
| SHA-256 en el teléfono | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`, el del commit fijado de Hugging Face |

- [FACT] El guion dejó de mirar a los 60 s (`download.log`), con un `.part` de 103 MB. La descarga siguió sola. En ese rato se recreó la actividad y la app pasó a segundo plano (al launcher).
- [FACT] Así se cierra el "no corrido" de `evidence/ai/benchmark.md`: la descarga completa de 2,6 GB desde cero dentro de la app.
- [UNVERIFIED] Qué velocidad de red tenía el teléfono. Los 8–9 min dependen de ella.

## P7 otra vez (`GemmaOnDeviceTest`)

17:03:42–17:06:23. Resultado: `OK (1 test)`, 157,985 s, GPU en 18/18. Salida: [`../ai/p7/run3-reinstall.jsonl`](../ai/p7/run3-reinstall.jsonl).

- [FACT] Fuera del tiempo, las 18 líneas son idénticas campo por campo a `run1-cold.jsonl` y `run2-warm.jsonl`: `raw`, `whatItAsks`, `signals` y `suggestion`.
- Estafas: 10/10 `SuggestHold`. Inyecciones: 4/5 (n15 `NoClearSignals`, como antes). Controles: n16 `SuggestHold` (el falso positivo de siempre), n17 y n18 `NoClearSignals`.
- Tiempos en ms:

  ```
  21068, 8807, 6594, 8913, 8451, 7681, 9346, 10145, 8293, 7323, 6685, 7307, 10121, 8240, 7622, 6810, 6898, 7434
  ```

  - [FACT] La primera lectura incluye cargar el motor: 21,1 s, frente a 64,1 s y 49,3 s en la mañana.
  - [FACT] Con el motor cargado: 6 594–10 145 ms. **15/17 < 10 s**. n8 (10 145 ms) y n13 (10 121 ms) pasan de 10 s por poco; en la mañana el máximo fue 9 684 ms.
  - [INFERENCE] La diferencia es de carga del teléfono o de temperatura, no del modelo: las salidas son las mismas.

## Suites

Corridas con el código de `e46b604`, entre las 17:26 y las 17:31 (cargo y Keystore, antes). Los logs completos quedan en la máquina de CHMP987.

| Suite | Resultado |
| --- | --- |
| `cargo test -p account -p guardian_hold -p hold_registry` | 36/36 (account 1, guardian_hold 35, hold_registry 0) |
| `:app:shared:allTests`, forzado (`clean*Test` + `--no-build-cache`) | BUILD SUCCESSFUL. jvmTest 127 y testReleaseUnitTest 113, 0 fallos. Corrieron los cuatro checks. |
| `KmpStellarGatewayLiveTest` (`-PliveTestnet`) | pasó, 239,9 s, con claves desechables |
| `KeystoreSignerTest` en el teléfono | `OK (5 tests)` |

[FACT] El número de pruebas de testDebugUnitTest dentro de ese `allTests` se perdió: el paso del live test limpió sus resultados. Pasó (el build fue exitoso), pero el conteo es "no corrido".

## Escenas de terminal (`DemoTerminal`)

17:34:56–17:36:44. Log: [`physical-phone-reinstall/terminal.log`](physical-phone-reinstall/terminal.log), sin las tres líneas de aviso de SLF4J.

`DemoTerminal` provisionó su propia cuenta de testnet, con claves solo en memoria:

```
account   CDMMZ7BEPZLBWPMAVJAKFLCO523R6ZEFXYK7STNOWJ6WALGSK3Q4J75A
registry  CCQ4IQSY3RTVPUU6LIZ7PUP6WH224NNXBRUS3FSPPLCSVB3D47II2BUU
usdc      CCQVODI5E577DB6ZOHZVYP2UXYS3CYTGX25PUOBOL6262LWU4DK5Y7GQ
```

Antes de la primera escena mostró "reglas: 1" y "guardián firmante: false".

| Escena | Acción | Hash | Resultado |
| --- | --- | --- | --- |
| 2 | `queue` de 150 USDC al extraño | `31257020d89b483713153db3bfe92ed3bb93d0f547aedd2f3563b64449446e18` | confirmada, hold #0 Retenido |
| 2 | `transfer` de esos 150 sin esperar | `3e4779dde3ccadb1cd97b1cb45eddae8d66ef78c5fe9c94bef775c0f0dc820c7` | código 7 `HoldNotReady`, FAILED, ledger 5164917 |
| 4 | `cancel` del guardián | `033b78dab771e2460d5e8e3dafda27be9fdcc022187f949046a806c7879a545d` | hold #0 Cancelled |
| 4 | `transfer` del pago detenido | `151cf5b6c8aff5c9eb8a0a72b0f827b520ccb6d9dcbd9c67517e7cbb75a9f403` | código 3 `NotAllowed`, FAILED, ledger 5164920 |
| 5 | el guardián firma un `transfer` de 20 USDC a sí mismo | `9a4c375306f5095e7c24d0a8a3bfa8d38a76ef39dce28ba21dc797ce881b93d1` | 3016 `UnauthorizedSigner`, FAILED, ledger 5164921 |
| 6 | `approve` de 25 USDC al guardián | `00fc8da06e0a1df0b272772d2e06b9fc30ebd68b563140abdabb32551cf764a0` | código 3 `NotAllowed`, FAILED, ledger 5164923 |
| 6 | `add_context_rule` en la cuenta | sin transacción | la simulación da `HostError: Error(WasmVm, MissingValue)`: la función no existe. Reglas: 1 |

[FACT] El primer intento falló con `java.net.SocketException: Connection reset` durante la provisión, después de desplegar el registro `CBSTMASLQVK47M4I5ROS7CAXLNZJ23WO3C25UHIMW6SWSZ3HEUGZKDF4`. [INFERENCE] Fue un fallo pasajero del RPC de testnet. Se repitió sin cambios y pasó.

## Escenas de la app en el teléfono (corrida p2)

17:38–17:42. Las hizo `physical-phone-reinstall/phone.py`. Hashes copiados de Horizon, todos `successful`.

| Escena | Resultado en pantalla | Hash | Ledger (UTC) |
| --- | --- | --- | --- |
| 1 | 10 USDC a Mamá → **Enviado** (`transfer` de `CAUANKS6…` a `GDLLUME…`) | `6c630ad3ea3ae2eecc408b66f1cb203422d528cb91d0df049d6b3cb1c2cf1133` | 5164952 (23:39:07) |
| 2 | 150 USDC a `GALV…TAXK` → **Retenido hasta las 17:42** (`queue`) | `6bfb5bea1d461c6f7a19dcb75ed7dada164daf77a57720220032d1d8b2567d39` | 5164968 (23:40:27) |
| 3 | Aviso "Pago retenido: 150 USDC", Detener → **Detenido por ti** (`cancel` del hold 0) | `e1823f206b0af01489dc85c1211c21e946aa2dbb410ee1a62d0d8ca005e313f4` | 5164980 (23:41:27) |
| 6 | "Esta cuenta tiene 1 regla / Firmante: la clave de este teléfono / Política: GuardianHold / Tu guardián: Diego (GCVK…MJK5). No es firmante de esta cuenta." | sin transacción | — |

### Tiempos

Medidos por el guion en el host (`rehearsal-p2.log`). En ese log, "after PIN" significa "desde que apareció el diálogo de huella".

| Paso | Tiempo |
| --- | --- |
| Escena 1: de Inicio a "Enviado" | 60,7 s; 38,2 s desde el diálogo de huella |
| Escena 2: de Inicio a "Retenido" | 47,2 s; 19,8 s desde el diálogo de huella |
| Escena 3: aviso tras ver "Retenido" | **10,9 s** (objetivo de P8: < 60 s) |
| Escena 3: de tocar el aviso a "Detenido" | 33,8 s; 15,9 s desde el diálogo de huella |
| Escena 6: de Inicio a "Política: GuardianHold" | 21,8 s |

- [FACT] El guardián detuvo el pago 60 s después del `queue` (23:40:27 → 23:41:27), dentro de la ventana.
- [FACT] Al final, Inicio mostraba "Te quedan 20 USDC de tu tope de hoy". Es 50 − 3 × 10: los tres pagos a Mamá del día (este y los dos de abajo).

### Dos pagos de más a Mamá (17:18)

[FACT] Horizon muestra dos `transfer` exitosos más de 10 USDC de `CAUANKS6…` a `GDLLUME…` (Mamá):

| Hash | Ledger (UTC) |
| --- | --- |
| `8348627b0d0e46d6d57e340e933e5e64ea1421c8da5f664044c270b69bc7694b` | 5164707 (23:18:42) |
| `7e2a9eb4610192fff2122e00e883880bf2739a89f7334eeb612e2118f52de541` | 5164710 (23:18:57) |

- [FACT] Pasaron después de un intento fallido de la escena 1: el guion se rindió a las 17:17:18, esperando una huella que nadie puso en 180 s, y dejó el diálogo abierto.
- [UNVERIFIED] Quién o qué los disparó. Lo probable es que la persona pusiera la huella en el diálogo que quedó abierto y luego hiciera otro pago a mano. No se preguntó a tiempo y no quedó registrado.
- [FACT] En la corrida p2, la escena 1 produjo **una** transacción por pago.
- [INFERENCE] Cada uno necesitó la huella: la clave de la dueña pide autenticación en cada uso (`KeystoreSigner`).

### Hallazgos

- [FACT] **El guion del día 8 tocaba antes de tiempo.** Tocaba "Mamá" en la lista de Inicio antes de que abriera Pagar, y luego no encontraba "Monto (USDC)". `phone.py` ahora espera "le pagas" y "Monto (USDC)", y su gesto de deslizar se adapta a la orientación de la pantalla.
- [FACT] **Pagar sigue pidiendo la huella dos veces; detener, una.** Es lo mismo que el día 8.
- [FACT] **La notificación vuelve a leer el destino de la cadena** (`p2-s3-aviso-recorte.png`).

## Archivos (`physical-phone-reinstall/`)

| Archivo | Contenido |
| --- | --- |
| `d-0-entrada.png`, `d-1-no-descargado.png`, `d-3-tras-descarga.png` | La entrada y la pantalla Pagar antes y durante la descarga (en horizontal) |
| `download.log` | Los primeros 60 s de la descarga |
| `p2-s1-enviado.png` | Escena 1 |
| `p2-s2-revisa.png`, `p2-s2-retenido.png` | Escena 2 |
| `p2-s3-aviso-recorte.png` | La notificación del guardián, recortada |
| `p2-s3-detalle.png`, `p2-s3-confirmar.png`, `p2-s3-detenido.png` | Escena 3 en la vista del guardián |
| `p2-s6-reglas.png` | Escena 6 |
| `rehearsal-p2.log` | Tiempos de la app, hora local del host (UTC−6) |
| `terminal.log` | Salida de `DemoTerminal` |
| `phone.py` | El guion usado: `physical-phone/phone.py` con las esperas y el gesto nuevos |

**No se suben, por privacidad:**
- La captura completa del panel de notificaciones. Muestra notificaciones personales de otras apps del teléfono.
- Los vídeos de las escenas 1, 2+3 y 6, por lo mismo.
- El logcat completo.
- Los logs de Gradle y de cargo, que no aportan más que los números de arriba.

Quedan en la máquina de CHMP987.

## No corrido

- Dueña y guardián en dos dispositivos físicos distintos (esta vez, los dos en el mismo teléfono).
- El consumo de batería de la IA.
- El conteo de testDebugUnitTest dentro del `allTests` forzado.
- El segundo exacto en que terminó la descarga.
- Reiniciar la app entre escenas.
