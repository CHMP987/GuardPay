// Submits one account call with the delegated-signer auth the CLI does not build.
// The seed stays in scripts/.testnet/. This file never prints it.
import { execFileSync } from "node:child_process";
import { createHash, randomInt } from "node:crypto";
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const configDir = join(root, "scripts", ".testnet");
const networkId = createHash("sha256")
  .update("Test SDF Network ; September 2015")
  .digest("hex");

function stellar(args, input) {
  return execFileSync("stellar", ["--config-dir", configDir, ...args], {
    input,
    encoding: "utf8",
    cwd: root,
    maxBuffer: 8 * 1024 * 1024,
  });
}

function stellarTry(args, input) {
  try {
    const stdout = stellar(args, input);
    return { code: 0, stdout, stderr: "" };
  } catch (err) {
    return {
      code: err.status ?? 1,
      stdout: err.stdout?.toString() ?? "",
      stderr: err.stderr?.toString() ?? "",
    };
  }
}

function xdrLine(out) {
  const line = out
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter((s) => /^[A-Za-z0-9+/=]+$/.test(s) && s.length > 8)
    .at(-1);
  return line || null;
}

function xdrEncode(type, value) {
  const run = stellarTry(
    ["xdr", "encode", "--type", type, "--output", "single-base64"],
    JSON.stringify(value),
  );
  const line = xdrLine(run.stdout);
  if (!line) {
    throw new Error(`no xdr from encode ${type}:\n${run.stdout}\n${run.stderr}`);
  }
  return line;
}

function xdrDecode(type, b64) {
  const run = stellarTry(
    ["xdr", "decode", "--type", type, "--output", "json"],
    b64,
  );
  if (run.code !== 0) throw new Error(run.stderr || run.stdout);
  return JSON.parse(run.stdout);
}

