# Escena 1 — Pago de confianza

**REAL.** Dos corridas en emulador contra testnet, 6 oct 2026. No se borró `evidence/demo/rehearsal.md`. Esta ficha la cita.

| Campo | Valor |
| --- | --- |
| Test | Laura paga 10 del SAC de prueba a Mamá, contacto, dentro del tope. |
| Expected | Enviado. |
| Actual | La app mostró "Enviado a Mamá". |
| Contract | Cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`. SAC `CC2K3NDVHI52YKCDORJVNIZYRYNO2TE6MN3LIK2LWDRIZMJDBEG3VDBH`. No es el USDC de Circle. |
| Transaction | `transfer` firmado por la dueña del Keystore. |
| Hash | Ensayo 1: `01187e29c1099b386cb6e3d66382db1fcb104aed01081ae05b5033951061e4c5`. Ensayo 2: `b664e69db168c4aad18e1841874fbb73daecc788e74b29d152843ec80089ed5a`. |
| Timestamp | 15:33:32 UTC y 15:43:52 UTC. Ledgers 5055445 y 5055569. |
| Input | 10 unidades de display. Contacto Mamá. |
| Output | Horizon `successful` (según rehearsal.md). |
| Captura | `evidence/demo/rehearsal/r1-s1.mp4`, `r2-s1.mp4`, `rN-s1-enviado.png`. |
| Conclusión | El carril inmediato salió dos veces. Tiempos de la app: 47,8 s y 48,9 s. |

Estos hashes no son los de `deployment.md`.
