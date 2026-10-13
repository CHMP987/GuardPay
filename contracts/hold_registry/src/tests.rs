//! The registry on its own (F-13): what `queue` computes, who may end a hold,
//! that terminal states are final, and that the retained-hold index (F-02)
//! stays in step with the records and keeps lookups flat as history grows.

extern crate std;

use super::*;
use soroban_sdk::testutils::{Address as _, EnvTestConfig, Ledger as _, MockAuth, MockAuthInvoke};
use soroban_sdk::{vec, IntoVal, InvokeError, Symbol};

const BASE_TS: u64 = 1_700_000_000;

type CallResult = Result<(), Result<HoldRegistryError, InvokeError>>;

struct Setup {
    env: Env,
    registry: Address,
    guardian: Address,
    guardian_hold: Address,
    account: Address,
    token: Address,
    to: Address,
}

impl Setup {
    fn new() -> Self {
        Self::in_env(Env::default())
    }

    fn in_env(env: Env) -> Self {
        env.ledger().with_mut(|li| li.timestamp = BASE_TS);
        let guardian = Address::generate(&env);
        let guardian_hold = Address::generate(&env);
        let registry = env.register(HoldRegistry, (guardian.clone(), guardian_hold.clone()));
        Setup {
            account: Address::generate(&env),
            token: Address::generate(&env),
            to: Address::generate(&env),
            env,
            registry,
            guardian,
            guardian_hold,
        }
    }

    fn client(&self) -> HoldRegistryClient<'_> {
        HoldRegistryClient::new(&self.env, &self.registry)
    }

    /// Queues as the account (its auth mocked, as the policy would grant it).
    fn queue(&self, to: &Address, amount: i128) -> u64 {
        self.env.mock_all_auths();
        let id = self.client().queue(&self.account, &self.token, to, &amount);
        self.env.set_auths(&[]);
        id
    }

    /// Calls `fn_name(account, id)` with only `signer`'s auth mocked.
    fn end_as(&self, signer: &Address, fn_name: &str, id: u64) -> CallResult {
        let args = (self.account.clone(), id).into_val(&self.env);
        self.env.mock_auths(&[MockAuth {
            address: signer,
            invoke: &MockAuthInvoke {
                contract: &self.registry,
                fn_name,
                args,
                sub_invokes: &[],
            },
        }]);
        let out = self.env.try_invoke_contract::<(), HoldRegistryError>(
            &self.registry,
            &Symbol::new(&self.env, fn_name),
            vec![&self.env, self.account.into_val(&self.env), id.into_val(&self.env)],
        );
        self.env.set_auths(&[]);
        out.map(|_| ())
    }

    fn find(&self, to: &Address, amount: i128) -> Option<u64> {
        self.client().find_retained(&self.account, &self.token, to, &amount)
    }

    fn has_active_entry(&self, to: &Address, amount: i128) -> bool {
        self.env.as_contract(&self.registry, || {
            self.env
                .storage()
                .persistent()
                .has(&active_key(&self.account, &self.token, to, amount))
        })
    }
}

#[test]
fn queue_computes_ready_at_and_numbers_holds() {
    let s = Setup::new();
    let first = s.queue(&s.to, 100);
    s.env.ledger().with_mut(|li| li.timestamp = BASE_TS + 7);
    let second = s.queue(&s.to, 200);
    assert_eq!((first, second), (0, 1));

    let record = s.client().get_hold(&s.account, &second);
    assert_eq!(
        record,
        HoldRecord {
            token: s.token.clone(),
            destination: s.to.clone(),
            amount: 200,
            created_at: BASE_TS + 7,
            ready_at: BASE_TS + 7 + HOLD_DURATION_SECS,
            status: HoldStatus::Retained,
        }
    );
    let next: u64 = s.env.as_contract(&s.registry, || {
        s.env
            .storage()
            .persistent()
            .get(&DataKey::NextId(s.account.clone()))
            .unwrap()
    });
    assert_eq!(next, 2);
}

#[test]
fn queue_requires_the_account() {
    let s = Setup::new();
    let out = s.client().try_queue(&s.account, &s.token, &s.to, &100);
    assert!(out.is_err());
    assert_eq!(s.find(&s.to, 100), None);
}

#[test]
fn queue_rejects_non_positive_amounts() {
    let s = Setup::new();
    s.env.mock_all_auths();
    for amount in [0i128, -1] {
        let out = s.client().try_queue(&s.account, &s.token, &s.to, &amount);
        assert_eq!(out, Err(Ok(HoldRegistryError::InvalidAmount.into())));
    }
}

#[test]
fn duplicate_binding_is_rejected_until_the_hold_ends() {
    let s = Setup::new();
    let id = s.queue(&s.to, 100);
    s.env.mock_all_auths();
    let again = s.client().try_queue(&s.account, &s.token, &s.to, &100);
    assert_eq!(again, Err(Ok(HoldRegistryError::DuplicateActive.into())));
    // Any field that differs is another binding.
    assert_eq!(s.queue(&s.to, 101), 1);

    s.end_as(&s.guardian, "cancel", id).unwrap();
    assert_eq!(s.queue(&s.to, 100), 2);
}

