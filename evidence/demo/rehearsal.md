# Día 6: ensayo de las escenas de la app (1, 2, 3 y 6)

**Veredicto: las cuatro escenas que tocan la app salieron dos veces seguidas, en emulador y contra testnet.** Las partes de terminal **no se corrieron** aquí: son trabajo de P9. Eso incluye el envío inmediato rechazado de la escena 2, y el `approve` y la segunda regla de la escena 6. El vídeo de respaldo es el de estos ensayos. Está grabado con `adb screenrecord`, así que el diálogo de huella o PIN sale en negro.

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet + emulador. Hashes copiados de Horizon, todos `successful`. Tiempos medidos por el script. |
| Fecha | 2026-10-06, 15:33–15:50 UTC, ledgers 5055445–5055600 |
| Dispositivo | Emulador Android, AVD `Medium_Phone_API_37.0` (API 37), con PIN. **No es un teléfono físico.** |
| App | APK de debug de este commit, con el `testnet.json` de la cuenta de abajo |
| Guion | `rehearsal/rehearse.py`. Toca la UI con `adb` (uiautomator), escribe el PIN cuando aparece "Confirma con tu huella" y graba cada escena. Lo que se ve es la app real; solo los toques los hace el script. |

## Cuenta de la demo (nueva el día 6)

Se provisionó con `DeviceProvisioningLiveTest` para las mismas claves del Keystore del día 5. Es una cuenta nueva para que la demo no arranque con las tres retenciones que quedaron en la del día 5 (20, 5 y 7 USDC). La evidencia del día 5 sigue valiendo para su propia cuenta.

```
account   CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6
registry  CC5NG54ICWDH7N46RMESSAMXTFAFEL243ZIJC5RNFZ3FFHQ6CHY5NRY2
policy    CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI
usdc      CC2K3NDVHI52YKCDORJVNIZYRYNO2TE6MN3LIK2LWDRIZMJDBEG3VDBH   (SAC de prueba, no el USDC de Circle)
owner     GDIFOBI5NW4N5DHBBYMRLJ766X4EWN6VQABCRUJ6SRMJ3SW2AI56U5UD   (Keystore, guardpay-owner)
guardian  GCCKC6JFAA3J2TWUOF32S4TERWZFGSF7NDHTDJ7FBZK5KWHV5VGJE4ZI   (Keystore, guardpay-guardian)
stranger  GALVMKSSOCH5QOOQHCFI53LBQUIZ2HDED3KYN7MCAAJL7ZQFITADTAXK
```

- Setup: el deploy del SAC es `5932bc0e3c14a95ebb7eda96bf7e30acaf1d15cb9141b3da34f302f52618834f`. Las trustlines y el mint también están en el log del test; aquí no se copiaron completos.
- [FACT] Forma leída al provisionar: `contextRuleCount=1`, `signers=[owner]`, `policies=[CCGIEMIU…]`. La aserción explícita "el guardián no es firmante" pasó contra la red en esta provisión.
- Saldo inicial de 500 USDC y tope diario de 50. El contacto es "Mamá" y el guardián, Diego. La retención dura unos 120 s.

## Transacciones

| Ensayo | Escena | Acción en la app | Firmante | Hash | Ledger (cierre UTC) |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | Pagar 10 USDC a Mamá → **Enviado** (`transfer`) | dueña | `01187e29c1099b386cb6e3d66382db1fcb104aed01081ae05b5033951061e4c5` | 5055445 (15:33:32) |
| 1 | 2 | Pagar 150 USDC a una cuenta nueva → **Retenido** (`queue`, hold 0) | dueña | `c3941e02ecdb8c1123907d1db69ac87c5b10198371d3bcfd91711dcde4d1c6d0` | 5055467 (15:35:22) |
| 1 | 3 | Guardián: **Detener** el hold 0 (`cancel`) | guardián | `fb9eafad4c9ec2934fd4711b933077a1ee9d91ae92314f25f3d193ed688fa56c` | 5055479 (15:36:22) |
| 2 | 1 | Pagar 10 USDC a Mamá → **Enviado** | dueña | `b664e69db168c4aad18e1841874fbb73daecc788e74b29d152843ec80089ed5a` | 5055569 (15:43:52) |
| 2 | 2 | Pagar 150 USDC a una cuenta nueva → **Retenido** (hold 1) | dueña | `269319d0cc4d53aec4d096f8a20eeff9d6960aff8c4e6452005aae05489e568a` | 5055588 (15:45:27) |
| 2 | 3 | Guardián: **Detener** el hold 1 | guardián | `bb1f4dd7105ab3f0ebbbae3b5b45eb1e27e8feaf07755a04b8c262c420521a22` | 5055600 (15:46:27) |

