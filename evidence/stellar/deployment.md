# Deploy en testnet

6 oct 2026. Datos REAL leídos de Horizon y del RPC de testnet. Red: Test SDF Network ; September 2015. Sin mainnet.

`[FACT]` El activo es un SAC de prueba. El emisor es la dueña de la demo. No es el USDC de Circle (emisor `GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5`). Escala 10^7. Tope diario 50 = `500000000` en unidades crudas. Fondo de la cuenta 500 = `5000000000`. La retención es la constante del registro, 120 segundos. No existe un parámetro de caducidad de 600 segundos.

## Direcciones

| Rol | Dirección |
| --- | --- |
| Dueña | `GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y` |
| Guardián | `GA7G4HPJFYRWD6SI5DW7YYXYNAXZD7JXD3Z4L5GFJIMVBSNBNPBF7JZ3` |
| Contacto 1 | `GDCL4MPCRHK4D4Q5INJV3TFQ5QIDG3S3IJUOS7QR2O52DSVZ2X3HQGYT` |
| Contacto 2 | `GB7SOOHDMJHZ5WEUG3CH4JAG33IPWLCQXVFJ3PWQ5GUA73EJ3W5OXFPG` |
| Contacto 3 | `GDD7SMHDQNJG4KYDJW7CT6OFABRBV2EZYEGLRTRTC3VJIPJMWNRGK6L3` |
| Atacante | `GA77MSV4XBYMA24E7IEMWPUY63MQ2YYAQWP5U6QL577RCLFHVNMBS4GL` |
| SAC de prueba | `CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R` |
| `guardian_hold` | `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI` |
| `hold_registry` | `CBEDM2DZYJ7VEU434J7D44VVQ6ADW4IAG5UHE74PY72KI5GNRTMRU36S` |
| Cuenta | `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` |

Las semillas están en `scripts/.testnet/`, ignorado por git. El guardián no es firmante de la cuenta.

## Wasm

`[FACT]` sha256 de los archivos construidos con `stellar contract build`. El ejecutable de la cuenta en el ledger coincide con el hash de `account.wasm`.

| Contrato | sha256 |
| --- | --- |
| `account.wasm` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` |
| `guardian_hold.wasm` | `b36588780d05c900d891f63cbbade21bdf6b6db4a3b12554c0124c2e7a2a85ce` |
| `hold_registry.wasm` | `031173c212bdec97626a952c7edc5d624cf30434295d3b48d162bd678d60d0cb` |

## Transacciones de deploy

`[FACT]` Hashes de la cuenta de la dueña en Horizon, en este orden.

| Paso | Hash |
| --- | --- |
| Deploy del SAC | `f51836fec80c3ef40be955eb3ad452f18788e31eb561f2dbaf562f1b145062e2` |
| Upload de `guardian_hold` | `3c188d813e79a2c8eae80b5bedd040eaf95d783442bcdf256ff1f479a41beaf5` |
| Deploy de `guardian_hold` | `c443aa422dbd4de2ebb22956391142238e17943ec864bb103448a8a967fcb384` |
| Upload del registro | `23981d22951d10c512d347bf814ac22d963975ab5af396a640064162c32d3500` |
| Deploy del registro | `17e280bfdf508c13cf582cbb2585ca63e829e01d2529cf8cd853d3078e8a45ea` |
| Upload de la cuenta | `11ec4b5f5f454792989c556f9e61b936949541b510e276ad86df8c259aa9c962` |
| Deploy de la cuenta | `ef645b92197b2bb2c3fbef24c016136e96df02eb26f930f4d6d99975333e07f5` |
| Mint de 500 al contrato de la cuenta | `77df45ce52ce781741c3ac922cd9b14de8aeaeb769e194be8d94a47cac0942e8` |

`scripts/deploy-testnet.ps1` se ejecutó una segunda vez y no desplegó contratos nuevos. `scripts/deploy-testnet.sh` ejecutó esa misma rama. En el bash de esta máquina no está `stellar`; el primer deploy del `.sh` queda no corrido.

Los destinos `G...` necesitaron trustline del SAC antes de poder recibir. Esas trustlines no son ensayos ★.

## Ensayos ★

`[FACT]` Un hash de ledger por ensayo. Los que dicen falla están en Horizon con `successful=false` y el contrato atrapó la llamada. La CLI de Stellar 28.1.0 no firma una cuenta contrato; `scripts/sign-delegated.mjs` arma la autorización delegada de la dueña y, si la simulación rechaza, envía igual para que el rechazo quede en el ledger.

| ID | Resultado | Hash |
| --- | --- | --- |
| GH-02 | Pasa. 10 unidades de display (`100000000`) al contacto 1. `spent_today` quedó en `100000000`. | `90fac9d8292ec3c9b22750700159ec31bda505703e6eba17823932bd9dee0701` |
| GH-03 | Falla. Transferencia a un destino que no es contacto y no tiene hold. | `a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492` |
| GH-04 | Pasa. `queue` calculó `ready_at = created_at + 120` (`1791276887` → `1791277007`). | `8361eff0c06a1a52bcf264ada0de6c41d0bbfa63e440891d1fef08d24e7b94b6` |
| GH-05 | Falla en el ledger con error de contrato 7 (hold no listo). Hold 1, monto `200000000`. | `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed` |
| GH-06 | Pasa. El hold 0 quedó `Executed`. | `35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573` |
| GH-07 | Pasa. El guardián canceló el hold 1. El registro lo dejó `Stopped`. | `09838694770afb1d17d60f3a65f3ee94e92c2b804c4337672f5f99c2bef9e8e8` |
| GH-08 | Falla. Transferencia del hold ya detenido. | `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f` |
| GH-09 | Falla. El payload de autorización nombra al guardián, no a la dueña. | `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de` |
| GH-10 | Falla. La firma de la dueña no autoriza un destino fuera de la lista. | `89e02fa36233c70729e10adab4ce682f53265e931bc21845b16d5deb2418040d` |
| GH-16 | Falla. `approve` del SAC. | `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434` |

El hold usado por GH-05 se encoló en `e1d0719b0686010d1d0a09c3fee35f626aad765718ef172ef37763b1f1124cb0`. No es uno de los diez ★.

CP-3 no está firmado. Estos hashes quedan para revisión humana.
