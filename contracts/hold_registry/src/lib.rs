//! Hold records. This contract never moves tokens.
//! `ready_at` is `created_at` plus the deploy-time retention, never a caller argument.
//! Records are never deleted. Lookups go through an index of the retained holds
//! (F-02), so no call reads more entries as the account's history grows.

#![no_std]

use soroban_sdk::{
    contract, contracterror, contractevent, contractimpl, contracttype, panic_with_error, Address,
    Env, Vec,
};

/// Demo retention. Applied inside `queue`. Not an argument of any public function.
pub const HOLD_DURATION_SECS: u64 = 120;

const TTL_THRESHOLD: u32 = 17280;
const TTL_EXTEND_TO: u32 = 518400;

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

#[contractevent]
#[derive(Clone)]
struct Queued {
    #[topic]
    account: Address,
    id: u64,
    token: Address,
    destination: Address,
    amount: i128,
    ready_at: u64,
}

#[contractevent]
#[derive(Clone)]
struct Canceled {
    #[topic]
    account: Address,
    id: u64,
}

#[contractevent]
#[derive(Clone)]
struct Executed {
    #[topic]
    account: Address,
    id: u64,
}

#[contracttype]
#[derive(Clone)]
enum DataKey {
    Guardian,
    GuardianHold,
    NextId(Address),
    Hold(Address, u64),
    /// (account, token, destination, amount) -> id of the one retained hold with that binding.
    Active(Address, Address, Address, i128),
    /// account -> ids of its retained holds, oldest first.
    RetainedIds(Address),
}

#[contracterror]
#[derive(Copy, Clone, Debug, Eq, PartialEq, PartialOrd, Ord)]
#[repr(u32)]
pub enum HoldRegistryError {
    InvalidAmount = 1,
    HoldNotFound = 2,
    InvalidStatus = 3,
    DuplicateActive = 4,
}

#[contract]
pub struct HoldRegistry;

fn persist<K, V>(env: &Env, key: &K, value: &V)
where
    K: soroban_sdk::IntoVal<Env, soroban_sdk::Val> + Clone,
    V: soroban_sdk::IntoVal<Env, soroban_sdk::Val> + Clone,
{
    env.storage().persistent().set(key, value);
    env.storage()
        .persistent()
        .extend_ttl(key, TTL_THRESHOLD, TTL_EXTEND_TO);
}

#[contractimpl]
impl HoldRegistry {
    pub fn __constructor(env: Env, guardian: Address, guardian_hold: Address) {
        persist(&env, &DataKey::Guardian, &guardian);
        persist(&env, &DataKey::GuardianHold, &guardian_hold);
    }

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
        let active_key = active_key(&account, &token, &destination, amount);
        if env.storage().persistent().has(&active_key) {
            panic_with_error!(&env, HoldRegistryError::DuplicateActive);
        }

        let id_key = DataKey::NextId(account.clone());
        let id: u64 = env.storage().persistent().get(&id_key).unwrap_or(0);
        persist(&env, &id_key, &(id + 1));

        let created_at = env.ledger().timestamp();
        let ready_at = created_at + HOLD_DURATION_SECS;
        let record = HoldRecord {
            token: token.clone(),
            destination: destination.clone(),
            amount,
            created_at,
            ready_at,
            status: HoldStatus::Retained,
        };
        persist(&env, &DataKey::Hold(account.clone(), id), &record);
        persist(&env, &active_key, &id);
        let mut retained = retained_ids(&env, &account);
        retained.push_back(id);
        persist(&env, &DataKey::RetainedIds(account.clone()), &retained);

        Queued {
            account,
            id,
            token,
            destination,
            amount,
            ready_at,
        }
        .publish(&env);
        id
    }

    pub fn cancel(env: Env, account: Address, id: u64) {
        let guardian: Address = env
            .storage()
            .persistent()
            .get(&DataKey::Guardian)
            .unwrap_or_else(|| panic_with_error!(&env, HoldRegistryError::HoldNotFound));
        guardian.require_auth();
        set_terminal(&env, &account, id, HoldStatus::Stopped);
        Canceled { account, id }.publish(&env);
    }

    pub fn mark_executed(env: Env, account: Address, id: u64) {
        let guardian_hold: Address = env
            .storage()
            .persistent()
            .get(&DataKey::GuardianHold)
            .unwrap_or_else(|| panic_with_error!(&env, HoldRegistryError::HoldNotFound));
        guardian_hold.require_auth();
        set_terminal(&env, &account, id, HoldStatus::Executed);
        Executed { account, id }.publish(&env);
    }

    pub fn get_hold(env: Env, account: Address, id: u64) -> HoldRecord {
        load_hold(&env, &account, id)
    }

    pub fn find_retained(
        env: Env,
        account: Address,
        token: Address,
        destination: Address,
        amount: i128,
    ) -> Option<u64> {
        find_retained_id(&env, &account, &token, &destination, amount)
    }

    /// Ids of retained holds for one account, oldest first. Read only.
    pub fn list_retained(env: Env, account: Address) -> Vec<u64> {
        retained_ids(&env, &account)
    }
}

fn load_hold(env: &Env, account: &Address, id: u64) -> HoldRecord {
    env.storage()
        .persistent()
        .get(&DataKey::Hold(account.clone(), id))
        .unwrap_or_else(|| panic_with_error!(env, HoldRegistryError::HoldNotFound))
}

fn set_terminal(env: &Env, account: &Address, id: u64, status: HoldStatus) {
    let record = load_hold(env, account, id);
    if record.status != HoldStatus::Retained {
        panic_with_error!(env, HoldRegistryError::InvalidStatus);
    }
    env.storage().persistent().remove(&active_key(
        account,
        &record.token,
        &record.destination,
        record.amount,
    ));
    let mut retained = retained_ids(env, account);
    if let Some(index) = retained.first_index_of(id) {
        retained.remove(index);
    }
    persist(env, &DataKey::RetainedIds(account.clone()), &retained);
    let updated = HoldRecord { status, ..record };
    persist(env, &DataKey::Hold(account.clone(), id), &updated);
}

fn active_key(account: &Address, token: &Address, destination: &Address, amount: i128) -> DataKey {
    DataKey::Active(account.clone(), token.clone(), destination.clone(), amount)
}

fn retained_ids(env: &Env, account: &Address) -> Vec<u64> {
    env.storage()
        .persistent()
        .get(&DataKey::RetainedIds(account.clone()))
        .unwrap_or_else(|| Vec::new(env))
}

/// One index read plus the record it names: the cost does not depend on how many
/// records the account has.
fn find_retained_id(
    env: &Env,
    account: &Address,
    token: &Address,
    destination: &Address,
    amount: i128,
) -> Option<u64> {
    let id: u64 = env
        .storage()
        .persistent()
        .get(&active_key(account, token, destination, amount))?;
    let record = load_hold(env, account, id);
    if record.status == HoldStatus::Retained
        && record.token == *token
        && record.destination == *destination
        && record.amount == amount
    {
        Some(id)
    } else {
        None
    }
}

#[cfg(test)]
mod tests;
