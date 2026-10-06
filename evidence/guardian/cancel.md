# El guardián detiene. No mueve.

Etiqueta **REAL**. SAC de prueba, no el USDC de Circle.

## Test

El guardián llama `cancel` sobre un hold Retained. Después no puede hacer `transfer` desde la cuenta.

## Expected

`cancel` pasa y el hold queda Stopped. Un `transfer` firmado por el guardián no mueve el SAC.

## Actual

App, cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`, guardián Diego, dos veces:

- `fb9eafad4c9ec2934fd4711b933077a1ee9d91ae92314f25f3d193ed688fa56c` ledger 5055479, 2026-10-06T15:36:22Z
- `bb1f4dd7105ab3f0ebbbae3b5b45eb1e27e8feaf07755a04b8c262c420521a22` ledger 5055600, 2026-10-06T15:46:27Z

Cuenta de ataques, otro guardián (`GAU7JGHX…`), cancel: `8b39ea26414037c633f4ce5c995dda354f333553da53cc5eb442fdabf6280a8b`.

El mismo guardián intentó `transfer` hacia sí: `6c445cd95ca7b98e1ba76aab3406c77965835923c25983d39b3cb073511ebdf1`, `successful=false`, código 3016. Ledger 5059688, 2026-10-06T21:27:07Z.

GH-09 de la cuenta publicada: `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`.

## Contract

`hold_registry.cancel` lo autoriza el guardián. No pasa por `enforce`. El registro no llama al token.

## Transaction

`cancel`, y el `transfer` rechazado.

## Hash

Los de arriba. Cuentas distintas.

## Timestamp

2026-10-06

## Input

Hold Retained. Luego un `transfer` con la clave del guardián.

## Output

Stopped en los `cancel` que pasaron. El `transfer` del guardián no se incluyó como éxito.

## Captura o log

`evidence/demo/rehearsal.md`, `evidence/security/bypass-matrix.md`

## Conclusión

`[FACT]` En estos casos el guardián detuvo un hold y no movió el SAC. No es firmante de la cuenta (`rules-onchain.md`, y la provisión de la app en `rehearsal.md`).
