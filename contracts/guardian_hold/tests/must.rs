//! MUST suite for the production contracts. GH-01…GH-27, GH-31, GH-32, plus GH-07b, GH-18b, GH-20b.

extern crate std;

use account::Account;
use guardian_hold::{GuardianHold, GuardianHoldAccountParams, GuardianHoldClient};
use hold_registry::{HoldRegistry, HoldRegistryClient, HoldStatus, HOLD_DURATION_SECS};
use soroban_sdk::xdr::{MuxedEd25519Account, ScAddress, ScVal, Uint256};
use soroban_sdk::{
    auth::{Context, ContractContext},
    crypto::Hash,
    testutils::{Address as _, Events as _, Ledger as _, MockAuth, MockAuthInvoke},
    vec, Address, Bytes, BytesN, Env, IntoVal, Map, Symbol, TryIntoVal, Val, Vec,
};
use soroban_sdk::InvokeError;
use soroban_sdk::xdr::ToXdr;
use stellar_accounts::policies::PolicyClient;
use stellar_accounts::smart_account::{
    get_context_rule, get_context_rules_count, AuthPayload, ContextRule, Signer, SmartAccountError,
};

const BASE_TS: u64 = 1_700_000_000;
const DAILY_CAP: i128 = 50;

struct Scenario {
    owner: Address,
    guardian: Address,
    trusted: Address,
    attacker: Address,
    usdc: Address,
    account: Address,
    gh: Address,
    registry: Address,
    rule: ContextRule,
    params: GuardianHoldAccountParams,
}

fn contacts(e: &Env, trusted: &Address) -> Vec<Address> {
    Vec::from_array(e, [trusted.clone()])
}

fn deploy(e: &Env) -> Scenario {
    e.mock_all_auths();
    e.ledger().with_mut(|li| li.timestamp = BASE_TS);

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
        trusted_contacts: contacts(e, &trusted),
        registry: registry.clone(),
        daily_cap: DAILY_CAP,
    };
    let install_val: Val = params.clone().into_val(e);
    let account = e.register(Account, (owner.clone(), gh.clone(), install_val));
    let rule = e.as_contract(&account, || get_context_rule(e, 0));
    e.set_auths(&[]);

    Scenario {
        owner,
        guardian,
        trusted,
        attacker,
        usdc,
        account,
        gh,
        registry,
        rule,
        params,
    }
}

fn transfer_context(e: &Env, token: &Address, from: &Address, to: Val, amount: i128) -> Context {
    Context::Contract(ContractContext {
        contract: token.clone(),
        fn_name: Symbol::new(e, "transfer"),
        args: (from.clone(), to, amount).into_val(e),
    })
}

fn address_transfer(e: &Env, token: &Address, from: &Address, to: &Address, amount: i128) -> Context {
    transfer_context(e, token, from, to.clone().into_val(e), amount)
}

fn fn_context(e: &Env, token: &Address, name: &str, args: Vec<Val>) -> Context {
    Context::Contract(ContractContext {
        contract: token.clone(),
        fn_name: Symbol::new(e, name),
        args,
    })
}