#[test]
fn only_the_guardian_cancels() {
    let s = Setup::new();
    let id = s.queue(&s.to, 100);
    for other in [s.account.clone(), s.guardian_hold.clone(), Address::generate(&s.env)] {
        assert!(s.end_as(&other, "cancel", id).is_err());
    }
    assert_eq!(s.client().get_hold(&s.account, &id).status, HoldStatus::Retained);

    s.end_as(&s.guardian, "cancel", id).unwrap();
    assert_eq!(s.client().get_hold(&s.account, &id).status, HoldStatus::Stopped);
}

#[test]
fn only_guardian_hold_marks_executed() {
    let s = Setup::new();
    let id = s.queue(&s.to, 100);
    for other in [s.account.clone(), s.guardian.clone(), Address::generate(&s.env)] {
        assert!(s.end_as(&other, "mark_executed", id).is_err());
    }
    assert_eq!(s.client().get_hold(&s.account, &id).status, HoldStatus::Retained);

    s.end_as(&s.guardian_hold, "mark_executed", id).unwrap();
    assert_eq!(s.client().get_hold(&s.account, &id).status, HoldStatus::Executed);
}

#[test]
fn terminal_states_are_final() {
    let s = Setup::new();
    let stopped = s.queue(&s.to, 100);
    let executed = s.queue(&s.to, 200);
    s.end_as(&s.guardian, "cancel", stopped).unwrap();
    s.end_as(&s.guardian_hold, "mark_executed", executed).unwrap();

    for id in [stopped, executed] {
        assert_eq!(
            s.end_as(&s.guardian, "cancel", id),
            Err(Ok(HoldRegistryError::InvalidStatus))
        );
        assert_eq!(
            s.end_as(&s.guardian_hold, "mark_executed", id),
            Err(Ok(HoldRegistryError::InvalidStatus))
        );
    }
}

#[test]
fn unknown_ids_are_not_found() {
    let s = Setup::new();
    assert_eq!(
        s.client().try_get_hold(&s.account, &0),
        Err(Ok(HoldRegistryError::HoldNotFound.into()))
    );
    assert_eq!(
        s.end_as(&s.guardian, "cancel", 0),
        Err(Ok(HoldRegistryError::HoldNotFound))
    );
    assert_eq!(
        s.end_as(&s.guardian_hold, "mark_executed", 0),
        Err(Ok(HoldRegistryError::HoldNotFound))
    );
}

#[test]
fn index_follows_the_records() {
    let s = Setup::new();
    let other_to = Address::generate(&s.env);
    let a = s.queue(&s.to, 100);
    let b = s.queue(&other_to, 100);
    let c = s.queue(&s.to, 300);
    assert_eq!(s.client().list_retained(&s.account), vec![&s.env, a, b, c]);
    assert_eq!(s.find(&s.to, 100), Some(a));
    assert_eq!(s.find(&other_to, 100), Some(b));
    assert_eq!(s.find(&s.to, 300), Some(c));
    assert_eq!(s.find(&s.to, 999), None);

    // Another account with the same binding sees nothing.
    let stranger = Address::generate(&s.env);
    assert_eq!(s.client().find_retained(&stranger, &s.token, &s.to, &100), None);
    assert_eq!(s.client().list_retained(&stranger).len(), 0);

    s.end_as(&s.guardian, "cancel", b).unwrap();
    s.end_as(&s.guardian_hold, "mark_executed", a).unwrap();
    assert_eq!(s.find(&s.to, 100), None);
    assert_eq!(s.find(&other_to, 100), None);
    assert!(!s.has_active_entry(&s.to, 100));
    assert!(!s.has_active_entry(&other_to, 100));
    assert!(s.has_active_entry(&s.to, 300));
    assert_eq!(s.client().list_retained(&s.account), vec![&s.env, c]);
    // The records themselves stay.
    assert_eq!(s.client().get_hold(&s.account, &a).status, HoldStatus::Executed);
    assert_eq!(s.client().get_hold(&s.account, &b).status, HoldStatus::Stopped);
}

/// Ledger entries the last call touched (reads plus writes), as the footprint counts them.
fn entries_of(e: &Env) -> u32 {
    let r = e.cost_estimate().resources();
    r.memory_read_entries + r.disk_read_entries + r.write_entries
}

/// F-02: the entries `queue` and `find_retained` touch do not grow with the
/// number of finished holds. Before the index, each record cost one more entry.
#[test]
fn lookups_do_not_grow_with_history() {
    let measure = |history: u64| -> (u32, u32) {
        // No snapshot: with 60 holds it would be hundreds of KB of noise.
        let s = Setup::in_env(Env::new_with_config(EnvTestConfig {
            capture_snapshot_at_drop: false,
        }));
        for i in 0..history {
            let id = s.queue(&s.to, 1 + i as i128);
            s.end_as(&s.guardian, "cancel", id).unwrap();
        }
        let target = Address::generate(&s.env);
        s.queue(&target, 100);
        let queued = entries_of(&s.env);
        assert!(s.find(&target, 100).is_some());
        let found = entries_of(&s.env);
        (queued, found)
    };
    let empty = measure(0);
    let long = measure(60);
    std::println!("queue/find entries: empty={empty:?} after 60 finished={long:?}");
    assert_eq!(empty, long);
}
