# Spike A — evidencia

Pin OpenZeppelin `stellar-contracts` @ `b40c5eaefe6a29f0030f00bd2d730b7a91cce330`. soroban-sdk 28.0.0; `stellar-accounts` 0.7.1.

**Firmas congeladas:** `docs/INTERFACES.md`

## Veredicto Spike A: **GO**

A0, A1 y los 13 tests GH listados pasan en la corrida de verificación T-028 (6 oct 2026).

| ID | `[FACT]` | Salida clave |
| --- | --- | --- |
| A0 | pasa | `do_check_auth_rule_selection_downgrade_fails - should panic ... ok` |
| A1 | pasa | `A1 veredicto R6: pasa`; CPU budget 122372; write_entries=2 |
| GH-01 | pasa | `GH-01 error literal: Contract(2)`; test `ok` |
| GH-04 | pasa | `GH-04 hold id=0 ready_at=1700000120` |
| GH-05 | pasa | `GH-05 error literal: Contract(7)` |
| GH-06 | pasa | `(a) Contract(2)`; `(b) result: Ok(())` |
| GH-09 | pasa | `(a)(b) UnauthorizedSigner`; `(c) G no mueve fondos` |
| GH-13 | pasa | `ContextRuleNotFound` / `Abort` |
| GH-20 | pasa | `Contract(3)` |
| GH-21 | pasa | `Contract(3)` |
| GH-22 | pasa | `Contract(3)` |
| GH-23 | pasa | `Contract(3)`; holds `Retained` |
| GH-24 | pasa | `(a) Contract(2)`; `(b) UnauthorizedSigner` |
| GH-25 | pasa | `execute`/`upgrade`/admin → `Abort` |
| GH-26 | pasa | `HostError: Error(Auth, InvalidAction)` |

---

## A0 — digest liga `context_rule_ids`

`[FACT]` pasa (T-019; no repetido en T-028).

Directorio: `.scratch/oz-g2`

```
cargo test -p stellar-accounts do_check_auth_rule_selection_downgrade_fails -- --nocapture
```

```
test smart_account::test::context_rules::do_check_auth_rule_selection_downgrade_fails - should panic ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 186 filtered out; finished in 0.16s
```

## A1 — reentrada / recursos R6

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/Cargo.toml spike_a1 -- --nocapture
```

```
running 1 test
A1 CPU instructions (budget): 122372
A1 memory bytes (budget): 52996
A1 I/O resources: instructions=90962 mem_bytes=17920 disk_read_entries=0 memory_read_entries=6 write_entries=2 disk_read_bytes=0 write_bytes=164
A1 veredicto R6: pasa
test spike_a1::spike_a1_r6_policy_registry_in_check_auth ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.01s
```

---

## GH-01 — transfer sin firma de dueña

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_01 -- --nocapture
```

```
GH-01 error literal: Contract(2)
test gh_01_transfer_without_owner_signature_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-04 — queue crea hold Retained con ready_at

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_04 -- --nocapture
```

```
GH-04 hold id=0 ready_at=1700000120
test gh_04_queue_creates_retained_hold_with_ready_at ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.16s
```

## GH-05 — transfer antes de ready_at

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_05 -- --nocapture
```

```
GH-05 error literal: Contract(7)
test gh_05_transfer_before_ready_at_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.16s
```

## GH-06 — transfer maduro + mark Executed

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_06 -- --nocapture
```

```
GH-06(a) error literal: Contract(2)
GH-06(b) result: Ok(())
test gh_06_mature_transfer_requires_owner_and_marks_executed ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.16s
```

## GH-09 — guardián no es firmante ni mueve fondos

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_09 -- --nocapture
```

```
GH-09(a) error literal: SmartAccountError(UnauthorizedSigner)
GH-09(b) error literal: SmartAccountError(UnauthorizedSigner)
GH-09(c) queue as G: HostError: Error(Auth, InvalidAction)
GH-09(c) mark_executed as G: HostError: Error(Auth, InvalidAction)
GH-09(c) enforce as G: HostError: Error(Auth, InvalidAction)
GH-09(c) install as G: HostError: Error(Auth, InvalidAction)
GH-09(c) uninstall as G: HostError: Error(Auth, InvalidAction)
test gh_09_a_transfer_to_guardian_signed_by_guardian_rejected ... ok
test gh_09_b_approve_with_guardian_cosigner_rejected ... ok
test gh_09_c_guardian_public_calls_no_funds_or_config_change ... ok