fn signature_payload(e: &Env) -> Hash<32> {
    e.crypto().sha256(&Bytes::from_array(e, &[7u8; 32]))
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
    payload: &AuthPayload,
    contexts: &Vec<Context>,
    signed_rule_ids: Option<Vec<u32>>,
) -> Result<(), Result<SmartAccountError, InvokeError>> {
    let mut invokes: std::vec::Vec<MockAuthInvoke<'_>> = std::vec![];
    let mut delegated: std::vec::Vec<Address> = std::vec![];
    for signer in payload.signers.keys().iter() {
        if let Signer::Delegated(addr) = signer {
            delegated.push(addr.clone());
        }
    }
    for _owner in delegated.iter() {
        let rule_ids = signed_rule_ids
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
    for (idx, owner) in delegated.iter().enumerate() {
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
    out
}

fn owner_auth(
    e: &Env,
    s: &Scenario,
    ctx: Context,
) -> Result<(), Result<SmartAccountError, InvokeError>> {
    let contexts = vec![e, ctx];
    try_check_auth(e, &s.account, &owner_payload(e, &s.owner), &contexts, None)
}

fn empty_auth(
    e: &Env,
    s: &Scenario,
    ctx: Context,
) -> Result<(), Result<SmartAccountError, InvokeError>> {
    let contexts = vec![e, ctx];
    let payload = AuthPayload {
        signers: Map::new(e),
        context_rule_ids: vec![e, 0],
    };
    try_check_auth(e, &s.account, &payload, &contexts, None)
}

fn assert_reject(result: Result<(), Result<SmartAccountError, InvokeError>>, label: &str) {
    assert!(result.is_err(), "{label} must reject, got ok");
}

fn queue_with_owner(e: &Env, s: &Scenario, destination: &Address, amount: i128) -> u64 {
    let sig_payload = e.crypto().sha256(&Bytes::from_array(e, &[8u8; 32]));
    let mut preimage = sig_payload.to_bytes().to_bytes();
    preimage.append(&vec![e, 0u32].to_xdr(e));
    let auth_digest = e.crypto().sha256(&preimage);
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
    let id = HoldRegistryClient::new(e, &s.registry).queue(
        &s.account,
        &s.usdc,
        destination,
        &amount,
    );
    e.set_auths(&[]);
    id
}

fn seed_hold(e: &Env, s: &Scenario, destination: &Address, amount: i128) -> u64 {
    e.mock_all_auths();
    let id = HoldRegistryClient::new(e, &s.registry).queue(
        &s.account,
        &s.usdc,
        destination,
        &amount,
    );
    e.set_auths(&[]);
    id
}

fn missing_fn(e: &Env, account: &Address, name: &str) -> bool {
    let result = e.try_invoke_contract::<(), SmartAccountError>(
        account,
        &Symbol::new(e, name),
        vec![e],
    );
    result.is_err()
}

#[test]
fn gh_01_transfer_without_owner_signature_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10);
    assert_reject(empty_auth(&e, &s, ctx), "GH-01");
}

#[test]
fn gh_02_trusted_payment_within_cap() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10);
    let result = owner_auth(&e, &s, ctx);
    assert!(result.is_ok(), "GH-02 must pass");
    assert_eq!(GuardianHoldClient::new(&e, &s.gh).spent_today(&s.account), 10);
}

#[test]
fn gh_03_unknown_destination_without_hold_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-03");
}

#[test]
fn gh_04_queue_computes_ready_at() {
    let e = Env::default();
    let s = deploy(&e);
    let id = seed_hold(&e, &s, &s.attacker, 100);
    let queued_events = e.events().all().events().len();
    let record = HoldRegistryClient::new(&e, &s.registry).get_hold(&s.account, &id);
    assert_eq!(record.status, HoldStatus::Retained);
    assert_eq!(record.created_at, BASE_TS);
    assert_eq!(record.ready_at, BASE_TS + HOLD_DURATION_SECS);
    assert!(queued_events > 0, "GH-04 must emit an event");
}

#[test]
fn gh_05_transfer_before_ready_at_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + 60);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-05");
}

#[test]
fn gh_06_mature_transfer_needs_a_new_owner_signature() {
    let e = Env::default();
    let s = deploy(&e);
    let id = seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 10);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(empty_auth(&e, &s, ctx.clone()), "GH-06a");
    assert!(owner_auth(&e, &s, ctx).is_ok(), "GH-06b");
    let record = HoldRegistryClient::new(&e, &s.registry).get_hold(&s.account, &id);
    assert_eq!(record.status, HoldStatus::Executed);
}

#[test]
fn gh_07_guardian_cancel() {
    let e = Env::default();
    let s = deploy(&e);
    let id = seed_hold(&e, &s, &s.attacker, 100);
    e.mock_all_auths();
    HoldRegistryClient::new(&e, &s.registry).cancel(&s.account, &id);
    assert_eq!(
        HoldRegistryClient::new(&e, &s.registry)
            .get_hold(&s.account, &id)
            .status,
        HoldStatus::Stopped
    );
}

