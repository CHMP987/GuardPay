//! GH-09: guardian cannot spend — (a) G-signed transfer, (b) G co-signer on approve, (c) G calls R/GH APIs.

extern crate std;

use guardpay_spike_account::SpikeAccount;
use guardian_hold::{GuardianHold, GuardianHoldAccountParams};
use hold_registry::{HoldRegistry, HoldRegistryClient, HoldStatus};
use soroban_sdk::xdr::ToXdr;
use soroban_sdk::{
    auth::{Context, ContractContext},
    crypto::Hash,
    testutils::{Address as _, MockAuth, MockAuthInvoke},
    vec, Address, Bytes, BytesN, Env, IntoVal, Map, Val, Vec,
};
use soroban_sdk::InvokeError;
use stellar_accounts::policies::PolicyClient;
use stellar_accounts::smart_account::{
    get_context_rule, get_context_rules_count, AuthPayload, ContextRule, Signer, SmartAccountError,
};

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
    install_params: GuardianHoldAccountParams,
}

struct AccountConfigSnapshot {
    rules_count: u32,
    owner_signer: Signer,
    gh_policy: Address,
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

fn approve_context(
    e: &Env,
    token: &Address,
    from: &Address,
    spender: &Address,
    amount: i128,
    expiration_ledger: u32,
) -> Context {
    Context::Contract(ContractContext {
        contract: token.clone(),
        fn_name: soroban_sdk::symbol_short!("approve"),
        args: (from.clone(), spender.clone(), amount, expiration_ledger).into_val(e),
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
    let install_params = GuardianHoldAccountParams {
        owner: owner.clone(),
        usdc: usdc.clone(),
        trusted_contact: trusted.clone(),
        registry: registry.clone(),
    };
    let install_val: Val = install_params.clone().into_val(e);
    let account = e.register(SpikeAccount, (owner.clone(), gh.clone(), install_val));

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
        install_params,
    }
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

fn panic_literal(err: std::thread::Result<()>) -> String {
    match err {
        Ok(()) => "Ok(())".to_string(),
        Err(payload) => {
            if let Some(s) = payload.downcast_ref::<&str>() {
                (*s).to_string()
            } else if let Some(s) = payload.downcast_ref::<String>() {
                s.clone()
            } else {
                format!("{:?}", payload)
            }
        }
    }
}

fn snapshot_account_config(e: &Env, account: &Address, owner: &Address, gh: &Address) -> AccountConfigSnapshot {
    let snap = e.as_contract(account, || AccountConfigSnapshot {
        rules_count: get_context_rules_count(e),
        owner_signer: get_context_rule(e, 0).signers.get(0).unwrap(),
        gh_policy: get_context_rule(e, 0).policies.get(0).unwrap(),
    });
    assert_eq!(snap.owner_signer, Signer::Delegated(owner.clone()));
    assert_eq!(snap.gh_policy, *gh);
    snap
}

fn assert_config_unchanged(before: &AccountConfigSnapshot, after: &AccountConfigSnapshot) {
    assert_eq!(before.rules_count, after.rules_count, "rule count must stay 1");
    assert_eq!(before.owner_signer, after.owner_signer, "owner signer unchanged");
    assert_eq!(before.gh_policy, after.gh_policy, "policy address unchanged");
    assert_eq!(before.rules_count, 1, "exactly one context rule");
}

#[test]
fn gh_09_a_transfer_to_guardian_signed_by_guardian_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.guardian, 10);
    let contexts = vec![&e, ctx];

    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.guardian.clone()), Bytes::new(&e));
    let payload = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 0],
    };

    let result = try_check_auth(&e, &s.account, &s.rule, &payload, &contexts, None);
    let literal = reject_error_literal(&result);
    println!("GH-09(a) error literal: {literal}");
    assert!(result.is_err(), "GH-09(a) must reject guardian-signed transfer; got {literal}");
    assert!(
        literal.contains("UnauthorizedSigner")
            || literal.contains("#3003")
            || literal.contains("OwnerNotAuthenticated")
            || literal.contains("Contract(2)")
            || literal.contains("#2")
            || literal.contains("NotAllowed")
            || literal.contains("Contract(3)"),
        "GH-09(a) expected auth/policy rejection; got {literal}"
    );
}

#[test]
fn gh_09_b_approve_with_guardian_cosigner_rejected() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let ctx = approve_context(&e, &s.usdc, &s.account, &s.attacker, 100, 999_999);
    let contexts = vec![&e, ctx];

    let mut signers = Map::new(&e);
    signers.set(Signer::Delegated(s.owner.clone()), Bytes::new(&e));
    signers.set(Signer::Delegated(s.guardian.clone()), Bytes::new(&e));
    let payload = AuthPayload {
        signers,
        context_rule_ids: vec![&e, 0],
    };

    let result = try_check_auth(&e, &s.account, &s.rule, &payload, &contexts, None);
    let literal = reject_error_literal(&result);
    println!("GH-09(b) error literal: {literal}");
    assert!(result.is_err(), "GH-09(b) must reject approve with guardian co-signer; got {literal}");
    assert!(
        literal.contains("UnauthorizedSigner")
            || literal.contains("#3003")
            || literal.contains("NotAllowed")
            || literal.contains("Contract(3)"),
        "GH-09(b) expected UnauthorizedSigner or NotAllowed; got {literal}"
    );

    let rule = e.as_contract(&s.account, || get_context_rule(&e, 0));
    assert_eq!(rule.signers.len(), 1, "guardian must not be added as account signer");
    assert_eq!(
        rule.signers.get(0).unwrap(),
        Signer::Delegated(s.owner.clone())
    );
}

