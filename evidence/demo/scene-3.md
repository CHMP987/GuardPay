# Escena 3 — Diego detiene

Etiqueta **REAL**. SAC de prueba. Guardián Diego. Misma cuenta que la escena 1.

## Test

El guardián abre el detalle y cancela el hold.

## Expected

Detenido. El registro no mueve el token.

## Actual

Dos veces. De tocar el aviso a "Detenido": 43,0 s y 39,3 s. El aviso tras "Retenido" lo midió el script en 0,2 s las dos veces, con el servicio en primer plano.

- `fb9eafad4c9ec2934fd4711b933077a1ee9d91ae92314f25f3d193ed688fa56c` (5055479, 2026-10-06T15:36:22Z)
- `bb1f4dd7105ab3f0ebbbae3b5b45eb1e27e8feaf07755a04b8c262c420521a22` (5055600, 2026-10-06T15:46:27Z)

## Contract

`hold_registry` `CC5NG54ICWDH7N46RMESSAMXTFAFEL243ZIJC5RNFZ3FFHQ6CHY5NRY2`

## Transaction

`cancel`

## Hash

Los dos de arriba.

## Timestamp

2026-10-06, 15:36 y 15:46 UTC

## Input

Hold 0 y hold 1 de la escena 2.

## Output

Pantalla "Detenido por ti".

## Captura o log

`rehearsal/r1-s23.mp4`, `r2-s23.mp4`

## Conclusión

`[FACT]` Diego detuvo los dos holds dentro de la ventana de 120 s. Si no mira, nadie detiene nada: el contrato no cancela solo.
