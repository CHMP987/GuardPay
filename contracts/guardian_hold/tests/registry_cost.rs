//! F-02: cost of the registry scan as an account accumulates hold records.
//! Opt-in: it needs the deployed wasm, so it is ignored unless run with
//! `GP_WASM_DIR=<dir with account.wasm, guardian_hold.wasm, hold_registry.wasm>
//!  cargo test -p guardian_hold --test registry_cost -- --ignored --nocapture`.
//! Records are written straight into the registry's storage (the same keys it uses),
//! so seeding N records costs nothing; then each measured call runs in the wasm VM.
//! With `GP_REGISTRY=source` the registry is this repo's `hold_registry` crate,
//! registered natively: ledger entries count the same, instructions do not.

extern crate std;

use guardian_hold::GuardianHoldAccountParams;
use hold_registry::{HoldRecord, HoldStatus, HOLD_DURATION_SECS};
use soroban_sdk::xdr::ToXdr;
use soroban_sdk::{
    auth::{Context, ContractContext},
    contracttype,
    testutils::{Address as _, EnvTestConfig, Ledger as _, MockAuth, MockAuthInvoke},
    vec, Address, Bytes, BytesN, Env, IntoVal, Map, Symbol, Val, Vec,
};
use stellar_accounts::smart_account::{AuthPayload, Signer, SmartAccountError};

const BASE_TS: u64 = 1_700_000_000;

/// Per-transaction limits the SDK enforces by default in tests
/// (`InvocationResourceLimits::mainnet()`, its snapshot of 2026-07-10).
const MAX_INSTRUCTIONS: i64 = 400_000_000;
const MAX_LEDGER_ENTRIES: u32 = 400;

/// Same variants as the registry's private `DataKey`; a contracttype enum is
/// encoded by variant name, so these keys address the registry's own entries.
#[contracttype]
#[derive(Clone)]
enum RegistryKey {
    Guardian,
    GuardianHold,
    NextId(Address),
    Hold(Address, u64),
}

struct Wasm {
    account: std::vec::Vec<u8>,
    guardian_hold: std::vec::Vec<u8>,
    /// None: register the registry from source instead of its wasm.
    hold_registry: Option<std::vec::Vec<u8>>,
}

fn load_wasm() -> Wasm {
    let dir = std::env::var("GP_WASM_DIR").expect("set GP_WASM_DIR to the wasm artifact directory");
    let read = |name: &str| std::fs::read(std::path::Path::new(&dir).join(name)).unwrap();
    Wasm {
        account: read("account.wasm"),
        guardian_hold: read("guardian_hold.wasm"),
        hold_registry: match std::env::var("GP_REGISTRY").as_deref() {
            Ok("source") => None,
            _ => Some(read("hold_registry.wasm")),
        },
    }
}

struct Scenario {
    owner: Address,
    trusted: Address,
    stranger: Address,
    usdc: Address,
    account: Address,
    registry: Address,
}

fn deploy(e: &Env, w: &Wasm) -> Scenario {
    e.mock_all_auths();
    e.ledger().with_mut(|li| li.timestamp = BASE_TS);
    let owner = Address::generate(e);
    let trusted = Address::generate(e);
    let stranger = Address::generate(e);
    let guardian = Address::generate(e);
    let usdc = Address::generate(e);
    let gh = e.register(w.guardian_hold.as_slice(), ());
    let registry = match &w.hold_registry {
        Some(wasm) => e.register(wasm.as_slice(), (guardian, gh.clone())),
        None => e.register(hold_registry::HoldRegistry, (guardian, gh.clone())),
    };
    let params = GuardianHoldAccountParams {
        owner: owner.clone(),
        usdc: usdc.clone(),
        trusted_contacts: Vec::from_array(e, [trusted.clone()]),
        registry: registry.clone(),
        daily_cap: 1_000_000,
    };
    let install_val: Val = params.into_val(e);
    let account = e.register(w.account.as_slice(), (owner.clone(), gh, install_val));
    e.set_auths(&[]);
    Scenario {
        owner,
        trusted,
        stranger,
        usdc,
        account,
        registry,
    }
}

/// Writes `n` finished records (Executed) for the account, each to its own destination.
fn seed_finished(e: &Env, s: &Scenario, n: u64) {
    e.as_contract(&s.registry, || {
        let store = e.storage().persistent();
        let first: u64 = store.get(&RegistryKey::NextId(s.account.clone())).unwrap_or(0);
        for id in first..first + n {
            let record = HoldRecord {
                token: s.usdc.clone(),
                destination: Address::generate(e),
                amount: 100,
                created_at: BASE_TS,
                ready_at: BASE_TS + HOLD_DURATION_SECS,
                status: HoldStatus::Executed,
            };
            store.set(&RegistryKey::Hold(s.account.clone(), id), &record);
        }
        store.set(&RegistryKey::NextId(s.account.clone()), &(first + n));
    });
}

fn transfer_context(e: &Env, s: &Scenario, to: &Address, amount: i128) -> Context {
    Context::Contract(ContractContext {
        contract: s.usdc.clone(),
        fn_name: Symbol::new(e, "transfer"),
        args: (s.account.clone(), to.clone(), amount).into_val(e),
    })
}

