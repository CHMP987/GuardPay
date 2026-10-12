# Día 10: escenas 2 y 3 en dos dispositivos Android (A54 dueña, emulador guardián)

**Veredicto: la dueña retuvo un pago desde el Galaxy A54 y el guardián lo detuvo desde otro dispositivo, el emulador, que solo tiene la clave del guardián.** El aviso llegó al emulador 7,8 s después de que el A54 mostrara "Retenido", y 17 s después del ledger del `queue`. No fueron dos teléfonos físicos: el guardián fue un emulador.

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, A54 físico más emulador. Hashes copiados de Horizon, los dos `successful`. |
| Fecha | 2026-10-08, 16:06–16:08 UTC (10:06–10:08 hora local del A54, UTC−6), ledgers 5090403–5090415 |
| Dueña | Samsung Galaxy A54 (SM-A546E), Android 14, Keystore `guardpay-owner`. **La huella la puso la persona.** |
| Guardián | Emulador `Medium_Phone_API_37.0` (API 37), Keystore `guardpay-guardian`, PIN de prueba del AVD |
| App | APK de debug de `8285692` en los dos dispositivos. Ese commit agrega los roles por dispositivo en `Wiring.kt`. |
| Quién tocó | `two-devices/two.py` manejó los dos dispositivos por adb y uiautomator. La persona solo puso la huella. Después de la corrida, el número de serie del A54 escrito en el guion pasó a ser un argumento, y el log ahora se escribe junto al guion. No cambió nada más. |

## Cómo se arma

1. Cada dispositivo crea sus dos claves en el Keystore y escribe sus direcciones G en `keys.json`, como siempre.
2. `DeviceProvisioningLiveTest` se corrió con `-Pgp.owner=` la clave de dueña del **A54** y `-Pgp.guardian=` la clave de guardián del **emulador**.
3. Se copió el mismo `testnet.json` a los dos dispositivos.
4. `Wiring.realSetup` compara el archivo con las claves de cada dispositivo:
   - el A54 solo coincide como dueña: registra `owner's phone`, tiene solo la sesión de dueña y no arranca el sondeo;
   - el emulador solo coincide como guardián: registra `guardian's phone`, tiene solo la sesión de guardián, arranca `HoldWatchService` y deja "Entrar a mi cuenta" apagado ([emu-00-entrada](two-devices/emu-00-entrada.png)).

En ningún momento hay una clave del otro dispositivo en este: cada uno firma solo con la suya.

## Cuenta

```
account   CA5Z7JZSGBUXGVV7FDW5B6M66PJCAXHYIQJDG7GXZST3VFTOQE4ARA2J
registry  CDW5KKA753R5HGJ7QF52WTKT2HHB4OQ3STUFLOK5FBYB5HMCVG2QTAX6
policy    CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI
usdc      CBLQQ7PQUMD6EHWYDXVUYZV2RYRVOMB7XMCGC73EMNXCJOK737KJ3IXU   (SAC de prueba, no el USDC de Circle)
owner     GDA3N4HGO2DU6K5C5LV6Z2MNZ47AARFMHZN2UMM23JJMLMXYIKJTPJXD   (Keystore del A54)
guardian  GCCKC6JFAA3J2TWUOF32S4TERWZFGSF7NDHTDJ7FBZK5KWHV5VGJE4ZI   (Keystore del emulador)
stranger  GDLCCVSYCCGT3AW2CX5NDB457WYOCNYUY4CCN22KCQDD2LQFRTJLF2NB
```

- [FACT] Se provisionó el 8 oct a las 15:55 UTC. Hashes de preparación: SAC `d9358eeb…`, trustlines `266222c6…` y `cd810ecb…`, mint `d8bed3ed…`.
- [FACT] El test leyó la cuenta en la cadena: una regla, la dueña como única firmante y la política GuardianHold. El guardián no es firmante.
- Es **otra cuenta de testnet**. No es la de la demo (`CDWPPTDM…`) ni la del A54 del día 8 (`CBGZSY…`). El `testnet.json` anterior del A54 quedó respaldado fuera del repo. Qué cuenta usa la demo final sigue siendo decisión del equipo (T-005).
- [FACT] La clave de guardián del emulador es la misma que firmó los `cancel` de la cuenta de la demo el 6 oct (`bb1f4dd7…` y `fb9eafad…` sobre `CDWPPTDM…`, según Horizon). El emulador ya no tenía el `testnet.json` de esa cuenta.

