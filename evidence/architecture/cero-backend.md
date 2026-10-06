# Cero backend

6 oct 2026. Etiqueta **REAL**.

## Test

¿Hay un servidor que pueda mover el SAC de prueba o cambiar la regla?

## Expected

No hay servidor, base de datos, ni push (FCM/APNs). El guardián se entera por sondeo en el teléfono.

## Actual

`[FACT]` El repo no tiene un servicio de pagos. El aviso está en la app: `evidence/guardian/notification.md` (sondeo cada 30 s, medido en emulador). Si la app está cerrada, el aviso puede llegar tarde.

## Contract

Ninguno. La regla la aplican los tres contratos, no un servidor.

## Transaction

no hay tx

## Hash

no hay tx

## Timestamp

2026-10-06

## Input

Árbol del repo en esta fecha.

## Output

No hay proceso que reciba una orden de pago.

## Captura o log

`evidence/guardian/notification.md`

## Conclusión

`[FACT]` No hay backend que firme o que cambie la lista blanca. `[INFERENCE]` Eso no impide que alguien con la clave de la dueña firme lo que la lista blanca ya permite.
