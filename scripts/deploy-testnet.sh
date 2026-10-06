#!/usr/bin/env bash
# Same demo as deploy-testnet.ps1. Keys stay in scripts/.testnet/.
# Retention is the contract constant 120s. There is no 600s expiry parameter.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
CONFIG="$ROOT/scripts/.testnet"
STATE="$CONFIG/state.json"
mkdir -p "$CONFIG"

stellar_cmd() {
  local args=()
  local placed=0
  local arg
  for arg in "$@"; do
    if [[ "$arg" == "--" && "$placed" -eq 0 ]]; then
      args+=(--network testnet --)
      placed=1
    else
      args+=("$arg")
    fi
  done
  if [[ "$placed" -eq 0 ]]; then
    args+=(--network testnet)
  fi
  stellar --config-dir "$CONFIG" "${args[@]}"
}

if [[ -f "$STATE" ]]; then
  echo "State file exists. Second run will not deploy new contracts."
  python3 - "$STATE" <<'PY'
import json, sys
state = json.load(open(sys.argv[1], encoding="utf-8-sig"))
for key in ("owner", "guardian", "account", "guardian_hold", "registry", "usdc"):
    print(f"{key} {state[key]}")
print("Second run is safe: contracts are loaded from scripts/.testnet/state.json.")
PY
  exit 0
fi

ensure_identity() {
  local name="$1"
  if ! stellar keys address "$name" --config-dir "$CONFIG" >/dev/null 2>&1; then
    stellar_cmd keys generate "$name" --fund
    sleep 2
  fi
  stellar keys address "$name" --config-dir "$CONFIG"
}

owner="$(ensure_identity owner)"
guardian="$(ensure_identity guardian)"
trusted1="$(ensure_identity trusted1)"
trusted2="$(ensure_identity trusted2)"
trusted3="$(ensure_identity trusted3)"
attacker="$(ensure_identity attacker)"

usdc_id="$(stellar_cmd contract asset deploy --asset "USDC:$owner" --source owner | tr -d '[:space:]' | grep -oE 'C[A-Z2-7]{55}' | tail -n 1)"
echo "Test SAC (issuer is the owner, not Circle): $usdc_id"

deploy_wasm() {
  local wasm="$1" salt="$2"
  shift 2
  if ! stellar_cmd contract deploy --wasm "$wasm" --source owner --salt "$salt" "$@"; then
    stellar_cmd contract id wasm --salt "$salt" --source owner
  fi
}

gh_id="$(deploy_wasm target/wasm/guardian_hold.wasm 0000000000000000000000000000000000000000000000000000000000000001 | grep -oE 'C[A-Z2-7]{55}' | tail -n 1)"
reg_id="$(deploy_wasm target/wasm/hold_registry.wasm 0000000000000000000000000000000000000000000000000000000000000002 -- --guardian "$guardian" --guardian_hold "$gh_id" | grep -oE 'C[A-Z2-7]{55}' | tail -n 1)"

params="$CONFIG/install-params.json"
python3 - "$params" "$owner" "$reg_id" "$usdc_id" "$trusted1" "$trusted2" "$trusted3" <<'PY'
import json, sys
path, owner, registry, usdc, t1, t2, t3 = sys.argv[1:]
json.dump({"map": [
  {"key": {"symbol": "daily_cap"}, "val": {"i128": "500000000"}},
  {"key": {"symbol": "owner"}, "val": {"address": owner}},
  {"key": {"symbol": "registry"}, "val": {"address": registry}},
  {"key": {"symbol": "trusted_contacts"}, "val": {"vec": [
    {"address": t1}, {"address": t2}, {"address": t3}
  ]}},
  {"key": {"symbol": "usdc"}, "val": {"address": usdc}},
]}, open(path, "w", encoding="ascii"), separators=(",", ":"))
PY

acc_id="$(stellar_cmd contract deploy --wasm target/wasm/account.wasm --source owner --salt 0000000000000000000000000000000000000000000000000000000000000003 -- --owner "$owner" --policy "$gh_id" --install_params-file-path "$params" | grep -oE 'C[A-Z2-7]{55}' | tail -n 1)"
stellar_cmd contract invoke --id "$usdc_id" --source owner -- mint --to "$acc_id" --amount 5000000000

python3 - "$STATE" "$owner" "$guardian" "$trusted1" "$trusted2" "$trusted3" "$attacker" "$usdc_id" "$gh_id" "$reg_id" "$acc_id" <<'PY'
import hashlib, json, sys
path, owner, guardian, t1, t2, t3, attacker, usdc, gh, reg, acc = sys.argv[1:]
def wasm_hash(name):
    data = open(f"target/wasm/{name}.wasm", "rb").read()
    return hashlib.sha256(data).hexdigest()
json.dump({
  "network": "testnet",
  "usdc_kind": "SAC de prueba. Emisor = dueña. No es el USDC de Circle.",
  "owner": owner,
  "guardian": guardian,
  "trusted": [t1, t2, t3],
  "attacker": attacker,
  "usdc": usdc,
  "guardian_hold": gh,
  "registry": reg,
  "account": acc,
  "daily_cap_raw": 500000000,
  "funded_raw": 5000000000,
  "scale": 10000000,
  "hold_seconds": 120,
  "expiry_seconds": None,
  "wasm": {
    "account": wasm_hash("account"),
    "guardian_hold": wasm_hash("guardian_hold"),
    "hold_registry": wasm_hash("hold_registry"),
  },
}, open(path, "w", encoding="utf-8"), indent=2)
print(f"owner {owner}")
print(f"guardian {guardian}")
print(f"account {acc}")
print(f"guardian_hold {gh}")
print(f"registry {reg}")
print(f"usdc {usdc}")
PY
