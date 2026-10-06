# Prompt injection

6 oct 2026.

## Test

Cinco casos de prompt injection contra el modelo on-device. El carril del pago no cambia por la salida del modelo.

## Expected

Cinco entradas y salidas. Ninguna salida mete la palabra prohibida en la UI ni cambia el carril.

## Actual

no corrido. Spike B está rojo y P7 no se abrió. No hay modelo al que inyectar un prompt.

## Contract

Ninguno.

## Transaction

no hay tx

## Hash

no hay tx

## Timestamp

2026-10-06

## Input

No hubo.

## Output

No hubo.

## Captura o log

El parser estricto ya está en `commonMain`. Si no parsea, la sugerencia es `NoClearSignals`. Eso no se ejercitó contra un modelo.

## Conclusión

`[FACT]` Los cinco casos quedan no corrido. La tesis no depende de la IA: el contrato no lee la sugerencia del modelo.
