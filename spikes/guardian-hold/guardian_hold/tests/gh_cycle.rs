//! Cycle tests GH-04, GH-05, GH-06 — queue → wait → transfer with owner signature.

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
use stellar_accounts::smart_account::{
    get_context_rule, AuthPayload, ContextRule, Signer, SmartAccountError,
};

const HOLD_DURATION_SECS: u64 = 120;
const BASE_TS: u64 = 1_700_000_000;
const HOLD_AMOUNT: i128 = 100;

struct Scenario {
    owner: Address,
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

/// Separate payload for queue `require_auth` mocks so transfer `try_check_auth` is not short-circuited.
fn queue_signature_payload(e: &Env) -> Hash<32> {
    e.crypto().sha256(&Bytes::from_array(e, &[8u8; 32]))
}

fn queue_auth_digest(e: &Env) -> Hash<32> {
    let sig_payload = queue_signature_payload(e);
    let mut preimage = sig_payload.to_bytes().to_bytes();
    preimage.append(&vec![e, 0u32].to_xdr(e));
    e.crypto().sha256(&preimage)
}

fn owner_payload(e: &Env, owner: &Address) -> AuthPayload {
    let mut signers = Map::new(e);
    signers.set(Signer::Delegated(owner.clone()), Bytes::new(e));
    AuthPayload {
        signers,
        context_rule_ids: vec![e, 0],
    }
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

fn invoke_queue_with_owner_auth(
    e: &Env,
    s: &Scenario,
    destination: &Address,
    amount: i128,
) -> u64 {
    let auth_digest = queue_auth_digest(e);

    let owner_check_auth = MockAuthInvoke {
        contract: &s.account,
        fn_name: "__check_auth",
        args: (auth_digest,).into_val(e),
        sub_invokes: &[],
    };

    let queue_invoke = MockAuthInvoke {
        contract: &s.registry,
        fn_name: "queue",
        args: (
            s.account.clone(),
            s.usdc.clone(),
            destination.clone(),
            amount,
        )
            .into_val(e),
        sub_invokes: &[owner_check_auth.clone()],
    };

    e.mock_auths(&[
        MockAuth {
            address: &s.owner,
            invoke: &owner_check_auth,
        },
        MockAuth {
            address: &s.account,
            invoke: &queue_invoke,
        },
    ]);

    let id = hold_registry::HoldRegistryClient::new(e, &s.registry).queue(
        &s.account,
        &s.usdc,
        destination,
        &amount,
    );
    e.set_auths(&[]);
    id
}

fn queue_hold_via_owner_auth(e: &Env, s: &Scenario, destination: &Address) -> u64 {
    e.ledger().with_mut(|li| li.timestamp = BASE_TS);

    let id = invoke_queue_with_owner_auth(e, s, destination, HOLD_AMOUNT);

    let client = hold_registry::HoldRegistryClient::new(e, &s.registry);
    let record = client.get_hold(&s.account, &id);
    assert_eq!(record.status, HoldStatus::Retained);
    assert_eq!(record.amount, HOLD_AMOUNT);
    assert_eq!(record.destination, destination.clone());
    assert_eq!(record.token, s.usdc);
    assert_eq!(record.created_at, BASE_TS);
    assert_eq!(record.ready_at, BASE_TS + HOLD_DURATION_SECS);
    id
}

/// Registry row for transfer timing tests (GH-05/06). `mock_all_auths` is limited
/// to the `queue` host call; transfer assertions use `try_check_auth` only.
fn seed_retained_hold(e: &Env, s: &Scenario, destination: &Address) {
    e.ledger().with_mut(|li| li.timestamp = BASE_TS);
    e.mock_all_auths();
    hold_registry::HoldRegistryClient::new(e, &s.registry).queue(
        &s.account,
        &s.usdc,
        destination,
        &HOLD_AMOUNT,
    );
    e.set_auths(&[]);
}

#[test]
fn gh_04_queue_creates_retained_hold_with_ready_at() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let id = queue_hold_via_owner_auth(&e, &s, &s.attacker);
    println!(
        "GH-04 hold id={} ready_at={}",
        id,
        BASE_TS + HOLD_DURATION_SECS
    );
}

#[test]
fn gh_05_transfer_before_ready_at_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    seed_retained_hold(&e, &s, &s.attacker);

    e.ledger().set_timestamp(BASE_TS + 60);

    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.attacker, HOLD_AMOUNT);
    let contexts = vec![&e, ctx];
    let payload = owner_payload(&e, &s.owner);

    let result = try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload, &contexts, None);
    let literal = reject_error_literal(&result);
    println!("GH-05 error literal: {literal}");
    assert!(result.is_err(), "GH-05 must reject transfer before ready_at; got {literal}");
    assert!(
        literal.contains("HoldNotReady")
            || literal.contains("Contract(7)")
            || literal.contains("#7"),
        "GH-05 expected HoldNotReady; got {literal}"
    );

    let record = hold_registry::HoldRegistryClient::new(&e, &s.registry)
        .get_hold(&s.account, &0);
    assert_eq!(record.status, HoldStatus::Retained);
}

#[test]
fn gh_06_mature_transfer_requires_owner_and_marks_executed() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    seed_retained_hold(&e, &s, &s.attacker);

    e.ledger().set_timestamp(BASE_TS + 130);

    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.attacker, HOLD_AMOUNT);
    let contexts = vec![&e, ctx.clone()];

    let payload_unsigned = AuthPayload {
        signers: Map::new(&e),
        context_rule_ids: vec![&e, 0],
    };
    let result_a =
        try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload_unsigned, &contexts, None);
    let literal_a = reject_error_literal(&result_a);
    println!("GH-06(a) error literal: {literal_a}");
    assert!(
        result_a.is_err(),
        "GH-06(a) must reject unsigned mature transfer; got {literal_a}"
    );
    assert!(
        literal_a.contains("Contract(2)")
            || literal_a.contains("OwnerNotAuthenticated")
            || literal_a.contains("#2"),
        "GH-06(a) expected OwnerNotAuthenticated; got {literal_a}"
    );

    let client = hold_registry::HoldRegistryClient::new(&e, &s.registry);
    assert_eq!(client.get_hold(&s.account, &0).status, HoldStatus::Retained);

    let payload_owner = owner_payload(&e, &s.owner);
    let result_b =
        try_check_auth(&e, &s.account, &s.gh, &s.rule, &payload_owner, &contexts, None);
    let literal_b = reject_error_literal(&result_b);
    println!("GH-06(b) result: {literal_b}");
    assert!(
        result_b.is_ok(),
        "GH-06(b) owner-signed mature transfer must pass; got {literal_b}"
    );

    assert_eq!(
        client.get_hold(&s.account, &0).status,
        HoldStatus::Executed
    );
}
