# Día 8: la app en el teléfono físico (Galaxy A54)

**Veredicto: las escenas 1, 2, 3 y 6 corrieron en un teléfono físico contra testnet**, con la firma del Keystore liberada por huella. Dueña y guardián estaban en el **mismo** teléfono. (8 oct: las escenas 2 y 3 corrieron después con la dueña en este A54 y el guardián en un emulador, en `two-devices-android.md`.)

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet + teléfono físico. Hashes copiados de Horizon, todos `successful`. |
| Fecha | 2026-10-07, 05:14–05:20 UTC (6 oct, 23:14–23:20 hora local, UTC−6), ledgers 5065294–5065354 |
| Dispositivo | Samsung Galaxy A54 (SM-A546E), Android 14 (API 34), con huella |
| App | APK de debug de `8123191`. `assembleDebug` dijo que estaba al día con el código, y es el mismo APK instalado en el teléfono. |
| Quién tocó | La escena 1 la hizo la persona a mano. Las escenas 2, 3 y 6 las hizo `physical-phone/phone.py` (el guion del día 6 sin escribir el PIN). **La huella la puso la persona** en cada diálogo. |

## Cuenta (la del teléfono)

```
account   CBGZSYKWUI6WBYZPYXYTMPEJONDDDVD6GRLL2GTBT2ZI5CVAKCMUCZCD
registry  CCLG6N2TLOGLZEEPNUUOLRNM34ILD6VFU7JLL2U3V5BF3SDM3DLE2KMT
policy    CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI
usdc      CAYIFEM765S2SKUJ57JJJ5HSKNGTND5WSMYHALB47EIOG37X5KOUVHHX   (SAC de prueba, no el USDC de Circle)
owner     GDA3N4HGO2DU6K5C5LV6Z2MNZ47AARFMHZN2UMM23JJMLMXYIKJTPJXD   (Keystore del A54, guardpay-owner)
guardian  GASFV2FMIMHTKNROP2EL6LESZHX3LKGT2QJZVU3IXSMCREJY3A4GE4RP   (Keystore del A54, guardpay-guardian)
contacto  GBREUZTSZW5JM7B3NP6B3KPHP3GUKEZL67UXCE6Y2TNJKFDRJ5D2Y3KO   ("Mamá")
stranger  GALVMKSSOCH5QOOQHCFI53LBQUIZ2HDED3KYN7MCAAJL7ZQFITADTAXK
```

Es una de las cuatro cuentas que el equipo tiene en testnet. **No es la cuenta de la demo** (`CDWPPTDM…`). Se provisionó el 6 oct con `DeviceProvisioningLiveTest` para las claves del Keystore del A54. Saldo inicial de 500 USDC y tope diario de 50.

## Transacciones

| # | Escena | Acción en la app | Firmante | Hash | Ledger (UTC) |
| --- | --- | --- | --- | --- | --- |
| 0 | — | `transfer` de 50 USDC a Mamá. Es del 6 oct y **anterior a esta sesión**; aquí no quedó registrado quién lo hizo. | dueña | `3dab5371558900905465a65ccb95b2ca5776dfefb2c8ef54f1b992401e85c517` | 5056184 (16:35:07) |
| 1 | 1 | Pagar 50 USDC a Mamá → **Enviado** (`transfer`), a mano | dueña | `eea17f837a9c9ce30c726659e3c7718e0ba49b417c7c96a7bcb8a7e2d39d3286` | 5065294 (05:14:17) |
| 2 | 1 bis | Otro pago de 50 USDC a Mamá el mismo día → **Rechazado por el contrato** | dueña | sin hash: no se envió | — |
| 3 | 2 | Pagar 150 USDC a una cuenta nueva → **Retenido** (`queue`) | dueña | `c47124b30a16413da3d398de4f3195cc7b75384cd94639eb7dd8e8f9077f393a` | 5065341 (05:18:12) |
| 4 | 3 | Guardián: **Detener** ese pago (`cancel`) | guardián | `abf88a823f2d5a04b105ec66b0bc2e06a1edbc0477b2b62a237b4fa877ac0280` | 5065354 (05:19:17) |

- [FACT] Horizon confirma que las operaciones 0, 1, 3 y 4 son `invoke_host_function` con las funciones `transfer`, `transfer`, `queue` y `cancel`. Las dos `transfer` mueven 50 USDC de `CBGZSY…` a `GBREUZ…`.
- [FACT] El pago 2 no tiene ninguna transacción en Horizon, ni exitosa ni fallida, desde la clave de la dueña. La app lo rechazó antes de enviarlo: `SubmitResult.Rejected(null, …)` sale de la simulación fallida (`SubmitClassifier.simulationFailed`).
- [INFERENCE] El motivo es el tope diario. La transacción 1 ya había gastado los 50 USDC del día UTC, y la app mostraba "te quedan 0 USDC de tu tope de hoy". El código de error no quedó en el log, porque logcat se limpió antes de la escena 2.
- [FACT] La app mostraba 400 USDC (500 − 50 − 50) antes de la escena 2. El pago de 150 quedó detenido y no salió.

