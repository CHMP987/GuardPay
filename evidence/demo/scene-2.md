# Escena 2 — Retención, y el envío inmediato

**REAL** en la parte de la app. La IA no intervino: sigue en `Analysis.Unavailable`. No hay escena 2a.

## App, cuenta del emulador

| Campo | Valor |
| --- | --- |
| Test | Pagar 150 a una cuenta nueva. |
| Expected | Retenido, con la espera de unos 120 s. Diego puede detenerlo. |
| Actual | "Retenido hasta las 15:47. Diego puede detenerlo." |
| Contract | Cuenta `CDWPPTDM…`. Registro `CC5NG54I…`. SAC de prueba `CC2K3NDV…`, no el USDC de Circle. |
| Transaction | `queue`. Hold 0 en el ensayo 1, hold 1 en el ensayo 2. |
| Hash | `c3941e02ecdb8c1123907d1db69ac87c5b10198371d3bcfd91711dcde4d1c6d0` y `269319d0cc4d53aec4d096f8a20eeff9d6960aff8c4e6452005aae05489e568a`. |
| Timestamp | 15:35:22 UTC y 15:45:27 UTC. |
| Input | 150. Destino que termina en TAXK. Sin mensaje de la IA. |
| Output | Retenido en la app. |
| Captura | `evidence/demo/rehearsal/r1-s23.mp4`, `r2-s23.mp4`. |
| Conclusión | La retención se creó dos veces. Tiempos: 61,0 s y 61,7 s. |

## Envío inmediato, cuenta CLI

El rechazo de "enviarlo ya" no se corrió sobre los holds del emulador. En la cuenta CLI, GH-05 es ese rechazo.

| Campo | Valor |
| --- | --- |
| Test | GH-05. Transfer del hold antes de `ready_at`. |
| Expected | Rechazo. |
| Actual | `successful=false`. |
| Contract | Cuenta `CDBJMSUI…`. SAC de prueba `CBUSK46Y…`. |
| Transaction | `transfer` del hold 1, monto `200000000`. |
| Hash | `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed` |
| Timestamp | 2026-10-06T08:58:32Z. Ledger 5050705. |
| Input | Mismo monto del hold, antes de la hora. |
| Output | Error de contrato 7 (`HoldNotReady`). |
| Captura | `evidence/stellar/deployment.md`. |
| Conclusión | En esa cuenta, el envío antes de tiempo no pasó. No es un hash de `CDWPPTDM…`. |
