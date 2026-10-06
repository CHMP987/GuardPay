# Tests de políticas — P2

6 oct 2026. Datos REAL de `cargo test` nativo. Sin hashes de testnet.

## Comando

```text
cargo test -p account -p guardian_hold -p hold_registry --offline -- --test-threads=8
```

`[FACT]` Exit 0. Nombres y resultados en `evidence/policies/cargo-test-p2.txt` (rutas de binarios acortadas; el run real fue ese comando).

Resumen:

| Crate | Resultado |
| --- | --- |
| `account` | 1 passed |
| `guardian_hold` (lib) | 0 tests |
| `guardian_hold` `tests/must.rs` | 35 passed; 0 failed |
| `hold_registry` | 0 tests |

`cargo test --workspace --offline` en la misma sesión también terminó exit 0. Ese comando además ejecutó los tests del paquete vendorizado `stellar-accounts` (187 passed). No forman parte del MUST de GuardPay.

## Grep

`[FACT]`

- `rg "execute|upgrade" contracts/account` → vacío.
- `queue` no recibe `ready_at`. Lo calcula el registro: `created_at + HOLD_DURATION_SECS` (120).
- `guardian_hold` y `hold_registry` no llaman a `transfer`.

## Wasm

`stellar contract build` de los tres crates de producción: exit 0. Hashes en `evidence/stellar/deployment.md`. Los ensayos ★ de testnet están en ese mismo archivo.

## MUST

Cada fila es un test de `contracts/guardian_hold/tests/must.rs`. Todos `pasa`.

| ID | Test | Resultado |
| --- | --- | --- |
| GH-01 | `gh_01_transfer_without_owner_signature_rejected` | pasa |
| GH-02 | `gh_02_trusted_payment_within_cap` | pasa |
| GH-03 | `gh_03_unknown_destination_without_hold_rejected` | pasa |
| GH-04 | `gh_04_queue_computes_ready_at` | pasa |
| GH-05 | `gh_05_transfer_before_ready_at_rejected` | pasa |
| GH-06 | `gh_06_mature_transfer_needs_a_new_owner_signature` | pasa |
| GH-07 | `gh_07_guardian_cancel` | pasa |
| GH-07b | `gh_07b_stranger_cannot_cancel` | pasa |
| GH-08 | `gh_08_stopped_hold_cannot_be_sent` | pasa |
| GH-09 | `gh_09_guardian_cannot_spend` | pasa |
| GH-10 | `gh_10_owner_signature_still_obeys_the_allowlist` | pasa |
| GH-11 | `gh_11_fee_payer_authorizes_nothing` | pasa |
| GH-12 | `gh_12_no_ai_input_changes_rejection` | pasa |
| GH-13 | `gh_13_only_one_rule_and_no_admin_entry_points` | pasa |
| GH-14 | `gh_14_transfer_lanes` | pasa |
| GH-15 | `gh_15_transfer_from_rejected` | pasa |
| GH-16 | `gh_16_approve_rejected` | pasa |
| GH-17 | `gh_17_burn_rejected` | pasa |
| GH-18 | `gh_18_nested_contract_rejected` | pasa |
| GH-18b | `gh_18b_nested_contract_rejected_with_mature_hold` | pasa |
| GH-19 | `gh_19_replay_rejected` | pasa |
| GH-20 | `gh_20_other_destination_rejected` | pasa |
| GH-20b | `gh_20b_muxed_destination_rejected` | pasa |
| GH-21 | `gh_21_other_amount_rejected` | pasa |
| GH-22 | `gh_22_other_token_rejected` | pasa |
| GH-23 | `gh_23_other_account_hold_rejected` | pasa |
| GH-24 | `gh_24_mature_transfer_without_owner_rejected` | pasa |
| GH-25 | `gh_25_admin_names_are_not_entry_points` | pasa |
| GH-26 | `gh_26_direct_enforce_rejected` | pasa |
| GH-27 | `gh_27_queue_without_owner_rejected` | pasa |
| GH-31 | `gh_31_second_identical_queue_rejected` | pasa |
| GH-32 | `gh_32_second_install_rejected` | pasa |

## Fuera del conteo MUST

| Caso | Test | Resultado |
| --- | --- | --- |
| Overflow i128 | `overflow_near_i128_max_rejected` | pasa. Tope `i128::MAX`, primer `transfer` de `MAX` pasa, el siguiente de 1 se rechaza. |
| Persistencia | `persistent_records_survive_ledger_advance` | pasa. Sequence +50000 y timestamp +1000 s (mismo día UTC). El hold sigue Retained y `spent_today` sigue en 10. |
| Reinicio del día UTC | `daily_window_resets_on_the_next_utc_day` | pasa. Llena el tope (50), el siguiente 1 se rechaza, al día siguiente 1 pasa. Cubre el comportamiento de GH-28 con un solo pago que llena el tope, no la secuencia 30+20. |
| GH-28 secuencia 30+20 | — | no corrido. SHOULD. |
| GH-29 dos pagos en el mismo ledger | — | no corrido. SHOULD. |
| GH-30 vencimiento / `expires_at` | — | no corrido. T-049 sigue pendiente: T-006 no lo autorizó. |
| Cancel de la dueña | — | no corrido. Misma razón. |

## Delta respecto a `docs/INTERFACES.md`

`[FACT]` El archivo congelado en Sync 1 sigue describiendo el spike: un solo `trusted_contact` y sin `daily_cap`. El código de producción usa otra forma. No se reescribió `INTERFACES.md` (hace falta OK humano).

Producción, hoy:

```rust
pub struct GuardianHoldAccountParams {
    pub owner: Address,
    pub usdc: Address,
    pub trusted_contacts: Vec<Address>, // 1..=3
    pub registry: Address,
    pub daily_cap: i128,
}

// HoldRegistry, igual que el spike, más:
pub fn list_retained(env: Env, account: Address) -> Vec<u64>

// GuardianHold, lectura:
pub fn spent_today(env: Env, smart_account: Address) -> i128
```

El guardián entra en el constructor del registro, no en los parámetros de `install`. La duración es la constante `HOLD_DURATION_SECS = 120` dentro de `queue`, no un argumento.
