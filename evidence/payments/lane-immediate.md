# Carril inmediato

Etiqueta **REAL**. El activo es un SAC de prueba, no el USDC de Circle.

## Test

Pago a un contacto de confianza, dentro del tope diario.

## Expected

El `transfer` se incluye. No hay hold.

## Actual

Dos ensayos de la app, emulador, cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`, contacto Mamá, 10 unidades:

- `01187e29c1099b386cb6e3d66382db1fcb104aed01081ae05b5033951061e4c5` ledger 5055445, 2026-10-06T15:33:32Z
- `b664e69db168c4aad18e1841874fbb73daecc788e74b29d152843ec80089ed5a` ledger 5055569, 2026-10-06T15:43:52Z

El ensayo CLI GH-02 está en `evidence/stellar/deployment.md`: `90fac9d8292ec3c9b22750700159ec31bda505703e6eba17823932bd9dee0701`. Otra cuenta.

## Contract

`account` → `guardian_hold.enforce`, carril `transfer` a contacto.

## Transaction

`transfer` del SAC de prueba.

## Hash

Los tres de arriba. No son intercambiables.

## Timestamp

2026-10-06

## Input

10 unidades de display hacia el contacto.

## Output

Horizon `successful=true` en los dos de la app. La pantalla dijo "Enviado".

## Captura o log

`evidence/demo/rehearsal.md` y los mp4 `r1-s1.mp4`, `r2-s1.mp4`.

## Conclusión

`[FACT]` Esos tres pagos a contacto se incluyeron. El tope de la cuenta de ataques se probó aparte (GH-28 en `security/bypass-matrix.md`).
