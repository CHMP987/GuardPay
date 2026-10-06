#![allow(unused)]
use soroban_sdk::{
    contract, contracterror, contractimpl, contracttype, panic_with_error, Address, Env,
};

/// Fixed hold duration for the spike base scenario (seconds).
const HOLD_DURATION_SECS: u64 = 120;

#[contracttype]
#[derive(Clone, Debug, Eq, PartialEq)]
pub enum HoldStatus {
    Retained,
    Stopped,
    Executed,
}

#[contracttype]
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct HoldRecord {
    pub token: Address,
    pub destination: Address,
    pub amount: i128,
    pub created_at: u64,
    pub ready_at: u64,
    pub status: HoldStatus,
}

#[contracttype]
enum DataKey {
    Guardian,
    GuardianHold,
    NextId(Address),
    Hold(Address, u64),
}

#[contracterror]
#[derive(Copy, Clone, Debug, Eq, PartialEq, PartialOrd, Ord)]
#[repr(u32)]
pub enum HoldRegistryError {
    InvalidAmount = 1,
    HoldNotFound = 2,
    InvalidStatus = 3,
}

#[contract]
pub struct HoldRegistry;

#[contractimpl]
impl HoldRegistry {
    pub fn __constructor(env: Env, guardian: Address, guardian_hold: Address) {
        env.storage()
            .persistent()
            .set(&DataKey::Guardian, &guardian);
        env.storage()
            .persistent()
            .set(&DataKey::GuardianHold, &guardian_hold);
    }

    /// Creates a retained hold; `ready_at` is derived from the ledger timestamp.
    pub fn queue(
        env: Env,
        account: Address,
        token: Address,
        destination: Address,
        amount: i128,
    ) -> u64 {
        account.require_auth();
        if amount <= 0 {
            panic_with_error!(&env, HoldRegistryError::InvalidAmount);
        }

        let id_key = DataKey::NextId(account.clone());
        let id: u64 = env
            .storage()
            .persistent()
            .get(&id_key)
            .unwrap_or(0);
        env.storage().persistent().set(&id_key, &(id + 1));

        let created_at = env.ledger().timestamp();
        let ready_at = created_at + HOLD_DURATION_SECS;

        let record = HoldRecord {
            token,
            destination,
            amount,
            created_at,
            ready_at,
            status: HoldStatus::Retained,
        };

        env.storage()
            .persistent()
            .set(&DataKey::Hold(account, id), &record);

        id
    }

    pub fn cancel(env: Env, account: Address, id: u64) {
        let guardian: Address = env
            .storage()
            .persistent()
            .get(&DataKey::Guardian)
            .expect("guardian");
        guardian.require_auth();

        let key = DataKey::Hold(account, id);
        let record: HoldRecord = match env.storage().persistent().get(&key) {
            Some(r) => r,
            None => panic_with_error!(&env, HoldRegistryError::HoldNotFound),
        };

        if record.status != HoldStatus::Retained {
            panic_with_error!(&env, HoldRegistryError::InvalidStatus);
        }

        let updated = HoldRecord {
            status: HoldStatus::Stopped,
            ..record
        };
        env.storage().persistent().set(&key, &updated);
    }

    pub fn mark_executed(env: Env, account: Address, id: u64) {
        let guardian_hold: Address = env
            .storage()
            .persistent()
            .get(&DataKey::GuardianHold)
            .expect("guardian_hold");
        guardian_hold.require_auth();

        let key = DataKey::Hold(account, id);
        let record: HoldRecord = match env.storage().persistent().get(&key) {
            Some(r) => r,
            None => panic_with_error!(&env, HoldRegistryError::HoldNotFound),
        };

        if record.status != HoldStatus::Retained {
            panic_with_error!(&env, HoldRegistryError::InvalidStatus);
        }

        let updated = HoldRecord {
            status: HoldStatus::Executed,
            ..record
        };
        env.storage().persistent().set(&key, &updated);
    }

    pub fn get_hold(env: Env, account: Address, id: u64) -> HoldRecord {
        match env.storage().persistent().get(&DataKey::Hold(account, id)) {
            Some(r) => r,
            None => panic_with_error!(&env, HoldRegistryError::HoldNotFound),
        }
    }
}

#[cfg(test)]
mod tests {
    extern crate std;

    use super::*;
    use soroban_sdk::{
        testutils::{Address as _, Ledger as _, MockAuth, MockAuthInvoke},
        IntoVal,
    };

