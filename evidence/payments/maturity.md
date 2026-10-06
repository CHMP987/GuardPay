# Espera de 120 segundos

Etiqueta **REAL**.

## Test

`ready_at` es `timestamp + 120`. No hay caducidad a los 600 s.

## Expected

Un `transfer` antes de `ready_at` falla. No existe `expires_at`.

## Actual

GH-05 en la cuenta publicada: `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed`, error de contrato 7 (HoldNotReady). `ready_at = created_at + 120` en GH-04 (`1791276887` → `1791277007`).

GH-30 (vencimiento a 600 s): no corrido. T-049 no está autorizado.

## Contract

`hold_registry` escribe `ready_at`. `guardian_hold` compara el ledger con ese campo.

## Transaction

El rechazo temprano es el hash de GH-05.

## Hash

`0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed`

## Timestamp

Ver `evidence/stellar/deployment.md`.

## Input

Hold aún no maduro.

## Output

Contrato 7. El hold no pasa a Executed.

## Captura o log

`evidence/stellar/deployment.md`, `evidence/policies/whitelist-review.md`

## Conclusión

`[FACT]` La espera de estos contratos es la constante de 120 s. Un hold maduro no vence dentro del contrato.
