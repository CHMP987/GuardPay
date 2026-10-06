#!/usr/bin/env bash
# Helpers for scripts/attacks. Prints public addresses and tx hashes only.
# Never reads or prints a seed, and never runs `stellar keys secret`.

GP_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GP_STATE="$GP_ROOT/scripts/.testnet/state.json"
GP_CONFIG="$GP_ROOT/scripts/.testnet"

# Public addresses of the CLI deployment. Not the Keystore demo account.
GP_ACCOUNT="CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX"
GP_POLICY="CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI"
GP_REGISTRY="CBEDM2DZYJ7VEU434J7D44VVQ6ADW4IAG5UHE74PY72KI5GNRTMRU36S"
GP_SAC="CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R"
GP_OWNER="GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y"
GP_GUARDIAN="GA7G4HPJFYRWD6SI5DW7YYXYNAXZD7JXD3Z4L5GFJIMVBSNBNPBF7JZ3"
GP_ATTACKER="GA77MSV4XBYMA24E7IEMWPUY63MQ2YYAQWP5U6QL577RCLFHVNMBS4GL"
GP_CONTACT1="GDCL4MPCRHK4D4Q5INJV3TFQ5QIDG3S3IJUOS7QR2O52DSVZ2X3HQGYT"

gp_stellar() {
  if command -v stellar >/dev/null 2>&1; then
    command -v stellar
    return 0
  fi
  if [ -n "${USERPROFILE:-}" ] && [ -x "${USERPROFILE}/.local/bin/stellar.exe" ]; then
    printf '%s\n' "${USERPROFILE}/.local/bin/stellar.exe"
    return 0
  fi
  echo "stellar: no está en PATH" >&2
  return 1
}

gp_have_state() {
  [ -f "$GP_STATE" ]
}

gp_header() {
  echo "GuardPay ataque: $1"
  echo "Activo: SAC de prueba $GP_SAC. No es el USDC de Circle."
  echo "Cuenta CLI: $GP_ACCOUNT"
  echo "Cuenta de la demo en emulador: CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6 (hashes aparte)"
}

gp_horizon() {
  python3 - "$1" << 'PY'
import json, sys, urllib.request
h = sys.argv[1]
url = "https://horizon-testnet.stellar.org/transactions/" + h
try:
    with urllib.request.urlopen(url, timeout=30) as r:
        tx = json.load(r)
    print(f"horizon successful={str(tx.get('successful')).lower()} ledger={tx.get('ledger')} created={tx.get('created_at')}")
except Exception as e:
    print(f"horizon no corrido ({type(e).__name__})")
PY
}

gp_missing_export() {
  local fn="$1"
  local stellar
  stellar="$(gp_stellar)"
  echo "Acción: invoke $fn en la cuenta. El spec publicado no tiene esa función."
  set +e
  local out
  out="$("$stellar" contract invoke --network testnet --id "$GP_ACCOUNT" --source-account "$GP_OWNER" --send no -- "$fn" 2>&1)"
  local code=$?
  set -e
  printf '%s\n' "$out"
  echo "hash: no hay tx"
  echo "exit: $code"
}

gp_signed() {
  # Remaining args are passed to sign-delegated.mjs after --invoke.
  if ! gp_have_state; then
    echo "Reejecución firmada: no corrido. No está scripts/.testnet/state.json."
    return 2
  fi
  echo "Reejecución firmada con scripts/sign-delegated.mjs"
  node "$GP_ROOT/scripts/sign-delegated.mjs" --invoke "$@"
}
