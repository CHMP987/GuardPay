#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "approve y transfer_from"
echo "ID: GH-16 y GH-15. Propiedad: P4."
echo "approve esperado: NotAllowed (3). Rama catch-all. No entra en transfer ni en queue."
echo "hash approve con firma de la dueña: 25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434"
gp_horizon "25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434"
echo "transfer_from esperado: sin allowance. Nativo gh_15_transfer_from_rejected: verde."
stellar="$(gp_stellar)"
set +e
out="$("$stellar" contract invoke --network testnet --id "$GP_SAC" --source-account "$GP_ATTACKER" --send no -- transfer_from --spender "$GP_ATTACKER" --from "$GP_ACCOUNT" --to "$GP_CONTACT1" --amount 10000000 2>&1)"
code=$?
set -e
printf '%s\n' "$out" | tail -n 12
echo "hash transfer_from: no hay tx"
echo "exit: $code"
if gp_have_state; then
  echo "Reejecución de approve con la firma de la dueña:"
  node "$GP_ROOT/scripts/sign-delegated.mjs" --invoke --id "$GP_SAC" -- approve --from "$GP_ACCOUNT" --spender "$GP_ATTACKER" --amount 10000000 --live_until_ledger 6000000
fi