function latestLedger() {
  const run = stellarTry(["ledger", "latest", "--network", "testnet"]);
  const out = `${run.stdout}\n${run.stderr}`;
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

export function loadState() {
  return JSON.parse(
    readFileSync(join(configDir, "state.json"), "utf8").replace(/^\uFEFF/, ""),
  );
}

function ruleVec(ruleIds) {
  return { vec: ruleIds.map((id) => ({ u32: id })) };
}

function countContractFns(invocation) {
  if (!invocation) return 0;
  const here = invocation.function?.contract_fn ? 1 : 0;
  const subs = invocation.sub_invocations || [];
  return here + subs.reduce((n, sub) => n + countContractFns(sub), 0);
}

function authPayload(state, signerAddress, ruleIds) {
  return {
    map: [
      {
        key: { symbol: "context_rule_ids" },
        val: ruleVec(ruleIds),
      },
      {
        key: { symbol: "signers" },
        val: {
          map: [
            {
              key: {
                vec: [{ symbol: "Delegated" }, { address: signerAddress }],
              },
              val: { bytes: "" },
            },
          ],
        },
      },
    ],
  };
}

function withDelegatedAuth(state, envelopeXdr, signerAddress, ruleId, accountAddress) {
  const decoded = xdrDecode("TransactionEnvelope", envelopeXdr);
  const op = decoded.tx.tx.operations[0].body.invoke_host_function;
  const accountAuth = op.auth.find(
    (entry) => entry.credentials.address?.address === accountAddress,
  );
  if (!accountAuth) throw new Error("simulation recorded no auth for the account");
  const expiration = latestLedger() + 1000;
  accountAuth.credentials.address.signature_expiration_ledger = expiration;
  const accountHash = payloadHash(
    accountAuth.root_invocation,
    accountAuth.credentials.address.nonce,
    expiration,
  );
  const contexts = Math.max(1, countContractFns(accountAuth.root_invocation));
  const ruleIds = Array.from({ length: contexts }, () => ruleId);
  const ruleXdr = Buffer.from(xdrEncode("ScVal", ruleVec(ruleIds)), "base64");
  const digest = createHash("sha256")
    .update(Buffer.concat([accountHash, ruleXdr]))
    .digest("hex");
  accountAuth.credentials.address.signature = authPayload(state, signerAddress, ruleIds);
  op.auth.push({
    credentials: "source_account",
    root_invocation: {
      function: {
        contract_fn: {
          contract_address: accountAddress,
          function_name: "__check_auth",
          args: [{ bytes: digest }],
        },
      },
      sub_invocations: [],
    },
  });
  return xdrEncode("TransactionEnvelope", decoded);
}

function instance(contract) {
  return data(contract, "ledger_key_contract_instance");
}

function data(contract, key) {
  return { contract_data: { contract, key, durability: "persistent" } };
}

function submitEnvelope(source, xdr) {
  const signed = stellarTry(
    ["tx", "sign", "--network", "testnet", "--sign-with-key", source],
    xdr,
  );
  const signedXdr = xdrLine(signed.stdout);
  if (!signedXdr) {
    return { ok: false, hash: null, text: `${signed.stdout}\n${signed.stderr}` };
  }
  const sent = stellarTry(
    ["tx", "send", "--network", "testnet"],
    signedXdr,
  );
  const text = `${sent.stdout}\n${sent.stderr}`;
  const status = text.match(/"status":\s*"(SUCCESS|FAILED)"/)?.[1] || null;
  const sorobanInvalid = /TxSorobanInvalid/.test(text);
  const trapped = /TxFailed|InvokeHostFunction\(\s*Trapped/.test(text);
  const hash =
    text.match(/"tx_hash":\s*"([a-f0-9]{64})"/)?.[1] ||
    text.match(/Transaction hash is ([a-f0-9]{64})/)?.[1] ||
    text.match(/tx\/([a-f0-9]{64})/)?.[1] ||
    null;
  const failed = status === "FAILED" || (trapped && status !== "SUCCESS");
  const success = status === "SUCCESS";
  const included = Boolean(hash) && !sorobanInvalid && (success || failed);
  return {
    ok: included && success && !failed,
    hash: included ? hash : null,
    text,
    failed,
    included,
  };
}

function submitRejected(state, source, preparedXdr) {
  const decoded = xdrDecode("TransactionEnvelope", preparedXdr);
  const tx = decoded.tx.tx;
  const op = tx.operations[0].body.invoke_host_function;
  const accountEntry = op.auth.find((entry) => entry.credentials.address?.nonce);
  const nonceKey = accountEntry?.credentials.address?.nonce;
  const nonceContract = accountEntry?.credentials.address?.address || state.account;
  const readOnly = [
    instance(state.usdc),
    instance(state.guardian_hold),
    instance(state.registry),
    instance(state.account),
    data(state.account, { vec: [{ symbol: "ContextRuleData" }, { u32: 0 }] }),
    data(state.account, { vec: [{ symbol: "PolicyData" }, { u32: 0 }] }),
    data(state.account, { vec: [{ symbol: "SignerData" }, { u32: 0 }] }),
    ...Array.from({ length: 16 }, (_, id) =>
      data(state.registry, {
        vec: [{ symbol: "Hold" }, { address: state.account }, { u64: String(id) }],
      }),
    ),
    { contract_code: { hash: state.wasm.account } },
    { contract_code: { hash: state.wasm.guardian_hold } },
    { contract_code: { hash: state.wasm.hold_registry } },
  ];
  if (state.nested) {
    readOnly.push(instance(state.nested));
    if (state.wasm.nested) readOnly.push({ contract_code: { hash: state.wasm.nested } });
  }
  const readWrite = [
    data(state.guardian_hold, { vec: [{ symbol: "Config" }, { address: state.account }] }),
    data(state.registry, { vec: [{ symbol: "NextId" }, { address: state.account }] }),
  ];
  if (nonceKey) {
    readWrite.push({
      contract_data: {
        contract: nonceContract,
        key: { ledger_key_nonce: { nonce: String(nonceKey) } },
        durability: "temporary",
      },
    });
  }
  tx.fee = 4001000;
  tx.ext = {
    v1: {
      ext: "v0",
      resources: {
        footprint: { read_only: readOnly, read_write: readWrite },
        instructions: 10000000,
        disk_read_bytes: 40000,
        write_bytes: 2000,
      },
      resource_fee: "4000000",
    },
  };
  return submitEnvelope(source, xdrEncode("TransactionEnvelope", decoded));
}

function classify(text, hash) {
  const marker = text.lastIndexOf("Transaction hash is");
  const source = marker >= 0 ? text.slice(marker) : text;
  const footprintMiss = /outside of the footprint/.test(source);
  const contractMatch =
    source.match(/Error\(Contract,\s*#(\d+)\)/) ||
    source.match(/"error"\s*:\s*\{\s*"contract"\s*:\s*(\d+)\s*\}/);
  const error =
    (footprintMiss && !contractMatch
      ? "storage exceeded_limit: key outside of the footprint"
      : null) ||
    contractMatch?.[0] ||
    source.match(/HostError: [^\n]+/)?.[0] ||
    source.match(/error: [^\n]+/i)?.[0] ||
    null;
  return { error, contractCode: contractMatch?.[1] || null, hash: hash || null };
}

/**
 * @param {{ source: string, signerAddress: string, contractId: string, fnAndArgs: string[], auth?: "delegated"|"none", ruleId?: number }} opts
 */
export function invokeAccount(opts) {
  const ruleId = opts.ruleId ?? 0;
  const accountAddress = opts.accountAddress || state.account;
  const state = loadState();
  const source = opts.source;
  const auth = opts.auth || "delegated";
  const built = stellarTry([
    "contract",
    "invoke",
    "--network",
    "testnet",
    "--source",
    source,
    "--build-only",
    "--id",
    opts.contractId,
    "--",
    ...opts.fnAndArgs,
  ]);
  const skeleton = xdrLine(built.stdout);
  if (!skeleton) {
    const text = `${built.stdout}\n${built.stderr}`;
    return {
      ok: false,
      hash: null,
      noTx: true,
      ...classify(text, null),
      text,
    };
  }

  const simulated = stellarTry(
    [
      "tx",
      "simulate",
      "--network",
      "testnet",
      "--source",
      source,
      "--auth-mode",
      "root",
    ],
    skeleton,
  );
  let envelope = xdrLine(simulated.stdout);
  const simText = `${simulated.stdout}\n${simulated.stderr}`;
  if (!envelope) {
    if (!/Error\(Contract,/.test(simText) && auth === "delegated") {
      return { ok: false, hash: null, noTx: true, ...classify(simText, null), text: simText };
    }
    const decoded = xdrDecode("TransactionEnvelope", skeleton);
    const call =
      decoded.tx.tx.operations[0].body.invoke_host_function.host_function.invoke_contract;
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
    envelope = xdrEncode("TransactionEnvelope", decoded);
  }

  if (auth === "none") {
    const sent = submitEnvelope(source, envelope);
    const info = classify(sent.text, sent.hash);
    return { ok: sent.ok, noTx: !sent.hash, ...info, text: sent.text };
  }

  let prepared;
  try {
    prepared = withDelegatedAuth(state, envelope, opts.signerAddress, ruleId, accountAddress);
  } catch (err) {
    const text = String(err.message || err);
    return { ok: false, hash: null, noTx: true, ...classify(text, null), text };
  }

  const enforced = stellarTry(
    [
      "tx",
      "simulate",
      "--network",
      "testnet",
      "--source",
      source,
      "--auth-mode",
      "enforce",
      "--instruction-leeway",
      "8000000",
    ],
    prepared,
  );
  const enforcedXdr = xdrLine(enforced.stdout);
  const enforcedText = `${enforced.stdout}\n${enforced.stderr}`;
  if (!enforcedXdr) {
    if (!/Error\(Contract,/.test(enforcedText)) {
      const forced = submitRejected(state, source, prepared);
      const info = classify(`${enforcedText}\n${forced.text}`, forced.hash);
      return { ok: false, noTx: !forced.hash, ...info, text: `${enforcedText}\n${forced.text}` };
    }
    const forced = submitRejected(state, source, prepared);
    const info = classify(`${enforcedText}\n${forced.text}`, forced.hash);
    return { ok: forced.ok, noTx: !forced.hash, ...info, text: `${enforcedText}\n${forced.text}` };
  }

  const sent = submitEnvelope(source, enforcedXdr);
  const info = classify(sent.text, sent.hash);
  return { ok: sent.ok && !info.contractCode, noTx: !sent.hash, ...info, text: sent.text };
}

export function viewCall(contractId, fnAndArgs) {
  const run = stellarTry([
    "contract",
    "invoke",
    "--network",
    "testnet",
    "--source",
    "owner",
    "--send",
    "no",
    "--id",
    contractId,
    "--",
    ...fnAndArgs,
  ]);
  return { code: run.code, text: `${run.stdout}\n${run.stderr}` };
}

export function missingFunction(functionName) {
  const state = loadState();
  const run = stellarTry([
    "contract",
    "invoke",
    "--network",
    "testnet",
    "--source",
    "owner",
    "--id",
    state.account,
    "--",
    functionName,
  ]);
  const text = `${run.stdout}\n${run.stderr}`;
  const unrecognized = /unrecognized subcommand/i.test(text);
  return {
    ok: false,
    noTx: unrecognized || !/[a-f0-9]{64}/.test(text),
    hash: unrecognized ? null : text.match(/[a-f0-9]{64}/)?.[1] || null,
    error: unrecognized ? `unrecognized subcommand '${functionName}'` : classify(text, null).error,
    contractCode: null,
    text,
    unrecognized,
  };
}

function printResult(name, result) {
  const hash = result.hash || (result.noTx ? "no hay tx" : "no hay tx");
  const lines = [
    `ATAQUE ${name}`,
    `RESULTADO ${result.ok ? "pasa" : "rechazo"}`,
    `HASH ${hash}`,
    `ERROR ${result.error || result.contractCode || (result.ok ? "ok" : "sin codigo")}`,
  ];
  if (result.contractCode) lines.push(`CODIGO ${result.contractCode}`);
  process.stdout.write(`${lines.join("\n")}\n`);
  const dir = join(configDir, "logs");
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, `${name}.log`), result.text || "", "utf8");
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const name = process.argv[2] || "invoke";
  const source = process.env.GUARDPAY_SOURCE || "owner";
  const state = loadState();
  const signerName = process.env.GUARDPAY_SIGNER || "owner";
  const signerAddress =
    signerName === "guardian"
      ? state.guardian
      : signerName === "attacker"
        ? state.attacker
        : state.owner;
  const dash = process.argv.indexOf("--");
  const fnAndArgs = dash === -1 ? process.argv.slice(3) : process.argv.slice(dash + 1);
  const idFlag = process.argv.indexOf("--id");
  const contractId = idFlag === -1 ? state.account : process.argv[idFlag + 1];
  const auth = process.env.GUARDPAY_AUTH === "none" ? "none" : "delegated";
  const ruleId = Number(process.env.GUARDPAY_RULE_ID || "0");
  const result = invokeAccount({
    source,
    signerAddress,
    contractId,
    fnAndArgs,
    auth,
    ruleId,
  });
  printResult(name, result);
  process.exit(result.hash || result.noTx ? 0 : 1);
}