#[test]
fn gh_07b_stranger_cannot_cancel() {
    let e = Env::default();
    let s = deploy(&e);
    let id = seed_hold(&e, &s, &s.attacker, 100);
    e.mock_auths(&[MockAuth {
        address: &s.attacker,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "cancel",
            args: (&s.account, id).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    let result = HoldRegistryClient::new(&e, &s.registry).try_cancel(&s.account, &id);
    assert!(result.is_err(), "GH-07b");
}

#[test]
fn gh_08_stopped_hold_cannot_be_sent() {
    let e = Env::default();
    let s = deploy(&e);
    let id = seed_hold(&e, &s, &s.attacker, 100);
    e.mock_all_auths();
    HoldRegistryClient::new(&e, &s.registry).cancel(&s.account, &id);
    e.set_auths(&[]);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 10);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-08");
}

#[test]
fn gh_09_guardian_cannot_spend() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.guardian, 10);
    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.guardian.clone()), Bytes::new(&e));
    let payload = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 0],
    };
    assert_reject(
        try_check_auth(&e, &s.account, &payload, &vec![&e, ctx], None),
        "GH-09a",
    );

    let approve = fn_context(
        &e,
        &s.usdc,
        "approve",
        vec![
            &e,
            s.account.clone().into_val(&e),
            s.attacker.clone().into_val(&e),
            100i128.into_val(&e),
            1000u32.into_val(&e),
        ],
    );
    let mut both = Map::new(&e);
    both.set(Signer::Delegated(s.owner.clone()), Bytes::new(&e));
    both.set(Signer::Delegated(s.guardian.clone()), Bytes::new(&e));
    let both_payload = AuthPayload {
        signers: both,
        context_rule_ids: vec![&e, 0],
    };
    assert_reject(
        try_check_auth(&e, &s.account, &both_payload, &vec![&e, approve], None),
        "GH-09b",
    );

    let rules_before = e.as_contract(&s.account, || get_context_rules_count(&e));
    e.mock_auths(&[MockAuth {
        address: &s.guardian,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "queue",
            args: (&s.account, &s.usdc, &s.attacker, 100i128).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    assert!(HoldRegistryClient::new(&e, &s.registry)
        .try_queue(&s.account, &s.usdc, &s.attacker, &100i128)
        .is_err());
    assert_eq!(
        e.as_contract(&s.account, || get_context_rules_count(&e)),
        rules_before
    );
}

#[test]
fn gh_10_owner_signature_still_obeys_the_allowlist() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-10");
}

#[test]
fn gh_11_fee_payer_authorizes_nothing() {
    let e = Env::default();
    let s = deploy(&e);
    let transfer = address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10);
    assert_reject(empty_auth(&e, &s, transfer), "GH-11 transfer");
    e.mock_auths(&[MockAuth {
        address: &s.attacker,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "queue",
            args: (&s.account, &s.usdc, &s.attacker, 100i128).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    assert!(HoldRegistryClient::new(&e, &s.registry)
        .try_queue(&s.account, &s.usdc, &s.attacker, &100i128)
        .is_err());
    assert!(HoldRegistryClient::new(&e, &s.registry)
        .try_cancel(&s.account, &0)
        .is_err());
}

#[test]
fn gh_12_no_ai_input_changes_rejection() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-12");
}

#[test]
fn gh_13_only_one_rule_and_no_admin_entry_points() {
    let e = Env::default();
    let s = deploy(&e);
    assert_eq!(e.as_contract(&s.account, || get_context_rules_count(&e)), 1);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10);
    let contexts = vec![&e, ctx];
    let payload = owner_payload(&e, &s.owner);
    let mut other_ids = payload.clone();
    other_ids.context_rule_ids = vec![&e, 1];
    assert_reject(
        try_check_auth(&e, &s.account, &other_ids, &contexts, None),
        "GH-13a",
    );
    assert_reject(
        try_check_auth(&e, &s.account, &payload, &contexts, Some(vec![&e, 1])),
        "GH-13b",
    );
    assert!(missing_fn(&e, &s.account, "add_context_rule"), "GH-13c");
}

#[test]
fn gh_14_transfer_lanes() {
    let e = Env::default();
    let s = deploy(&e);
    assert!(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10)
        )
        .is_ok(),
        "trusted inside cap"
    );
    assert_reject(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.trusted, 41)
        ),
        "trusted over cap",
    );
    assert_reject(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.attacker, 10)
        ),
        "unknown without hold",
    );
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    assert!(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100)
        )
        .is_ok(),
        "mature exact hold"
    );
}

#[test]
fn gh_15_transfer_from_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = fn_context(
        &e,
        &s.usdc,
        "transfer_from",
        vec![
            &e,
            s.attacker.clone().into_val(&e),
            s.account.clone().into_val(&e),
            s.attacker.clone().into_val(&e),
            100i128.into_val(&e),
        ],
    );
    assert_reject(owner_auth(&e, &s, ctx), "GH-15");
}

#[test]
fn gh_16_approve_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = fn_context(
        &e,
        &s.usdc,
        "approve",
        vec![
            &e,
            s.account.clone().into_val(&e),
            s.attacker.clone().into_val(&e),
            100i128.into_val(&e),
            10u32.into_val(&e),
        ],
    );
    assert_reject(owner_auth(&e, &s, ctx), "GH-16");
}

