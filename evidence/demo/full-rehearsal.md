# Día 10: las seis escenas seguidas, cronometradas, dos veces

**Veredicto: las seis escenas del plan (§13) corrieron dos veces seguidas en una sola sentada y sin fallos: 226,4 s (3:46) y 215,0 s (3:35). Las dos quedaron por debajo de 5 minutos.**

Las escenas de app corrieron en el A54 (dueña) y en un emulador (guardián), sobre la cuenta del día 10. Las de terminal corrieron en vivo dentro de la misma sentada, pero sobre **otra cuenta de testnet**, preparada al inicio con claves desechables.

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, A54 físico más emulador más terminal. Todos los hashes se copiaron de Horizon. |
| Fecha | 2026-10-08, de 16:38:41 a 16:46:33 UTC, ledgers 5090795–5090875 |
| Dueña | Samsung Galaxy A54, Keystore `guardpay-owner`. **La huella la puso la persona**, 4 veces en total. |
| Guardián (app) | Emulador `Medium_Phone_API_37.0`, Keystore `guardpay-guardian`, PIN de prueba del AVD |
| Terminal | `DemoTerminal` (`app/shared/src/androidUnitTest/.../stellar/DemoTerminal.kt`), un `main` de Kotlin con `java -cp`. Al arrancar prepara su propia cuenta, y sus claves de dueña, guardián y extraño solo viven en memoria: no se imprimen ni se guardan. |
| App | El mismo APK de debug del día 10 (`8285692`) |
| Quién tocó | `full-rehearsal/full.py` manejó los dos dispositivos por adb y uiautomator y le pasó cada escena a la terminal por stdin. La persona solo puso la huella. Log completo: [full.log](full-rehearsal/full.log). |

## Cuentas

| Uso | Cuenta | Registro | Firmante | Guardián |
| --- | --- | --- | --- | --- |
| Escenas de app (1, 2, 3 y la pantalla de la 6) | `CA5Z7JZSGBUXGVV7FDW5B6M66PJCAXHYIQJDG7GXZST3VFTOQE4ARA2J` | `CDW5KKA…TAX6` | `GDA3N4…` (Keystore del A54) | `GCCKC6…` (Keystore del emulador) |
| Escenas de terminal (2, 4, 5 y 6) | `CBE2VJ37HF6JM3CQHBPRPYOAKFGGWVKUWYWT67ISQ5EBBBKAD6NWXLRB` | `CA3TNH2IY2AJZ73GQNT6NSOPAJCGRWHSRTTY44FGNUUMRXEQJ5MPQQKK` | `GCUSBH…` (en memoria) | `GBQKLI…` (en memoria) |

- [FACT] Las dos cuentas usan el wasm de `account` `986956cc…cf3b` y el de `hold_registry` `031173c2…d0cb`, y tienen la misma política, `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI`.
- [FACT] Al arrancar, la terminal leyó su cuenta en la cadena: `reglas: 1, firmantes: [GCUSBH], guardián firmante: false`.
- **Por qué hay dos cuentas.** Las claves del Keystore no salen de los teléfonos, así que la terminal no puede firmar con ellas. La escena 5 ("el guardián firma un transfer hacia sí mismo") la firmó la clave de guardián desechable de la terminal, **no** la del emulador. Preparar la cuenta de la terminal llevó 60 s, de 16:37:41 a 16:38:41, y quedó **fuera** del tiempo.

## Tiempos

Los tomó el guion en el host. Cada búsqueda en pantalla es un volcado de uiautomator de alrededor de 1 s, y eso cuenta dentro de los tiempos.

| Escena | Corrida 1 | Corrida 2 |
| --- | --- | --- |
| 1. Pagar 10 a Mamá → Enviado | 37,9 s. "Enviado" llegó 21,3 s después de "Firmar y enviar". | 36,6 s (20,4 s) |
| 2. Retener 150 a una cuenta nueva → Retenido; envío inmediato rechazado | 68,6 s: 51,5 s en la app y 15,7 s en la terminal | 73,9 s: 54,2 s y 18,3 s |
| 3. El guardián lo detiene desde el aviso → Detenido | 53 s. El aviso ya estaba a los 17,4 s del Retenido. "Detenido" salió 15,8 s después del PIN. El A54 mostró Detenido. | 47 s (19,8 s; 15,4 s; el A54 también) |
| 4. Transfer del pago detenido → rechazado | 16,1 s | 12,7 s |
| 5. El guardián firma un transfer hacia sí mismo → rechazado | 4,4 s | 4,5 s |
| 6. `approve` rechazado; `add_context_rule` no existe; la app muestra 1 regla | 44,5 s: 14,1 s en la terminal y el resto para llegar a la pantalla de reglas | 38,5 s (8,1 s) |
| **Total** | **226,4 s (3,77 min)** | **215,0 s (3,58 min)** |

