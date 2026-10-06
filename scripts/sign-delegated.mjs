// Fills the delegated-signer auth the Stellar CLI does not build, then submits.
// The owner seed stays in scripts/.testnet/. This file never prints it.
import { execFileSync as exec } from "node:child_process";
import { createHash, randomInt } from "node:crypto";
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const configDir = join(root, "scripts", ".testnet");
const state = JSON.parse(
  readFileSync(join(configDir, "state.json"), "utf8").replace(/^\uFEFF/, ""),
);
const networkId = createHash("sha256")
  .update("Test SDF Network ; September 2015")
  .digest("hex");

function stellar(args, input) {
  const out = exec("stellar", ["--config-dir", configDir, ...args], {
    input,
    encoding: "utf8",
    cwd: root,
    maxBuffer: 8 * 1024 * 1024,
  });
  const line = out
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter((s) => /^[A-Za-z0-9+/=]+$/.test(s) && s.length > 8)
    .at(-1);
  if (!line) throw new Error(`no xdr from stellar ${args.join(" ")}:\n${out}`);
  return line;
}

function xdrEncode(type, value) {
  return stellar(
    ["xdr", "encode", "--type", type, "--output", "single-base64"],
    JSON.stringify(value),
  );
}

function xdrDecode(type, b64) {
  const out = exec(
    "stellar",
    ["--config-dir", configDir, "xdr", "decode", "--type", type, "--output", "json"],
    { input: b64, encoding: "utf8", cwd: root },
  );
  return JSON.parse(out);
}

function latestLedger() {
  const out = exec(
    "stellar",
    ["--config-dir", configDir, "ledger", "latest", "--network", "testnet"],
    { encoding: "utf8", cwd: root },
  );
  const match = out.match(/"sequence"\s*:\s*(\d+)/) || out.match(/(\d{6,})/);
  if (!match) throw new Error(`no ledger sequence in:\n${out}`);
  return Number(match[1]);
}

function payloadHash(invocation, nonce, expiration) {
  const xdr = xdrEncode("HashIdPreimage", {
    soroban_authorization: {
      network_id: networkId,
      nonce: String(nonce),
      signature_expiration_ledger: expiration,
      invocation,
    },
  });
  return createHash("sha256").update(Buffer.from(xdr, "base64")).digest();
}

function authPayload() {
  const signer = process.env.GUARDPAY_SIGNER === "guardian" ? state.guardian : state.owner;
  return {
    map: [
      {
        key: { symbol: "context_rule_ids" },
        val: { vec: [{ u32: 0 }] },
      },
      {
        key: { symbol: "signers" },
        val: {
          map: [
            {
              key: {
                vec: [{ symbol: "Delegated" }, { address: signer }],
              },
              val: { bytes: "" },
            },
          ],
        },
      },
    ],
  };
}

function withDelegatedAuth(envelopeXdr) {
  const decoded = xdrDecode("TransactionEnvelope", envelopeXdr);
  const op = decoded.tx.tx.operations[0].body.invoke_host_function;
  const accountAuth = op.auth.find(
    (entry) => entry.credentials.address.address === state.account,
  );
  if (!accountAuth) throw new Error("simulation recorded no auth for the account");
  const expiration = latestLedger() + 1000;
  accountAuth.credentials.address.signature_expiration_ledger = expiration;
  const accountHash = payloadHash(
    accountAuth.root_invocation,
    accountAuth.credentials.address.nonce,
    expiration,
  );
  const ruleXdr = Buffer.from(xdrEncode("ScVal", { vec: [{ u32: 0 }] }), "base64");
  const digest = createHash("sha256").update(Buffer.concat([accountHash, ruleXdr])).digest("hex");
  accountAuth.credentials.address.signature = authPayload();
  op.auth.push({
    credentials: "source_account",
    root_invocation: {
      function: {
        contract_fn: {
          contract_address: state.account,
          function_name: "__check_auth",
          args: [{ bytes: digest }],
        },
      },
      sub_invocations: [],
    },
  });
  return xdrEncode("TransactionEnvelope", decoded);
}

