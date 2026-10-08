# P8: aviso al guardián sin backend (día 5)

**Veredicto: VERDE en emulador, con la app viva.** Cada pago retenido produjo una notificación local en menos de 60 s. El destino y el monto se leen del registro en cadena. Al tocarla se abre el Detalle del guardián, que vuelve a leer del RPC. No hay servidor, FCM, APNs ni endpoint propio: el teléfono consulta el RPC público de testnet. **No** se midió con la app cerrada por el sistema ni en un teléfono físico.

| | |
| --- | --- |
| Etiqueta | **REAL** (testnet + emulador; tiempos medidos, textos copiados de `dumpsys notification`) |
| Fecha | 2026-10-06, 14:25–14:51 UTC |
| Dispositivo | Emulador Android, AVD `Medium_Phone_API_37.0` (API 37). **No es un teléfono físico.** |
| Código | `app/shared/.../ui/guardian/HoldWatcher.kt`, `app/android/.../notifications/HoldWatchService.kt`, `app/android/.../notifications/HoldNotifications.kt` |
| Tests | `HoldWatcherTest` (3), dentro de `:app:shared:allTests`: 338 OK |

## Cómo funciona

1. `HoldWatcher` (commonMain) solo lee: llama a `readHolds()` y devuelve una alerta por cada registro `Held` que no se haya avisado antes. No puede firmar ni cancelar. Si la lectura falla, lanza una excepción, así que un fallo nunca parece "nada nuevo".
2. `HoldWatchService` es un servicio en primer plano de tipo `dataSync`. Llama a `check()` cada **30 s** (`INTERVAL_MS`). Mientras corre, muestra un aviso fijo: "Revisando pagos retenidos · Lee Stellar cada 30 segundos". Guarda los ids ya avisados por cuenta en `SharedPreferences`, para no repetirlos tras un reinicio.
3. Cada alerta se publica en el canal `holds` (importancia alta). El título y el texto salen del registro leído en ese mismo ciclo, nunca de un payload ni de un caché.
4. Las notificaciones van en el grupo `com.guardpay.android.HOLDS`, con un resumen "Pagos retenidos".
5. Tocar una alerta abre Entrada → Lista del guardián → **Detalle del guardián** de ese hold. Tocar el resumen abre la **Lista del guardián**. Las dos pantallas leen de nuevo del RPC al abrirse.
6. La lista del guardián tiene el botón **Actualizar**, para no depender del servicio.

Texto real de una alerta (de `dumpsys notification`, hold 0):

```
title: Pago retenido: 150 USDC
text:  A GC2TXZHP5K2C3Y57ROOGOKNDKW4ZK3NSFGD6NMHUDMYWTF6WJHUR4HXP. Puedes detenerlo hasta las 14:27.
channel: holds, importance 4, groupKey com.guardpay.android.HOLDS
```

El destino va completo, como en Detalle: una forma corta dejaría pasar una dirección parecida.

## Latencia: del cierre del ledger a la notificación publicada

**Método.**

- **Inicio:** la hora de cierre del ledger que incluye el `queue`, tomada de Horizon.
- **Fin:** la hora en que la notificación aparece publicada en el emulador, tomada de `dumpsys notification` y logcat.
- **Corrección de reloj.** Una consulta NTP mostró el reloj del host 5,5 s atrasado. El emulador iba 0,26 s detrás del host. A cada hora del teléfono se le suman **5,76 s** para llevarla a UTC real.
- **Resolución.** El cierre del ledger tiene una resolución de 1 s, así que cada medida es ±1 s.

| Hold | Monto | Ledger (cierre UTC) | Estado de la app al encolar | Latencia |
| --- | --- | --- | --- | --- |
| 0 | 150 USDC | 5054626 (14:25:17) | en primer plano | 7,7 s |
| 1 | 60 USDC | 5054644 (14:26:47) | no anotado | 11,6 s |
| 2 | 20 USDC | 5054852 (14:44:07) | no anotado | 21,3 s |
| 3 | 5 USDC | 5054881 (14:46:32) | enviada al inicio de Android justo tras firmar | 1,9 s |
| 4 | 7 USDC | 5054922 (14:49:57) | en el inicio de Android | ≈16,5 s (teléfono 14:50:07.753 + 5,76 s) |

