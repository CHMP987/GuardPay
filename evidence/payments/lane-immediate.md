# Carril inmediato

**REAL.**

| Campo | Valor |
| --- | --- |
| Test | GH-02 en la cuenta CLI. Escena 1 en la cuenta del emulador. |
| Expected | Un pago a un contacto, dentro del tope, sale. |
| Actual | Los dos salieron. |
| Contract | CLI: `CDBJMSUI…`, SAC `CBUSK46Y…`. Emulador: `CDWPPTDM…`, SAC `CC2K3NDV…`. Ninguno es el USDC de Circle. |
| Transaction | `transfer`. |
| Hash | CLI: `90fac9d8292ec3c9b22750700159ec31bda505703e6eba17823932bd9dee0701`. Emulador: `01187e29c1099b386cb6e3d66382db1fcb104aed01081ae05b5033951061e4c5` y `b664e69db168c4aad18e1841874fbb73daecc788e74b29d152843ec80089ed5a`. |
| Timestamp | CLI 2026-10-06T08:51:52Z. Emulador 15:33:32Z y 15:43:52Z. |
| Input | 10 unidades de display. |
| Output | `spent_today` de la cuenta CLI quedó en `100000000` y el 6 oct por la noche seguía ahí. |
| Captura | `evidence/stellar/deployment.md`, `evidence/demo/scene-1.md`. |
| Conclusión | El carril de contactos se vio en las dos cuentas. |