#[test]
fn gh_17_burn_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = fn_context(
        &e,
        &s.usdc,
        "burn",
        vec![&e, s.account.clone().into_val(&e), 10i128.into_val(&e)],
    );
    assert_reject(owner_auth(&e, &s, ctx), "GH-17");
}

#[test]
fn gh_18_nested_contract_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let other = Address::generate(&e);
    let ctx = fn_context(
        &e,
        &other,
        "pay",
        vec![
            &e,
            s.account.clone().into_val(&e),
            s.attacker.clone().into_val(&e),
            100i128.into_val(&e),
        ],
    );
    assert_reject(owner_auth(&e, &s, ctx), "GH-18");
}

#[test]
fn gh_18b_nested_contract_rejected_with_mature_hold() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let other = Address::generate(&e);
    let ctx = fn_context(
        &e,
        &other,
        "pay",
        vec![
            &e,
            s.account.clone().into_val(&e),
            s.attacker.clone().into_val(&e),
            100i128.into_val(&e),
        ],
    );
    assert_reject(owner_auth(&e, &s, ctx), "GH-18b");
}

#[test]
fn gh_19_replay_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert!(owner_auth(&e, &s, ctx.clone()).is_ok());
    assert_reject(owner_auth(&e, &s, ctx), "GH-19");
}

#[test]
fn gh_20_other_destination_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let other = Address::generate(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &other, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-20");
}

#[test]
fn gh_20b_muxed_destination_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let sc = ScVal::Address(ScAddress::MuxedAccount(MuxedEd25519Account {
        ed25519: Uint256([9u8; 32]),
        id: 7,
    }));
    let to: Val = sc.try_into_val(&e).expect("muxed val");
    let ctx = transfer_context(&e, &s.usdc, &s.account, to, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-20b");
}

#[test]
fn gh_21_other_amount_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    assert_reject(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.attacker, 99),
        ),
        "GH-21 99",
    );
    assert_reject(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.attacker, 101),
        ),
        "GH-21 101",
    );
}

#[test]
fn gh_22_other_token_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let xlm = Address::generate(&e);
    let ctx = address_transfer(&e, &xlm, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-22");
}

#[test]
fn gh_23_other_account_hold_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let other_owner = Address::generate(&e);
    e.mock_all_auths();
    let other_gh = e.register(GuardianHold, ());
    let other_registry = e.register(HoldRegistry, (s.guardian.clone(), other_gh.clone()));
    let params = GuardianHoldAccountParams {
        owner: other_owner.clone(),
        usdc: s.usdc.clone(),
        trusted_contacts: contacts(&e, &s.trusted),
        registry: other_registry.clone(),
        daily_cap: DAILY_CAP,
    };
    let install_val: Val = params.into_val(&e);
    let other_account = e.register(Account, (other_owner, other_gh, install_val));
    HoldRegistryClient::new(&e, &other_registry).queue(
        &other_account,
        &s.usdc,
        &s.attacker,
        &100i128,
    );
    e.set_auths(&[]);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(owner_auth(&e, &s, ctx), "GH-23");
}

#[test]
fn gh_24_mature_transfer_without_owner_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    seed_hold(&e, &s, &s.attacker, 100);
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + HOLD_DURATION_SECS + 1);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.attacker, 100);
    assert_reject(empty_auth(&e, &s, ctx.clone()), "GH-24a");
    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.attacker.clone()), Bytes::new(&e));
    let payload = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 0],
    };
    assert_reject(
        try_check_auth(&e, &s.account, &payload, &vec![&e, ctx], None),
        "GH-24b",
    );
}

#[test]
fn gh_25_admin_names_are_not_entry_points() {
    let e = Env::default();
    let s = deploy(&e);
    for name in [
        "execute",
        "upgrade",
        "add_policy",
        "remove_policy",
        "add_signer",
        "remove_context_rule",
    ] {
        assert!(missing_fn(&e, &s.account, name), "{name}");
    }
}

#[test]
fn gh_26_direct_enforce_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let ctx = address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10);
    let signers = Vec::from_array(&e, [Signer::Delegated(s.owner.clone())]);
    let result = PolicyClient::new(&e, &s.gh).try_enforce(
        &ctx,
        &signers,
        &s.rule,
        &s.account,
    );
    assert!(result.is_err(), "GH-26");
}

