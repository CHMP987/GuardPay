# Escena 4 — Ejecutar un pago ya detenido

Etiqueta **REAL**. Terminal. Cuenta de ataques, no la de la app. SAC de prueba.

## Test

Con una firma de la dueña, enviar el hold que el guardián ya detuvo.

## Expected

El contrato rechaza. Código visible en el explorador.

## Actual

Una vez. Cola `065654755b5f27c08d4f83e2e6130e7e6656af67ad267d36b21a62d448034329`. Cancel `8b39ea26414037c633f4ce5c995dda354f333553da53cc5eb442fdabf6280a8b`. El `transfer` posterior falló.

No se cronometró como parte del ensayo de la app. No hay una segunda corrida de esta escena.

## Contract

Cuenta `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`. Registro `CDCABEI4EPTDARZMMLKRLWP3WJDCF2O7U6GNBRXWA3UW3I2CPZFTJZWM`.

## Transaction

`transfer` del hold 0, estado Stopped, 17 unidades, destino `GBODF2JDHLG4X74JYJUIUW4XZYWKXOWUV7RZZQ33ZDLC2PPIPRXVA6NQ`.

## Hash

`5776967c045f1751262668c41c22ef7c1613c12329242f040d1e3e836d642eac`

## Timestamp

2026-10-06T21:27:32Z, ledger 5059693

## Input

Misma cuenta, token, destino y monto del hold Stopped.

## Output

`successful=false`. Código 3 (NotAllowed).

## Captura o log

`evidence/security/bypass-matrix.md`. El vídeo de terminal, si está grabado, se apunta en `video.md`.

## Conclusión

`[FACT]` Ese pago detenido no salió. El caso equivalente en la cuenta publicada es GH-08, hash `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`.
