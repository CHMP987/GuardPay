//! Blocker tests GH-01, GH-13, GH-24, GH-25, GH-26 — host enters `__check_auth`.

extern crate std;

use guardpay_spike_account::SpikeAccount;
use guardian_hold::{GuardianHold, GuardianHoldAccountParams};
use hold_registry::HoldRegistry;
use soroban_sdk::{
    auth::{Context, ContractContext},
    crypto::Hash,
    testutils::{Address as _, Ledger as _, MockAuth, MockAuthInvoke},
    vec, Address, Bytes, BytesN, Env, IntoVal, Map, Symbol, Val, Vec,
};
use soroban_sdk::InvokeError;
use soroban_sdk::xdr::ToXdr;
use stellar_accounts::policies::PolicyClient;
use stellar_accounts::smart_account::{
    get_context_rule, AuthPayload, ContextRule, Signer, SmartAccountError,
};

const HOLD_DURATION_SECS: u64 = 120;

struct Scenario {
    owner: Address,
    trusted: Address,
    attacker: Address,
    usdc: Address,
    account: Address,
    gh: Address,
    registry: Address,
    rule: ContextRule,
}

fn transfer_context(
    e: &Env,
    token: &Address,
    from: &Address,
    to: &Address,
    amount: i128,
) -> Context {
    Context::Contract(ContractContext {
        contract: token.clone(),
        fn_name: soroban_sdk::symbol_short!("transfer"),
        args: (from.clone(), to.clone(), amount).into_val(e),
    })
}

fn deploy_scenario(e: &Env) -> Scenario {
    e.mock_all_auths();

    let owner = Address::generate(e);
    let trusted = Address::generate(e);
    let attacker = Address::generate(e);
    let guardian = Address::generate(e);
    let usdc = Address::generate(e);
    let gh = e.register(GuardianHold, ());
    let registry = e.register(HoldRegistry, (guardian.clone(), gh.clone()));
    let params = GuardianHoldAccountParams {
        owner: owner.clone(),
        usdc: usdc.clone(),
        trusted_contact: trusted.clone(),
        registry: registry.clone(),
    };
    let install_val: Val = params.into_val(e);
    let account = e.register(SpikeAccount, (owner.clone(), gh.clone(), install_val));

    let rule = e.as_contract(&account, || get_context_rule(e, 0));
    e.set_auths(&[]);

    Scenario {
        owner,
        trusted,
        attacker,
        usdc,
        account,
        gh,
        registry,
        rule,
    }
}

fn signature_payload(e: &Env) -> Hash<32> {
    e.crypto().sha256(&Bytes::from_array(e, &[7u8; 32]))
}

fn try_check_auth(
    e: &Env,
    account: &Address,
    _gh: &Address,
    rule: &ContextRule,
    payload: &AuthPayload,
    contexts: &Vec<Context>,
    delegated_signed_rule_ids: Option<Vec<u32>>,
) -> Result<(), Result<SmartAccountError, InvokeError>> {
    let mut invokes: std::vec::Vec<MockAuthInvoke<'_>> = std::vec![];

    let mut delegated_owners: std::vec::Vec<Address> = std::vec![];
    for signer in payload.signers.keys().iter() {
        if let Signer::Delegated(addr) = signer {
            delegated_owners.push(addr.clone());
        }
    }

    for _owner in delegated_owners.iter() {
        let rule_ids = delegated_signed_rule_ids
            .clone()
            .unwrap_or_else(|| payload.context_rule_ids.clone());
        let sig_payload = signature_payload(e);
        let mut preimage = sig_payload.to_bytes().to_bytes();
        preimage.append(&rule_ids.to_xdr(e));
        let auth_digest = e.crypto().sha256(&preimage);
        invokes.push(MockAuthInvoke {
            contract: account,
            fn_name: "__check_auth",
            args: (auth_digest,).into_val(e),
            sub_invokes: &[],
        });
    }

    let mut mocks: std::vec::Vec<MockAuth<'_>> = std::vec![];
    for (idx, owner) in delegated_owners.iter().enumerate() {
        mocks.push(MockAuth {
            address: owner,
            invoke: &invokes[idx],
        });
    }

    if !mocks.is_empty() {
        e.mock_auths(&mocks);
    }

    let sig_payload: BytesN<32> = signature_payload(e).into();
    let out = e.try_invoke_contract_check_auth::<SmartAccountError>(
        account,
        &sig_payload,
        payload.clone().into_val(e),
        contexts,
    );
    e.set_auths(&[]);
    let _ = rule;
    out
}

fn reject_error_literal(result: &Result<(), Result<SmartAccountError, InvokeError>>) -> String {
    match result {
        Ok(()) => "Ok(())".to_string(),
        Err(Err(e)) => format!("{e:?}"),
        Err(Ok(contract_err)) => format!("SmartAccountError({contract_err:?})"),
    }
}

fn invoke_missing_account_fn(e: &Env, account: &Address, fn_name: &str) -> String {
    let args = vec![e];
    let result = e.try_invoke_contract::<(), SmartAccountError>(
        account,
        &Symbol::new(e, fn_name),
        args,
    );
    match result {
        Ok(Ok(_)) => "Ok".to_string(),
        Ok(Err(contract_err)) => format!("SmartAccountError({contract_err:?})"),
        Err(Err(host_err)) => format!("{host_err:?}"),
        Err(Ok(_)) => "unexpected".to_string(),
    }
}

fn queue_mature_hold(e: &Env, s: &Scenario, destination: &Address, amount: i128) {
    e.mock_all_auths();
    hold_registry::HoldRegistryClient::new(e, &s.registry).queue(
        &s.account,
        &s.usdc,
        destination,
        &amount,
    );
    e.ledger().set_timestamp(e.ledger().timestamp() + HOLD_DURATION_SECS + 1);
    e.set_auths(&[]);
}