- [FACT] En las 5 medidas, la latencia fue de 1,9 a 21,3 s. Todas están por debajo de 60 s.
- [INFERENCE] La cota esperada es el intervalo (30 s) más una lectura del RPC. La latencia depende de en qué punto del ciclo cae el ledger, no de si la app está al frente. El servicio sigue vivo en los dos casos.
- [FACT] El log del servicio ("hold N notified Ns after the ledger recorded it") resta la hora del **teléfono** a la del ledger. Con el reloj desfasado llegó a dar valores negativos (−4 s). No se usa como medida. La tabla usa la hora corregida.

Los hashes de cada `queue` están en `evidence/smart-account/signing.md` (filas 1, 2, 6, 7 y 8).

## Enlaces profundos

| Prueba | Resultado |
| --- | --- |
| Tocar la alerta única "Pago retenido: 7 USDC" | Abre el Detalle del guardián del hold 4: "Retenido hasta las 14:51. Puedes detenerlo.", 7 USDC, con el motivo de la dueña. Datos leídos del RPC. |
| Tocar el grupo de varias alertas | Abre la Lista del guardián |
| `am start --el com.guardpay.android.HOLD_ID 1` con la app abierta | `onNewIntent` abre el Detalle del hold 1 |
| Detener desde ese Detalle | Hold 1 cancelado: tx `1ee14565…89ce9` (fila 3 de signing.md) |

**Hallazgo y arreglo.** Con varias alertas sin grupo propio, Android las agrupaba solo. Al tocar ese grupo automático se abría el lanzador en vez de la app (`n1-shade.png`). Ahora las alertas llevan un grupo explícito con un resumen que abre la lista (`n3-grupo.png` → `n4-lista.png`). Al tocar el resumen se descartan también las alertas del grupo. Es aceptable porque la lista las muestra todas.

Capturas en `screens/`:

- `n1-shade`: grupo automático, antes del arreglo.
- `n3-grupo`: grupo explícito.
- `n4-lista`: lista abierta desde el grupo.
- `n5-una`: alerta única expandida, con el destino completo y el aviso fijo del servicio.
- `n6-detalle-guardian`: Detalle abierto desde la alerta.
- `g3-lista`: lista con "Actualizar", "Ya detuviste 60 USDC" y "Espera terminada".

## Limitaciones (van al README y al pitch)

- [FACT] **Sin backend no hay aviso garantizado.** Si el sistema detiene el servicio, el aviso se retrasa hasta que el guardián abra la app o toque "Actualizar". Puede detenerlo por ahorro de batería, por gestión agresiva del fabricante o porque el usuario fuerza el cierre. El servicio devuelve `START_STICKY`, pero el sistema decide cuándo reiniciarlo.
- [FACT] **Android 15+ limita los servicios `dataSync`** a unas 6 horas por día. Pasado ese tope, el sistema los detiene. Este servicio no implementa `onTimeout`. [UNVERIFIED] Su comportamiento exacto al llegar al tope no se probó.
- [INFERENCE] **Batería.** Una lectura RPC cada 30 s con un servicio fijo no es aceptable en producción. [RECOMMENDATION] Para producción hacen falta push (FCM/APNs) con un indexador o un intervalo largo (WorkManager, mínimo 15 min) más "Actualizar". Las dos cosas cambian el compromiso de latencia, y la primera añade un backend que este MVP excluye a propósito.
- [INFERENCE] **Ventana de retención.** Si el aviso se retrasa más que la ventana de retención, el guardián llega tarde para detener. La retención en cadena sigue impidiendo el envío antes de `ready_at`, pero no avisa a nadie.
- [FACT] **Las dos claves están en el mismo teléfono** en debug, así que el "guardián" de esta prueba es el mismo emulador que la dueña. El aviso entre dos teléfonos **no se ha probado**.

## No corrido

- Latencia con el servicio detenido por el sistema (Doze, ahorro de batería, tope de 6 h).
- Dos dispositivos (dueña y guardián separados).
- Teléfono físico. Se corrió después, el 7 oct UTC, en un Galaxy A54 con dueña y guardián en el mismo teléfono: la notificación se publicó unos 29 s después del ledger del `queue` (05:18:12 → 05:18:40,8 UTC). Ver `evidence/demo/physical-phone.md`.
- iOS: no existe implementación de iOS del aviso. La app de iOS solo corre en simulación en el simulador del CI (`evidence/ios/simulator.md`).