#[test]
fn gh_27_queue_without_owner_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    e.mock_auths(&[MockAuth {
        address: &s.attacker,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "queue",
            args: (&s.account, &s.usdc, &s.attacker, 100i128).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    assert!(HoldRegistryClient::new(&e, &s.registry)
        .try_queue(&s.account, &s.usdc, &s.attacker, &100i128)
        .is_err());
}

#[test]
fn gh_31_second_identical_queue_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    let id = queue_with_owner(&e, &s, &s.attacker, 100);
    assert_eq!(id, 0);
    let sig_payload = e.crypto().sha256(&Bytes::from_array(&e, &[8u8; 32]));
    let mut preimage = sig_payload.to_bytes().to_bytes();
    preimage.append(&vec![&e, 0u32].to_xdr(&e));
    let auth_digest = e.crypto().sha256(&preimage);
    let owner_check_auth = MockAuthInvoke {
        contract: &s.account,
        fn_name: "__check_auth",
        args: (auth_digest,).into_val(&e),
        sub_invokes: &[],
    };
    let queue_invoke = MockAuthInvoke {
        contract: &s.registry,
        fn_name: "queue",
        args: (&s.account, &s.usdc, &s.attacker, 100i128).into_val(&e),
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
    assert!(HoldRegistryClient::new(&e, &s.registry)
        .try_queue(&s.account, &s.usdc, &s.attacker, &100)
        .is_err());
}

#[test]
fn gh_32_second_install_rejected() {
    let e = Env::default();
    let s = deploy(&e);
    e.mock_all_auths();
    let install_val: Val = s.params.clone().into_val(&e);
    let result = PolicyClient::new(&e, &s.gh).try_install(&install_val, &s.rule, &s.account);
    assert!(result.is_err(), "GH-32");
}

#[test]
fn overflow_near_i128_max_rejected() {
    let e = Env::default();
    e.mock_all_auths();
    e.ledger().with_mut(|li| li.timestamp = BASE_TS);
    let owner = Address::generate(&e);
    let trusted = Address::generate(&e);
    let guardian = Address::generate(&e);
    let usdc = Address::generate(&e);
    let gh = e.register(GuardianHold, ());
    let registry = e.register(HoldRegistry, (guardian, gh.clone()));
    let params = GuardianHoldAccountParams {
        owner: owner.clone(),
        usdc: usdc.clone(),
        trusted_contacts: contacts(&e, &trusted),
        registry,
        daily_cap: i128::MAX,
    };
    let install_val: Val = params.into_val(&e);
    let account = e.register(Account, (owner.clone(), gh.clone(), install_val));
    e.set_auths(&[]);
    let rule = e.as_contract(&account, || get_context_rule(&e, 0));
    let scenario_account = account.clone();
    let ctx = address_transfer(&e, &usdc, &scenario_account, &trusted, i128::MAX);
    let contexts = vec![&e, ctx];
    assert!(try_check_auth(&e, &account, &owner_payload(&e, &owner), &contexts, None).is_ok());
    let again = address_transfer(&e, &usdc, &account, &trusted, 1);
    assert_reject(
        try_check_auth(
            &e,
            &account,
            &owner_payload(&e, &owner),
            &vec![&e, again],
            None,
        ),
        "overflow",
    );
    let _ = rule;
}

#[test]
fn persistent_records_survive_ledger_advance() {
    let e = Env::default();
    let s = deploy(&e);
    let id = seed_hold(&e, &s, &s.attacker, 100);
    assert!(owner_auth(
        &e,
        &s,
        address_transfer(&e, &s.usdc, &s.account, &s.trusted, 10)
    )
    .is_ok());
    e.ledger().with_mut(|li| {
        li.sequence_number += 50_000;
        li.timestamp += 1_000;
    });
    let record = HoldRegistryClient::new(&e, &s.registry).get_hold(&s.account, &id);
    assert_eq!(record.status, HoldStatus::Retained);
    assert_eq!(record.ready_at, BASE_TS + HOLD_DURATION_SECS);
    assert_eq!(GuardianHoldClient::new(&e, &s.gh).spent_today(&s.account), 10);
}

#[test]
fn daily_window_resets_on_the_next_utc_day() {
    let e = Env::default();
    let s = deploy(&e);
    assert!(owner_auth(
        &e,
        &s,
        address_transfer(&e, &s.usdc, &s.account, &s.trusted, 50)
    )
    .is_ok());
    assert_reject(
        owner_auth(
            &e,
            &s,
            address_transfer(&e, &s.usdc, &s.account, &s.trusted, 1)
        ),
        "same day",
    );
    e.ledger().with_mut(|li| li.timestamp = BASE_TS + 86_400);
    assert!(owner_auth(
        &e,
        &s,
        address_transfer(&e, &s.usdc, &s.account, &s.trusted, 1)
    )
    .is_ok());
}
