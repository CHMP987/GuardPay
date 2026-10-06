#![allow(unused)]
use soroban_sdk::{
    auth::{Context, CustomAccountInterface},
    contract, contracterror, contractimpl, contracttype,
    crypto::Hash,
    Address, Env, Vec,
};

#[contracttype]
enum RegistryDataKey {
    Counter,
}

#[contract]
pub struct SpikeRegistry;

#[contractimpl]
impl SpikeRegistry {
    pub fn touch(env: Env) {
        let count: u32 = env
            .storage()
            .persistent()
            .get(&RegistryDataKey::Counter)
            .unwrap_or(0);
        env.storage()
            .persistent()
            .set(&RegistryDataKey::Counter, &(count + 1));
    }

    pub fn get_counter(env: Env) -> u32 {
        env.storage()
            .persistent()
            .get(&RegistryDataKey::Counter)
            .unwrap_or(0)
    }
}

#[contracttype]
enum PolicyDataKey {
    Registry,
}

#[contract]
pub struct SpikePolicy;

#[contractimpl]
impl SpikePolicy {
    pub fn __constructor(env: Env, registry: Address) {
        env.storage()
            .instance()
            .set(&PolicyDataKey::Registry, &registry);
    }

    /// Minimal `enforce`: read and write persistent storage on the registry contract.
    pub fn enforce(env: Env) {
        let registry: Address = env
            .storage()
            .instance()
            .get(&PolicyDataKey::Registry)
            .expect("registry");
        SpikeRegistryClient::new(&env, &registry).touch();
    }
}

#[contracttype]
enum AccountDataKey {
    Policy,
}

#[contracterror]
#[derive(Copy, Clone, Debug, Eq, PartialEq, PartialOrd, Ord)]
#[repr(u32)]
pub enum SpikeAccountError {
    Unauthorized = 1,
}

#[contract]
pub struct SpikeAccount;

#[contractimpl]
impl SpikeAccount {
    pub fn __constructor(env: Env, policy: Address) {
        env.storage().instance().set(&AccountDataKey::Policy, &policy);
    }
}

#[contractimpl]
impl CustomAccountInterface for SpikeAccount {
    type Signature = ();
    type Error = SpikeAccountError;

    fn __check_auth(
        env: Env,
        _signature_payload: Hash<32>,
        _signature: Self::Signature,
        _auth_contexts: Vec<Context>,
    ) -> Result<(), Self::Error> {
        let policy: Address = env
            .storage()
            .instance()
            .get(&AccountDataKey::Policy)
            .expect("policy");
        SpikePolicyClient::new(&env, &policy).enforce();
        Ok(())
    }
}

#[contract]
pub struct SpikeCaller;

#[contractimpl]
impl SpikeCaller {
    pub fn invoke(env: Env, account: Address) {
        account.require_auth();
    }
}

#[cfg(test)]
mod spike_a1 {
    extern crate std;

    use super::*;
    use soroban_sdk::{
        testutils::EnvTestConfig,
        xdr::{
            InvokeContractArgs, ScAddress, ScVal, SorobanAddressCredentials,
            SorobanAuthorizationEntry, SorobanAuthorizedFunction, SorobanAuthorizedInvocation,
            SorobanCredentials, StringM, VecM,
        },
    };

    fn build_account_auth(
        e: &Env,
        account: &Address,
        caller: &Address,
    ) -> SorobanAuthorizationEntry {
        let account_addr: ScAddress = account.clone().try_into().unwrap();
        SorobanAuthorizationEntry {
            credentials: SorobanCredentials::Address(SorobanAddressCredentials {
                address: account_addr.clone(),
                nonce: 1,
                signature_expiration_ledger: e.ledger().sequence() + 10_000,
                signature: ScVal::Void,
            }),
            root_invocation: SorobanAuthorizedInvocation {
                function: SorobanAuthorizedFunction::ContractFn(InvokeContractArgs {
                    contract_address: caller.clone().try_into().unwrap(),
                    function_name: StringM::try_from("invoke").unwrap().into(),
                    args: std::vec![ScVal::Address(account_addr)].try_into().unwrap(),
                }),
                sub_invocations: VecM::default(),
            },
        }
    }

    fn classify_panic(msg: &str) -> &'static str {
        let lower = msg.to_ascii_lowercase();
        if lower.contains("reentry")
            || lower.contains("re-enter")
            || lower.contains("reenter")
            || lower.contains("call stack")
            || lower.contains("cross")
        {
            "rechazado por reentrada"
        } else if lower.contains("budget") || lower.contains("exceededlimit") {
            "excede recursos"
        } else {
            "BLOQUEO"
        }
    }

    #[test]
    fn spike_a1_r6_policy_registry_in_check_auth() {
        let e = Env::new_with_config(EnvTestConfig {
            capture_snapshot_at_drop: false,
        });

        let registry = e.register(SpikeRegistry, ());
        let policy = e.register(SpikePolicy, (registry.clone(),));
        let account = e.register(SpikeAccount, (policy.clone(),));
        let caller = e.register(SpikeCaller, ());

        assert_eq!(
            SpikeRegistryClient::new(&e, &registry).get_counter(),
            0,
            "counter before"
        );

        e.set_auths(&[build_account_auth(&e, &account, &caller)]);

        let caller_client = SpikeCallerClient::new(&e, &caller);
        let invoke_outcome = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            caller_client.invoke(&account);
        }));

        let budget = e.cost_estimate().budget();
        let cpu_instructions = budget.cpu_instruction_cost();
        let mem_bytes = budget.memory_bytes_cost();
        let resources = e.cost_estimate().resources();

        println!("A1 CPU instructions (budget): {}", cpu_instructions);
        println!("A1 memory bytes (budget): {}", mem_bytes);
        println!(
            "A1 I/O resources: instructions={} mem_bytes={} disk_read_entries={} memory_read_entries={} write_entries={} disk_read_bytes={} write_bytes={}",
            resources.instructions,
            resources.mem_bytes,
            resources.disk_read_entries,
            resources.memory_read_entries,
            resources.write_entries,
            resources.disk_read_bytes,
            resources.write_bytes
        );

        let verdict = match invoke_outcome {
            Ok(()) => {
                let counter = SpikeRegistryClient::new(&e, &registry).get_counter();
                assert_eq!(counter, 1, "registry write inside __check_auth");
                "pasa"
            }
            Err(payload) => {
                let msg = if let Some(s) = payload.downcast_ref::<&str>() {
                    (*s).to_string()
                } else if let Some(s) = payload.downcast_ref::<String>() {
                    s.clone()
                } else {
                    format!("{:?}", payload)
                };
                println!("A1 invoke panic: {}", msg);
                classify_panic(&msg)
            }
        };

        println!("A1 veredicto R6: {}", verdict);

        if verdict != "pasa" {
            panic!("R6 spike A1 no pasó: {}", verdict);
        }
    }
}
