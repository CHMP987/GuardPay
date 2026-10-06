# Escena 2 — Retención, y el envío inmediato rechazado

Etiqueta **REAL**. SAC de prueba. La IA no intervino: Spike B está rojo (`evidence/ai/benchmark.md`). La escena de la app se hizo sin pegar el mensaje.

## Test

Un pago a un destino que no es contacto queda Retenido. Un `transfer` inmediato de ese pago, sin hold que coincida, lo rechaza el contrato.

## Expected

`queue` pasa. El `transfer` inmediato falla.

## Actual

**App, dos veces**, cuenta del emulador `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`, wasm `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b`. De Inicio a "Retenido": 61,0 s y 61,7 s (`rehearsal.md`).

- `c3941e02ecdb8c1123907d1db69ac87c5b10198371d3bcfd91711dcde4d1c6d0` (5055467, 2026-10-06T15:35:22Z)
- `269319d0cc4d53aec4d096f8a20eeff9d6960aff8c4e6452005aae05489e568a` (5055588, 2026-10-06T15:45:27Z)

**Terminal, una vez**, otra cuenta y otro wasm. Cuenta `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`, wasm `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c`. `transfer` de 7 unidades a un extraño, sin hold:

- `e344af8576918c00916505fc1fcb342e648ee65ac9239a39dfa1c6f3e7ed208a`
- `successful=false`, ledger 5059687, 2026-10-06T21:27:02Z, código 3

Ese rechazo no es un segundo intento sobre los holds de la app. No es el GH-05 de la mañana (`0ebb4ba4…` en `CDBJMSUI…`, wasm `986956cc…`), que es el mismo monto antes de `ready_at`. Las semillas de la app no están en este entorno.

## Contract

App: la cuenta de `rehearsal.md`. Terminal: la cuenta de ataques de `security/bypass-matrix.md`.

## Transaction

`queue` en la app. `transfer` rechazado en la terminal.

## Hash

Los tres de arriba, cada uno en la cuenta que lo produjo.

## Timestamp

App: 2026-10-06, 15:35 y 15:45 UTC. Terminal: 2026-10-06T21:27:02Z.

## Input

150 unidades a una cuenta que no es contacto, en la app. 7 unidades al extraño, en la terminal.

## Output

La app mostró Retenido. La terminal: contrato 3.

## Captura o log

`rehearsal/r1-s23.mp4`, `r2-s23.mp4`. El rechazo de terminal está en `evidence/demo/terminal-rejections.mp4` y en la matriz.

## Conclusión

`[FACT]` La retención de la app ocurrió dos veces, en el emulador. El rechazo inmediato ocurrió una vez, de noche, en la cuenta de ataques y en su wasm. No hay un tiempo único de las dos partes juntas.