    fn setup(
        e: &Env,
    ) -> (
        Address,
        Address,
        Address,
        Address,
        Address,
        Address,
        HoldRegistryClient<'_>,
    ) {
        let guardian = Address::generate(e);
        let guardian_hold = Address::generate(e);
        let account = Address::generate(e);
        let token = Address::generate(e);
        let destination = Address::generate(e);
        let registry_id = e.register(HoldRegistry, (guardian.clone(), guardian_hold.clone()));
        let client = HoldRegistryClient::new(e, &registry_id);
        (
            guardian,
            guardian_hold,
            account,
            token,
            destination,
            registry_id,
            client,
        )
    }

    #[test]
    fn queue_sets_ready_at_from_ledger_not_client() {
        let e = Env::default();
        let (_g, _gh, account, token, dest, _reg, client) = setup(&e);

        let base_ts = 1_700_000_000u64;
        e.ledger().with_mut(|li| li.timestamp = base_ts);

        e.mock_all_auths();
        let id = client.queue(&account, &token, &dest, &100i128);
        assert_eq!(id, 0);

        let record = client.get_hold(&account, &id);
        assert_eq!(record.created_at, base_ts);
        assert_eq!(record.ready_at, base_ts + HOLD_DURATION_SECS);
        assert_eq!(record.status, HoldStatus::Retained);
        assert_eq!(record.amount, 100);
    }

    #[test]
    fn cancel_guardian_succeeds_stranger_fails() {
        let e = Env::default();
        let (guardian, _gh, account, token, dest, registry, client) = setup(&e);

        e.mock_all_auths();
        let id = client.queue(&account, &token, &dest, &50i128);

        e.mock_auths(&[MockAuth {
            address: &guardian,
            invoke: &MockAuthInvoke {
                contract: &registry,
                fn_name: "cancel",
                args: (&account, id).into_val(&e),
                sub_invokes: &[],
            },
        }]);
        client.cancel(&account, &id);
        assert_eq!(
            client.get_hold(&account, &id).status,
            HoldStatus::Stopped
        );

        let account2 = Address::generate(&e);
        let token2 = Address::generate(&e);
        let dest2 = Address::generate(&e);
        e.mock_all_auths();
        let id2 = client.queue(&account2, &token2, &dest2, &10i128);

        let stranger = Address::generate(&e);
        e.mock_auths(&[MockAuth {
            address: &stranger,
            invoke: &MockAuthInvoke {
                contract: &registry,
                fn_name: "cancel",
                args: (&account2, id2).into_val(&e),
                sub_invokes: &[],
            },
        }]);
        let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            client.cancel(&account2, &id2);
        }));
        assert!(err.is_err(), "stranger cancel must fail");
    }

    #[test]
    fn mark_executed_guardian_hold_succeeds_stranger_fails() {
        let e = Env::default();
        let (_g, guardian_hold, account, token, dest, registry, client) = setup(&e);

        e.mock_all_auths();
        let id = client.queue(&account, &token, &dest, &75i128);

        e.mock_auths(&[MockAuth {
            address: &guardian_hold,
            invoke: &MockAuthInvoke {
                contract: &registry,
                fn_name: "mark_executed",
                args: (&account, id).into_val(&e),
                sub_invokes: &[],
            },
        }]);
        client.mark_executed(&account, &id);
        assert_eq!(
            client.get_hold(&account, &id).status,
            HoldStatus::Executed
        );

        let account2 = Address::generate(&e);
        e.mock_all_auths();
        let id2 = client.queue(&account2, &token, &dest, &5i128);

        let stranger = Address::generate(&e);
        e.mock_auths(&[MockAuth {
            address: &stranger,
            invoke: &MockAuthInvoke {
                contract: &registry,
                fn_name: "mark_executed",
                args: (&account2, id2).into_val(&e),
                sub_invokes: &[],
            },
        }]);
        let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            client.mark_executed(&account2, &id2);
        }));
        assert!(err.is_err(), "stranger mark_executed must fail");
    }

    #[test]
    fn source_has_no_sac_token_or_transfer() {
        let path = concat!(env!("CARGO_MANIFEST_DIR"), "/src/lib.rs");
        let source = std::fs::read_to_string(path).expect("read lib.rs");
        let contract_source = source
            .split("#[cfg(test)]")
            .next()
            .expect("contract section");
        let lower = contract_source.to_ascii_lowercase();
        assert!(
            !lower.contains("transfer("),
            "hold_registry must not invoke transfer"
        );
        assert!(
            !lower.contains("stellarasset") && !lower.contains("tokenclient"),
            "hold_registry must not reference SAC token helpers"
        );
    }
}
