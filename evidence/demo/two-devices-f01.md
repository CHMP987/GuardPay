# F-01 en dos dispositivos: reprovisión y retención a un contacto dentro del tope (13 oct)

**Veredicto: con la política F-01, la dueña retuvo desde el teléfono un pago de 10 USDC a un contacto de confianza (dentro del tope). El guardián lo vio en el emulador y no lo detuvo. Cuando maduró, la dueña lo envió con una segunda firma, y el tope del día siguió en 50.** Con la política del 6 oct ese envío salía por el tope y el registro quedaba `Retained` ([F-01](../../AUDIT_REPORT.md#f-01)).

Primero se reprovisionaron los dos dispositivos con una cuenta nueva (abajo) y después se corrió la prueba ([Prueba F-01](#prueba-f-01-retención-a-un-contacto-dentro-del-tope)).

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, teléfono físico más emulador. Hashes de preparación consultados en Horizon, los cuatro `successful`. |
| Fecha | 2026-10-13, 14:58:52–14:59:17 UTC, ledgers 5175989–5175994 |
| Dueña | Teléfono, Keystore `guardpay-owner` |
| Guardián | Emulador `Medium_Phone_API_37.0` (API 37), Keystore `guardpay-guardian`. Es la misma clave de guardián de la cuenta de dos dispositivos del 8 oct ([`two-devices-android.md`](two-devices-android.md)). |
| Política | `CBTIF2BF7YGCR6OT7TQMHGGMY6BVHKPOAJV5BYZQ2P3IWKSEXYHLWFMW`, de la [release F-01](../stellar/deployment.md#release-f-01-12-oct-2026) |
| App | El APK de debug que ya estaba instalado en cada dispositivo. No se reinstaló. |

## Cómo se armó

1. Se leyó el `keys.json` de cada dispositivo.
2. `DeviceProvisioningLiveTest` se corrió con `-Pgp.owner=` la clave de dueña del teléfono y `-Pgp.guardian=` la clave de guardián del emulador. Esa prueba usa las constantes de `TestnetProvisioner`, que desde la release F-01 apuntan a `CBTIF2BF…`.
3. Se copió el mismo `testnet.json` a los dos dispositivos. Los `testnet.json` anteriores quedaron respaldados fuera del repo.
4. Se cerró y abrió la app en cada uno.

## Cuenta

```
account   CB4PUYVYMQ24CFHUMHGDJI3EAPDX7ZPG4FW74CXL3QIYQNNW3UUOAYYI
registry  CAC7OJL76QBBFYCYEBIJVDQDK4Y2WFNWSPKALOSUM5YJPP6CTT5ETH3K
policy    CBTIF2BF7YGCR6OT7TQMHGGMY6BVHKPOAJV5BYZQ2P3IWKSEXYHLWFMW
usdc      CBAVMFUZDGC35VOTTDGXTGDEGRVGFBNGZLXY5XIV3CIOHKC65SQ7IBK7   (SAC de prueba, no el USDC de Circle)
owner     GABPAWVGD5QFDNUGG5P5BETPUTBSKYQCQ35AGZGCTVDP4H6QKKOXGRRW   (Keystore del teléfono)
guardian  GCCKC6JFAA3J2TWUOF32S4TERWZFGSF7NDHTDJ7FBZK5KWHV5VGJE4ZI   (Keystore del emulador)
contact   GDJQ77JQGVV3WUBUSQ3QSFY6WFVPG56HNO4HRNBU63JEV6CF22OCLSMO
stranger  GAMEDE63CPID42ZUNWDKPBEO2YZKT2X2ARCHIJG53XJYPPTZ7JQMPMLC
```

| Paso | Hash | Ledger (UTC) |
| --- | --- | --- |
| Desplegar el SAC de prueba | `fc243cefcc7669267ca0bbaf6ea6396a7475b4b0993c89ce437da4d93e085a0e` | 5175989 (14:58:52) |
| Trustline | `c7ea2915c2da68c88e0bafa37f7c35e4a132ebcc69e435d3af4b9a5239fbd0ad` | 5175990 (14:58:57) |
| Trustline | `773c4eb9322ec79217f8ff6848c03ba56ce5af1c8703e2cbcea539112429b4f6` | 5175991 (14:59:02) |
| Mint de 500 a la cuenta | `845be23b0f9c29d853bfed1dad2fa526dd3011775024ccac726269f743e6ffd2` | 5175994 (14:59:17) |

- [FACT] El test leyó la cuenta en la cadena: `contextRuleCount=1`, `signers=[GABPAWVG…]` (la dueña) y `policies=[CBTIF2BF…]`. El guardián no es firmante.
- [FACT] La prueba terminó con `BUILD SUCCESSFUL`.

## Roles en cada dispositivo

- [FACT] Teléfono: `GuardPayWiring` registró `owner's phone`. En la Entrada, "Soy guardián de alguien" está apagado ([tel-00-entrada](two-devices-f01/tel-00-entrada.png)).
- [FACT] Emulador: `GuardPayWiring` registró `guardian's phone`, y `dumpsys` muestra `HoldWatchService` en primer plano. En la Entrada, "Entrar a mi cuenta" está apagado ([emu-00-entrada](two-devices-f01/emu-00-entrada.png)).
- [FACT] Ninguno de los dos registró `testnet.json rejected` ni `provisioned for other keys`.

En las capturas (REAL, reducidas al 50 %) se recortó la barra de estado del teléfono.

- La cuenta de dos dispositivos del 8 oct (`CA5Z7J…`) y la del emulador de la demo (`CDWPPT…`) siguen en testnet con la política anterior. Ningún dispositivo apunta ya a ellas.

## Prueba F-01: retención a un contacto dentro del tope

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, teléfono físico más emulador. Hashes copiados de Horizon, los dos `successful`. |
| Fecha | 2026-10-13, 15:20–15:24 UTC (09:20–09:24 hora local del teléfono, UTC−6) |
| Quién tocó | [`f01.py`](two-devices-f01/f01.py) manejó los dos dispositivos por adb y uiautomator. La persona puso la huella dos veces. Un primer intento se cortó antes de firmar porque el teléfono se bloqueó; no envió nada. |
| Pasos | Pagar → Mamá → 10 USDC → la tarjeta dice "Sale ahora" → "Retener este pago" → "Firmar y retener" (huella). El guardián abre el aviso y no lo detiene. Al madurar, "Firmar y enviar" (huella). |

| # | Dispositivo | Acción | Firmante | Hash | Ledger (UTC) |
| --- | --- | --- | --- | --- | --- |
| 1 | Teléfono | Retener 10 USDC a Mamá (`queue`) | dueña | `14782c60f06cc5921cdad52b0937108fc9adba6870b9be5ea602a5bd985e6d12` | 5176250 (15:20:37) |
| 2 | Teléfono | Enviar la retención madura (`transfer`) | dueña | `ed8d5531b0b3114e810352e8909da4b9acc651f6940633e32174c771817c22bd` | 5176283 (15:23:22) |

- [FACT] Según Horizon, las dos las envió `GABPAWVG…`, la clave del teléfono. La segunda es un `transfer` con un cambio de saldo de 10 USDC de la cuenta `CB4PUYVY…` al contacto `GDJQ77JQ…`.
- [FACT] El `transfer` salió 165 s después del `queue` (15:20:37 → 15:23:22), después de que la app mostrara "Listo para enviar". La espera es de 120 s.
- [FACT] El emulador, que solo lee la cadena, pasó el Detalle del guardián de "Retenido · Puedes detenerlo" a "Enviado a Mamá".
- [INFERENCE] El registro quedó `Executed`: la app del guardián toma el estado del registro (`ChainDecoders.kt:90`) y no tiene otra fuente. No se leyó el registro por RPC aparte.
- [FACT] Antes de la prueba, la Inicio del teléfono decía saldo 500 y "Te quedan 50 USDC de tu tope de hoy". Después decía saldo 490, el pago a Mamá como "Enviado" y **"Te quedan 50 USDC"**: la liberación no gastó tope.
- [INFERENCE] Con la política del 6 oct, el mismo `transfer` habría pasado por la rama del tope (quedarían 40) y el registro habría seguido `Retained`, que es lo que encontró la auditoría en la copia aislada.

### Tiempos

Medidos por el guion en el host ([`f01.log`](two-devices-f01/f01.log)).

| Paso | Tiempo |
| --- | --- |
| Teléfono: de "Firmar y retener" a "Retenido" | 24,6 s. Incluye la huella. |
| Emulador: aviso tras ver "Retenido" en el teléfono | 4,4 s |
| Teléfono: de "Retenido" a "Firmar y enviar" | 115,6 s |
| Teléfono: de "Firmar y enviar" a "Enviado" | 38,0 s. Incluye la huella. |
| Emulador: "Enviado" en el Detalle abierto | 15 s después del "Enviado" del teléfono |

### Capturas (REAL, reducidas al 50 %)

En las del teléfono se recortó la barra de estado.

| Captura | Qué muestra |
| --- | --- |
| [tel-01-inicio](two-devices-f01/tel-01-inicio.png) | Teléfono: saldo 500, te quedan 50 de tope |
| [tel-02-sale-ahora](two-devices-f01/tel-02-sale-ahora.png) | Teléfono: 10 a Mamá, "Sale ahora" |
| [tel-03-elegiste](two-devices-f01/tel-03-elegiste.png) | Teléfono: "Con espera · Elegiste retenerlo." |
| [tel-04-revisa](two-devices-f01/tel-04-revisa.png) | Teléfono: Revisa tu pago, "Firmar y retener" |
| [tel-05-retenido](two-devices-f01/tel-05-retenido.png) | Teléfono: Retenido hasta las 09:22, "Diego puede detenerlo" |
| [emu-01-aviso](two-devices-f01/emu-01-aviso.png) | Emulador: aviso "Pago retenido: 10 USDC" |
| [emu-02-detalle](two-devices-f01/emu-02-detalle.png) | Emulador: Detalle del guardián, contacto de confianza, "Detener este pago" (no se tocó) |
| [tel-06-listo](two-devices-f01/tel-06-listo.png) | Teléfono: Listo para enviar, "Firmar y enviar" |
| [tel-07-enviado](two-devices-f01/tel-07-enviado.png) | Teléfono: Enviado a Mamá |
| [emu-03-final](two-devices-f01/emu-03-final.png) | Emulador: Enviado a Mamá |
| [tel-08-inicio](two-devices-f01/tel-08-inicio.png) | Teléfono: saldo 490, te quedan 50 de tope |

El diálogo de huella no sale: no se capturó durante la firma.

## No corrido

- El caso sobre el tope desde los dispositivos (en testnet se corrió con una cuenta desechable: [`gateway-live.md`, corrida 6](../stellar/gateway-live.md#corrida-6-política-f-01-12-oct)).
- Que el guardián ya no pueda cancelar la retención después del envío, en testnet (sí en los contratos, `f01_*`).
- Leer el registro por RPC fuera de la app.
- Reprovisionar la cuenta publicada `CDBJMS…` y la de ataques `CC6FFXGG…`.
