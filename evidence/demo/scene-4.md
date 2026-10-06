# Escena 4 — Ejecutar un pago ya detenido

Etiqueta **REAL**. Terminal. No es la cuenta de la app.

## Test

Con una firma de la dueña, enviar el hold que el guardián ya detuvo.

## Expected

El contrato rechaza. Código visible en el explorador.

## Actual

Una vez, de noche, en la cuenta de ataques. Cola `065654755b5f27c08d4f83e2e6130e7e6656af67ad267d36b21a62d448034329`. Cancel `8b39ea26414037c633f4ce5c995dda354f333553da53cc5eb442fdabf6280a8b`. El `transfer` posterior falló.

No se cronometró como parte del ensayo de la app. No hay una segunda corrida de esta escena.

El caso de la mañana, en otra cuenta y otro momento, es GH-08: `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`, ledger 5050733, 2026-10-06T09:00:52Z, cuenta `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX`, wasm `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b`. No es el hash de esta escena.

## Contract

Cuenta `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`. Wasm `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c`. Registro `CDCABEI4EPTDARZMMLKRLWP3WJDCF2O7U6GNBRXWA3UW3I2CPZFTJZWM`.

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

`evidence/security/bypass-matrix.md`. `evidence/demo/terminal-rejections.mp4`.

## Conclusión

`[FACT]` Ese pago detenido no salió, en la cuenta de ataques y en el wasm sin optimizar. La cuenta del emulador no se usó para este ataque.
