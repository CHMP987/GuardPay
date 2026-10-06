# Escena 4 — Pago detenido, desde la terminal

**REAL** en la cuenta CLI. No se repitió sobre la cuenta del emulador: la semilla del Keystore no está en esta máquina.

| Campo | Valor |
| --- | --- |
| Test | GH-08. Con la firma de la dueña, ejecutar el pago que el guardián ya detuvo. |
| Expected | Rechazo del contrato. |
| Actual | Horizon `successful=false`. |
| Contract | Cuenta `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX`. SAC de prueba `CBUSK46Y…`, no el USDC de Circle. |
| Transaction | `transfer` del hold detenido. |
| Hash | `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f` |
| Timestamp | 2026-10-06T09:00:52Z. Ledger 5050733. |
| Input | Destino atacante, monto `200000000`. |
| Output | Error de contrato 3, `NotAllowed`. Rama: el hold ya no está Retained. |
| Captura | `scripts/attacks/held-after-cancel.sh` relee el hash. Log: `evidence/security/p9-cli-run.log`. |
| Conclusión | El rechazo está en el ledger de la cuenta CLI. La escena 4 de la cuenta `CDWPPTDM…` queda no corrido. |
