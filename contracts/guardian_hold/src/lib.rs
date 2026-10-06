//! Allowlist policy. It never calls a token.
//! `enforce` requires the account first, then the owner's signature, then the allowlist.

#![no_std]

use soroban_sdk::{
    auth::{Context, ContractContext},
    contract, contractclient, contracterror, contractimpl, contracttype, panic_with_error,
    symbol_short,
    Address, Env, Symbol, TryFromVal, Val, Vec,
};
use stellar_accounts::policies::Policy;
use stellar_accounts::smart_account::{ContextRule, Signer};

const SECONDS_PER_DAY: u64 = 86_400;
const TTL_THRESHOLD: u32 = 17280;
const TTL_EXTEND_TO: u32 = 518400;
const MAX_TRUSTED_CONTACTS: u32 = 3;

#[contracttype]
#[derive(Clone, Debug, PartialEq)]
pub struct GuardianHoldAccountParams {
    pub owner: Address,
    pub usdc: Address,
    /// One to three trusted contacts. The daily cap applies to the sum of payments to all of them.
    pub trusted_contacts: Vec<Address>,
    pub registry: Address,
    pub daily_cap: i128,
}

#[contracttype]
#[derive(Clone)]
struct GuardianHoldConfig {
    owner: Address,
    usdc: Address,
    trusted_contacts: Vec<Address>,
    registry: Address,
    daily_cap: i128,
    spent: i128,
    spent_day: u64,
}

#[contracttype]
enum DataKey {
    Config(Address),
}

#[contracterror]
#[derive(Copy, Clone, Debug, Eq, PartialEq, PartialOrd, Ord)]
#[repr(u32)]
pub enum GuardianHoldError {
    AlreadyInstalled = 1,
    OwnerNotAuthenticated = 2,
    NotAllowed = 3,
    InvalidAmount = 4,
    CapExceeded = 5,
    Overflow = 6,
    HoldNotReady = 7,
    InvalidDestination = 8,
    InvalidConfig = 9,
}

/// Same fields as the registry record. Declared here so this crate does not
/// link the registry contract into this wasm.
#[contracttype]
#[derive(Clone)]
struct HoldRecord {
    token: Address,
    destination: Address,
    amount: i128,
    created_at: u64,
    ready_at: u64,
    status: HoldStatus,
}

#[contracttype]
#[derive(Clone, Eq, PartialEq)]
enum HoldStatus {
    Retained,
    Stopped,
    Executed,
}

#[allow(dead_code)]
#[contractclient(name = "HoldRegistryClient")]
trait HoldRegistryInterface {
    fn find_retained(
        env: Env,
        account: Address,
        token: Address,
        destination: Address,
        amount: i128,
    ) -> Option<u64>;
    fn get_hold(env: Env, account: Address, id: u64) -> HoldRecord;
    fn mark_executed(env: Env, account: Address, id: u64);
}

#[contract]
pub struct GuardianHold;

fn config_key(smart_account: &Address) -> DataKey {
    DataKey::Config(smart_account.clone())
}

fn load_config(e: &Env, smart_account: &Address) -> GuardianHoldConfig {
    e.storage()
        .persistent()
        .get(&config_key(smart_account))
        .unwrap_or_else(|| panic_with_error!(e, GuardianHoldError::NotAllowed))
}

fn store_config(e: &Env, smart_account: &Address, config: &GuardianHoldConfig) {
    let key = config_key(smart_account);
    e.storage().persistent().set(&key, config);
    e.storage()
        .persistent()
        .extend_ttl(&key, TTL_THRESHOLD, TTL_EXTEND_TO);
}

fn owner_is_authenticated(config: &GuardianHoldConfig, authenticated_signers: &Vec<Signer>) -> bool {
    let expected = Signer::Delegated(config.owner.clone());
    for signer in authenticated_signers.iter() {
        if signer == expected {
            return true;
        }
    }
    false
}

fn reject_not_allowed(e: &Env) -> ! {
    panic_with_error!(e, GuardianHoldError::NotAllowed);
}

fn is_trusted(config: &GuardianHoldConfig, to: &Address) -> bool {
    for contact in config.trusted_contacts.iter() {
        if contact == *to {
            return true;
        }
    }
    false
}

fn parse_address(e: &Env, value: &Val, destination: bool) -> Address {
    match Address::try_from_val(e, value) {
        Ok(address) => address,
        Err(_) if destination => panic_with_error!(e, GuardianHoldError::InvalidDestination),
        Err(_) => reject_not_allowed(e),
    }
}

fn parse_amount(e: &Env, value: &Val) -> i128 {
    match i128::try_from_val(e, value) {
        Ok(amount) => amount,
        Err(_) => reject_not_allowed(e),
    }
}

fn arg(e: &Env, args: &Vec<Val>, index: u32) -> Val {
    match args.get(index) {
        Some(value) => value,
        None => reject_not_allowed(e),
    }
}

