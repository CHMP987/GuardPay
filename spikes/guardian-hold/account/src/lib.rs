//! Spike smart account: one Default context rule at deploy time.
//! This contract deliberately has no execute or upgrade entry points.

#![no_std]

use soroban_sdk::{
    auth::{Context, CustomAccountInterface},
    contract, contractimpl,
    crypto::Hash,
    Address, Env, Map, String, Val, Vec,
};
use stellar_accounts::policies::Policy;
use stellar_accounts::smart_account::{
    add_context_rule, do_check_auth, AuthPayload, ContextRule, ContextRuleType, Signer,
    SmartAccountError,
};

#[contract]
pub struct SpikeAccount;

#[contractimpl]
impl SpikeAccount {
    pub fn __constructor(env: Env, owner: Address, policy: Address, install_params: Val) {
        let signers = Vec::from_array(&env, [Signer::Delegated(owner)]);
        let mut policies = Map::new(&env);
        policies.set(policy, install_params);
        add_context_rule(
            &env,
            &ContextRuleType::Default,
            &String::from_str(&env, "default"),
            None,
            &signers,
            &policies,
        );
    }
}

#[contractimpl]
impl CustomAccountInterface for SpikeAccount {
    type Signature = AuthPayload;
    type Error = SmartAccountError;

    fn __check_auth(
        env: Env,
        signature_payload: Hash<32>,
        signature: Self::Signature,
        auth_contexts: Vec<Context>,
    ) -> Result<(), Self::Error> {
        do_check_auth(&env, &signature_payload, &signature, &auth_contexts)
    }
}

/// Policy stub used only so `add_context_rule` can call `install` during deploy.
#[contract]
pub struct InstallStub;

#[contractimpl]
impl Policy for InstallStub {
    type AccountParams = Val;

    fn enforce(
        _e: &Env,
        _context: Context,
        _authenticated_signers: Vec<Signer>,
        _rule: ContextRule,
        _smart_account: Address,
    ) {
    }

    fn install(_e: &Env, _param: Val, _rule: ContextRule, _smart_account: Address) {}

    fn uninstall(_e: &Env, _rule: ContextRule, _smart_account: Address) {}
}

#[cfg(test)]
mod tests {
    use super::*;
    use soroban_sdk::testutils::Address as _;
    use stellar_accounts::smart_account::{get_context_rule, get_context_rules_count};

    #[test]
    fn constructor_creates_single_default_rule() {
        let env = Env::default();
        env.mock_all_auths();

        let owner = Address::generate(&env);
        let policy = env.register(InstallStub, ());
        let install_params = Val::from_void();
        let account = env.register(SpikeAccount, (owner.clone(), policy.clone(), install_params));

        env.as_contract(&account, || {
            assert_eq!(get_context_rules_count(&env), 1);
            let rule = get_context_rule(&env, 0);
            assert_eq!(rule.id, 0);
            assert_eq!(rule.context_type, ContextRuleType::Default);
            assert_eq!(rule.signers.len(), 1);
            assert_eq!(rule.signers.get(0).unwrap(), Signer::Delegated(owner));
            assert_eq!(rule.policies.len(), 1);
            assert_eq!(rule.policies.get(0).unwrap(), policy);
        });
    }
}
