#![no_std]

use hold_registry::HoldRegistryClient;
use soroban_sdk::{
    auth::{Context, ContractContext},
    contract, contracterror, contractimpl, contracttype, panic_with_error, symbol_short,
    Address, Env, TryFromVal, Vec,
};
use stellar_accounts::policies::Policy;
use stellar_accounts::smart_account::{ContextRule, Signer};

/// Trusted-contact daily cap for the spike base scenario (USDC units).
const DAILY_CAP: i128 = 50;

#[contracttype]
#[derive(Clone, Debug, PartialEq)]
pub struct GuardianHoldAccountParams {
    pub owner: Address,
    pub usdc: Address,
    pub trusted_contact: Address,
    pub registry: Address,
}

#[contracttype]
#[derive(Clone)]
struct GuardianHoldConfig {
    owner: Address,
    usdc: Address,
    trusted_contact: Address,
    registry: Address,
    spent: i128,
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
    e.storage()
        .persistent()
        .set(&config_key(smart_account), config);
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

fn try_enforce_usdc_transfer(
    e: &Env,
    contract: &Address,
    fn_name: &soroban_sdk::Symbol,
    args: &Vec<soroban_sdk::Val>,
    config: &mut GuardianHoldConfig,
    smart_account: &Address,
) -> bool {
    if contract != &config.usdc || fn_name != &symbol_short!("transfer") {
        return false;
    }

    let from = match args.get(0) {
        Some(v) => Address::try_from_val(e, &v),
        None => reject_not_allowed(e),
    };
    let from = match from {
        Ok(a) => a,
        Err(_) => {
            reject_not_allowed(e);
        }
    };
    if from != *smart_account {
        reject_not_allowed(e);
    }

    let to = match args.get(1) {
        Some(v) => Address::try_from_val(e, &v),
        None => reject_not_allowed(e),
    };
    let to = match to {
        Ok(a) => a,
        Err(_) => {
            panic_with_error!(e, GuardianHoldError::InvalidDestination);
        }
    };

    let amount = match args.get(2) {
        Some(v) => i128::try_from_val(e, &v),
        None => reject_not_allowed(e),
    };
    let amount = match amount {
        Ok(a) => a,
        Err(_) => {
            reject_not_allowed(e);
        }
    };

    if to == config.trusted_contact {
        if amount <= 0 {
            panic_with_error!(e, GuardianHoldError::InvalidAmount);
        }
        let new_spent = match config.spent.checked_add(amount) {
            Some(s) => s,
            None => panic_with_error!(e, GuardianHoldError::Overflow),
        };
        if new_spent > DAILY_CAP {
            panic_with_error!(e, GuardianHoldError::CapExceeded);
        }
        config.spent = new_spent;
        return true;
    }

    let registry = HoldRegistryClient::new(e, &config.registry);
    let hold_id = match registry.find_retained(
        &smart_account.clone(),
        &config.usdc.clone(),
        &to,
        &amount,
    ) {
        Some(id) => id,
        None => reject_not_allowed(e),
    };

    let record = registry.get_hold(&smart_account.clone(), &hold_id);
    if e.ledger().timestamp() < record.ready_at {
        panic_with_error!(e, GuardianHoldError::HoldNotReady);
    }

    registry.mark_executed(&smart_account.clone(), &hold_id);
    true
}

fn try_enforce_queue(
    e: &Env,
    contract: &Address,
    fn_name: &soroban_sdk::Symbol,
    args: &Vec<soroban_sdk::Val>,
    config: &GuardianHoldConfig,
    smart_account: &Address,
) -> bool {
    if contract != &config.registry || fn_name != &symbol_short!("queue") {
        return false;
    }

    let account = match args.get(0) {
        Some(v) => Address::try_from_val(e, &v),
        None => reject_not_allowed(e),
    };
    let account = match account {
        Ok(a) => a,
        Err(_) => reject_not_allowed(e),
    };
    if account != *smart_account {
        reject_not_allowed(e);
    }

    let amount = match args.get(3) {
        Some(v) => i128::try_from_val(e, &v),
        None => reject_not_allowed(e),
    };
    let amount = match amount {
        Ok(a) => a,
        Err(_) => reject_not_allowed(e),
    };
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

    let config = GuardianHoldConfig {
        owner: install_params.owner.clone(),
        usdc: install_params.usdc.clone(),
        trusted_contact: install_params.trusted_contact.clone(),
        registry: install_params.registry.clone(),
        spent: 0,
    };
    e.storage().persistent().set(&key, &config);
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
            if try_enforce_usdc_transfer(e, contract, fn_name, args, &mut config, smart_account) {
                store_config(e, smart_account, &config);
                true
            } else if try_enforce_queue(e, contract, fn_name, args, &config, smart_account) {
                true
            } else {
                false
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
impl Policy for GuardianHold {
    type AccountParams = GuardianHoldAccountParams;

    fn enforce(
        e: &Env,
        context: Context,
        authenticated_signers: Vec<Signer>,
        context_rule: ContextRule,
        smart_account: Address,
    ) {
        enforce_impl(e, &context, &authenticated_signers, &context_rule, &smart_account);
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

#[cfg(test)]
mod tests {
    extern crate std;

    use super::*;
    use soroban_sdk::{
        auth::ContractContext,
        testutils::{Address as _, MockAuth, MockAuthInvoke},
        IntoVal,
    };
    use stellar_accounts::policies::PolicyClient;
    use stellar_accounts::smart_account::{ContextRuleType, Signer};

    fn sample_rule(e: &Env) -> ContextRule {
        ContextRule {
            id: 0,
            context_type: ContextRuleType::Default,
            name: soroban_sdk::String::from_str(e, "default"),
            signers: Vec::from_array(e, [Signer::Delegated(Address::generate(e))]),
            signer_ids: Vec::new(e),
            policies: Vec::new(e),
            policy_ids: Vec::new(e),
            valid_until: None,
        }
    }

    fn install_params(
        _e: &Env,
        owner: &Address,
        usdc: &Address,
        trusted: &Address,
        registry: &Address,
    ) -> GuardianHoldAccountParams {
        GuardianHoldAccountParams {
            owner: owner.clone(),
            usdc: usdc.clone(),
            trusted_contact: trusted.clone(),
            registry: registry.clone(),
        }
    }

    fn setup_registry(e: &Env) -> Address {
        let guardian = Address::generate(e);
        let guardian_hold = Address::generate(e);
        e.register(hold_registry::HoldRegistry, (guardian, guardian_hold))
    }

    fn transfer_context(
        e: &Env,
        usdc: &Address,
        from: &Address,
        to: &Address,
        amount: i128,
    ) -> Context {
        let mut args = Vec::new(e);
        args.push_back(from.into_val(e));
        args.push_back(to.into_val(e));
        args.push_back(amount.into_val(e));
        Context::Contract(ContractContext {
            contract: usdc.clone(),
            fn_name: symbol_short!("transfer"),
            args,
        })
    }

    fn mock_enforce_auth(e: &Env, policy: &Address, smart_account: &Address, context: &Context) {
        e.mock_auths(&[MockAuth {
            address: smart_account,
            invoke: &MockAuthInvoke {
                contract: policy,
                fn_name: "enforce",
                args: (
                    context.clone(),
                    Vec::<Signer>::new(e),
                    sample_rule(e),
                    smart_account.clone(),
                )
                    .into_val(e),
                sub_invokes: &[],
            },
        }]);
    }

    #[test]
    fn second_install_panics() {
        let e = Env::default();
        let policy_id = e.register(GuardianHold, ());
        let owner = Address::generate(&e);
        let usdc = Address::generate(&e);
        let trusted = Address::generate(&e);
        let registry = setup_registry(&e);
        let smart_account = Address::generate(&e);
        let params = install_params(&e, &owner, &usdc, &trusted, &registry);
        let rule = sample_rule(&e);

        e.mock_all_auths();
        PolicyClient::new(&e, &policy_id).install(&params.into_val(&e), &rule, &smart_account);

        let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            PolicyClient::new(&e, &policy_id).install(&params.into_val(&e), &rule, &smart_account);
        }));
        assert!(err.is_err(), "second install must panic");
    }

    #[test]
    fn enforce_without_account_auth_panics() {
        let e = Env::default();
        let policy_id = e.register(GuardianHold, ());
        let owner = Address::generate(&e);
        let usdc = Address::generate(&e);
        let trusted = Address::generate(&e);
        let registry = setup_registry(&e);
        let smart_account = Address::generate(&e);
        let params = install_params(&e, &owner, &usdc, &trusted, &registry);
        let rule = sample_rule(&e);

        e.mock_all_auths();
        PolicyClient::new(&e, &policy_id).install(&params.into_val(&e), &rule, &smart_account);

        let ctx = transfer_context(&e, &usdc, &smart_account, &trusted, 10);
        let signers = Vec::from_array(&e, [Signer::Delegated(owner.clone())]);
        e.set_auths(&[]);

        let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            PolicyClient::new(&e, &policy_id).enforce(
                &ctx,
                &signers,
                &rule,
                &smart_account,
            );
        }));
        assert!(err.is_err(), "enforce without smart_account auth must panic");
    }

    #[test]
    fn enforce_without_owner_signer_panics() {
        let e = Env::default();
        let policy_id = e.register(GuardianHold, ());
        let owner = Address::generate(&e);
        let usdc = Address::generate(&e);
        let trusted = Address::generate(&e);
        let registry = setup_registry(&e);
        let smart_account = Address::generate(&e);
        let params = install_params(&e, &owner, &usdc, &trusted, &registry);
        let rule = sample_rule(&e);

        e.mock_all_auths();
        PolicyClient::new(&e, &policy_id).install(&params.into_val(&e), &rule, &smart_account);

        let ctx = transfer_context(&e, &usdc, &smart_account, &trusted, 10);
        mock_enforce_auth(&e, &policy_id, &smart_account, &ctx);
        let strangers = Vec::from_array(&e, [Signer::Delegated(Address::generate(&e))]);

        let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            PolicyClient::new(&e, &policy_id).enforce(
                &ctx,
                &strangers,
                &rule,
                &smart_account,
            );
        }));
        assert!(err.is_err(), "enforce without owner in signers must panic");
    }

    #[test]
    fn trusted_transfer_within_cap_succeeds() {
        let e = Env::default();
        let policy_id = e.register(GuardianHold, ());
        let owner = Address::generate(&e);
        let usdc = Address::generate(&e);
        let trusted = Address::generate(&e);
        let registry = setup_registry(&e);
        let smart_account = Address::generate(&e);
        let params = install_params(&e, &owner, &usdc, &trusted, &registry);
        let rule = sample_rule(&e);

        e.mock_all_auths();
        PolicyClient::new(&e, &policy_id).install(&params.into_val(&e), &rule, &smart_account);

        let ctx = transfer_context(&e, &usdc, &smart_account, &trusted, 10);
        let signers = Vec::from_array(&e, [Signer::Delegated(owner.clone())]);
        e.mock_all_auths();

        PolicyClient::new(&e, &policy_id).enforce(&ctx, &signers, &rule, &smart_account);
    }

    #[test]
    fn transfer_to_unknown_without_hold_panics() {
        let e = Env::default();
        let policy_id = e.register(GuardianHold, ());
        let owner = Address::generate(&e);
        let usdc = Address::generate(&e);
        let trusted = Address::generate(&e);
        let registry = setup_registry(&e);
        let smart_account = Address::generate(&e);
        let stranger = Address::generate(&e);
        let params = install_params(&e, &owner, &usdc, &trusted, &registry);
        let rule = sample_rule(&e);

        e.mock_all_auths();
        PolicyClient::new(&e, &policy_id).install(&params.into_val(&e), &rule, &smart_account);

        let ctx = transfer_context(&e, &usdc, &smart_account, &stranger, 10);
        mock_enforce_auth(&e, &policy_id, &smart_account, &ctx);
        let signers = Vec::from_array(&e, [Signer::Delegated(owner.clone())]);

        let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            PolicyClient::new(&e, &policy_id).enforce(&ctx, &signers, &rule, &smart_account);
        }));
        assert!(err.is_err(), "transfer to unknown without hold must panic");
    }
}