- [FACT] El guion revisa el aviso del emulador solo **después** de que la terminal termina la escena 2. Por eso 17,4 s y 19,8 s son cotas superiores: el aviso pudo llegar antes.
- [FACT] Entre una corrida y la otra no hubo pausa ni ningún arreglo.

## Transacciones de la app (cuenta `CA5Z7JZS…`)

Todas aparecen como `successful` en Horizon.

| Corrida | Escena | Acción | Firmante | Hash | Ledger (UTC) |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | `transfer` de 10 a Mamá en el SAC `CBLQQ7PQ…` | dueña (A54) | `3bc84bcb6992f7df1c8b40d2f67c2c88faf3a6cca3f2d8ea2aa84438329734fd` | 5090795 (16:39:22) |
| 1 | 2 | `queue` de 150 en el registro `CDW5KKA…` | dueña (A54) | `f8f0f100874f982c4debb15e048a83890c1f60a39cbb51837d1c402255503f8d` | 5090806 (16:40:17) |
| 1 | 3 | `cancel` | guardián (emulador) | `ca727e0cec94e3d68a62be3b4c24ef5830e61c3bac01deec7f2807b17cb3f981` | 5090819 (16:41:22) |
| 2 | 1 | `transfer` de 10 a Mamá | dueña (A54) | `cf8634aeb541c68d8a02dfdffdd56897bee4fea7ff78bc9d516543d304d40d72` | 5090844 (16:43:27) |
| 2 | 2 | `queue` de 150 | dueña (A54) | `97760c203cbc65e8e0968416710f2e72dc371bbb3716869a19888d0a68ad5ab0` | 5090855 (16:44:22) |
| 2 | 3 | `cancel` | guardián (emulador) | `4d08f3ce2bab5aa098454fbd62b4dea785a28528d07e7e2e609e65bf01eeff17` | 5090868 (16:45:27) |

- [FACT] En Horizon, los dos `cancel` los envió `GCCKC6…`, la clave del emulador. Los `transfer` y los `queue` los envió `GDA3N4…`, la del A54. Las direcciones de contrato salen de decodificar los parámetros de cada operación.

## Transacciones de la terminal (cuenta `CBE2VJ37…`)

En las escenas que se rechazan, la terminal hace dos cosas:

1. Simula `enforce` con la misma autorización, y de ahí saca el código del contrato.
2. Envía la transacción de todos modos, con la huella armada a mano, para que el rechazo quede en la cadena.

| Corrida | Escena | Acción | Firmante | Código en la simulación | Resultado en Horizon | Hash | Ledger |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 2 | `queue` de 150 al extraño | dueña | — | `successful` | `f89b6a714262384b33e42ba3a6f380b36a8628ab4e6ceef79e6370e0b6974636` | 5090809 |
| 1 | 2 | `transfer` de esos 150, sin esperar | dueña | 7 `HoldNotReady` | **failed** | `bcbf23b1eb2cc3bcd993ee5bba9d3e14fb78c6e8e3418e88515af0d1349cabb5` | 5090811 |
| 1 | 4 | `cancel` del hold #0 | guardián | — | `successful` | `2d76533a048ca8d47eb7724d2bfc0f87f9332d4056d361f0bc9f809d64dcdf5c` | 5090823 |
| 1 | 4 | `transfer` del pago detenido | dueña | 3 `NotAllowed` | **failed** | `6ac460061dd92e0a044b82fb5e07867315e74da08ee4af449d8b1a894cbdc979` | 5090825 |
| 1 | 5 | `transfer` de 20 hacia el guardián | guardián | 3016 `UnauthorizedSigner` | **failed** | `4d9c20844d04e978b6a403ac75a4a39a3d79ba3da6f50e44970383db9e634a67` | 5090826 |
| 1 | 6 | `approve` de 25 al guardián | dueña | 3 `NotAllowed` | **failed** | `ea483077215fc099bc3e2e665bed695f02829eceef7842d52c06050c4fbd8224` | 5090828 |
| 2 | 2 | `queue` de 150 al extraño | dueña | — | `successful` | `23651a9422f599d02440097bc8c7abd1a200bee293030147b7e308995192e94a` | 5090859 |
| 2 | 2 | `transfer` sin esperar | dueña | 7 `HoldNotReady` | **failed** | `881318697b0194b9d1266006fb80a64370b0a8fc8f12171d28729f43973be54e` | 5090861 |
| 2 | 4 | `cancel` del hold #1 | guardián | — | `successful` | `02fd63ea9d4a261d9b64747dc4f62e4718df37adafceb8a30a0f71417c72c7c8` | 5090872 |
| 2 | 4 | `transfer` del pago detenido | dueña | 3 `NotAllowed` | **failed** | `9d15a663f0ddc76336383ac4245743ce3236deebc8e463592c60456718ecf0b4` | 5090873 |
| 2 | 5 | `transfer` de 20 hacia el guardián | guardián | 3016 `UnauthorizedSigner` | **failed** | `485b649e757fe42f315b915ced8d0e1aab44ebeef2a8ba6fbb6d5d3427a2c65b` | 5090874 |
| 2 | 6 | `approve` de 25 al guardián | dueña | 3 `NotAllowed` | **failed** | `c5beca110cc00128c12bec53988b82c2e53a123288a38ff2a71f071e16ba974f` | 5090875 |