Después de los dos ensayos, la app muestra 480 USDC (500 − 10 − 10) y "Te quedan 30 USDC de tu tope de hoy". Los dos pagos de 150 quedaron detenidos y no salieron.

## Tiempos

Los midió el script en el host. "Tras el PIN" va desde que se escribe el PIN hasta que la pantalla muestra el estado.

| Escena | Ensayo 1 | Ensayo 2 |
| --- | --- | --- |
| 1: de Inicio a "Enviado" | 47,8 s (18,1 s tras el PIN) | 48,9 s (18,4 s) |
| 2: de Inicio a "Retenido" | 61,0 s (18,7 s) | 61,7 s (19,8 s) |
| 3: aviso tras ver "Retenido" | 0,2 s | 0,2 s |
| 3: de tocar el aviso a "Detenido" | 43,0 s (13,4 s) | 39,3 s (9,2 s) |
| 6: de Inicio a "Política: GuardianHold" | 19,4 s | 21,9 s |

- [FACT] Las dos veces, la escena 3 terminó dentro de la ventana de unos 2 minutos. En el ensayo 2, el pago quedó retenido a las 15:45:27 y detenido a las 15:46:27.
- [INFERENCE] El aviso salió casi a la vez que "Retenido" porque el servicio de sondeo estaba en primer plano y su ciclo coincidió. El día 5 la latencia medida fue de 1,9 a 21,3 s (`evidence/guardian/notification.md`). Con un intervalo de 30 s, en el peor caso el aviso llega unos 30 s después.
- [INFERENCE] Las duraciones totales incluyen las esperas del script: uiautomator tarda 1–2 s en cada lectura. A mano, en vivo, deberían ser parecidas o menores.

## Lo que muestra cada escena (texto leído de la pantalla)

- **Escena 1:** "Enviado a Mamá · 15:43", "10 USDC" y "Ver en Stellar".
- **Escena 2:** "Retenido hasta las 15:47. Diego puede detenerlo.", "Destino: dirección que termina en TAXK" y "Podrás enviarlo a las 15:47".
- **Escena 3 (guardián):** "Retenido hasta las 15:47. Puedes detenerlo." y "Cuenta nueva: Laura nunca le ha pagado". Después de detenerlo: "Detenido por ti".
- **Escena 6:** "Esta cuenta tiene 1 regla", "Firmante: la clave de este teléfono", "Política: GuardianHold" y "Tu guardián: Diego (GCCK…E4ZI). No es firmante de esta cuenta."

## Archivos (`rehearsal/`)

| Archivo | Contenido |
| --- | --- |
| `r1-s1.mp4`, `r2-s1.mp4` | Escena 1 |
| `r1-s23.mp4`, `r2-s23.mp4` | Escenas 2 y 3 seguidas, con la notificación |
| `r1-s6.mp4`, `r2-s6.mp4` | Escena 6. En `r1-s6` las reglas quedaron por debajo del borde de la pantalla; `r2-s6` sí las muestra. |
| `rN-s1-enviado`, `rN-s2-revisa`, `rN-s2-retenido`, `rN-s3-aviso`, `rN-s3-detalle`, `rN-s3-confirmar`, `rN-s3-detenido`, `rN-s6-reglas` (.png) | Capturas de cada paso |
| `seal-fix.png` | Inicio después del arreglo del sello "En Stellar" |
| `rehearsal-r1.log`, `rehearsal-r2.log` | Tiempos, en hora local del host (UTC−6) |
| `rehearse.py` | El guion |

