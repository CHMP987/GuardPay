# Relectura de la cadena

6 oct 2026. **REAL.** RPC de testnet vía stellar-cli 28.1.0. Sin semilla. Ledger de las lecturas: alrededor de 5060374–5060385.

## Cuenta de los ensayos CLI

Contrato `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX`.

| Lectura | Valor |
| --- | --- |
| Wasm | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` |
| `Count` | 1 |
| `NextId`, `NextPolicyId`, `NextSignerId` | 1, 1, 1 |
| `ContextRuleData(0)` | contexto `Default`, nombre `default`, `signer_ids` `[0]`, `policy_ids` `[0]` |
| `ContextRuleData(1)` | sin entrada |
| `SignerData(0)` | `Delegated` `GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y` |
| `SignerData(1)` | sin entrada |
| `PolicyData(0)` | `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI` |

El spec publicado tiene `__constructor` y `__check_auth`. No tiene `execute`, `upgrade`, `add_context_rule`, `add_signer`, `add_policy` ni `remove_context_rule`. La CLI respondió `unrecognized subcommand` para cada una. Hash: no hay tx.

## Cuenta de la demo en emulador

Contrato `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`. Mismo wasm. `Count` 1. Firmante `Delegated` `GDIFOBI5NW4N5DHBBYMRLJ766X4EWN6VQABCRUJ6SRMJ3SW2AI56U5UD`. Política la misma `CCGIEMIU…`. Sin regla 1.

## Registro CLI

`CBEDM2DZYJ7VEU434J7D44VVQ6ADW4IAG5UHE74PY72KI5GNRTMRU36S`.

- `Guardian`: `GA7G4HPJFYRWD6SI5DW7YYXYNAXZD7JXD3Z4L5GFJIMVBSNBNPBF7JZ3`
- `GuardianHold`: `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI`
- Funciones publicadas: `queue`, `cancel`, `mark_executed`, `get_hold`, `find_retained`, `list_retained`, `__constructor`. Ninguna se llama `transfer`.

## SAC de prueba

`CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R`. No es el USDC de Circle.

- Símbolo publicado: `USDC`. Nombre: `USDC:GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y`. Decimales: 7.
- Admin: `GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y`.
- También publica `mint`, `clawback`, `set_admin`, `approve`, `transfer_from`. Esas de admin no pasan por la política. Ver la sesión atacante.

## Conclusión de la lectura

`[FACT]` Hay una regla. La política almacenada es `guardian_hold`. El guardián del registro no es el firmante. El registro no publica una función de transferencia. `execute` y `upgrade` no están en el spec.
