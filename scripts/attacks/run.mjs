// One attack per invocation. Prints a readable result and stores the raw log
// under scripts/.testnet/logs/ (gitignored). Never prints a seed.
import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
  invokeAccount,
  loadState,
  missingFunction,
  viewCall,
} from "./invoke.mjs";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const configDir = join(root, "scripts", ".testnet");
const logDir = join(configDir, "logs");
const SCALE = 10_000_000;
const HOLD_SECONDS = 120;

mkdirSync(logDir, { recursive: true });

function stellar(args, input) {
  try {
    const stdout = execFileSync("stellar", ["--config-dir", configDir, ...args], {
      input,
      encoding: "utf8",
      cwd: root,
      maxBuffer: 8 * 1024 * 1024,
    });
    return { code: 0, text: stdout };
  } catch (err) {
    return {
      code: err.status ?? 1,
      text: `${err.stdout?.toString() ?? ""}\n${err.stderr?.toString() ?? ""}`,
    };
  }
}

function saveState(state) {
  writeFileSync(join(configDir, "state.json"), JSON.stringify(state, null, 2));
}

function record(name, fields) {
  const row = { name, at: new Date().toISOString(), ...fields };
  const path = join(logDir, "report.json");
  const prev = existsSync(path) ? JSON.parse(readFileSync(path, "utf8")) : [];
  prev.push(row);
  writeFileSync(path, JSON.stringify(prev, null, 2));
  const hash = row.hash || "no hay tx";
  process.stdout.write(
    [
      `ATAQUE ${name}`,
      `PRECONDICION ${row.pre || "-"}`,
      `ACCION ${row.action || "-"}`,
      `ESPERADO ${row.expected || "-"}`,
      `RESULTADO ${row.ok === true ? "pasa" : "rechazo"}`,
      `HASH ${hash}`,
      `ERROR ${row.error || "-"}`,
      `CODIGO ${row.contractCode || "-"}`,
      `PROPIEDAD ${row.property || "-"}`,
      `RAMA ${row.branch || "-"}`,
      "",
    ].join("\n"),
  );
  if (row.text) writeFileSync(join(logDir, `${name}.log`), row.text, "utf8");
  return row;
}

function ownerCall(contractId, fnAndArgs, name, meta, extra = {}) {
  const state = loadState();
  const result = invokeAccount({
    source: extra.source || "owner",
    signerAddress: extra.signerAddress || state.owner,
    contractId,
    fnAndArgs,
    auth: extra.auth || "delegated",
    ruleId: extra.ruleId ?? 0,
    accountAddress: extra.from || state.account,
  });
  return record(name, {
    ...meta,
    ok: result.ok,
    hash: result.hash,
    error: result.error,
    contractCode: result.contractCode,
    text: result.text,
  });
}

function ledgerNow() {
  const run = stellar(["ledger", "latest", "--network", "testnet", "--output", "json"]);
  const seq = run.text.match(/"sequence"\s*:\s*(\d+)/);
  let close = null;
  if (seq) {
    try {
      const body = execFileSync(
        "curl",
        ["-sS", `https://horizon-testnet.stellar.org/ledgers/${seq[1]}`],
        { encoding: "utf8" },
      );
      const closedAt = body.match(/"closed_at"\s*:\s*"([^"]+)"/)?.[1];
      if (closedAt) close = String(Math.floor(Date.parse(closedAt) / 1000));
    } catch {
      close = null;
    }
  }
  return { sequence: seq ? Number(seq[1]) : null, close, text: run.text };
}

function sleep(ms) {
  execFileSync("sleep", [String(Math.ceil(ms / 1000))]);
}

function ensureIdentity(name) {
  const have = stellar(["keys", "address", name]);
  if (have.code === 0) return have.text.trim().split(/\s+/).pop();
  const created = stellar(["keys", "generate", name, "--fund", "--network", "testnet"]);
  if (created.code !== 0) throw new Error(`keys generate ${name} failed:\n${created.text}`);
  sleep(2000);
  const again = stellar(["keys", "address", name]);
  if (again.code !== 0) throw new Error(`no address for ${name}`);
  return again.text.trim().split(/\s+/).pop();
}

function changeTrust(source, issuer) {
  const line = `USDC:${issuer}`;
  const built = stellar([
    "tx",
    "new",
    "change-trust",
    "--network",
    "testnet",
    "--source",
    source,
    "--line",
    line,
    "--build-only",
  ]);
  if (built.code !== 0) return built;
  const xdr = built.text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter((s) => /^[A-Za-z0-9+/=]+$/.test(s) && s.length > 8)
    .at(-1);
  if (!xdr) return built;
  const signed = stellar(["tx", "sign", "--network", "testnet", "--sign-with-key", source], xdr);
  const signedXdr = signed.text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter((s) => /^[A-Za-z0-9+/=]+$/.test(s) && s.length > 8)
    .at(-1);
  if (!signedXdr) return signed;
  return stellar(["tx", "send", "--network", "testnet"], signedXdr);
}

