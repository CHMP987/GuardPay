# Escena 5 — El guardián intenta gastar

Etiqueta **REAL**. Terminal. Obligatoria. No es la cuenta de la app.

## Test

El guardián firma un `transfer` desde la cuenta de la dueña hacia sí mismo.

## Expected

Rechazo. El guardián no es firmante.

## Actual

Una vez, de noche. No forma parte de los dos ensayos de la app.

El caso de la mañana es GH-09: `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`, ledger 5050731, 2026-10-06T09:00:42Z, cuenta `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX`, wasm `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b`. No es el hash de esta escena.

## Contract

Cuenta `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`. Wasm `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c`. Guardián `GAU7JGHXGN3RE4TXW3AYNDMQFLIJCJ4EGHTAW42B2MOT5A572UWH6PFN`.

## Transaction

`transfer` hacia el guardián. El payload nombra al guardián como Delegated.

## Hash

`6c445cd95ca7b98e1ba76aab3406c77965835923c25983d39b3cb073511ebdf1`

## Timestamp

2026-10-06T21:27:07Z, ledger 5059688

## Input

Firma del guardián, no de la dueña.

## Output

`successful=false`. Código 3016 (UnauthorizedSigner). El SAC no se movió.

## Captura o log

`evidence/security/bypass-matrix.md`. `evidence/demo/terminal-rejections.mp4`.

## Conclusión

`[FACT]` En este intento el guardián no gastó. El hash es de la cuenta de ataques y de su wasm.