test result: ok. 3 passed; 0 failed; 0 ignored; 0 measured; 17 filtered out; finished in 0.14s
```

## GH-13 — sin regla de contexto alternativa

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_13 -- --nocapture
```

```
GH-13(a) error literal: SmartAccountError(ContextRuleNotFound)
GH-13(b) error literal: SmartAccountError(ContextRuleNotFound)
GH-13(c) error literal: Abort
test gh_13_no_alternate_context_rule ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-20 — destino incorrecto

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_20 -- --nocapture
```

```
GH-20 error literal: Contract(3)
test gh_20_wrong_destination_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-21 — monto incorrecto

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_21 -- --nocapture
```

```
GH-21(a) error literal: Contract(3)
GH-21(b) error literal: Contract(3)
test gh_21_wrong_amount_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-22 — token incorrecto

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_22 -- --nocapture
```

```
GH-22 error literal: Contract(3)
test gh_22_wrong_token_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-23 — hold de otra cuenta no autoriza

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_23 -- --nocapture
```

```
GH-23 error literal: Contract(3)
test gh_23_other_accounts_hold_does_not_authorize_transfer ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-24 — hold maduro sin dueña en signers

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_24 -- --nocapture
```

```
GH-24(a) error literal: Contract(2)
GH-24(b) error literal: SmartAccountError(UnauthorizedSigner)
test gh_24_mature_hold_transfer_without_owner_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-25 — entrypoints admin no existen

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_25 -- --nocapture
```

```
GH-25 execute error literal: Abort
GH-25 upgrade error literal: Abort
GH-25 add_policy error literal: Abort
GH-25 remove_policy error literal: Abort
GH-25 add_signer error literal: Abort
GH-25 remove_context_rule error literal: Abort
test gh_25_admin_entrypoints_do_not_exist ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

## GH-26 — enforce directo por atacante

`[FACT]` pasa.

```
cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml gh_26 -- --nocapture
```

```
GH-26 error literal: HostError: Error(Auth, InvalidAction)
test gh_26_direct_enforce_by_attacker_rejected ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 19 filtered out; finished in 0.17s
```

---

## Verificación obligatoria T-028 (salida literal)

### `cargo test --manifest-path spikes/guardian-hold/account/Cargo.toml -- --nocapture`

```
    Finished `test` profile [unoptimized + debuginfo] target(s) in 0.80s
     Running unittests src\lib.rs (...)

running 1 test
test tests::constructor_creates_single_default_rule ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.08s
```

### `cargo test --manifest-path spikes/guardian-hold/guardian_hold/Cargo.toml -- --nocapture`

```
    Finished `test` profile [unoptimized + debuginfo] target(s) in 4.88s
     Running unittests src\lib.rs (...)

running 5 tests
test tests::enforce_without_account_auth_panics ... ok
test tests::second_install_panics ... ok
test tests::transfer_to_unknown_without_hold_panics ... ok
test tests::enforce_without_owner_signer_panics ... ok
test tests::trusted_transfer_within_cap_succeeds ... ok

test result: ok. 5 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.12s

     Running tests\gh_09.rs (...)

running 3 tests
test gh_09_b_approve_with_guardian_cosigner_rejected ... ok
test gh_09_a_transfer_to_guardian_signed_by_guardian_rejected ... ok
test gh_09_c_guardian_public_calls_no_funds_or_config_change ... ok

test result: ok. 3 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.14s

     Running tests\gh_binding.rs (...)

running 4 tests
test gh_20_wrong_destination_rejected ... ok
test gh_21_wrong_amount_rejected ... ok
test gh_23_other_accounts_hold_does_not_authorize_transfer ... ok
test gh_22_wrong_token_rejected ... ok