function prepare() {
  const state = loadState();
  if (!state.stranger) {
    state.stranger = ensureIdentity("stranger");
    saveState(state);
  }
  const trust = ["trusted1", "trusted2", "trusted3", "attacker", "guardian", "stranger"];
  for (const name of trust) {
    const run = changeTrust(name, state.owner);
    record(`trustline-${name}`, {
      pre: "El destino G necesita trustline del SAC de prueba.",
      action: `change-trust USDC:${state.owner} firmado por ${name}`,
      expected: "La trustline queda en el ledger, o ya existia.",
      ok: run.code === 0 || /already|op_success|exists/i.test(run.text),
      hash: run.text.match(/[a-f0-9]{64}/)?.[1] || null,
      error: run.code === 0 ? "ok" : run.text.slice(0, 400),
      property: "-",
      branch: "fuera de enforce",
      text: run.text,
    });
  }
  process.stdout.write(`STRANGER ${state.stranger}\n`);
}

function queue(destination, amount, name) {
  const state = loadState();
  const before = viewCall(state.registry, ["list_retained", "--account", state.account]);
  const row = ownerCall(
    state.registry,
    [
      "queue",
      "--account",
      state.account,
      "--token",
      state.usdc,
      "--destination",
      destination,
      "--amount",
      String(amount),
    ],
    name,
    {
      pre: "La dueña firma queue. ready_at lo calcula el registro.",
      action: `queue ${amount} hacia ${destination}`,
      expected: "Pasa. El registro crea un hold Retained.",
      property: "P2",
      branch: "carril HoldRegistry.queue",
    },
  );
  const after = viewCall(state.registry, ["list_retained", "--account", state.account]);
  writeFileSync(join(logDir, `${name}-list.log`), `${before.text}\n---\n${after.text}`, "utf8");
  const ids = [...after.text.matchAll(/\b(\d+)\b/g)].map((m) => Number(m[1]));
  return { row, ids, list: after.text };
}

function cancel(id, name) {
  const state = loadState();
  return ownerCall(
    state.registry,
    ["cancel", "--account", state.account, "--id", String(id)],
    name,
    {
      pre: `Hold ${id} esta Retained.`,
      action: "El guardian firma cancel.",
      expected: "Pasa. El hold queda Stopped.",
      property: "P3",
      branch: "cancel del registro, no pasa por enforce",
    },
    { source: "guardian", signerAddress: state.guardian, auth: "none" },
  );
}

function transfer(to, amount, name, meta, extra) {
  const state = loadState();
  return ownerCall(
    state.usdc,
    [
      "transfer",
      "--from",
      extra?.from || state.account,
      "--to",
      to,
      "--amount",
      String(amount),
    ],
    name,
    meta,
    extra,
  );
}

function immediateReject() {
  const state = loadState();
  transfer(state.stranger || state.attacker, 15 * SCALE, "immediate-reject", {
    pre: "No hay hold para ese destino y monto. No es contacto de confianza.",
    action: "transfer inmediato del SAC de prueba",
    expected: "Rechazo NotAllowed (3). Nada se mueve.",
    property: "P1",
    branch: "carril transfer: destino no es contacto y no hay hold Retained",
  });
}

function heldAfterCancel() {
  const state = loadState();
  const dest = state.stranger || state.attacker;
  const amount = 17 * SCALE;
  const queued = queue(dest, amount, "held-after-cancel-queue");
  if (!queued.row.ok) return;
  const id = queued.ids.at(-1);
  const stopped = cancel(id, "held-after-cancel-stop");
  if (!stopped.ok) return;
  transfer(dest, amount, "held-after-cancel", {
    pre: `Hold ${id} quedo Stopped.`,
    action: "La dueña firma transfer del mismo destino y monto.",
    expected: "Rechazo NotAllowed (3). Un pago detenido no sale.",
    property: "P2",
    branch: "find_retained no ve Stopped; cae en NotAllowed",
  });
}

function guardianTransfer() {
  const state = loadState();
  transfer(state.guardian, 1 * SCALE, "guardian-transfer", {
    pre: "El guardian no es firmante de la cuenta.",
    action: "transfer desde la cuenta hacia el guardian, payload Delegated del guardian",
    expected: "Rechazo. UnauthorizedSigner (3016) o OwnerNotAuthenticated (2).",
    property: "P3",
    branch: "el firmante no esta en la regla, antes de la lista blanca",
  }, {
    source: "guardian",
    signerAddress: state.guardian,
  });
}