- **`add_context_rule`, escena 6.** La simulación devolvió `HostError: Error(WasmVm, MissingValue)` en las dos corridas, porque la cuenta no tiene esa función. No se envió ninguna transacción. Después, la terminal volvió a leer `reglas de la cuenta: 1`.
- **Qué está probado y qué se infiere.**
  - [FACT] Horizon marca como fallidas las 8 transacciones rechazadas, y el RPC devolvió `invoke_host_function: trapped`.
  - [FACT] El RPC de testnet no devuelve eventos de diagnóstico, así que el código de error no se ve en la cadena.
  - [FACT] Los códigos de la tabla salen de simular `enforce` con la misma autorización, igual que en `scene-2.md` a `scene-6.md`.
  - [INFERENCE] El fallo en la cadena corresponde a ese código.
- La terminal hace su propio `cancel` en la escena 4 porque el hold de la escena 3 está en la cuenta de la app. Necesita un pago detenido en su propia cuenta.
- Antes de las dos corridas hubo un ensayo en seco de la terminal sobre otra cuenta desechable, `CCZ6DWLSET2B7AYLL2FTA26JROA7XVXIZAZ6ZQWIMQV64F3PDQ5NQKM4`, con los mismos resultados (ledgers 5090699–5090706). No forma parte del tiempo.

## Capturas (REAL, reducidas al 50 %)

En las del A54 se recortó la barra de estado. Hay una captura por escena y corrida, de `r1-…` a `r2-…`:

| Captura | Qué muestra |
| --- | --- |
| [r1-1-enviado](full-rehearsal/r1-1-enviado.png), [r2-1-enviado](full-rehearsal/r2-1-enviado.png) | A54: Pago a Mamá, Enviado |
| [r1-2-retenido](full-rehearsal/r1-2-retenido.png), [r2-2-retenido](full-rehearsal/r2-2-retenido.png) | A54: Retenido, "Diego puede detenerlo" |
| [r1-3-aviso](full-rehearsal/r1-3-aviso.png), [r2-3-aviso](full-rehearsal/r2-3-aviso.png) | Emulador: aviso "Pago retenido: 150 USDC" |
| [r1-3-detenido](full-rehearsal/r1-3-detenido.png), [r2-3-detenido](full-rehearsal/r2-3-detenido.png) | Emulador: Detenido por ti |
| [r1-3-telefono](full-rehearsal/r1-3-telefono.png), [r2-3-telefono](full-rehearsal/r2-3-telefono.png) | A54: Detenido por Diego |
| [r1-6-reglas](full-rehearsal/r1-6-reglas.png), [r2-6-reglas](full-rehearsal/r2-6-reglas.png) | A54: "Esta cuenta tiene 1 regla", la política GuardianHold y "Diego… No es firmante de esta cuenta" |

La terminal no tiene capturas: su salida completa está en `full.log`. El diálogo de huella no aparece en ninguna captura, porque `screencap` lo muestra negro.

## No corrido

- **Una sola cuenta para todo.** Las escenas de terminal no corrieron sobre `CA5Z7JZS…`, porque las claves del Keystore no salen del teléfono.
- La escena 5 firmada con la clave de guardián del emulador.
- Dos teléfonos físicos: el guardián fue un emulador.
- Una persona presentando en lugar del guion. Los tiempos son del guion, que toca más rápido en algunos pasos y espera los volcados de uiautomator en otros.
- Un vídeo continuo de esta sentada: solo hay capturas y el log.
