package com.guardpay.shared.stellar

/**
 * PROVISIONAL contract function and field names. Nothing here is frozen: the
 * security spike fixes behaviour, not read-function names, and INTERFACES.md
 * (Sync 1) replaces this object with Persona 1's real ABI. Only the token
 * functions are stable (SEP-41).
 */
internal object ProvisionalAbi {
    // SEP-41 token (USDC SAC).
    const val TOKEN_BALANCE = "balance" // balance(id) -> i128
    const val TOKEN_TRANSFER = "transfer" // transfer(from, to, amount)

    // hold_registry. ready_at is computed by the registry, never an argument.
    const val REGISTRY_QUEUE = "queue" // queue(account, token, destination, amount) -> u64
    const val REGISTRY_CANCEL = "cancel" // cancel(caller, id)
    const val REGISTRY_HOLDS_OF = "holds_of" // holds_of(account) -> Vec<HoldRecord>

    // guardian_hold policy.
    const val POLICY_CONFIG = "config" // config(account) -> Config
    const val POLICY_CONTACTS = "contacts" // contacts(account) -> Vec<Address>
    const val POLICY_SPENT_TODAY = "spent_today" // spent_today(account) -> i128

    /** `HoldRecord` struct fields. */
    object Hold {
        const val ID = "id"
        const val ACCOUNT = "account"
        const val TOKEN = "token"
        const val DESTINATION = "destination"
        const val AMOUNT = "amount"
        const val CREATED_AT = "created_at"
        const val READY_AT = "ready_at"
        const val EXPIRES_AT = "expires_at"
        const val STATUS = "status"
        const val CANCELLED_BY = "cancelled_by"
        const val CANCELLED_AT = "cancelled_at"
    }

    /** `HoldStatus` unit enum variants. */
    object Status {
        const val HELD = "Held"
        const val EXECUTED = "Executed"
        const val CANCELLED = "Cancelled"
    }

    /** guardian_hold `Config` struct fields. */
    object Config {
        const val GUARDIAN = "guardian"
        const val DAILY_CAP = "daily_cap"
        const val HOLD_SECS = "hold_secs"
        const val EXPIRY_SECS = "expiry_secs"
    }
}
