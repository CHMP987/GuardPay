package com.guardpay.shared.stellar

/**
 * The deployed contracts' names, from docs/INTERFACES.md and the sources at the
 * wasm hashes in evidence/stellar/deployment.md. The token functions are SEP-41.
 *
 * Reads that have no view function (the registry's records, the policy's config,
 * the account's rule) go to contract storage, so the storage keys below mirror
 * each contract's `DataKey` enum. A new wasm with another layout must update them.
 */
internal object ContractAbi {
    // SEP-41 token (the test SAC standing in for USDC).
    const val TOKEN_BALANCE = "balance" // balance(id) -> i128
    const val TOKEN_TRANSFER = "transfer" // transfer(from, to, amount)

    // hold_registry. ready_at is computed by the registry, never an argument.
    const val REGISTRY_QUEUE = "queue" // queue(account, token, destination, amount) -> u64; account.require_auth()
    const val REGISTRY_CANCEL = "cancel" // cancel(account, id); guardian.require_auth(), so only the guardian stops

    // guardian_hold policy.
    const val POLICY_SPENT_TODAY = "spent_today" // spent_today(smart_account) -> i128

    // account. The owner is a Delegated signer: their G account authorizes this call.
    const val ACCOUNT_CHECK_AUTH = "__check_auth"

    /** `HOLD_DURATION_SECS` in hold_registry. A constant of the wasm, not readable from storage. */
    const val HOLD_DURATION_SECS = 120L

    /** `HoldRecord` fields. The id is not a field: it is part of the storage key. */
    object Hold {
        const val TOKEN = "token"
        const val DESTINATION = "destination"
        const val AMOUNT = "amount"
        const val CREATED_AT = "created_at"
        const val READY_AT = "ready_at"
        const val STATUS = "status"
    }

    /** `HoldStatus` variants. `Stopped` is only reachable through the guardian's `cancel`. */
    object Status {
        const val RETAINED = "Retained"
        const val STOPPED = "Stopped"
        const val EXECUTED = "Executed"
    }

    /** guardian_hold's stored `GuardianHoldConfig`. */
    object Config {
        const val OWNER = "owner"
        const val USDC = "usdc"
        const val TRUSTED_CONTACTS = "trusted_contacts"
        const val REGISTRY = "registry"
        const val DAILY_CAP = "daily_cap"
    }

    /** `DataKey` variants. All persistent, except the account's `Count` (instance). */
    object Storage {
        const val REGISTRY_GUARDIAN = "Guardian" // -> Address
        const val REGISTRY_NEXT_ID = "NextId" // (account) -> u64
        const val REGISTRY_HOLD = "Hold" // (account, id) -> HoldRecord
        const val POLICY_CONFIG = "Config" // (smart_account) -> GuardianHoldConfig
        const val ACCOUNT_RULE_COUNT = "Count" // instance -> u32
        const val ACCOUNT_SIGNER = "SignerData" // (id) -> { signer: Signer, .. }
        const val ACCOUNT_POLICY = "PolicyData" // (id) -> { policy: Address, .. }
        const val ACCOUNT_RULE = "ContextRuleData" // (id) -> { signer_ids, policy_ids, .. }
    }

    /** The account's `AuthPayload` (OZ smart account) and the owner's signer variant. */
    object AuthPayload {
        const val CONTEXT_RULE_IDS = "context_rule_ids"
        const val SIGNERS = "signers"
        const val DELEGATED = "Delegated"
        const val SIGNER_FIELD = "signer"
        const val POLICY_FIELD = "policy"
        const val EXTERNAL = "External"
        const val SIGNER_IDS = "signer_ids"
        const val POLICY_IDS = "policy_ids"
    }
}
