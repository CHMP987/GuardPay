# Carril retenido

Etiqueta **REAL**. SAC de prueba, no el USDC de Circle.

## Test

Pago a un destino que no es contacto. La app encola. El contrato calcula `ready_at`.

## Expected

`queue` pasa. El estado en la app es Retenido solo porque el registro tiene el hold.

## Actual

Cuenta de la app, dos ensayos, 150 unidades a una cuenta nueva:

- `c3941e02ecdb8c1123907d1db69ac87c5b10198371d3bcfd91711dcde4d1c6d0` ledger 5055467, 2026-10-06T15:35:22Z, hold 0
- `269319d0cc4d53aec4d096f8a20eeff9d6960aff8c4e6452005aae05489e568a` ledger 5055588, 2026-10-06T15:45:27Z, hold 1

GH-04 en la cuenta publicada: `8361eff0c06a1a52bcf264ada0de6c41d0bbfa63e440891d1fef08d24e7b94b6`, `ready_at = created_at + 120`.

## Contract

`hold_registry.queue`. `ready_at` lo calcula el registro. No es un parámetro.

## Transaction

`queue`

## Hash

Los de arriba.

## Timestamp

2026-10-06

## Input

Destino que no está en los contactos. Monto 150 en la app.

## Output

La app mostró "Retenido hasta las 15:47. Diego puede detenerlo."

## Captura o log

`evidence/demo/rehearsal.md`

## Conclusión

`[FACT]` Esos `queue` se incluyeron. La escena 2 de la app no pegó un mensaje: la IA no corrió. Ver `evidence/ai/benchmark.md`.