/// The account's `__check_auth` for one context, signed by the owner. This is the
/// part of a payment that runs the policy and, through it, the registry.
fn owner_check_auth(
    e: &Env,
    s: &Scenario,
    ctx: Context,
) -> Result<(), Result<SmartAccountError, soroban_sdk::InvokeError>> {
    let sig_payload = e.crypto().sha256(&Bytes::from_array(e, &[7u8; 32]));
    let rule_ids: Vec<u32> = vec![e, 0];
    let mut preimage = sig_payload.to_bytes().to_bytes();
    preimage.append(&rule_ids.clone().to_xdr(e));
    let digest = e.crypto().sha256(&preimage);
    let invoke = MockAuthInvoke {
        contract: &s.account,
        fn_name: "__check_auth",
        args: (digest,).into_val(e),
        sub_invokes: &[],
    };
    e.mock_auths(&[MockAuth {
        address: &s.owner,
        invoke: &invoke,
    }]);
    let mut signers = Map::new(e);
    signers.set(Signer::Delegated(s.owner.clone()), Bytes::new(e));
    let payload = AuthPayload {
        signers,
        context_rule_ids: rule_ids,
    };
    let sig: BytesN<32> = sig_payload.into();
    let out = e.try_invoke_contract_check_auth::<SmartAccountError>(
        &s.account,
        &sig,
        payload.into_val(e),
        &vec![e, ctx],
    );
    e.set_auths(&[]);
    out
}

/// The registry's `queue` as the top-level call (account auth mocked, so the
/// policy's constant cost for the queue context is not included).
fn registry_queue(e: &Env, s: &Scenario, to: &Address, amount: i128) -> Result<(), ()> {
    e.mock_all_auths();
    let out = e.try_invoke_contract::<u64, soroban_sdk::Error>(
        &s.registry,
        &Symbol::new(e, "queue"),
        vec![
            e,
            s.account.clone().into_val(e),
            s.usdc.clone().into_val(e),
            to.clone().into_val(e),
            amount.into_val(e),
        ],
    );
    e.set_auths(&[]);
    match out {
        Ok(Ok(_)) => Ok(()),
        _ => Err(()),
    }
}

struct Row {
    ok: bool,
    instructions: i64,
    entries: u32,
    mem_read: u32,
    disk_read: u32,
    writes: u32,
}

fn row(e: &Env, ok: bool) -> Row {
    let r = e.cost_estimate().resources();
    Row {
        ok,
        instructions: r.instructions,
        entries: r.memory_read_entries + r.disk_read_entries + r.write_entries,
        mem_read: r.memory_read_entries,
        disk_read: r.disk_read_entries,
        writes: r.write_entries,
    }
}

fn print(op: &str, n: u64, r: &Row) {
    std::println!(
        "{op},{n},{},{},{},{},{},{}",
        if r.ok { "ok" } else { "FAIL" },
        r.instructions,
        r.entries,
        r.mem_read,
        r.disk_read,
        r.writes
    );
}

/// One fresh environment per (operation, N), with limits off so the cost past
/// the limit is still reported; the verdict is checked against the limits below.
fn measure(w: &Wasm, n: u64, op: &str) -> Row {
    let e = Env::new_with_config(EnvTestConfig {
        capture_snapshot_at_drop: false,
    });
    let s = deploy(&e, w);
    e.cost_estimate().disable_resource_limits();
    seed_finished(&e, &s, n);
    let ok = match op {
        // A payment to a trusted contact with no hold: since F-01 it asks the registry first.
        "contact_payment" => {
            let ctx = transfer_context(&e, &s, &s.trusted, 10);
            owner_check_auth(&e, &s, ctx).is_ok()
        }
        // A new hold: the registry rejects duplicates by scanning.
        "queue" => registry_queue(&e, &s, &s.stranger, 100).is_ok(),
        // Releasing the newest matured hold: the scan reaches it last.
        "release" => {
            registry_queue(&e, &s, &s.stranger, 100).unwrap();
            e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
            let ctx = transfer_context(&e, &s, &s.stranger, 100);
            owner_check_auth(&e, &s, ctx).is_ok()
        }
        _ => unreachable!(),
    };
    row(&e, ok)
}

#[test]
#[ignore = "needs GP_WASM_DIR (deployed wasm); prints a CSV"]
fn registry_scan_cost() {
    let w = load_wasm();
    let registry = if w.hold_registry.is_some() { "wasm" } else { "source" };
    std::println!("registry: {registry}");
    std::println!("limits: instructions={MAX_INSTRUCTIONS} ledger_entries={MAX_LEDGER_ENTRIES}");
    std::println!("op,records,result,instructions,ledger_entries,memory_read,disk_read,writes");
    let sizes: &[u64] = &[0, 10, 50, 100, 200, 300, 350, 380, 390, 400, 500];
    for op in ["contact_payment", "queue", "release"] {
        for &n in sizes {
            let r = measure(&w, n, op);
            print(op, n, &r);
            let within = r.instructions <= MAX_INSTRUCTIONS && r.entries <= MAX_LEDGER_ENTRIES;
            std::println!("  within_limits={within}");
        }
    }
}
