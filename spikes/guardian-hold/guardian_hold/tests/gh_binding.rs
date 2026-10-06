//! Binding tests GH-20, GH-21, GH-22, GH-23 — typed field-by-field hold match.

extern crate std;

use guardpay_spike_account::SpikeAccount;
use guardian_hold::{GuardianHold, GuardianHoldAccountParams};
use hold_registry::{HoldRegistry, HoldStatus};
use soroban_sdk::{
    auth::{Context, ContractContext},
    crypto::Hash,
    testutils::{Address as _, Ledger as _, MockAuth, MockAuthInvoke},
    vec, Address, Bytes, BytesN, Env, IntoVal, Map, Val, Vec,
};
use soroban_sdk::InvokeError;
use soroban_sdk::xdr::ToXdr;
use hold_registry::HoldRegistryClient;
use stellar_accounts::smart_account::{
    get_context_rule, AuthPayload, ContextRule, Signer, SmartAccountError,
};

const HOLD_DURATION_SECS: u64 = 120;
const HOLD_AMOUNT: i128 = 100;

struct Scenario {
    owner: Address,
    trusted: Address,
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
        usdc,
        account,
        gh,
        registry,
        rule,
    }
}

fn deploy_sibling_account(e: &Env, s: &Scenario, owner: Address) -> Address {
    e.mock_all_auths();
    let params = GuardianHoldAccountParams {
        owner: owner.clone(),
        usdc: s.usdc.clone(),
        trusted_contact: s.trusted.clone(),
        registry: s.registry.clone(),
    };
    let install_val: Val = params.into_val(e);
    let account = e.register(SpikeAccount, (owner.clone(), s.gh.clone(), install_val));
    e.set_auths(&[]);
    account
}

fn signature_payload(e: &Env) -> Hash<32> {
    e.crypto().sha256(&Bytes::from_array(e, &[7u8; 32]))
}

fn try_check_auth(
    e: &Env,
    account: &Address,
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

fn owner_payload(e: &Env, owner: &Address) -> AuthPayload {
    let mut signers = Map::new(e);
    signers.set(Signer::Delegated(owner.clone()), Bytes::new(e));
    AuthPayload {
        signers,
        context_rule_ids: vec![e, 0],
    }
}

fn queue_mature_hold(
    e: &Env,
    registry: &Address,
    account: &Address,
    token: &Address,
    destination: &Address,
    amount: i128,
) -> u64 {
    e.mock_all_auths();
    let id = HoldRegistryClient::new(e, registry).queue(account, token, destination, &amount);
    e.ledger().set_timestamp(e.ledger().timestamp() + HOLD_DURATION_SECS + 1);
    e.set_auths(&[]);
    id
}

fn assert_hold_retained(e: &Env, registry: &Address, account: &Address, hold_id: u64) {
    let record = HoldRegistryClient::new(e, registry).get_hold(account, &hold_id);
    assert_eq!(
        record.status,
        HoldStatus::Retained,
        "hold must stay Retained (RETENIDO)"
    );
}

fn owner_transfer_rejected(
    e: &Env,
    s: &Scenario,
    token: &Address,
    from: &Address,
    to: &Address,
    amount: i128,
    gh_id: &str,
) {
    let ctx = transfer_context(e, token, from, to, amount);
    let contexts = vec![e, ctx];
    let payload = owner_payload(e, &s.owner);
    let result = try_check_auth(e, from, &s.rule, &payload, &contexts, None);
    let literal = reject_error_literal(&result);
    println!("{gh_id} error literal: {literal}");
    assert!(result.is_err(), "{gh_id} must reject mismatched binding; got {literal}");
    assert!(
        literal.contains("NotAllowed")
            || literal.contains("Contract(3)")
            || literal.contains("#3"),
        "{gh_id} expected NotAllowed; got {literal}"
    );
}

struct BaseHold {
    dest_x: Address,
    hold_id: u64,
}

fn setup_base_mature_hold(e: &Env, s: &Scenario) -> BaseHold {
    let dest_x = Address::generate(e);
    let hold_id = queue_mature_hold(
        e,
        &s.registry,
        &s.account,
        &s.usdc,
        &dest_x,
        HOLD_AMOUNT,
    );
    BaseHold { dest_x, hold_id }
}

#[test]
fn gh_20_wrong_destination_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let base = setup_base_mature_hold(&e, &s);
    let dest_x2 = Address::generate(&e);

    owner_transfer_rejected(
        &e,
        &s,
        &s.usdc,
        &s.account,
        &dest_x2,
        HOLD_AMOUNT,
        "GH-20",
    );
    assert_hold_retained(&e, &s.registry, &s.account, base.hold_id);
}

#[test]
fn gh_21_wrong_amount_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let base = setup_base_mature_hold(&e, &s);

    owner_transfer_rejected(
        &e,
        &s,
        &s.usdc,
        &s.account,
        &base.dest_x,
        99,
        "GH-21(a)",
    );
    assert_hold_retained(&e, &s.registry, &s.account, base.hold_id);

    owner_transfer_rejected(
        &e,
        &s,
        &s.usdc,
        &s.account,
        &base.dest_x,
        101,
        "GH-21(b)",
    );
    assert_hold_retained(&e, &s.registry, &s.account, base.hold_id);
}

#[test]
fn gh_22_wrong_token_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let base = setup_base_mature_hold(&e, &s);
    let other_token = Address::generate(&e);

    owner_transfer_rejected(
        &e,
        &s,
        &other_token,
        &s.account,
        &base.dest_x,
        HOLD_AMOUNT,
        "GH-22",
    );
    assert_hold_retained(&e, &s.registry, &s.account, base.hold_id);
}

#[test]
fn gh_23_other_accounts_hold_does_not_authorize_transfer() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let dest_x = Address::generate(&e);

    let owner_c2 = Address::generate(&e);
    let account_c2 = deploy_sibling_account(&e, &s, owner_c2.clone());
    let hold_c2 = queue_mature_hold(
        &e,
        &s.registry,
        &account_c2,
        &s.usdc,
        &dest_x,
        HOLD_AMOUNT,
    );

    // Owner O of C signs transfer C→X; only C2 has a matching retained hold.
    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.owner.clone()), Bytes::new(&e));
    let payload = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 0],
    };
    let ctx = transfer_context(&e, &s.usdc, &s.account, &dest_x, HOLD_AMOUNT);
    let contexts = vec![&e, ctx];
    let result = try_check_auth(&e, &s.account, &s.rule, &payload, &contexts, None);
    let literal = reject_error_literal(&result);
    println!("GH-23 error literal: {literal}");
    assert!(
        result.is_err(),
        "GH-23 must reject transfer on C using C2's hold; got {literal}"
    );
    assert!(
        literal.contains("NotAllowed")
            || literal.contains("Contract(3)")
            || literal.contains("#3"),
        "GH-23 expected NotAllowed; got {literal}"
    );
    assert_hold_retained(&e, &s.registry, &account_c2, hold_c2);
}