## Tiempos

Medidos por el guion en el host y por las marcas de tiempo de logcat (`BiometricService`, `NotificationManager`).

| Paso | Tiempo |
| --- | --- |
| Escena 2: de Inicio a "Retenido" | 115,6 s en total. Incluye 65,2 s del primer diálogo de huella, que esperó a la persona. |
| Escena 2: de la segunda huella a "Retenido" en pantalla | unos 15,5 s (05:18:05,7 → 05:18:21) |
| Escena 3: aviso tras ver "Retenido" | 16,8 s. La notificación se publicó a las 23:18:40,8 locales. |
| Escena 3: de tocar el aviso a "Detenido" | 37,8 s. La huella tardó 5,1 s (una lectura fallida y luego la buena), y "Detenido" apareció 13 s después. |
| Escena 6: de Inicio a "Política: GuardianHold" | 19,3 s |

- [FACT] El guardián detuvo el pago 65 s después del `queue` (05:18:12 → 05:19:17), dentro de la ventana de unos 2 minutos. La app mostraba "Retenido hasta las 23:20".
- [FACT] El aviso llegó en menos de 60 s, que es el objetivo de P8, con el destino `GALV…` leído de la cadena (`physical-phone/p1-s3-aviso-recorte.png`).

## Hallazgos

- [FACT] **Pagar pide la huella dos veces; detener, una.** Logcat registra dos `authenticate` de la app para el `queue`: a las 23:16:52 (resuelto a las 23:17:57) y a las 23:18:03 (resuelto a las 23:18:05). Para el `cancel` registra uno solo (23:19:07).
  - [INFERENCE] La causa está en `KmpStellarGateway.submit`. La clave de la dueña firma la entrada de autorización de la smart account (línea 226) y, como también es la cuenta origen que paga la comisión, firma el sobre de la transacción (línea 240). Con `setUserAuthenticationParameters(0, …)`, cada firma pide su propia huella. El guardián es una cuenta G, así que su `cancel` solo lleva la firma del sobre.
  - [UNVERIFIED] Por qué el ensayo del día 6 en el emulador pasó escribiendo el PIN una sola vez.
  - Para la demo: avisar en voz alta de que la huella se pide dos veces. [RECOMMENDATION] Después de la entrega, valorar un tiempo de validez corto (unos 10 s) en la clave de la dueña. Es un cambio de seguridad, así que no entra en la congelación.
- [INFERENCE] **El primer diálogo esperó 65 s y la retención salió igual.** La firma de autorización caduca a los 60 ledgers (unos 5 min, `AUTH_VALIDITY_LEDGERS`). Una espera más larga que eso no se probó.
- [FACT] **Sin errores de la app en logcat.** Durante las escenas 2, 3 y 6 no hay líneas `E`/`F` ni excepciones del proceso de la app. Tampoco hay en todo el log ninguna cadena con forma de semilla Stellar (`S` + 55 caracteres base32).
- [FACT] **La notificación se publica con `vis=PRIVATE`.** [INFERENCE] Así, Android no muestra el destino ni el monto en la pantalla de bloqueo. No se comprobó con el teléfono bloqueado.

## Archivos (`physical-phone/`)

| Archivo | Contenido |
| --- | --- |
| `p-00-entrada.png` | Entrada en modo testnet (sin el aviso de simulación) |
| `p1-s2-revisa.png`, `p1-s2-retenido.png` | Escena 2 |
| `p1-s3-aviso-recorte.png` | La notificación del guardián, recortada |
| `p1-s3-detalle.png`, `p1-s3-confirmar.png`, `p1-s3-detenido.png` | Escena 3 en la vista del guardián |
| `p1-s6-reglas.png` | Escena 6: "Esta cuenta tiene 1 regla", la firmante, la política y "No es firmante de esta cuenta" |
| `rehearsal-p1.log` | Tiempos, en hora local del host (UTC−6) |
| `phone.py` | El guion usado. Es `rehearsal/rehearse.py` con `pin()` cambiado para esperar la huella de la persona. |

**No se suben, por privacidad:**
- La captura completa del panel de notificaciones y el vídeo de las escenas 2 y 3. Muestran notificaciones personales de otras apps del teléfono.
- El vídeo de la escena 6, que no se revisó cuadro por cuadro.
- El logcat completo, que es de todo el sistema.

Quedan en la máquina de CHMP987.

## No corrido

- Dos teléfonos físicos. Dos dispositivos (A54 más emulador) corrió el 8 oct: `two-devices-android.md`.
- La escena 1 con el guion (se hizo a mano), y sus tiempos.
- Reiniciar la app entre escenas: los pagos inmediatos siguen viviendo solo en memoria (`rehearsal.md`).
- La IA: sigue siendo un stub.
- Una espera de huella de más de 5 minutos.
