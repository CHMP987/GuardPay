# Dos dispositivos con el registro B-2 (13 oct)

**Veredicto: con la cuenta `CDHRWCRP…`, cuyo registro es el wasm con índice de la [release B-2](../stellar/deployment.md#release-b-2-13-oct-2026), los dos casos corrieron en testnet desde la app.**

- **Retener y detener:** el teléfono (dueña) retuvo 20 USDC a un contacto, por elección, y el emulador (guardián) lo detuvo.
- **Sobre el tope, madurar y enviar:** el teléfono pagó 60 USDC al mismo contacto, por encima del tope. El pago quedó retenido, maduró y la dueña lo envió.

Al terminar, el registro tiene una retención `Stopped` y una `Executed`, y el índice de activas está vacío.

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, teléfono físico más emulador. Hashes consultados en Horizon (por cuenta firmante) y con `getTransaction` en el RPC: los cuatro `SUCCESS`. Estado del registro leído con `getLedgerEntries`. |
| Fecha | 2026-10-13, 22:32–22:56 UTC, ledgers 5181432–5181709 |
| Dueña | Teléfono, Keystore `guardpay-owner`, huella puesta por la persona |
| Guardián | Emulador `Medium_Phone_API_37.0` (API 37), Keystore `guardpay-guardian`, PIN |
| App | El APK de debug ya instalado en cada dispositivo. No se reinstaló. |
| Script | [`two-devices-b2/b2.py`](two-devices-b2/b2.py), con uiautomator. Su log es [`b2.log`](two-devices-b2/b2.log), y las capturas y volcados de texto están en esa carpeta. |

El teléfono muestra la hora local (UTC−6) y el emulador muestra UTC. Por eso el mismo pago dice 16:43 en uno y 22:43 en el otro.

## Transacciones

| Paso | Firma | Hash | Ledger (UTC) |
| --- | --- | --- | --- |
| Retener 20 a Mamá (`queue`, por elección) | Dueña | `2524be7de057cba336e47e2d42a0f3baa8e64aa74a7ac54c8f59d358d9077270` | 5181432 (22:32:27) |
| Detener el 20 (`cancel`, segundo intento) | Guardián | `efe465b652e73d137f157daabf28711dd62b0ac21fe6804727f76753e88d7083` | 5181556 (22:42:47) |
| Retener 60 a Mamá (`queue`, sobre el tope) | Dueña | `0c63af5bc1df7a976fcc7b1718ad43d7428b52e26cc1a77e1330e9cbfc93eb5c` | 5181569 (22:43:52) |
| Enviar el 60 madurado (`transfer`) | Dueña | `4c234fadd1c55bc8598b72f098187562e718d09e2428cebbf108a55b1e525e18` | 5181709 (22:55:32) |

Estado del registro `CB7ARUGT…` para la cuenta, después del envío:

```
Hold(0)       20 USDC  Stopped
Hold(1)       60 USDC  Executed
Hold(2)       no existe
RetainedIds   vector vacío
Active(…20)   no existe
Active(…60)   no existe
```

## Línea de tiempo

| Momento | Qué pasó | Captura |
| --- | --- | --- |
| Inicio | Saldo 500, sin pagos en espera, tope de contactos 50 | `tel-01-inicio` |
| 22:32:41 | El teléfono muestra "Retenido" 29.9 s después de "Firmar y retener", incluida la huella | `tel-02-*` |
| 22:34:16 | Llega el aviso al emulador. **La latencia no vale:** la app del guardián se abrió a mano hacia las 22:34:10, y el sondeo no corre hasta que la app arranca `HoldWatchService` | `emu-01-aviso` |
| 22:35 | Primer intento de detener: el PIN no quedó registrado y la transacción venció. La app mostró "No llegó a Stellar. Nada cambió." No hay transacción del guardián en ese momento. | (en `emu-02-detalle` se ve el aviso) |
| 22:42:56 | Segundo intento: PIN 25.2 s después de "Detener el pago"; "Detenido" en el emulador a los 38.3 s | `emu-03-confirmar`, `emu-04-detenido` |
| 22:43:04 | "Detenido" en el teléfono, 45.5 s después del "Detener el pago" del guardián | `tel-03-detenido`, `tel-04-inicio` |
| 22:44:05 | 60 a Mamá: la app dice "Supera tu tope de hoy con contactos" y ofrece la espera. "Retenido" 21.3 s después de "Firmar y retener" | `tel-05-*` |
| 22:44:17 | Aviso del 60 en el emulador, **12.6 s** después de "Retenido" en el teléfono | `emu-05-detalle` |
| 22:45 | Termina la espera de 120 s ("Espera terminada a las 16:45") | |
| 22:55:32 | La persona tocó "Firmar y enviar" y puso la huella. El script no lo hizo porque el teléfono tenía otra app al frente. | `tel-07-enviado` |
| 22:56 | "Enviado" en los dos dispositivos | `tel-07-enviado`, `emu-06-final` |

- [FACT] La retención de 20 ya había madurado ("Espera terminada a las 22:34") cuando el guardián la detuvo. `cancel` solo pide la firma del guardián, así que un pago madurado y no enviado todavía se puede detener.
- [FACT] El envío de los 60 salió por la rama de retención madurada, no por la del tope: superaba el tope de 50.
- [FACT] El guardián nunca firmó nada que mueva fondos. Su única transacción es el `cancel`.

## Hallazgos para la demo

- [FACT] Una transacción vale 60 s desde que se arma (`TX_VALIDITY_SECONDS` y `AUTH_VALIDITY_LEDGERS` en `KmpStellarGateway`). Si el guardián tarda más en poner el PIN o la huella, la red la rechaza, la app dice "No llegó a Stellar. Nada cambió." y basta con volver a intentar. Pasó en el primer intento de esta corrida.
- [FACT] El guardián solo recibe avisos si su app se abrió al menos una vez desde que arrancó el dispositivo, porque es la app la que inicia `HoldWatchService`.
- [INFERENCE] Antes de retener, la estimación "saldrá después de las …" usa la última hora de ledger leída, no una nueva. Aquí dijo 16:44 y la espera terminó a las 16:45. La frase sigue siendo cierta ("después de"), pero puede quedarse un minuto corta.
- [RECOMMENDATION] Para grabar: abrir primero la app del guardián, no cambiar de app en el teléfono durante la toma, y que el guardián confirme dentro de los 60 s.

## Cómo se armó

1. Las claves son las de [`two-devices-f01.md`](two-devices-f01.md). Se leyeron del `keys.json` de cada dispositivo: la dueña del teléfono es `GABPAWVG…` y el guardián del emulador es `GCCKC6JF…`.
2. `DeviceProvisioningLiveTest` se corrió con esas dos claves y desplegó el registro `6db027bc…` (17:20 UTC).
3. Se copió el mismo `testnet.json` a los dos dispositivos con `adb push`, y se leyó de vuelta en cada uno: los dos nombran `CDHRWCRP…`.

En el emulador, después de un arranque en frío, el almacenamiento compartido no se monta hasta que se desbloquea la pantalla con el PIN (el usuario queda `RUNNING_LOCKED`). Hasta entonces `/sdcard` no existe y `adb push` falla.

## Cuenta

```
account   CDHRWCRPMFHAVHH2T3NYQ4FUPSXKPRZUVH6GSLHGA35DILLHN725ITXB
registry  CB7ARUGTWGH3PZTKOO7QHLKKVQ3UUJJVRPEYWD66UDCEURTJA2OOKN5R
policy    CBTIF2BF7YGCR6OT7TQMHGGMY6BVHKPOAJV5BYZQ2P3IWKSEXYHLWFMW
usdc      CBC26725AIHDL6RRFVCHANFCBW5DJ6WZES24YXWC7YGPXGTRYWQTIHSK   (SAC de prueba, no el USDC de Circle)
owner     GABPAWVGD5QFDNUGG5P5BETPUTBSKYQCQ35AGZGCTVDP4H6QKKOXGRRW   (Keystore del teléfono)
guardian  GCCKC6JFAA3J2TWUOF32S4TERWZFGSF7NDHTDJ7FBZK5KWHV5VGJE4ZI   (Keystore del emulador)
contact   GDY6XXYHPRM47GIGFYCNF3KW6IAONO27BAQKOC2UBXVWSC46BD5FZHAX
stranger  GDRXTNND5DRT7F6HVQC7D7U7ADJU3DM3BE5TSLID5SYJHXGJOOPMOI7Q
shape     contextRuleCount=1, signers=[dueña], policies=[CBTIF2BF…FMW]
```

| Paso de preparación | Hash | Ledger (UTC) |
| --- | --- | --- |
| Desplegar el SAC de prueba | `d5c8fcb69dd20a548cccd4622342b5807a087164f236a846b37e11e5b5ec63bf` | 5177686 (17:20:17) |
| Trustline | `dfaef772e565a79f8fa5c7c566ddde3acf9f6d8a9b7b23557bcc7ad6f8f0b8a1` | 5177687 (17:20:22) |
| Trustline | `75e67d0e22b7699519d1d01dc0c30c708429ba46a79d44059176dbb7042276d1` | 5177688 (17:20:27) |
| Mint de 500 a la cuenta | `297bd01cdc41ebd21be168e0c6492454cd9b6b4bc77422862550fd87a62b4c49` | 5177691 (17:20:42) |

- [FACT] `getLedgerEntries` sobre la instancia del registro `CB7ARUGT…` devuelve el ejecutable `6db027bc…`.
- [FACT] El guardián no es firmante: la cuenta tiene una regla, con la dueña como único firmante.

## No corrido

- Un pago a un contacto dentro del tope (sin espera) con esta cuenta, desde los dispositivos. Sí se corrió con la cuenta anterior ([`two-devices-f01.md`](two-devices-f01.md)) y con una cuenta desechable ([`gateway-live.md`, corrida 7](../stellar/gateway-live.md#corrida-7-registro-con-índice-b-2-13-oct)).
- La latencia del aviso para el 20 (ver arriba). La del 60 sí vale.
- La captura `tel-06-listo` ("Firmar y enviar" antes del envío): el script la perdió cuando el teléfono cambió de app.
- La cuenta anterior de los dispositivos, `CB4PUYVY…`, sigue en testnet con el registro `a57f01b7…`. Ningún dispositivo apunta ya a ella.
