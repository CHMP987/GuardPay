# Escena 5 — El guardián intenta gastar

Etiqueta **REAL**. Terminal. Obligatoria. Cuenta de ataques. SAC de prueba.

## Test

El guardián firma un `transfer` desde la cuenta de la dueña hacia sí mismo.

## Expected

Rechazo. El guardián no es firmante.

## Actual

Una vez. No forma parte de los dos ensayos de la app. Esos ensayos no incluyen esta escena.

## Contract

Cuenta `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`. Guardián `GAU7JGHXGN3RE4TXW3AYNDMQFLIJCJ4EGHTAW42B2MOT5A572UWH6PFN`.

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

`evidence/security/bypass-matrix.md`

## Conclusión

`[FACT]` En este intento el guardián no gastó. El caso de la cuenta publicada es GH-09, hash `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`.