fn try_enforce_usdc_transfer(
    e: &Env,
    contract: &Address,
    fn_name: &Symbol,
    args: &Vec<Val>,
    config: &mut GuardianHoldConfig,
    smart_account: &Address,
) -> bool {
    if contract != &config.usdc || fn_name != &symbol_short!("transfer") {
        return false;
    }

    let from = parse_address(e, &arg(e, args, 0), false);
    if from != *smart_account {
        reject_not_allowed(e);
    }
    let to = parse_address(e, &arg(e, args, 1), true);
    let amount = parse_amount(e, &arg(e, args, 2));
    if amount <= 0 {
        panic_with_error!(e, GuardianHoldError::InvalidAmount);
    }

    if is_trusted(config, &to) {
        let day = e.ledger().timestamp() / SECONDS_PER_DAY;
        if config.spent_day != day {
            config.spent = 0;
            config.spent_day = day;
        }
        let new_spent = match config.spent.checked_add(amount) {
            Some(sum) => sum,
            None => panic_with_error!(e, GuardianHoldError::Overflow),
        };
        if new_spent > config.daily_cap {
            panic_with_error!(e, GuardianHoldError::CapExceeded);
        }
        config.spent = new_spent;
        return true;
    }

    let registry = HoldRegistryClient::new(e, &config.registry);
    let hold_id = match registry.find_retained(smart_account, &config.usdc, &to, &amount) {
        Some(id) => id,
        None => reject_not_allowed(e),
    };
    let record = registry.get_hold(smart_account, &hold_id);
    if record.token != config.usdc
        || record.destination != to
        || record.amount != amount
        || e.ledger().timestamp() < record.ready_at
    {
        panic_with_error!(e, GuardianHoldError::HoldNotReady);
    }
    registry.mark_executed(smart_account, &hold_id);
    true
}

fn try_enforce_queue(
    e: &Env,
    contract: &Address,
    fn_name: &Symbol,
    args: &Vec<Val>,
    config: &GuardianHoldConfig,
    smart_account: &Address,
) -> bool {
    if contract != &config.registry || fn_name != &symbol_short!("queue") {
        return false;
    }
    let account = parse_address(e, &arg(e, args, 0), false);
    if account != *smart_account {
        reject_not_allowed(e);
    }
    let amount = parse_amount(e, &arg(e, args, 3));
    if amount <= 0 {
        panic_with_error!(e, GuardianHoldError::InvalidAmount);
    }
    true
}

pub fn install_impl(
    e: &Env,
    install_params: &GuardianHoldAccountParams,
    _context_rule: &ContextRule,
    smart_account: &Address,
) {
    smart_account.require_auth();

    let key = config_key(smart_account);
    if e.storage().persistent().has(&key) {
        panic_with_error!(e, GuardianHoldError::AlreadyInstalled);
    }
    let contact_count = install_params.trusted_contacts.len();
    if contact_count == 0 || contact_count > MAX_TRUSTED_CONTACTS || install_params.daily_cap <= 0
    {
        panic_with_error!(e, GuardianHoldError::InvalidConfig);
    }

    let config = GuardianHoldConfig {
        owner: install_params.owner.clone(),
        usdc: install_params.usdc.clone(),
        trusted_contacts: install_params.trusted_contacts.clone(),
        registry: install_params.registry.clone(),
        daily_cap: install_params.daily_cap,
        spent: 0,
        spent_day: e.ledger().timestamp() / SECONDS_PER_DAY,
    };
    store_config(e, smart_account, &config);
}

pub fn enforce_impl(
    e: &Env,
    context: &Context,
    authenticated_signers: &Vec<Signer>,
    _context_rule: &ContextRule,
    smart_account: &Address,
) {
    smart_account.require_auth();

    let mut config = load_config(e, smart_account);
    if !owner_is_authenticated(&config, authenticated_signers) {
        panic_with_error!(e, GuardianHoldError::OwnerNotAuthenticated);
    }

    let handled = match context {
        Context::Contract(ContractContext {
            contract,
            fn_name,
            args,
        }) => {
            if contract == smart_account {
                false
            } else if try_enforce_usdc_transfer(
                e,
                contract,
                fn_name,
                args,
                &mut config,
                smart_account,
            ) {
                store_config(e, smart_account, &config);
                true
            } else {
                try_enforce_queue(e, contract, fn_name, args, &config, smart_account)
            }
        }
        _ => false,
    };

    if !handled {
        reject_not_allowed(e);
    }
}

pub fn uninstall_impl(e: &Env, _context_rule: &ContextRule, smart_account: &Address) {
    smart_account.require_auth();
    panic_with_error!(e, GuardianHoldError::NotAllowed);
}

#[contractimpl]
impl GuardianHold {
    /// Spent amount in the current UTC day. Read only.
    pub fn spent_today(env: Env, smart_account: Address) -> i128 {
        let config = load_config(&env, &smart_account);
        let day = env.ledger().timestamp() / SECONDS_PER_DAY;
        if config.spent_day != day {
            0
        } else {
            config.spent
        }
    }
}

#[contractimpl]
impl Policy for GuardianHold {
    type AccountParams = GuardianHoldAccountParams;

    fn enforce(
        e: &Env,
        context: Context,
        authenticated_signers: Vec<Signer>,
        context_rule: ContextRule,
        smart_account: Address,
    ) {
        enforce_impl(
            e,
            &context,
            &authenticated_signers,
            &context_rule,
            &smart_account,
        );
    }

    fn install(
        e: &Env,
        install_params: Self::AccountParams,
        context_rule: ContextRule,
        smart_account: Address,
    ) {
        install_impl(e, &install_params, &context_rule, &smart_account);
    }

    fn uninstall(e: &Env, context_rule: ContextRule, smart_account: Address) {
        uninstall_impl(e, &context_rule, &smart_account);
    }
}
