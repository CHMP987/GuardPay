# Reprovisión de los dispositivos con la política F-01 (13 oct)

**Veredicto: el teléfono (dueña) y el emulador (guardián) apuntan a una cuenta nueva con la política F-01 `CBTIF2BF…`, y cada uno se reconoce en su rol.** Solo se reprovisionó y se abrió la app. No se envió ningún pago ni retención desde los dispositivos en esta cuenta.

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

## Qué cambia

- [INFERENCE] Con esta cuenta, la dueña puede enviar desde el teléfono una retención a un contacto de confianza cuando madura, lo que la política del 6 oct rechazaba ([F-01](../../AUDIT_REPORT.md#f-01)). No se probó en los dispositivos (ver abajo).
- La cuenta de dos dispositivos del 8 oct (`CA5Z7J…`) y la del emulador de la demo (`CDWPPT…`) siguen en testnet con la política anterior. Ningún dispositivo apunta ya a ellas.

## No corrido

- Un pago, una retención o una cancelación desde los dispositivos en esta cuenta.
- Enviar desde el teléfono una retención madura a un contacto de confianza, sobre el tope o dentro de él.
- El caso dentro del tope en testnet.
- Reprovisionar la cuenta publicada `CDBJMS…` y la de ataques `CC6FFXGG…`.
