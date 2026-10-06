# Escena 6 — Una sola regla, `approve`, y una segunda regla

Etiqueta **REAL** en lo que se corrió. SAC de prueba.

## Test

La app lee la regla. La terminal intenta `approve` y `add_context_rule`.

## Expected

La pantalla muestra una regla y la política GuardianHold. `approve` lo rechaza el contrato. La segunda regla no existe como función: la CLI lo dice y no hay transacción.

## Actual

**App, dos veces**, cuenta `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`. De Inicio a "Política: GuardianHold": 19,4 s y 21,9 s. Texto en pantalla: "Esta cuenta tiene 1 regla". No hay hash: es una lectura.

**Terminal, una vez**, cuenta de ataques:

- `approve`: `e1afaf76d12717f933a9a4b3d13ee025505e78d9fb79b2a20b248352747b1361`, `successful=false`, ledger 5059853, 2026-10-06T21:40:52Z, código 3
- `add_context_rule`: `unrecognized subcommand`. Hash: no hay tx

La cuenta publicada ya tenía la misma respuesta de la CLI y una sola regla: `evidence/smart-account/rules-onchain.md`. GH-16 publicado: `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434`.

## Contract

App: `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`. Terminal: `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP`.

## Transaction

`approve` del SAC de prueba. La segunda regla no llega a transacción.

## Hash

`e1afaf76d12717f933a9a4b3d13ee025505e78d9fb79b2a20b248352747b1361` y no hay tx.

## Timestamp

App: 2026-10-06, ensayo de las 15:33–15:50 UTC. Terminal: 2026-10-06T21:40:52Z.

## Input

`approve` de 25 unidades, spender el atacante. Luego el nombre `add_context_rule`.

## Output

Contrato 3 en `approve`. CLI: subcomando no reconocido.

## Captura o log

`rehearsal/r2-s6.mp4` muestra la regla. `r1-s6.mp4` la dejó fuera del borde. La terminal está en la matriz.

## Conclusión

`[FACT]` La app leyó una regla, dos veces. `approve` falló una vez en la cuenta de ataques. Añadir otra regla no produjo transacción.
