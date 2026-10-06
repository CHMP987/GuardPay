# Madurez y envío

**REAL** para GH-05 y GH-06 en la cuenta CLI.

| Campo | Valor |
| --- | --- |
| Test | GH-05 antes de `ready_at`. GH-06 después, con una firma nueva de la dueña. |
| Expected | El primero falla. El segundo marca Executed. |
| Actual | GH-05 `successful=false`, error 7. GH-06 `successful=true`, hold 0 Executed. |
| Contract | Cuenta `CDBJMSUI…`. SAC de prueba `CBUSK46Y…`. |
| Transaction | `transfer`. |
| Hash | `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed` y `35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573`. |
| Timestamp | 2026-10-06T08:58:32Z y 2026-10-06T08:57:02Z. |
| Input | Hold 1 antes de tiempo. Hold 0 ya maduro. |
| Output | Error 7, y luego Executed. |
| Captura | `deployment.md`. El reenvío del sobre de GH-06 dio `TxBadSeq` (`scene` de replay en la matriz). |
| Conclusión | No salió antes de la hora. Reenviar el mismo sobre no creó otra transacción. |

No hay vencimiento a los 600 s. GH-30: no corrido.