function send(xdr) {
  let out = "";
  try {
    out = exec(
      "stellar",
      ["--config-dir", configDir, "tx", "send", "--network", "testnet"],
      { input: xdr, encoding: "utf8", cwd: root, maxBuffer: 8 * 1024 * 1024 },
    );
  } catch (err) {
    out = `${err.stdout || ""}\n${err.stderr || ""}`;
  }
  const hash =
    out.match(/"tx_hash":\s*"([a-f0-9]{64})"/)?.[1] ||
    out.match(/Transaction hash is ([a-f0-9]{64})/)?.[1];
  if (!hash) throw new Error(out);
  const failed = /TxFailed|Error\(Contract/.test(out);
  process.stdout.write(`${failed ? "FAILED" : "SUCCESS"} ${hash}\n`);
}

let simulated = process.argv[2];
if (simulated === "--invoke") {
  const invokeArgs = process.argv.slice(3);
  const skeleton = stellar([
    "contract",
    "invoke",
    "--network",
    "testnet",
    "--source",
    "owner",
    "--build-only",
    ...invokeArgs,
  ]);
  try {
    simulated = stellar(
      ["tx", "simulate", "--network", "testnet", "--source", "owner", "--auth-mode", "root"],
      skeleton,
    );
  } catch (err) {
    const text = `${err.stderr || ""}\n${err.message || ""}`;
    if (!text.includes("Error(Contract,")) throw err;
    const decoded = xdrDecode("TransactionEnvelope", skeleton);
    const call = decoded.tx.tx.operations[0].body.invoke_host_function.host_function.invoke_contract;
    decoded.tx.tx.operations[0].body.invoke_host_function.auth = [
      {
        credentials: {
          address: {
            address: state.account,
            nonce: String(randomInt(1, 2 ** 31)),
            signature_expiration_ledger: 0,
            signature: "void",
          },
        },
        root_invocation: {
          function: { contract_fn: call },
          sub_invocations: [],
        },
      },
    ];
    simulated = xdrEncode("TransactionEnvelope", decoded);
  }
}
if (!simulated) {
  console.error("usage: node scripts/sign-delegated.mjs --invoke --id <contract> -- <fn> [args]");
  process.exit(2);
}

function submitRejected(preparedXdr) {
  const decoded = xdrDecode("TransactionEnvelope", preparedXdr);
  const tx = decoded.tx.tx;
  const op = tx.operations[0].body.invoke_host_function;
  const nonceKey = op.auth
    .map((entry) => entry.credentials.address?.nonce)
    .find(Boolean);
  const readOnly = [
    instance(state.usdc),
    instance(state.guardian_hold),
    instance(state.registry),
    instance(state.account),
    data(state.account, { vec: [{ symbol: "ContextRuleData" }, { u32: 0 }] }),
    data(state.account, { vec: [{ symbol: "PolicyData" }, { u32: 0 }] }),
    data(state.account, { vec: [{ symbol: "SignerData" }, { u32: 0 }] }),
    data(state.registry, { vec: [{ symbol: "NextId" }, { address: state.account }] }),
    data(state.registry, {
      vec: [{ symbol: "Hold" }, { address: state.account }, { u64: "0" }],
    }),
    data(state.registry, {
      vec: [{ symbol: "Hold" }, { address: state.account }, { u64: "1" }],
    }),
    { contract_code: { hash: state.wasm.account } },
    { contract_code: { hash: state.wasm.guardian_hold } },
    { contract_code: { hash: state.wasm.hold_registry } },
  ];
  const readWrite = [
    data(state.guardian_hold, { vec: [{ symbol: "Config" }, { address: state.account }] }),
    {
      contract_data: {
        contract: state.account,
        key: { ledger_key_nonce: { nonce: String(nonceKey) } },
        durability: "temporary",
      },
    },
  ];
  tx.fee = 4001000;
  tx.ext = {
    v1: {
      ext: "v0",
      resources: {
        footprint: { read_only: readOnly, read_write: readWrite },
        instructions: 10000000,
        disk_read_bytes: 8000,
        write_bytes: 2000,
      },
      resource_fee: "4000000",
    },
  };
  const signedTx = stellar(
    ["tx", "sign", "--network", "testnet", "--sign-with-key", "owner"],
    xdrEncode("TransactionEnvelope", decoded),
  );
  send(signedTx);
}

function instance(contract) {
  return data(contract, "ledger_key_contract_instance");
}

function data(contract, key) {
  return {
    contract_data: { contract, key, durability: "persistent" },
  };
}

const prepared = withDelegatedAuth(simulated.trim());
writeFileSync(join(configDir, "prepared.xdr"), prepared);
let enforced;
try {
  enforced = stellar(
    [
      "tx",
      "simulate",
      "--network",
      "testnet",
      "--source",
      "owner",
      "--auth-mode",
      "enforce",
      "--instruction-leeway",
      "8000000",
    ],
    prepared,
  );
} catch (err) {
  const text = `${err.stderr || ""}\n${err.stdout || ""}\n${err.message || ""}`;
  if (!text.includes("Error(Contract,")) throw err;
  process.stderr.write(text.slice(0, 500) + "\n");
  submitRejected(prepared);
  process.exit(0);
}
const signedTx = stellar(
  ["tx", "sign", "--network", "testnet", "--sign-with-key", "owner"],
  enforced,
);
send(signedTx);
