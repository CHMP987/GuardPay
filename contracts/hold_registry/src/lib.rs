#![no_std]

use soroban_sdk::{contract, contractimpl, Env};

#[contract]
pub struct HoldRegistry;

#[contractimpl]
impl HoldRegistry {
    pub fn __constructor(_env: Env) {}
}
