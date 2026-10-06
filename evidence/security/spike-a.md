# Spike A — evidencia

Pin OpenZeppelin `stellar-contracts` @ `b40c5eaefe6a29f0030f00bd2d730b7a91cce330` (clone en `.scratch/oz-g2/`, gitignorado). soroban-sdk 28.0.0; paquete `stellar-accounts` 0.7.1.

| ID | Veredicto | Comando y salida |
| --- | --- | --- |
| A0 | `[FACT]` pasa (exit 0) | Ver abajo |

## A0 — digest liga `context_rule_ids`

Directorio: `.scratch/oz-g2`

```
cargo test -p stellar-accounts do_check_auth_rule_selection_downgrade_fails -- --nocapture
```

Salida literal (últimas líneas relevantes; compilación omitida):

```
     Running unittests src\lib.rs (C:\Users\solan\AppData\Local\Temp\cursor-sandbox-cache\11fdb5d57c11404054657b9014365e6a\cargo-target\debug\deps\stellar_accounts-e388a9161e7148a8.exe)

running 1 test

thread 'smart_account::test::context_rules::do_check_auth_rule_selection_downgrade_fails' (40900) panicked at C:\Users\solan\.cargo\registry\src\index.crates.io-1949cf8c6b5b557f\soroban-env-host-28.0.2\src\host.rs:892:9:
HostError: Error(Contract, #3003)

Event log (newest first):
   0: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[error, Error(Contract, #3003)], data:"escalating error to panic"
   1: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[error, Error(Contract, #3003)], data:["failing with contract error", 3003]
   2: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, topics:[fn_return, verify], data:false
   3: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, verify], data:[Bytes(5175902a77cfb5cf33e321902d62b88fbb66dfcfe6fcd0e8c0e861b732a55db3), Bytes(01020304), Bytes(05060708)]
   4: [Contract Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[context_rule_added, 1], data:{context_type: [CallContract, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHK3M], name: "weak_rule", policy_ids: [], signer_ids: [0]}
   5: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, topics:[fn_return, batch_canonicalize_key], data:[Bytes(01020304)]
   6: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, batch_canonicalize_key], data:[Bytes(01020304)]
   7: [Contract Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[context_rule_added, 0], data:{context_type: [CallContract, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHK3M], name: "strict_rule", policy_ids: [0, 1], signer_ids: [0]}
   8: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM, topics:[fn_return, install], data:Void
   9: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM, install], data:[Void, {context_type: [CallContract, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHK3M], id: 0, name: "strict_rule", policies: [CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM], policy_ids: [0, 1], signer_ids: [0], signers: [[External, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, Bytes(01020304)]], valid_until: Void}, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM]
   10: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4, topics:[fn_return, install], data:Void
   11: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4, install], data:[Void, {context_type: [CallContract, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHK3M], id: 0, name: "strict_rule", policies: [CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM], policy_ids: [0, 1], signer_ids: [0], signers: [[External, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, Bytes(01020304)]], valid_until: Void}, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM]
   12: [Contract Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[policy_registered, 1], data:{policy: CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM}
   13: [Contract Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[policy_registered, 0], data:{policy: CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4}
   14: [Contract Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[signer_registered, 0], data:{signer: [External, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, Bytes(01020304)]}
   15: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, topics:[fn_return, batch_canonicalize_key], data:[Bytes(01020304)]
   16: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFCT4, batch_canonicalize_key], data:[Bytes(01020304)]
   17: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM, topics:[fn_return, __constructor], data:Void
   18: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK3IM, __constructor], data:Void
   19: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4, topics:[fn_return, __constructor], data:Void
   20: [Diagnostic Event] contract:CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD2KM, topics:[fn_call, CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITA4, __constructor], data:Void

note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
Writing test snapshot file for test "smart_account::test::context_rules::do_check_auth_rule_selection_downgrade_fails" to "test_snapshots\\smart_account\\test\\context_rules\\do_check_auth_rule_selection_downgrade_fails.1.json".
test smart_account::test::context_rules::do_check_auth_rule_selection_downgrade_fails - should panic ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 186 filtered out; finished in 0.16s
```

A1, GH-01…GH-33: `no corrido`.
