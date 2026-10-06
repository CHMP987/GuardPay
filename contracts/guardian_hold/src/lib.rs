#![no_std]

use soroban_sdk::{contract, contractimpl, Env};

#[contract]
pub struct GuardianHold;

#[contractimpl]
impl GuardianHold {
    pub fn __constructor(_env: Env) {}
}
