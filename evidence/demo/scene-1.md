# Escena 1 — Pago de confianza

Etiqueta **REAL**. SAC de prueba, no el USDC de Circle. Contacto Mamá. Cuenta de la app.

## Test

Laura paga 10 a un contacto, dentro del tope.

## Expected

Enviado.

## Actual

Dos veces en el emulador. Tiempos del script: 47,8 s y 48,9 s hasta "Enviado".

## Contract

Cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`.

## Transaction

`transfer`

## Hash

- `01187e29c1099b386cb6e3d66382db1fcb104aed01081ae05b5033951061e4c5` (5055445, 2026-10-06T15:33:32Z)
- `b664e69db168c4aad18e1841874fbb73daecc788e74b29d152843ec80089ed5a` (5055569, 2026-10-06T15:43:52Z)

## Timestamp

2026-10-06, 15:33 y 15:43 UTC

## Input

10 unidades a Mamá.

## Output

Pantalla "Enviado".

## Captura o log

`evidence/demo/rehearsal/r1-s1.mp4`, `r2-s1.mp4`. Tabla en `rehearsal.md`.

## Conclusión

`[FACT]` Los dos pagos se incluyeron. No se volvieron a correr en P9.
