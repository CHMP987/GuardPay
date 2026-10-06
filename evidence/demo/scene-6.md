# Escena 6 — Una sola regla, `approve`, y una segunda regla

Etiqueta **REAL** en lo que se corrió. SAC de prueba.

## Test

La app lee la regla. La terminal intenta `approve` y `add_context_rule`.

## Expected

La pantalla muestra una regla y la política GuardianHold. `approve` lo rechaza el contrato. La segunda regla no existe como función: la CLI lo dice y no hay transacción.

## Actual

**App, dos veces**, cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`, wasm `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b`. De Inicio a "Política: GuardianHold": 19,4 s y 21,9 s (`rehearsal.md`). Texto en pantalla: "Esta cuenta tiene 1 regla". No hay hash: es una lectura. `chain-reread.md` leyó `Count` = 1 y ninguna regla 1.

**Terminal, una vez**, cuenta de ataques `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`, wasm `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c`:

- `approve`: `e1afaf76d12717f933a9a4b3d13ee025505e78d9fb79b2a20b248352747b1361`, `successful=false`, ledger 5059853, 2026-10-06T21:40:52Z, código 3
- `add_context_rule`: `unrecognized subcommand`. Hash: no hay tx

La cuenta publicada, wasm `986956cc…`, ya tenía la misma respuesta de la CLI y una sola regla (`evidence/smart-account/rules-onchain.md`, `chain-reread.md`). Su `approve` de la mañana es GH-16: `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434`, ledger 5050729, 2026-10-06T09:00:32Z. No es el hash de esta escena.

## Contract

App: `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`. Terminal: `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`.

## Transaction

`approve` del SAC de prueba. La segunda regla no llega a transacción.

## Hash

`e1afaf76…` en la cuenta de ataques. En la cuenta publicada, la segunda regla es no hay tx. En el emulador, la segunda regla no se invocó.

## Timestamp

App: 2026-10-06, ensayo de las 15:33–15:50 UTC. Terminal: 2026-10-06T21:40:52Z.

## Input

`approve` de 25 unidades, spender el atacante, en la cuenta de ataques. Luego el nombre `add_context_rule`.

## Output

Contrato 3 en `approve`. CLI: subcomando no reconocido.

## Captura o log

`rehearsal/r2-s6.mp4` muestra la regla. `r1-s6.mp4` la dejó fuera del borde. La terminal está en `terminal-rejections.mp4` y en la matriz.

## Conclusión

`[FACT]` La app leyó una regla, dos veces, en el emulador. `approve` falló una vez, de noche, en la cuenta de ataques. Añadir otra regla no produjo transacción en esa cuenta ni en la publicada.
