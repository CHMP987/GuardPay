#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "El guardián intenta transfer desde la cuenta de la dueña hacia sí mismo"
echo "ID: GH-09. Propiedad: P3. Escena 5."
echo "Precondición: el payload nombra al guardián. El guardián no está en los firmantes."
echo "Esperado: rechazo antes de mover el SAC."
if gp_have_state; then
  echo "Reejecución con GUARDPAY_SIGNER=guardian"
  GUARDPAY_SIGNER=guardian node "$GP_ROOT/scripts/sign-delegated.mjs" --invoke --id "$GP_SAC" -- transfer --from "$GP_ACCOUNT" --to "$GP_GUARDIAN" --amount 10000000
else
  echo "Resultado real citado: UnauthorizedSigner (3016), luego auth invalid_action."
  echo "hash: ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de"
  gp_horizon "ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de"
  echo "Reejecución firmada: no corrido. No está scripts/.testnet/state.json."
fi
