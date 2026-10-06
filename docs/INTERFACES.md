# Firmas públicas (producción)

6 oct 2026. El humano pidió hacer lo que faltaba de la Persona 1, incluida esta actualización. soroban-sdk 28.0.0. La forma del spike (un solo `trusted_contact`, sin tope) queda abajo como historia.

`ready_at` no es parámetro de ninguna función. Lo calcula `queue` como `created_at + 120`. El vencimiento de 600 s no existe en el contrato.

## `Account` (`contracts/account`)

```rust
pub fn __constructor(env: Env, owner: Address, policy: Address, install_params: Val)

fn __check_auth(
    env: Env,
    signature_payload: Hash<32>,
    signature: AuthPayload,
    auth_contexts: Vec<Context>,
) -> Result<(), SmartAccountError>
```

`install_params` es el `Val` de `GuardianHoldAccountParams`. El constructor crea una regla `Default`: firmante = dueña, política = `policy`, y esa llamada instala la política. No hay `execute`, `upgrade`, `add_*` ni `remove_*`.

## `GuardianHold` (`contracts/guardian_hold`)

```rust
pub struct GuardianHoldAccountParams {
    pub owner: Address,
    pub usdc: Address,
    pub trusted_contacts: Vec<Address>, // 1..=3
    pub registry: Address,
    pub daily_cap: i128,
}

fn install(e: &Env, install_params: GuardianHoldAccountParams, context_rule: ContextRule, smart_account: Address)
fn enforce(e: &Env, context: Context, authenticated_signers: Vec<Signer>, context_rule: ContextRule, smart_account: Address)
fn uninstall(e: &Env, context_rule: ContextRule, smart_account: Address)

pub fn spent_today(env: Env, smart_account: Address) -> i128
```

El guardián no va en estos parámetros. `uninstall` rechaza. Un segundo `install` de la misma cuenta rechaza.

## `HoldRegistry` (`contracts/hold_registry`)

```rust
pub const HOLD_DURATION_SECS: u64 = 120;

pub fn __constructor(env: Env, guardian: Address, guardian_hold: Address)

pub fn queue(env: Env, account: Address, token: Address, destination: Address, amount: i128) -> u64
pub fn cancel(env: Env, account: Address, id: u64)
pub fn mark_executed(env: Env, account: Address, id: u64)
pub fn get_hold(env: Env, account: Address, id: u64) -> HoldRecord
pub fn find_retained(env: Env, account: Address, token: Address, destination: Address, amount: i128) -> Option<u64>
pub fn list_retained(env: Env, account: Address) -> Vec<u64>
```

`cancel` lo autoriza el guardián. `mark_executed` lo autoriza `guardian_hold`. Ninguna función mueve tokens.

```rust
pub struct HoldRecord {
    pub token: Address,
    pub destination: Address,
    pub amount: i128,
    pub created_at: u64,
    pub ready_at: u64,
    pub status: HoldStatus, // Retained | Stopped | Executed
}
```

## Historia: spike (T-028)

Una sola `trusted_contact: Address` y sin `daily_cap`. El código de producción ya no usa esa forma.