#[test]
fn gh_09_c_guardian_public_calls_no_funds_or_config_change() {
    let e = Env::default();
    let s = deploy_scenario(&e);
    let client = HoldRegistryClient::new(&e, &s.registry);
    let before = snapshot_account_config(&e, &s.account, &s.owner, &s.gh);

    // queue as guardian (no account auth) → reject
    e.set_auths(&[]);
    e.mock_auths(&[MockAuth {
        address: &s.guardian,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "queue",
            args: (&s.account, &s.usdc, &s.attacker, 50i128).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    let queue_err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        client.queue(&s.account, &s.usdc, &s.attacker, &50i128);
    }));
    let queue_failed = queue_err.is_err();
    let queue_literal = panic_literal(queue_err);
    println!("GH-09(c) queue as G: {queue_literal}");
    assert!(queue_failed, "GH-09(c) guardian queue must fail");
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // mark_executed as guardian → reject (only guardian_hold may call)
    e.mock_all_auths();
    let hold_id = client.queue(&s.account, &s.usdc, &s.attacker, &40i128);
    e.set_auths(&[]);
    e.mock_auths(&[MockAuth {
        address: &s.guardian,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "mark_executed",
            args: (&s.account, hold_id).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    let mark_err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        client.mark_executed(&s.account, &hold_id);
    }));
    let mark_failed = mark_err.is_err();
    let mark_literal = panic_literal(mark_err);
    println!("GH-09(c) mark_executed as G: {mark_literal}");
    assert!(mark_failed, "GH-09(c) guardian mark_executed must fail");
    assert_eq!(
        client.get_hold(&s.account, &hold_id).status,
        HoldStatus::Retained,
        "hold must stay retained after failed mark_executed"
    );
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // cancel as guardian → allowed (veto), does not change account config
    e.mock_auths(&[MockAuth {
        address: &s.guardian,
        invoke: &MockAuthInvoke {
            contract: &s.registry,
            fn_name: "cancel",
            args: (&s.account, hold_id).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    client.cancel(&s.account, &hold_id);
    assert_eq!(
        client.get_hold(&s.account, &hold_id).status,
        HoldStatus::Stopped
    );
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // read-only registry calls from guardian address (no auth required)
    let _ = client.get_hold(&s.account, &hold_id);
    let _ = client.find_retained(&s.account, &s.usdc, &s.attacker, &40i128);
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // GuardianHold: enforce as guardian → reject
    e.set_auths(&[]);
    let ctx = transfer_context(&e, &s.usdc, &s.account, &s.trusted, 10);
    let signers = Vec::from_array(&e, [Signer::Delegated(s.guardian.clone())]);
    let enforce_err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        PolicyClient::new(&e, &s.gh).enforce(&ctx, &signers, &s.rule, &s.account);
    }));
    let enforce_failed = enforce_err.is_err();
    let enforce_literal = panic_literal(enforce_err);
    println!("GH-09(c) enforce as G: {enforce_literal}");
    assert!(enforce_failed, "GH-09(c) guardian enforce must fail");
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // install as guardian → reject
    let install_params_val: Val = s.install_params.clone().into_val(&e);
    e.mock_auths(&[MockAuth {
        address: &s.guardian,
        invoke: &MockAuthInvoke {
            contract: &s.gh,
            fn_name: "install",
            args: (install_params_val.clone(), s.rule.clone(), s.account.clone()).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    let install_g_err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        PolicyClient::new(&e, &s.gh).install(&install_params_val, &s.rule, &s.account);
    }));
    let install_g_failed = install_g_err.is_err();
    let install_g_literal = panic_literal(install_g_err);
    println!("GH-09(c) install as G: {install_g_literal}");
    assert!(install_g_failed, "GH-09(c) guardian install must fail");
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // second install as owner (already installed at deploy) → reject
    let reinstall_params = GuardianHoldAccountParams {
        owner: s.owner.clone(),
        usdc: s.usdc.clone(),
        trusted_contact: s.trusted.clone(),
        registry: s.registry.clone(),
    };
    let reinstall_val: Val = reinstall_params.clone().into_val(&e);
    e.mock_auths(&[MockAuth {
        address: &s.account,
        invoke: &MockAuthInvoke {
            contract: &s.gh,
            fn_name: "install",
            args: (reinstall_val.clone(), s.rule.clone(), s.account.clone()).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    let install2_err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        PolicyClient::new(&e, &s.gh).install(&reinstall_val, &s.rule, &s.account);
    }));
    let install2_failed = install2_err.is_err();
    let install2_literal = panic_literal(install2_err);
    println!("GH-09(c) second install: {install2_literal}");
    assert!(install2_failed, "GH-09(c) second install must fail");
    assert!(
        install2_literal.contains("AlreadyInstalled")
            || install2_literal.contains("Contract(1)")
            || install2_literal.contains("Contract, #1)"),
        "GH-09(c) expected AlreadyInstalled; got {install2_literal}"
    );
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));

    // uninstall as guardian → reject
    e.mock_auths(&[MockAuth {
        address: &s.guardian,
        invoke: &MockAuthInvoke {
            contract: &s.gh,
            fn_name: "uninstall",
            args: (s.rule.clone(), s.account.clone()).into_val(&e),
            sub_invokes: &[],
        },
    }]);
    let uninstall_err = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        PolicyClient::new(&e, &s.gh).uninstall(&s.rule, &s.account);
    }));
    let uninstall_failed = uninstall_err.is_err();
    let uninstall_literal = panic_literal(uninstall_err);
    println!("GH-09(c) uninstall as G: {uninstall_literal}");
    assert!(uninstall_failed, "GH-09(c) guardian uninstall must fail");
    assert_config_unchanged(&before, &snapshot_account_config(&e, &s.account, &s.owner, &s.gh));
}
