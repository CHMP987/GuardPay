# Escena 5 — El guardián intenta gastar

**REAL** en la cuenta CLI. Obligatoria. No se omitió: el hash es el de GH-09. No es la cuenta del emulador.

| Campo | Valor |
| --- | --- |
| Test | GH-09. El guardián firma un `transfer` desde la cuenta de la dueña hacia sí mismo. |
| Expected | Rechazo. El guardián no mueve el SAC. |
| Actual | Horizon `successful=false`. |
| Contract | Cuenta `CDBJMSUI…`. Guardián `GA7G4HPJ…`, que no es firmante. SAC de prueba `CBUSK46Y…`. |
| Transaction | `transfer` cuyo payload nombra al guardián. |
| Hash | `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de` |
| Timestamp | 2026-10-06T09:00:42Z. Ledger 5050731. |
| Input | Autorización a nombre del guardián. |
| Output | `UnauthorizedSigner` 3016. |
| Captura | `scripts/attacks/guardian-transfer.sh`. Log: `evidence/security/p9-cli-run.log`. |
| Conclusión | En esa transacción el guardián no gastó. La misma escena con la clave de Diego del emulador (`GCCKC6JF…`) queda no corrido. |
