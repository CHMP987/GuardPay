# Sesión atacante

6 oct 2026, 22:18–22:32 UTC. Etiqueta **REAL** en lo que Horizon o la CLI devolvieron. No es un bloque de dos horas de reloj: es lo que se ejecutó en esta máquina. Lo que no se llegó a enviar queda escrito.

No se leyó ni se imprimió ninguna semilla. La cuenta de comisiones de los envíos nuevos es `GC7SNM4SEWEZTPDNXSVUWLTDXVOB7DGKKRQ3OTBTQ75OETUULQZFSFKZ`, fondeada con Friendbot. Su semilla quedó fuera del repo.

Premisa que se comprobó al cerrar: `spent_today` de la cuenta CLI seguía en `100000000` y el saldo del SAC de prueba en `3900000000` (390 unidades de display). Eso cuadra con los ensayos ★ anteriores (10 del carril de contacto y 100 del hold ejecutado) y con que estos intentos no movieron saldo.

## Intentos

| Hora UTC | Qué se intentó | Resultado | Hash |
| --- | --- | --- | --- |
| 22:26 | `queue` sin firma de la dueña, firma de cuenta vacía, y envío al ledger | Rechazo. El hold 2 no existe (`HoldNotFound` 2). | `20c5b59814acf635c33a92405c7897c398454a31481585b295983630e499b4c7` |
| 22:27 | `transfer` de 1 unidad a un contacto, firma vacía | Rechazo en `__check_auth`. | `a5d43e1dc7598aa04f05ee7f621f308ec2bbfffdf9b45426fc74a85d2870f336` |
| 22:27 | `burn` de la cuenta, firma vacía | Rechazo en `__check_auth`. No es el GH-17 con firma válida: ese sigue verde solo en nativo. | `ea0aa8a03b7e6f95ca53856d8cf8e01e332c0eb642769a88c534d9304360b436` |
| 22:29 | `approve` con firma vacía | Rechazo en `__check_auth`. El `approve` con firma válida sigue siendo GH-16. | `b3edc7ccb0d1ed2136e061f9cddb638140f29f3e9c3d31dd0a8c962d0aadadc0` |
| 22:31 | `enforce` directo de un `transfer` dentro del tope | La simulación en modo record aceptó la lista blanca. El ledger rechazó en `require_auth`. El gasto no cambió. | `0e46055b18304e54f69a1ec39891bf12fb75ece15e16680ab17393410804678f` |
| 22:29 | Reenvío del sobre de GH-06 | `TxBadSeq`. Mismo hash. No hay tx nueva. | no hay tx |
| 22:25 | `cancel` y `mark_executed` sobre holds ya terminales | La simulación en modo record llegó a `InvalidStatus` (3) del registro. No demuestra la falta de firma: ese modo anota la autorización y sigue. No se envió. | no hay tx |
| 22:25 | `transfer_from` con allowance 0 | Simulación: error 9 del SAC, "not enough allowance". No se envió. | no hay tx |
| 22:25 | `clawback` del saldo de la cuenta | Simulación: error 10 del SAC, "balance isn't clawbackable". No se envió. | no hay tx |
| 22:25 | `mint` hacia la cuenta de comisiones | Simulación: error 13, falta trustline. No se envió. | no hay tx |
| 22:24 | `execute`, `upgrade`, `add_context_rule`, `add_signer`, `add_policy`, `remove_context_rule` | `unrecognized subcommand`. | no hay tx |
| 22:29 | Argumento muxed inválido en `transfer` | La CLI dijo que el alias no existe. | no hay tx |

Esos cinco rechazos del ledger (`20c5b598…`, `a5d43e1d…`, `ea0aa8a0…`, `b3edc7cc…` y `0e46055b…`) están en Horizon con `successful=false`.

## Lo que no es un bypass, y hay que decirlo igual

`[FACT]` Una firma vacía no entra limpia a la lista blanca. `__check_auth` termina en `UnreachableCodeReached` y el host lo sube como `auth invalid_action`. La transacción revierte. No es una aceptación.

`[FACT]` Llamar a `enforce` en simulación con el modo que registra autorizaciones ejecuta la lista blanca sin exigir todavía la firma. Eso no escribe el ledger. El envío posterior sí exigió `require_auth` y falló. Quien mire solo la simulación puede creer que el gasto se actualizó. No se actualizó.

`[FACT]` El SAC de prueba publica `mint`, `clawback`, `set_admin` y `set_authorized`. El admin leído del storage es la dueña `GDJ3EJJG…`, no la cuenta contrato y no el guardián. Esas funciones autorizan al admin, no pasan por `guardian_hold`. No se invocaron con la clave del emisor: no está en esta máquina. `clawback` del saldo actual falló en simulación porque el saldo no es clawbackable. `[INFERENCE]` Con la clave del emisor, `mint` podría crear unidades de este SAC de prueba fuera de la política. Eso no le da al guardián una forma de gastar, y no es el USDC de Circle. No se afirma que la política cubra las funciones de admin del emisor.

`[FACT]` El spec del registro no tiene una función que transfiera tokens. `mark_executed` solo cambia el estado, y en esta simulación ni siquiera pasó de un hold que ya estaba terminado.

No se encontró una llamada que, ya en el ledger, moviera saldo, creara un hold o añadiera una regla.
