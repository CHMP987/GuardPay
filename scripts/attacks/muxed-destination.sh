#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Transfer a un destino muxed que no convierte a Address"
echo "ID: GH-20b. Propiedad: P1."
echo "Esperado en el contrato: InvalidDestination (8)."
echo "Nativo gh_20b_muxed_destination_rejected: verde."
stellar="$(gp_stellar)"
set +e
out="$("$stellar" contract invoke --network testnet --id "$GP_SAC" --source-account "$GP_OWNER" --send no -- transfer --from "$GP_ACCOUNT" --to MAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF --amount 1 2>&1)"
code=$?
set -e
printf '%s\n' "$out"
echo "hash: no hay tx"
echo "exit: $code"
echo "La CLI no armó una transacción. El rechazo de conversión dentro de enforce no se volvió a medir en el ledger."