## Transacciones

| # | Escena | Dispositivo | Acción | Firmante | Hash | Ledger (UTC) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 2 | A54 | Pagar 150 USDC a una cuenta nueva → **Retenido** (`queue`) | dueña | `4b85325ef71944c71e091b03a4aa31625edcb6184e241de5e2482133d95a758a` | 5090403 (16:06:42) |
| 2 | 3 | emulador | **Detener** ese pago (`cancel`) | guardián | `7997307795902fd0d9d3ef104563d0df03025c1602212550d160ca8ee42be1c0` | 5090415 (16:07:42) |

- [FACT] Según Horizon, las dos son `invoke_host_function` sobre el registro `CDW5KKA…`, con las funciones `queue` y `cancel`, y la cuenta `CA5Z7JZS…` como argumento.
- [FACT] La `queue` la envió `GDA3N4…`, la clave del A54. El `cancel` lo envió `GCCKC6…`, la clave del emulador.
- [FACT] La cuenta desconocida `GDLCCV…` tiene 0 USDC. Horizon no muestra más operaciones suyas que su creación y su trustline.

## Tiempos

Medidos por el guion en el host (`two-devices/two.log`).

| Paso | Tiempo |
| --- | --- |
| A54: de Pagar a "Retenido" en pantalla | 46,1 s. Incluye escribir la dirección y el diálogo de huella. |
| A54: de "Firmar y retener" a "Retenido" | 19,8 s. Incluye la huella de la persona. |
| Emulador: aviso tras ver "Retenido" en el A54 | **7,8 s**. Fue 17 s después del ledger del `queue` (16:06:42 → 16:06:59). |
| Emulador: de tocar el aviso a "Detenido" | 43,2 s. Incluye abrir Detalle, confirmar y el PIN. "Detenido" apareció 16,2 s después del PIN. |
| A54: "Detenido por Diego" tras el "Detenido" del emulador | 7,8 s, en la pantalla que ya estaba abierta |

- [FACT] El guardián detuvo el pago 60 s después del `queue` (16:06:42 → 16:07:42), dentro de la ventana de unos 2 minutos. El A54 mostraba "Retenido hasta las 10:08".
- [INFERENCE] Que el aviso tarde 7,8 s depende de en qué momento del ciclo de 30 s cayó el `queue`. El peor caso esperado sigue siendo unos 30 s más el tiempo de la lectura RPC.

## Capturas (REAL, reducidas al 50 %)

En las del A54 se recortó la barra de estado, que mostraba iconos de otras apps.

| Captura | Qué muestra |
| --- | --- |
| [emu-00-entrada](two-devices/emu-00-entrada.png) | Emulador: Entrada del teléfono del guardián, "Entrar a mi cuenta" apagado y sin aviso de simulación |
| [a54-01-inicio](two-devices/a54-01-inicio.png) | A54: Inicio de la dueña recién abierto. Se capturó antes de la primera lectura, así que el saldo sale "—" y las reglas "Todavía no se pudo leer". |
| [a54-02-revisa](two-devices/a54-02-revisa.png) | A54: Revisa tu pago, "Firmar y retener" |
| [a54-03-retenido](two-devices/a54-03-retenido.png) | A54: Retenido hasta las 10:08, "Diego puede detenerlo" |
| [emu-04-aviso](two-devices/emu-04-aviso.png) | Emulador: "Pago retenido: 150 USDC", con el destino completo leído de la cadena |
| [emu-05-detalle](two-devices/emu-05-detalle.png) | Emulador: Detalle del guardián, "Detener este pago" |
| [emu-06-detenido](two-devices/emu-06-detenido.png) | Emulador: Detenido por ti |
| [a54-07-final](two-devices/a54-07-final.png) | A54: Detenido por Diego |

El diálogo de huella no sale en las capturas: `screencap` lo muestra negro.

## No corrido

- Dos **teléfonos físicos**. Aquí el guardián fue un emulador. Con el iPhone de Abraham como guardián no se puede todavía: iOS solo corre en simulación (plan de Persona 2, §7.1).
- La escena 1 (contacto) y la escena 6 en esta cuenta.
- El aviso con el emulador en reposo o con la app del guardián cerrada a la fuerza.
- Las partes de terminal de las escenas 4, 5 y 6 en esta cuenta.
