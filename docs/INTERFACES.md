# Firmas públicas congeladas (Spike A → P2)

Origen: `spikes/guardian-hold/` al cierre de T-028. soroban-sdk 28.x. No modificar sin aprobación humana post–Sync 1.

## `SpikeAccount` (`spikes/guardian-hold/account`)

```rust
pub fn __constructor(env: Env, owner: Address, policy: Address, install_params: Val)

// CustomAccountInterface
fn __check_auth(
    env: Env,
    signature_payload: Hash<32>,
    signature: AuthPayload,
    auth_contexts: Vec<Context>,
) -> Result<(), SmartAccountError>
```

`AuthPayload` y `SmartAccountError`: tipos de `stellar_accounts::smart_account`.

## `GuardianHold` (`spikes/guardian-hold/guardian_hold`)

Trait `Policy` (`stellar_accounts::policies::Policy`):

```rust
type AccountParams = GuardianHoldAccountParams;

fn install(
    e: &Env,
    install_params: GuardianHoldAccountParams,
    context_rule: ContextRule,
    smart_account: Address,
)

fn enforce(
    e: &Env,
    context: Context,
    authenticated_signers: Vec<Signer>,
    context_rule: ContextRule,
    smart_account: Address,
)

fn uninstall(e: &Env, context_rule: ContextRule, smart_account: Address)
```

```rust
pub struct GuardianHoldAccountParams {
    pub owner: Address,
    pub usdc: Address,
    pub trusted_contact: Address,
    pub registry: Address,
}
```

## `HoldRegistry` (`spikes/guardian-hold/hold_registry`)

```rust
pub fn __constructor(env: Env, guardian: Address, guardian_hold: Address)

pub fn queue(
    env: Env,
    account: Address,
    token: Address,
    destination: Address,
    amount: i128,
) -> u64

pub fn cancel(env: Env, account: Address, id: u64)

pub fn mark_executed(env: Env, account: Address, id: u64)

pub fn get_hold(env: Env, account: Address, id: u64) -> HoldRecord

pub fn find_retained(
    env: Env,
    account: Address,
    token: Address,
    destination: Address,
    amount: i128,
) -> Option<u64>
```

```rust
pub struct HoldRecord {
    pub token: Address,
    pub destination: Address,
    pub amount: i128,
    pub created_at: u64,
    pub ready_at: u64,
    pub status: HoldStatus,
}
```

`ready_at` no es parámetro de entrada en ninguna función pública; lo calcula `queue` en el registro.
