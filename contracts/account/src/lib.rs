#![no_std]

use soroban_sdk::{contract, contractimpl, Env};
use stellar_accounts as _;

#[contract]
pub struct Account;

#[contractimpl]
impl Account {
    pub fn __constructor(_env: Env) {}
}