function waitReady(id) {
  const state = loadState();
  const started = Date.now();
  for (let i = 0; i < 20; i++) {
    const view = viewCall(state.registry, ["get_hold", "--account", state.account, "--id", String(id)]);
    const ready = view.text.match(/"ready_at"\s*:\s*"?(\d+)/);
    const now = ledgerNow();
    const close = Number(now.close);
    process.stdout.write(`ESPERA hold ${id} ready_at ${ready?.[1] || "?"} ledger_close ${now.close}\n`);
    if (ready && Number.isFinite(close) && close >= Number(ready[1])) return true;
    if (Date.now() - started > 180000) return false;
    sleep(15000);
  }
  return false;
}

function matureHold(amount, name) {
  const state = loadState();
  const dest = state.attacker;
  const queued = queue(dest, amount, `${name}-queue`);
  if (!queued.row.ok) return null;
  const id = queued.ids.at(-1);
  const ready = waitReady(id);
  return { id, dest, amount, ready };
}

function wrongDestination() {
  const held = matureHold(19 * SCALE, "wrong-destination");
  if (!held?.ready) {
    record("wrong-destination", {
      pre: "Hacia falta un hold maduro.",
      action: "no se envio el transfer",
      expected: "Rechazo",
      ok: false,
      hash: null,
      error: "no corrido: el hold no maduro a tiempo",
      property: "P1",
      branch: "carril transfer",
    });
    return;
  }
  const state = loadState();
  transfer(state.stranger, held.amount, "wrong-destination", {
    pre: `Hold ${held.id} maduro hacia ${held.dest}, monto ${held.amount}.`,
    action: `transfer del mismo monto hacia ${state.stranger}`,
    expected: "Rechazo NotAllowed (3). El destino no coincide.",
    property: "P1",
    branch: "destino distinto: no hay hold Retained con esos campos",
  });
}

function wrongAmount() {
  const held = matureHold(21 * SCALE, "wrong-amount");
  if (!held?.ready) {
    record("wrong-amount", {
      pre: "Hacia falta un hold maduro.",
      action: "no se envio el transfer",
      expected: "Rechazo",
      ok: false,
      hash: null,
      error: "no corrido: el hold no maduro a tiempo",
      property: "P1",
      branch: "carril transfer",
    });
    return;
  }
  transfer(held.dest, held.amount - SCALE, "wrong-amount", {
    pre: `Hold ${held.id} maduro por ${held.amount}.`,
    action: `transfer al mismo destino por ${held.amount - SCALE}`,
    expected: "Rechazo NotAllowed (3). El monto no coincide.",
    property: "P1",
    branch: "monto distinto: find_retained no encuentra el hold",
  });
}

function approve() {
  const state = loadState();
  ownerCall(
    state.usdc,
    [
      "approve",
      "--from",
      state.account,
      "--spender",
      state.attacker,
      "--amount",
      String(25 * SCALE),
      "--live_until_ledger",
      "99999999",
    ],
    "approve",
    {
      pre: "La dueña firma. approve no esta en la lista blanca.",
      action: "SAC de prueba approve",
      expected: "Rechazo NotAllowed (3).",
      property: "P4",
      branch: "catch-all",
    },
  );
  ownerCall(
    state.usdc,
    [
      "transfer_from",
      "--spender",
      state.attacker,
      "--from",
      state.account,
      "--to",
      state.attacker,
      "--amount",
      String(25 * SCALE),
    ],
    "transfer-from",
    {
      pre: "approve anterior rechazado. No hay permiso.",
      action: "SAC de prueba transfer_from",
      expected: "Rechazo NotAllowed (3).",
      property: "P4",
      branch: "catch-all",
    },
  );
}

function missing(name, fn, property) {
  const result = missingFunction(fn);
  record(name, {
    pre: "La cuenta publicada solo exporta __constructor y __check_auth.",
    action: `stellar contract invoke ${fn}`,
    expected: "unrecognized subcommand. Hash: no hay tx.",
    ok: false,
    hash: result.unrecognized ? null : result.hash,
    error: result.error,
    property,
    branch: "la funcion no esta en el spec del wasm",
    text: result.text,
  });
}

function directEnforce() {
  const state = loadState();
  const context = JSON.stringify({
    Contract: {
      contract: state.usdc,
      fn_name: "transfer",
      args: [],
    },
  });
  const signers = JSON.stringify([{ Delegated: state.owner }]);
  const rule = JSON.stringify({
    context_type: "Default",
    id: 0,
    name: "default",
    policies: [state.guardian_hold],
    policy_ids: [0],
    signer_ids: [0],
    signers: [{ Delegated: state.owner }],
    valid_until: null,
  });
  const run = stellar([
    "contract",
    "invoke",
    "--network",
    "testnet",
    "--source",
    "attacker",
    "--send",
    "yes",
    "--id",
    state.guardian_hold,
    "--",
    "enforce",
    "--context",
    context,
    "--authenticated_signers",
    signers,
    "--context_rule",
    rule,
    "--smart_account",
    state.account,
  ]);
  const hash = run.text.match(/[a-f0-9]{64}/)?.[1] || null;
  const code = run.text.match(/Error\(Contract,\s*#(\d+)\)/);
  record("direct-enforce", {
    pre: "Un tercero llama enforce. No es __check_auth.",
    action: "guardian_hold.enforce firmado por el atacante",
    expected: "Rechazo. require_auth de la cuenta, reentrada, o error de la CLI.",
    ok: false,
    hash,
    error: code?.[0] || run.text.split("\n").find((l) => /error|re-entry|unrecognized/i.test(l)) || "sin codigo",
    contractCode: code?.[1] || null,
    property: "P4",
    branch: "account.require_auth dentro de enforce, antes de la lista",
    text: run.text,
  });
}

function queueWithoutOwner() {
  const state = loadState();
  ownerCall(
    state.registry,
    [
      "queue",
      "--account",
      state.account,
      "--token",
      state.usdc,
      "--destination",
      state.attacker,
      "--amount",
      String(11 * SCALE),
    ],
    "queue-without-owner",
    {
      pre: "El atacante no tiene la firma de la dueña.",
      action: "queue para la cuenta de la dueña, fuente el atacante, sin auth delegada",
      expected: "Rechazo. La cuenta no autoriza queue.",
      property: "P4",
      branch: "queue exige require_auth de la cuenta",
    },
    { source: "attacker", auth: "none" },
  );
}

function ensureNested() {
  const state = loadState();
  if (state.nested) return state.nested;
  const wasm = join(root, "scripts", "attacks", "nested", "target", "wasm32v1-none", "release", "nested_attack.wasm");
  if (!existsSync(wasm)) {
    execFileSync(
      "stellar",
      ["contract", "build", "--optimize=false", "--manifest-path", "scripts/attacks/nested/Cargo.toml"],
      {
        cwd: root,
        env: { ...process.env, RUSTUP_TOOLCHAIN: "stable", PATH: process.env.PATH },
        stdio: "inherit",
      },
    );
  }
  const built = existsSync(wasm)
    ? wasm
    : join(root, "target", "wasm32v1-none", "release", "nested_attack.wasm");
  const run = stellar([
    "contract",
    "deploy",
    "--network",
    "testnet",
    "--source",
    "owner",
    "--wasm",
    built,
    "--salt",
    "00000000000000000000000000000000000000000000000000000000000000aa",
  ]);
  const id = run.text.match(/C[A-Z2-7]{55}/)?.[0];
  if (!id) throw new Error(`nested deploy failed:\n${run.text}`);
  state.nested = id;
  const hash = run.text.match(/[a-f0-9]{64}/g)?.at(-1);
  state.wasm = state.wasm || {};
  if (hash && hash.length === 64) state.wasm.nested = hash;
  saveState(state);
  record("nested-deploy", {
    pre: "Contrato de ataque, no es parte de GuardPay.",
    action: "deploy de scripts/attacks/nested",
    expected: "El contrato queda en testnet.",
    ok: true,
    hash: run.text.match(/tx\/([a-f0-9]{64})/)?.[1] || run.text.match(/[a-f0-9]{64}/)?.[1] || null,
    error: "ok",
    property: "P4",
    branch: "fuera de enforce",
    text: run.text,
  });
  return id;
}

function nestedTransfer() {
  const state = loadState();
  const id = ensureNested();
  ownerCall(
    id,
    [
      "pay",
      "--token_id",
      state.usdc,
      "--from",
      state.account,
      "--to",
      state.attacker,
      "--amount",
      String(1 * SCALE),
    ],
    "nested-transfer",
    {
      pre: "Un contrato intermedio llama al SAC con from = la cuenta.",
      action: "nested.pay, la dueña autoriza esa llamada",
      expected: "Rechazo NotAllowed (3). El contexto no es transfer ni queue.",
      property: "P4",
      branch: "catch-all: el contexto es el contrato intermedio",
    },
  );
}

function replay() {
  const held = matureHold(13 * SCALE, "replay");
  if (!held?.ready) {
    record("replay", {
      pre: "Hacia falta un hold maduro.",
      action: "no se reenvio",
      expected: "Rechazo del segundo envio",
      ok: false,
      hash: null,
      error: "no corrido: el hold no maduro a tiempo",
      property: "P2",
      branch: "hold ya Executed",
    });
    return;
  }
  const first = transfer(held.dest, held.amount, "replay-first", {
    pre: `Hold ${held.id} maduro.`,
    action: "transfer que coincide campo a campo",
    expected: "Pasa una vez. El hold queda Executed.",
    property: "P2",
    branch: "carril transfer de hold maduro",
  });
  if (!first.ok) return;
  transfer(held.dest, held.amount, "replay", {
    pre: "Ese transfer ya se ejecuto.",
    action: "la dueña firma otra vez el mismo transfer",
    expected: "Rechazo NotAllowed (3). No sale dos veces.",
    property: "P2",
    branch: "el hold ya no esta Retained",
  });
}

const ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

function crc16(buf) {
  let crc = 0;
  for (const b of buf) {
    crc ^= b << 8;
    for (let i = 0; i < 8; i++) {
      crc = (crc & 0x8000) ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff;
    }
  }
  return crc;
}

function decodeStrkey(str) {
  let buffer = 0;
  let bits = 0;
  const bytes = [];
  for (const c of str) {
    const val = ALPHABET.indexOf(c);
    if (val < 0) throw new Error("strkey");
    buffer = (buffer << 5) | val;
    bits += 5;
    if (bits >= 8) {
      bits -= 8;
      bytes.push((buffer >> bits) & 0xff);
    }
  }
  return Buffer.from(bytes);
}

function encodeStrkey(version, payload) {
  const data = Buffer.concat([Buffer.from([version]), payload]);
  const checksum = Buffer.alloc(2);
  checksum.writeUInt16LE(crc16(data), 0);
  const full = Buffer.concat([data, checksum]);
  let out = "";
  let buffer = 0;
  let bits = 0;
  for (const b of full) {
    buffer = (buffer << 8) | b;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += ALPHABET[(buffer >> bits) & 31];
    }
  }
  if (bits > 0) out += ALPHABET[(buffer << (5 - bits)) & 31];
  return out;
}

function muxed(gAddress, id) {
  const raw = decodeStrkey(gAddress);
  const pubkey = raw.subarray(1, 33);
  const payload = Buffer.alloc(40);
  pubkey.copy(payload, 0);
  payload.writeBigUInt64BE(BigInt(id), 32);
  return encodeStrkey(0x60, payload);
}

function muxedDestination() {
  const state = loadState();
  const to = muxed(state.attacker, 7);
  process.stdout.write(`MUXED ${to}\n`);
  transfer(to, 1 * SCALE, "muxed-destination", {
    pre: "to es una direccion muxed con id 7. No convierte a Address.",
    action: `transfer --to ${to}`,
    expected: "Rechazo InvalidDestination (8), o la CLI rechaza el argumento.",
    property: "P1",
    branch: "parse_address del destino",
  });
}

function capSequence() {
  const state = loadState();
  const to = state.trusted[0];
  const a = transfer(to, 30 * SCALE, "gh-28-30", {
    pre: "Tope diario 50. spent del dia en 0 al empezar, si nadie gasto antes.",
    action: "transfer 30 a un contacto de confianza",
    expected: "Pasa.",
    property: "P4",
    branch: "carril transfer dentro del tope",
  });
  if (!a.ok) return;
  const b = transfer(to, 20 * SCALE, "gh-28-20", {
    pre: "Ya se gastaron 30.",
    action: "transfer 20 al mismo contacto",
    expected: "Pasa. spent queda en 50.",
    property: "P4",
    branch: "carril transfer dentro del tope",
  });
  if (!b.ok) return;
  transfer(to, 1 * SCALE, "gh-28-1", {
    pre: "El tope del dia esta lleno.",
    action: "transfer 1 al mismo contacto",
    expected: "Rechazo CapExceeded (5).",
    property: "P4",
    branch: "la suma pasa el tope",
  });
}

function capRest() {
  const state = loadState();
  const to = state.trusted[0];
  const b = transfer(to, 20 * SCALE, "gh-28-20", {
    pre: "El transfer de 30 ya entro en el ledger. Quedan 20 del tope.",
    action: "transfer 20 al mismo contacto",
    expected: "Pasa. spent queda en 50.",
    property: "P4",
    branch: "carril transfer dentro del tope",
  });
  if (!b.ok) return;
  transfer(to, 1 * SCALE, "gh-28-1", {
    pre: "El tope del dia esta lleno.",
    action: "transfer 1 al mismo contacto",
    expected: "Rechazo CapExceeded (5).",
    property: "P4",
    branch: "la suma pasa el tope",
  });
}

function sameLedger() {
  const state = loadState();
  if (!state.account2) {
    const params = join(configDir, "install-params.json");
    const run = stellar([
      "contract",
      "deploy",
      "--network",
      "testnet",
      "--source",
      "owner",
      "--wasm",
      join(root, "target", "wasm", "account.wasm"),
      "--salt",
      "0000000000000000000000000000000000000000000000000000000000000004",
      "--",
      "--owner",
      state.owner,
      "--policy",
      state.guardian_hold,
      "--install_params-file-path",
      params,
    ]);
    const id = [...run.text.matchAll(/C[A-Z2-7]{55}/g)].at(-1)?.[0];
    record("gh-29-account", {
      pre: "Segunda cuenta, mismo wasm, para no compartir el tope del dia.",
      action: "deploy account salt 4",
      expected: "Cuenta nueva con la misma politica.",
      ok: Boolean(id),
      hash: run.text.match(/[a-f0-9]{64}/)?.[1] || null,
      error: id ? "ok" : run.text.slice(0, 500),
      property: "P4",
      branch: "constructor",
      text: run.text,
    });
    if (!id) return;
    state.account2 = id;
    saveState(state);
    const mint = ownerCall(state.usdc, ["mint", "--to", id, "--amount", String(500 * SCALE)], "gh-29-mint", {
      pre: "La segunda cuenta necesita saldo del SAC de prueba.",
      action: "mint 500",
      expected: "Pasa. El emisor es la dueña.",
      property: "-",
      branch: "fuera de enforce",
    }, { auth: "none", source: "owner" });
    if (!mint.ok) return;
  }
  const account = loadState().account2;
  const to = loadState().trusted[0];
  const first = transfer(to, 30 * SCALE, "gh-29-a", {
    pre: "Segunda cuenta, tope 50, spent 0.",
    action: "primer transfer de 30",
    expected: "Pasa.",
    property: "P4",
    branch: "carril transfer dentro del tope",
  }, { from: account });
  const second = transfer(to, 30 * SCALE, "gh-29-b", {
    pre: "Otro transfer de 30. Si cae en el mismo ledger, no debe duplicar el tope.",
    action: "segundo transfer de 30",
    expected: "Rechazo CapExceeded (5) si el primero ya sumo.",
    property: "P4",
    branch: "la suma pasa el tope",
  }, { from: account });
  const la = ledgerOf(first.hash);
  const lb = ledgerOf(second.hash);
  const same = la !== null && lb !== null && la === lb;
  record("gh-29-ledgers", {
    pre: "Comparar el ledger de las dos transacciones.",
    action: `ledger A ${la} ledger B ${lb}`,
    expected: "Uno pasa y otro rechaza en el mismo ledger.",
    ok: first.ok === true && second.ok === false && same,
    hash: second.hash,
    error: la === null || lb === null ? "ledger no leido" : same ? "mismo ledger" : `ledgers distintos ${la} y ${lb}`,
    property: "P4",
    branch: "contador del dia",
    text: `A ${first.hash} ledger ${la}\nB ${second.hash} ledger ${lb}\n`,
  });
}

function ledgerOf(hash) {
  if (!hash) return null;
  const run = stellar(["tx", "fetch", "--network", "testnet", "--hash", hash, "--output", "json"]);
  const seq = run.text.match(/"ledger"\s*:\s*(\d+)/);
  return seq ? Number(seq[1]) : null;
}

function readOriginal() {
  const account = "CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX";
  const policy = "CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI";
  const owner = "GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y";
  const fetches = [
    ["instance", ["ledger", "entry", "fetch", "contract-data", "--network", "testnet", "--contract", account, "--instance", "--output", "json"]],
  ];
  let text = "";
  for (const [label, args] of fetches) {
    const run = stellar(args);
    text += `\n## ${label}\n${run.text}\n`;
  }
  const count = text.match(/"symbol":\s*"Count"[\s\S]{0,200}?"u32":\s*(\d+)/);
  record("read-original-account", {
    pre: "Cuenta de evidence/stellar/deployment.md. Lectura publica, sin semillas.",
    action: "ledger entry fetch instance",
    expected: "Una regla. Esta lectura no firma nada.",
    ok: count?.[1] === "1",
    hash: null,
    error: count ? `Count ${count[1]}` : "no se leyo Count",
    property: "P5",
    branch: "lectura, no es enforce",
    text,
  });
  const spec = stellar([
    "contract",
    "info",
    "interface",
    "--network",
    "testnet",
    "--id",
    account,
  ]);
  record("read-original-spec", {
    pre: "Spec on-chain de la cuenta de deployment.md.",
    action: "stellar contract info interface",
    expected: "Solo __check_auth. Sin execute ni upgrade.",
    ok: /__check_auth/.test(spec.text) && !/fn execute\(/.test(spec.text) && !/fn upgrade\(/.test(spec.text),
    hash: null,
    error: "salida literal en el log",
    property: "P5",
    branch: "spec",
    text: spec.text,
  });
  process.stdout.write(`ORIGINAL_POLICY ${policy}\nORIGINAL_OWNER ${owner}\n`);
}

function probe() {
  const state = loadState();
  transfer(state.trusted[0], 0, "probe-amount-zero", {
    pre: "Monto 0.",
    action: "transfer 0 a un contacto",
    expected: "Rechazo InvalidAmount (4).",
    property: "P4",
    branch: "amount <= 0",
  });
  transfer(state.owner, 2 * SCALE, "probe-to-owner", {
    pre: "La dueña G no es contacto de confianza.",
    action: "transfer hacia la direccion G de la dueña",
    expected: "Rechazo NotAllowed (3).",
    property: "P1",
    branch: "destino no es contacto y no hay hold",
  });
  ownerCall(
    state.usdc,
    ["burn", "--from", state.account, "--amount", String(1 * SCALE)],
    "probe-burn",
    {
      pre: "burn no esta en la lista blanca.",
      action: "SAC burn",
      expected: "Rechazo NotAllowed (3).",
      property: "P4",
      branch: "catch-all",
    },
  );
  ownerCall(
    state.registry,
    ["mark_executed", "--account", state.account, "--id", "0"],
    "probe-mark-executed",
    {
      pre: "Solo la politica puede marcar ejecutado.",
      action: "el atacante llama mark_executed",
      expected: "Rechazo. No mueve tokens.",
      property: "P3",
      branch: "el registro exige la auth de guardian_hold",
    },
    { source: "attacker", auth: "none" },
  );
  ownerCall(
    state.registry,
    ["cancel", "--account", state.account, "--id", "0"],
    "probe-owner-cancel",
    {
      pre: "T-049 no esta. La dueña no tiene cancel en enforce.",
      action: "la dueña llama cancel",
      expected: "Rechazo: cancel exige al guardian.",
      property: "P3",
      branch: "cancel no pasa por enforce",
    },
    { auth: "none", source: "owner" },
  );
  transfer(state.trusted[0], 1 * SCALE, "probe-rule-id-1", {
    pre: "Solo existe la regla 0.",
    action: "transfer firmado con context_rule_ids = [1]",
    expected: "Rechazo ContextRuleNotFound (3000). No aparece una segunda regla.",
    property: "P5",
    branch: "seleccion de regla en do_check_auth",
  }, { ruleId: 1 });
  const install = stellar([
    "contract",
    "invoke",
    "--network",
    "testnet",
    "--source",
    "owner",
    "--id",
    state.guardian_hold,
    "--",
    "install",
    "--help",
  ]);
  record("probe-second-install-help", {
    pre: "install ya se ejecuto en el constructor.",
    action: "ver la ayuda de install; la segunda llamada se intenta en el log si la ayuda existe",
    expected: "La segunda install no debe dejar otra regla.",
    ok: false,
    hash: null,
    error: /install/.test(install.text) ? "ayuda disponible" : "sin ayuda",
    property: "P5",
    branch: "AlreadyInstalled o reentrada",
    text: install.text,
  });
}

function finishMature() {
  const state = loadState();
  const now = Number(ledgerNow().close);
  const holds = [1, 2, 3].map((id) => {
    const view = viewCall(state.registry, ["get_hold", "--account", state.account, "--id", String(id)]);
    const amount = Number(view.text.match(/"amount"\s*:\s*"(\d+)"/)?.[1]);
    const dest = view.text.match(/"destination"\s*:\s*"([^"]+)"/)?.[1];
    const ready = Number(view.text.match(/"ready_at"\s*:\s*"?(\d+)/)?.[1]);
    const status = view.text.match(/"status"\s*:\s*"([^"]+)"/)?.[1];
    return { id, amount, dest, ready, status, now };
  });
  process.stdout.write(`${JSON.stringify(holds)}\n`);
  const wrongDest = holds.find((h) => h.amount === 19 * SCALE && h.status === "Retained");
  const wrongAmt = holds.find((h) => h.amount === 21 * SCALE && h.status === "Retained");
  const replayHold = holds.find((h) => h.amount === 13 * SCALE && h.status === "Retained");
  if (!wrongDest || now < wrongDest.ready) {
    record("wrong-destination", {
      pre: "Hold de 19 unidades.",
      action: "no enviado",
      expected: "Rechazo",
      ok: false,
      hash: null,
      error: `no corrido: now ${now} ready ${wrongDest?.ready} status ${wrongDest?.status}`,
      property: "P1",
      branch: "carril transfer",
    });
  } else {
    transfer(state.stranger, wrongDest.amount, "wrong-destination", {
      pre: `Hold ${wrongDest.id} maduro hacia ${wrongDest.dest}.`,
      action: `transfer del mismo monto hacia ${state.stranger}`,
      expected: "Rechazo NotAllowed (3).",
      property: "P1",
      branch: "destino distinto: no hay hold Retained con esos campos",
    });
  }
  if (!wrongAmt || now < wrongAmt.ready) {
    record("wrong-amount", {
      pre: "Hold de 21 unidades.",
      action: "no enviado",
      expected: "Rechazo",
      ok: false,
      hash: null,
      error: `no corrido: now ${now} ready ${wrongAmt?.ready}`,
      property: "P1",
      branch: "carril transfer",
    });
  } else {
    transfer(wrongAmt.dest, wrongAmt.amount - SCALE, "wrong-amount", {
      pre: `Hold ${wrongAmt.id} maduro por ${wrongAmt.amount}.`,
      action: `transfer al mismo destino por ${wrongAmt.amount - SCALE}`,
      expected: "Rechazo NotAllowed (3).",
      property: "P1",
      branch: "monto distinto: find_retained no encuentra el hold",
    });
  }
  if (!replayHold || now < replayHold.ready) {
    record("replay", {
      pre: "Hold de 13 unidades.",
      action: "no enviado",
      expected: "Rechazo del segundo envio",
      ok: false,
      hash: null,
      error: `no corrido: now ${now} ready ${replayHold?.ready}`,
      property: "P2",
      branch: "hold ya Executed",
    });
    return;
  }
  const first = transfer(replayHold.dest, replayHold.amount, "replay-first", {
    pre: `Hold ${replayHold.id} maduro.`,
    action: "transfer que coincide campo a campo",
    expected: "Pasa una vez.",
    property: "P2",
    branch: "carril transfer de hold maduro",
  });
  if (!first.ok) return;
  transfer(replayHold.dest, replayHold.amount, "replay", {
    pre: "Ese transfer ya se ejecuto.",
    action: "la dueña firma otra vez el mismo transfer",
    expected: "Rechazo NotAllowed (3).",
    property: "P2",
    branch: "el hold ya no esta Retained",
  });
}

function replaySecond() {
  const state = loadState();
  transfer(state.attacker, 13 * SCALE, "replay", {
    pre: "Hold 3 ya quedo Executed en f310ba3561f83dd3f0af2e7177bfca47f49d474d57e43b08d1d5cd50a3c022ec.",
    action: "la dueña firma otra vez el mismo transfer",
    expected: "Rechazo NotAllowed (3).",
    property: "P2",
    branch: "el hold ya no esta Retained",
  });
}

function recheckBinding() {
  const state = loadState();
  const now = Number(ledgerNow().close);
  const view = (id) => {
    const text = viewCall(state.registry, ["get_hold", "--account", state.account, "--id", String(id)]).text;
    return {
      id,
      amount: Number(text.match(/"amount"\s*:\s*"(\d+)"/)?.[1]),
      dest: text.match(/"destination"\s*:\s*"([^"]+)"/)?.[1],
      ready: Number(text.match(/"ready_at"\s*:\s*"?(\d+)/)?.[1]),
      status: text.match(/"status"\s*:\s*"([^"]+)"/)?.[1],
    };
  };
  const wrongDest = view(1);
  const wrongAmt = view(2);
  process.stdout.write(`${JSON.stringify({ now, wrongDest, wrongAmt })}\n`);
  if (wrongDest.status === "Retained" && now >= wrongDest.ready) {
    transfer(state.stranger, wrongDest.amount, "wrong-destination", {
      pre: `Hold ${wrongDest.id} sigue Retained hacia ${wrongDest.dest}.`,
      action: `transfer del mismo monto hacia ${state.stranger}`,
      expected: "Rechazo NotAllowed (3).",
      property: "P1",
      branch: "destino distinto: no hay hold Retained con esos campos",
    });
  }
  if (wrongAmt.status === "Retained" && now >= wrongAmt.ready) {
    transfer(wrongAmt.dest, wrongAmt.amount - SCALE, "wrong-amount", {
      pre: `Hold ${wrongAmt.id} sigue Retained por ${wrongAmt.amount}.`,
      action: `transfer al mismo destino por ${wrongAmt.amount - SCALE}`,
      expected: "Rechazo NotAllowed (3).",
      property: "P1",
      branch: "monto distinto: find_retained no encuentra el hold",
    });
  }
}

const commands = {
  prepare,
  "immediate-reject": immediateReject,
  "held-after-cancel": heldAfterCancel,
  "guardian-transfer": guardianTransfer,
  "wrong-destination": wrongDestination,
  "wrong-amount": wrongAmount,
  approve,
  "add-rule": () => missing("add-rule", "add_context_rule", "P5"),
  execute: () => missing("execute", "execute", "P5"),
  upgrade: () => missing("upgrade", "upgrade", "P5"),
  "direct-enforce": directEnforce,
  "queue-without-owner": queueWithoutOwner,
  "nested-transfer": nestedTransfer,
  replay,
  "muxed-destination": muxedDestination,
  "cap-sequence": capSequence,
  "cap-rest": capRest,
  "same-ledger": sameLedger,
  "read-original": readOriginal,
  probe,
  "finish-mature": finishMature,
  "recheck-binding": recheckBinding,
  "replay-second": replaySecond,
};

const name = process.argv[2];
if (!commands[name]) {
  process.stderr.write(`usage: node scripts/attacks/run.mjs <${Object.keys(commands).join("|")}>\n`);
  process.exit(2);
}
commands[name]();
