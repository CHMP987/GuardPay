# Dos dispositivos con el registro B-2: reprovisión (13 oct)

**Veredicto: el teléfono (dueña) y el emulador (guardián) apuntan a una cuenta nueva, `CDHRWCRP…`, cuyo registro es el wasm con índice de la [release B-2](../stellar/deployment.md#release-b-2-13-oct-2026).** Las claves no cambiaron; solo la cuenta. No se corrió ningún pago desde los dispositivos con esta cuenta.

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, teléfono físico más emulador. Hashes de preparación consultados con `getTransaction` en el RPC, los cuatro `SUCCESS`. |
| Fecha | 2026-10-13, 17:20:17–17:20:42 UTC, ledgers 5177686–5177691 |
| Dueña | Teléfono, Keystore `guardpay-owner` |
| Guardián | Emulador `Medium_Phone_API_37.0` (API 37), Keystore `guardpay-guardian` |
| Política | `CBTIF2BF7YGCR6OT7TQMHGGMY6BVHKPOAJV5BYZQ2P3IWKSEXYHLWFMW` (la de F-01, sin cambios) |
| Registro | Wasm `6db027bc…`, de la release B-2 |
| App | El APK de debug que ya estaba instalado en cada dispositivo. No se reinstaló. |

## Cómo se armó

1. Las claves son las de [`two-devices-f01.md`](two-devices-f01.md). Se volvieron a leer del `keys.json` de cada dispositivo: la dueña del teléfono es `GABPAWVG…` y el guardián del emulador es `GCCKC6JF…`.
2. `DeviceProvisioningLiveTest` se corrió con esas dos claves. Usa las constantes de `TestnetProvisioner`, que desde la release B-2 despliegan el registro `6db027bc…`.
3. Se copió el mismo `testnet.json` a los dos dispositivos con `adb push`, y se leyó de vuelta en cada uno: los dos nombran la cuenta `CDHRWCRP…`.

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

| Paso | Hash | Ledger (UTC) |
| --- | --- | --- |
| Desplegar el SAC de prueba | `d5c8fcb69dd20a548cccd4622342b5807a087164f236a846b37e11e5b5ec63bf` | 5177686 (17:20:17) |
| Trustline | `dfaef772e565a79f8fa5c7c566ddde3acf9f6d8a9b7b23557bcc7ad6f8f0b8a1` | 5177687 (17:20:22) |
| Trustline | `75e67d0e22b7699519d1d01dc0c30c708429ba46a79d44059176dbb7042276d1` | 5177688 (17:20:27) |
| Mint de 500 a la cuenta | `297bd01cdc41ebd21be168e0c6492454cd9b6b4bc77422862550fd87a62b4c49` | 5177691 (17:20:42) |

- [FACT] `getLedgerEntries` sobre la instancia del registro `CB7ARUGT…` devuelve el ejecutable `6db027bc…`.
- [FACT] El guardián no es firmante: la cuenta tiene una regla, con la dueña como único firmante.
- [FACT] Los despliegues del registro y de la cuenta no imprimen hash en la prueba; sus direcciones están arriba.

## No corrido

- Abrir la cuenta nueva en la app de cada dispositivo. En el emulador se abrió la app y mostró la pantalla de inicio; no se entró como guardián. En el teléfono no se abrió la app.
- Una retención, un aviso al guardián o un envío desde los dispositivos con esta cuenta. El ciclo completo con el registro nuevo se corrió con una cuenta desechable ([`gateway-live.md`, corrida 7](../stellar/gateway-live.md#corrida-7-registro-con-índice-b-2-13-oct)).
- La cuenta anterior de los dispositivos, `CB4PUYVY…`, sigue en testnet con el registro `a57f01b7…`. Ningún dispositivo apunta ya a ella.
