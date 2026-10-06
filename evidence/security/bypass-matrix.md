# Matriz de bypass

6 oct 2026, noche. Etiqueta **REAL** donde hay un hash de Horizon. **SIMULATED** no se usa aquí. Lo que no se envió al ledger dice `no hay tx` o `no corrido`.

Ningún ataque de esta lista movió el SAC de prueba ni creó una retención. No apareció un bypass que obligue a retirar una propiedad. CP-9 no está firmado: lo firma un humano.

El activo de todas las filas CLI es el SAC `CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R`, emitido por la dueña `GDJ3EJJG…`. No es el USDC de Circle. La cuenta de estos hashes es `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX`. La cuenta del emulador (`CDWPPTDM…`) es otra. Sus hashes están en `evidence/demo/rehearsal.md` y no se mezclan.

Las ramas salen de `evidence/policies/whitelist-review.md`.

| Ataque | Precondición | Acción | Esperado | Real | Hash | Error | Propiedad |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pago ya detenido (GH-08, escena 4) | Hold cancelado por el guardián. Firma de la dueña. | `transfer` del mismo SAC, destino y monto. | Rechazo. Rama: hold que ya no está Retained. | `[FACT]` Horizon `successful=false`, ledger 5050733, 2026-10-06T09:00:52Z. Citado de `deployment.md`. No reenviado. | `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f` | contrato 3 `NotAllowed` | P2 |
| Guardián transfiere hacia sí mismo (GH-09, escena 5) | El payload nombra al guardián. | `transfer` desde la cuenta hacia el guardián. | Rechazo. El guardián no es firmante. | `[FACT]` `successful=false`, ledger 5050731, 2026-10-06T09:00:42Z. La cuenta rechazó antes de la lista blanca. | `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de` | `UnauthorizedSigner` 3016, luego `auth invalid_action` | P3 |
| Otro destino de un hold maduro (GH-20) | Hold Retained y maduro. Destino distinto y fuera de contactos. | `transfer` con otro `to`. | Rechazo. Rama: no hay hold con esos campos. | Testnet de este caso exacto: **no corrido**. Nativo `gh_20_other_destination_rejected`: verde. El ledger cercano es GH-03 (destino desconocido sin hold), error 3, hash abajo. | no hay tx | nativo: `NotAllowed` 3. GH-03 citado: `a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492` | P1 |
| Otro monto de un hold maduro (GH-21) | Hold Retained y maduro. Monto distinto. | `transfer` con otro `amount`. | Rechazo. Rama de relectura: otro campo no coincide. | Testnet de este caso exacto: **no corrido**. Nativo `gh_21_other_amount_rejected`: verde. GH-05 es el mismo monto antes de `ready_at`, no este caso. | no hay tx | nativo: `HoldNotReady` 7. GH-05 citado: `0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed` | P1 |
| Destino muxed (GH-20b) | Un `to` que no convierte a `Address`. | `transfer` con ese destino. | Rechazo. Rama: `InvalidDestination` (8). | `[FACT]` La CLI de esta corrida respondió `Account alias "MAAA…" not Found`. No armó transacción. Nativo `gh_20b_muxed_destination_rejected`: verde. | no hay tx | rechazo de la CLI, no del contrato | P1 |
| `approve` (GH-16, escena 6) | Firma de la dueña. | `approve` del SAC. | Rechazo. Rama catch-all. | `[FACT]` `successful=false`, ledger 5050729, 2026-10-06T09:00:32Z. | `25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434` | contrato 3 `NotAllowed` | P4 |
| `transfer_from` (GH-15) | Sin allowance previa. | `transfer_from` del atacante sobre la cuenta. | Rechazo. | `[FACT]` Simulación: `Error(Contract, #9)` del SAC, "not enough allowance to spend", 0 contra 10000000. No se envió. Nativo `gh_15_transfer_from_rejected`: verde. Esta simulación murió en el token, no llegó a `enforce`. | no hay tx | token 9, allowance | P4 |
| Segunda context rule (GH-13, GH-25, escena 6) | Cuenta desplegada. | `add_context_rule`. | La función no existe. | `[FACT]` `error: unrecognized subcommand 'add_context_rule'`. Log: `evidence/security/p9-cli-run.log`. | no hay tx | subcomando no reconocido | P5 |
| `execute` (GH-25) | Cuenta desplegada. | `execute`. | La función no existe. | `[FACT]` `error: unrecognized subcommand 'execute'`. | no hay tx | subcomando no reconocido | P5 |
| `upgrade` (GH-25) | Cuenta desplegada. | `upgrade`. | La función no existe. | `[FACT]` `error: unrecognized subcommand 'upgrade'`. | no hay tx | subcomando no reconocido | P5 |
| `enforce` directo (GH-26) | Un tercero llama a la política con un `transfer` a un contacto, dentro del tope. | `guardian_hold.enforce`. | Rechazo en `account.require_auth()`. El gasto no cambia. | `[FACT]` Simulación en modo record aceptó la lista blanca. El envío con firma vacía falló. `spent_today` siguió en `100000000`. Ledger 5060459, 2026-10-06T22:31:22Z, `successful=false`. Fee payer `GC7SNM4…` (Friendbot, semilla fuera del repo). | `0e46055b18304e54f69a1ec39891bf12fb75ece15e16680ab17393410804678f` | `auth invalid_action`; `__check_auth` → `UnreachableCodeReached` | P4 |
| `queue` sin la dueña (GH-27) | Un tercero llama al registro. | `hold_registry.queue` de la cuenta. | Rechazo. No nace un hold. | `[FACT]` `successful=false`, ledger 5060405, 2026-10-06T22:26:52Z. `get_hold` 2 → `HoldNotFound` (2). `list_retained` vacío. | `20c5b59814acf635c33a92405c7897c398454a31481585b295983630e499b4c7` | `auth invalid_action`; `__check_auth` → `UnreachableCodeReached` | P4 |
| Contrato intermedio (GH-18, GH-18b) | Un contrato N llama a `transfer` con la cuenta como `from`, y la dueña firma ese árbol. | Desplegar N y firmar. | Rechazo. Rama catch-all: el contexto no es el SAC `transfer` ni `queue`. | Testnet: **no corrido**. No se desplegó N. Sin semilla de la dueña no se firma el árbol. Nativos GH-18 y GH-18b: verde. | no hay tx | nativo: `NotAllowed` 3 | P1 |
| Replay (GH-19) | GH-06 ya ejecutó el hold 0. | Reenviar el mismo sobre. | La red no lo aplica otra vez. | `[FACT]` El reenvío respondió `TxBadSeq`. El hash que imprime la CLI es el original. No hay transacción nueva. Parte (b), firmar de nuevo el mismo `transfer`: **no corrido**. Nativo `gh_19_replay_rejected`: verde. | no hay tx. Original: `35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573` | `TxBadSeq` | P2 |