**Hashes en el vídeo.** La app no muestra el hash como texto: el Detalle tiene el botón "Ver en Stellar". El plan pide un vídeo de respaldo "con los hashes visibles en pantalla", así que hay que mostrar en el vídeo esta tabla o el explorador. [RECOMMENDATION] Que Ant (P10) monte el vídeo final con los hashes de la tabla de arriba como rótulos, o que grabe "Ver en Stellar" abriendo el explorador en un teléfono con navegador.

## Arreglos de hoy (encontrados al ensayar)

- **Sello partido.** En Inicio, "En Stellar" se partía en "En Stella/r" a 1080 px de ancho. Ahora el texto de al lado ocupa `weight(1f)` y el sello entra entero (`seal-fix.png`).
- **Un fallo de firma ya no se muestra como error de red.** Era la recomendación del día 5 (`signing.md`). El nuevo `SubmitResult.SigningFailed` cubre el caso en que la clave no puede firmar: sin permiso, clave invalidada o error del Keystore. La app dice "No se pudo firmar en este teléfono. No se envió nada." y no ofrece reintentar como si fuera la red. Test: `OwnerSessionTest.aBrokenKeyIsNotShownAsANetworkError`.
- **Textos de passkey.** La app firma con el Keystore, no con una passkey. "Esperando tu passkey…" pasa a "Esperando tu huella o PIN…", y "Entrar con passkey" pasa a "Entrar a mi cuenta". Se regeneraron `screens/00-entrada.png` y `screens/02-firmando.png`.

## Hallazgos que no se arreglaron (congelación)

- [FACT] **Los pagos inmediatos solo viven en memoria.** Un "Enviado" a un contacto desaparece de "Recientes" al reiniciar la app, aunque sigue en cadena (tabla de arriba). Las retenciones sí se vuelven a leer del registro. Para la demo: no reiniciar la app entre la escena 1 y la 6. Arreglarlo exige leer eventos de `transfer`, y eso no entra en "solo arreglos".
- [FACT] **La ventana de retención es de unos 2 minutos.** La escena 3 tiene que empezar en cuanto aparece "Retenido". Si se habla mucho entre la escena 2 y la 3, el pago pasa a "Listo para enviar" y el guardián ya no puede detenerlo.
- [FACT] **El diálogo de huella o PIN sale en negro en el vídeo**, porque el sistema bloquea su captura. Se ve un corte negro de 1–2 s.
- [FACT] **Un fallo del guion, no de la app.** En el primer intento, el script escribió el PIN antes de que saliera el diálogo y el PIN acabó en el campo de monto (10 → 101.234). La app mostró el monto nuevo en "Revisa". Se canceló con "Cambiar" y no se envió nada. Ahora el guion espera al diálogo.

## Las 9 capturas de estado

Las 9 están en `screens/` (`01`–`09`). Son **SIMULATED** y las regeneró hoy `ScreensAt360Test`. Este ensayo añade capturas **REAL** de tres estados:

- Enviado: `rN-s1-enviado`.
- Retenido: `rN-s2-retenido`.
- Detenido: `rN-s3-detenido`.

Firmando no tiene captura REAL porque el diálogo del sistema sale en negro. Borrador corresponde a `rN-s2-revisa`.

## No corrido

- Teléfono físico: todo fue en emulador.
- Desde la terminal (P9, Ant): el envío inmediato rechazado de la escena 2 y los ataques de la escena 6.
- Escenas 4 y 5, que son de terminal.
- La IA: hoy es un stub, así que la escena 2 se hizo sin pegar el mensaje sospechoso.
- Dos dispositivos: la dueña y el guardián están en el mismo emulador.
- Ensayo con una persona hablando encima: solo se midió la app.