test result: ok. 4 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.17s

     Running tests\gh_blockers.rs (...)

running 5 tests
test gh_26_direct_enforce_by_attacker_rejected ... ok
test gh_25_admin_entrypoints_do_not_exist ... ok
test gh_01_transfer_without_owner_signature_rejected ... ok
test gh_13_no_alternate_context_rule ... ok
test gh_24_mature_hold_transfer_without_owner_rejected ... ok

test result: ok. 5 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.17s

     Running tests\gh_cycle.rs (...)

running 3 tests
test gh_05_transfer_before_ready_at_rejected ... ok
test gh_06_mature_transfer_requires_owner_and_marks_executed ... ok
test gh_04_queue_creates_retained_hold_with_ready_at ... ok

test result: ok. 3 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.16s
```

### `cargo test --manifest-path spikes/guardian-hold/hold_registry/Cargo.toml -- --nocapture`

```
    Finished `test` profile [unoptimized + debuginfo] target(s) in 4.83s
     Running unittests src\lib.rs (...)

running 4 tests
test tests::source_has_no_sac_token_or_transfer ... ok
test tests::queue_sets_ready_at_from_ledger_not_client ... ok
test tests::cancel_guardian_succeeds_stranger_fails ... ok
test tests::mark_executed_guardian_hold_succeeds_stranger_fails ... ok

test result: ok. 4 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.11s
```

### `rg "execute|upgrade" spikes/guardian-hold`

```
spikes/guardian-hold\account\src\lib.rs://! This contract deliberately has no execute or upgrade entry points.
spikes/guardian-hold\guardian_hold\src\lib.rs:    registry.mark_executed(&smart_account.clone(), &hold_id);
spikes/guardian-hold\guardian_hold\tests\gh_09.rs:    // mark_executed as guardian → reject (only guardian_hold may call)
spikes/guardian-hold\guardian_hold\tests\gh_09.rs:            fn_name: "mark_executed",
spikes/guardian-hold\guardian_hold\tests\gh_09.rs:        client.mark_executed(&s.account, &hold_id);
spikes/guardian-hold\guardian_hold\tests\gh_09.rs:    println!("GH-09(c) mark_executed as G: {mark_literal}");
spikes/guardian-hold\guardian_hold\tests\gh_09.rs:    assert!(mark_failed, "GH-09(c) guardian mark_executed must fail");
spikes/guardian-hold\guardian_hold\tests\gh_09.rs:        "hold must stay retained after failed mark_executed"
spikes/guardian-hold\hold_registry\src\lib.rs:    pub fn mark_executed(env: Env, account: Address, id: u64) {
spikes/guardian-hold\hold_registry\src\lib.rs:    fn mark_executed_guardian_hold_succeeds_stranger_fails() {
spikes/guardian-hold\hold_registry\src\lib.rs:                fn_name: "mark_executed",
spikes/guardian-hold\hold_registry\src\lib.rs:        client.mark_executed(&account, &id);
spikes/guardian-hold\hold_registry\src\lib.rs:                fn_name: "mark_executed",
spikes/guardian-hold\hold_registry\src\lib.rs:            client.mark_executed(&account2, &id2);
spikes/guardian-hold\hold_registry\src\lib.rs:        assert!(err.is_err(), "stranger mark_executed must fail");
spikes/guardian-hold\hold_registry\test_snapshots\tests\mark_executed_guardian_hold_succeeds_stranger_fails.1.json:              "function_name": "mark_executed",
spikes/guardian-hold\guardian_hold\tests\gh_cycle.rs:fn gh_06_mature_transfer_requires_owner_and_marks_executed() {
spikes/guardian-hold\guardian_hold\tests\gh_blockers.rs:        "execute",
spikes/guardian-hold\guardian_hold\tests\gh_blockers.rs:        "upgrade",
```

`[FACT]` No hay funciones públicas `execute` ni `upgrade` en la cuenta del spike; las coincidencias son comentario, `mark_executed`, tests GH-25 o snapshots.
