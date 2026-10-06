# Escena 6 — approve, segunda regla, y la pantalla de reglas

Tres piezas. No se mezclan las cuentas.

## Pantalla, cuenta del emulador

**REAL.** Dos corridas. La app leyó una regla.

| Campo | Valor |
| --- | --- |
| Test | Abrir "Reglas de esta cuenta". |
| Expected | Una regla. Política GuardianHold. Diego no es firmante. |
| Actual | "Esta cuenta tiene 1 regla". "Política: GuardianHold". "Tu guardián: Diego (GCCK…E4ZI). No es firmante de esta cuenta." |
| Contract | `CDWPPTDM…`. Política `CCGIEMIU…`. |
| Transaction | Lectura. No hay un hash de escritura de esta pantalla. |
| Hash | no hay tx |
| Timestamp | Ensayos del 6 oct 2026, 15:33–15:50 UTC. |
| Input | Ninguno. La pantalla lee el ledger. |
| Output | Textos de arriba. |
| Captura | `r2-s6.mp4` muestra las reglas. `r1-s6.mp4` las dejó bajo el borde. |
| Conclusión | Tiempos hasta ver la política: 19,4 s y 21,9 s. |

La relectura del 6 oct por la noche confirmó `Count` 1, firmante la dueña del Keystore y la misma política. `evidence/security/chain-reread.md`.

## approve, cuenta CLI

| Campo | Valor |
| --- | --- |
| Test | GH-16. |
| Expected | Rechazo. |
| Actual | `successful=false`. Error de contrato 3, `NotAllowed`. |
| Contract | `CDBJMSUI…`. SAC de prueba `CBUSK46Y…`. |
| Transaction | `approve`. |
| Hash | `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434` |
| Timestamp | 2026-10-06T09:00:32Z. Ledger 5050729. |
| Captura | `scripts/attacks/approve-transfer-from.sh`. |

## Segunda regla

| Campo | Valor |
| --- | --- |
| Test | `add_context_rule` sobre la cuenta CLI, y lectura de `ContextRuleData(1)` en las dos cuentas. |
| Expected | La función no existe. Sigue habiendo una regla. |
| Actual | `error: unrecognized subcommand 'add_context_rule'`. Las dos cuentas siguen con `Count` 1 y sin regla 1. |
| Hash | no hay tx |
| Captura | `scripts/attacks/add-rule.sh` y `evidence/security/chain-reread.md`. |
