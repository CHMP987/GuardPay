# Cómo leer `evidence/`

Cada pieza dice de dónde sale el dato.

| Etiqueta | Significa |
| --- | --- |
| REAL | Salió de testnet, de un test corrido, o de una lectura del repo en la fecha que indica el archivo |
| SIMULATED | El caso es de prueba y está marcado así. No es un pago de una persona |
| ILLUSTRATIVE | Sirve para explicar. No es una medición ni una transacción |

Un test que no se corrió se escribe **no corrido**. Un hash que no existe se escribe **no hay tx**.

Las afirmaciones dentro de un archivo pueden llevar `[FACT]`, `[INFERENCE]`, `[RECOMMENDATION]` o `[UNVERIFIED]`.

Hay tres cuentas. No se mezclan sus hashes:

- Ensayos por CLI publicados: `stellar/deployment.md`
- App en el emulador (contacto Mamá, guardián Diego): `demo/rehearsal.md`, cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`
- Ataques firmados de P9, porque las semillas publicadas no estaban en el clon: `security/bypass-matrix.md`, cuenta `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`

El activo de las tres es un SAC de prueba emitido por la dueña. No es el USDC de Circle.
