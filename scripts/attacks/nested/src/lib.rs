//! Attack contract. It is not part of GuardPay.
//! `pay` asks the account to authorize this call, then calls the token
//! with that account as `from`. Lives only under scripts/attacks/nested/.

#![no_std]

use soroban_sdk::{contract, contractimpl, token, Address, Env};

#[contract]
pub struct NestedAttack;

#[contractimpl]
impl NestedAttack {
    pub fn pay(env: Env, token_id: Address, from: Address, to: Address, amount: i128) {
        from.require_auth();
        token::Client::new(&env, &token_id).transfer(&from, &to, &amount);
    }
}