#[test]
fn gh_01_transfer_without_owner_signature_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.trusted, 10);
    let contexts = vec![&e, ctx];
    let payload = AuthPayload {
        signers: Map::new(&e),
        context_rule_ids: vec![&e, 0],
    };

    let result = try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload, &contexts, None);
    let literal = reject_error_literal(&result);
    println!("GH-01 error literal: {literal}");
    assert!(
        result.is_err(),
        "GH-01 must reject unsigned transfer; got {literal}"
    );
    assert!(
        literal.contains("Contract(2)")
            || literal.contains("OwnerNotAuthenticated")
            || literal.contains("#2"),
        "GH-01 expected OwnerNotAuthenticated; got {literal}"
    );
}

#[test]
fn gh_13_no_alternate_context_rule() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.trusted, 10);
    let contexts = vec![&e, ctx.clone()];

    // (a) context_rule_ids=[1] — rule does not exist.
    let payload_a = AuthPayload {
        signers: Map::new(&e),
        context_rule_ids: vec![&e, 1],
    };
    let result_a = try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload_a, &contexts, None);
    let literal_a = reject_error_literal(&result_a);
    println!("GH-13(a) error literal: {literal_a}");
    assert!(result_a.is_err(), "GH-13(a) must reject missing rule id");
    assert!(
        literal_a.contains("ContextRuleNotFound") || literal_a.contains("#3000"),
        "GH-13(a) expected ContextRuleNotFound; got {literal_a}"
    );

    // (b) signed for rule 0, tampered to rule 1.
    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.owner.clone()), Bytes::new(&e));
    let payload_b = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 1],
    };
    let result_b = try_check_auth(
        &e,
        &s.account,
        &s.gh,
        &s.rule,
        &payload_b,
        &contexts,
        Some(vec![&e, 0]),
    );
    let literal_b = reject_error_literal(&result_b);
    println!("GH-13(b) error literal: {literal_b}");
    assert!(result_b.is_err(), "GH-13(b) must reject tampered rule id");

    // (c) add_context_rule is not exposed on the account.
    let literal_c = invoke_missing_account_fn(&e, &s.account, "add_context_rule");
    println!("GH-13(c) error literal: {literal_c}");
    assert!(
        literal_c.to_ascii_lowercase().contains("not")
            || literal_c.contains("InvalidAction")
            || literal_c.contains("Abort"),
        "GH-13(c) expected missing function; got {literal_c}"
    );
}

#[test]
fn gh_24_mature_hold_transfer_without_owner_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let amount: i128 = 25;
    queue_mature_hold(&e, &s, &s.attacker, amount);

    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.attacker, amount);
    let contexts = vec![&e, ctx.clone()];

    // (a) no signatures.
    let payload_a = AuthPayload {
        signers: Map::new(&e),
        context_rule_ids: vec![&e, 0],
    };
    let result_a = try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload_a, &contexts, None);
    let literal_a = reject_error_literal(&result_a);
    println!("GH-24(a) error literal: {literal_a}");
    assert!(result_a.is_err(), "GH-24(a) must reject unsigned mature hold transfer");
    assert!(
        literal_a.contains("Contract(2)")
            || literal_a.contains("OwnerNotAuthenticated")
            || literal_a.contains("#2"),
        "GH-24(a) expected OwnerNotAuthenticated; got {literal_a}"
    );

    // (b) attacker signature only, not owner.
    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.attacker.clone()), Bytes::new(&e));
    let payload_b = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 0],
    };
    let result_b = try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload_b, &contexts, None);
    let literal_b = reject_error_literal(&result_b);
    println!("GH-24(b) error literal: {literal_b}");
    assert!(result_b.is_err(), "GH-24(b) must reject attacker-signed transfer");
    assert!(
        literal_b.contains("UnauthorizedSigner") || literal_b.contains("#3003"),
        "GH-24(b) expected UnauthorizedSigner; got {literal_b}"
    );
}

#[test]
fn gh_25_admin_entrypoints_do_not_exist() {
    let e = Env::default();
    let s = deploy_scenario(&e);

    for fn_name in [
        "execute",
        "upgrade",
        "add_policy",
        "remove_policy",
        "add_signer",
        "remove_context_rule",
    ] {
        let literal = invoke_missing_account_fn(&e, &s.account, fn_name);
        println!("GH-25 {fn_name} error literal: {literal}");
        assert!(
            literal.to_ascii_lowercase().contains("not")
                || literal.contains("InvalidAction")
                || literal.contains("Abort"),
            "GH-25 {fn_name} must not exist; got {literal}"
        );
    }
}

#[test]
fn gh_26_direct_enforce_by_attacker_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.trusted, 10);
    let signers = Vec::from_array(&e, [Signer::Delegated(s.owner.clone())]);
    e.set_auths(&[]);

    let err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        PolicyClient::new(&e, &s.gh).enforce(&ctx, &signers, &s.rule, &s.account);
    }));
    assert!(err.is_err(), "GH-26 must reject direct enforce without account auth");
    let literal = if let Err(payload) = err {
        if let Some(s) = payload.downcast_ref::<&str>() {
            (*s).to_string()
        } else if let Some(s) = payload.downcast_ref::<String>() {
            s.clone()
        } else {
            format!("{:?}", payload)
        }
    } else {
        "Ok".to_string()
    };
    println!("GH-26 error literal: {literal}");
    assert!(
        literal.contains("Auth")
            || literal.contains("InvalidAction")
            || literal.to_ascii_lowercase().contains("auth")
            || literal.contains("Unauthorized function call"),
        "GH-26 expected auth failure; got {literal}"
    );
}
