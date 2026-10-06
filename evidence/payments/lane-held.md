# Carril retenido

**REAL.**

| Campo | Valor |
| --- | --- |
| Test | GH-04 en la cuenta CLI. Escena 2 en el emulador. |
| Expected | `queue` calcula `ready_at`. No es un argumento. |
| Actual | CLI: `1791276887` → `1791277007` (más 120 s). Emulador: la app mostró la hora de envío unos dos minutos después. |
| Contract | Registro CLI `CBEDM2DZ…`. Registro del emulador `CC5NG54I…`. SAC de prueba, no el USDC de Circle. |
| Transaction | `queue`. |
| Hash | CLI: `8361eff0c06a1a52bcf264ada0de6c41d0bbfa63e440891d1fef08d24e7b94b6`. Emulador: `c3941e02ecdb8c1123907d1db69ac87c5b10198371d3bcfd91711dcde4d1c6d0` y `269319d0cc4d53aec4d096f8a20eeff9d6960aff8c4e6452005aae05489e568a`. |
| Timestamp | CLI 2026-10-06T08:54:47Z. |
| Input | Monto y destino. Sin `ready_at`. |
| Output | Hold Retained. |
| Captura | `deployment.md`, `scene-2.md`. |
| Conclusión | La espera la escribe el registro. |
