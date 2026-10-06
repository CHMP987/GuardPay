# Reglas en la cuenta desplegada

6 oct 2026. Lectura REAL del ledger de testnet, después de los ensayos ★.

## Lectura

`[FACT]` `scripts/read-rules.ps1` terminó así:

```text
rules 1
context Default
name default
signer Delegated GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y
policy CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI
one Default rule, signer is the owner, policy is GuardianHold
```

La cuenta es `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX`. `Count` está en el storage de instancia y vale 1. No hay entrada `ContextRuleData(1)`. La política almacenada es el `guardian_hold` desplegado. El único firmante es `Delegated` de la dueña. El guardián no está en esa entrada.

## Entradas que no existen

`[FACT]` El spec publicado por la cuenta solo lista `__check_auth`.

- `execute`: la CLI respondió `unrecognized subcommand 'execute'`. No hubo transacción.
- `upgrade`: la CLI respondió `unrecognized subcommand 'upgrade'`. No hubo transacción.

## Segundo install

`[FACT]` Una segunda llamada a `guardian_hold.install`, con los mismos parámetros de la demo, se simuló en modo record y el contrato falló con error 1 (`AlreadyInstalled`). No hay hash de ledger de esa simulación: el RPC no devuelve un sobre enviable cuando la simulación revierte.

Al reintentar con la autorización de la cuenta, la simulación en modo enforce murió antes con `Contract re-entry is not allowed`: `install` llama a `require_auth` de la cuenta, y `__check_auth` vuelve a entrar en la política. Tampoco hay hash de esa simulación.

La cuenta sigue con una sola regla después de ese intento.