## Lectura de la cadena

`[FACT]` 6 oct 2026, ledger cercano a 5060385–5060459. Detalle en `evidence/security/chain-reread.md`.

- `Count` = 1 en la cuenta CLI y en la cuenta del emulador. No hay `ContextRuleData(1)` ni `SignerData(1)`.
- Regla 0: contexto `Default`, nombre `default`.
- Firmante 0: `Delegated` de la dueña. En la cuenta CLI es `GDJ3EJJG…`. En la del emulador es `GDIFOBI5…`. El guardián no está.
- Política 0: `CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI` en las dos cuentas.
- Wasm de ambas cuentas: `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b`.
- El spec de la cuenta solo publica `__constructor` y `__check_auth`.
- El spec del registro publica `queue`, `cancel`, `mark_executed`, `get_hold`, `find_retained`, `list_retained`. No publica `transfer`.
- El guardián guardado en el registro CLI es `GA7G4HPJ…`. La política guardada es `CCGIEMIU…`.

## GH-01 … GH-33

Tres estados y nada más. Los verdes nativos se citan desde `evidence/policies/tests.md`. No se volvieron a correr para conseguir otro hash.

| ID | Estado |
| --- | --- |
| GH-01 | verde |
| GH-02 | verde |
| GH-03 | verde |
| GH-04 | verde |
| GH-05 | verde |
| GH-06 | verde |
| GH-07 | verde |
| GH-07b | verde |
| GH-08 | verde |
| GH-09 | verde |
| GH-10 | verde |
| GH-11 | verde |
| GH-12 | verde |
| GH-13 | verde |
| GH-14 | verde |
| GH-15 | verde |
| GH-16 | verde |
| GH-17 | verde |
| GH-18 | verde |
| GH-18b | verde |
| GH-19 | verde |
| GH-20 | verde |
| GH-20b | verde |
| GH-21 | verde |
| GH-22 | verde |
| GH-23 | verde |
| GH-24 | verde |
| GH-25 | verde |
| GH-26 | verde |
| GH-27 | verde |
| GH-28 | no corrido |
| GH-29 | no corrido |
| GH-30 | no corrido |
| GH-31 | verde |
| GH-32 | verde |
| GH-33 | no corrido |

GH-28 (30 y después 20) y GH-29 (dos transferencias en el mismo ledger) no se corrieron: hace falta la semilla de la dueña. GH-30 no se implementa: no hay `expires_at`. GH-33 no se corre: no hay firmante passkey. El segundo `install` en testnet sigue sin hash de ledger; el nativo GH-32 es verde. Ver `evidence/smart-account/rules-onchain.md`.

Los intentos que no están en la tabla de arriba están en `evidence/security/attacker-session.md`.
