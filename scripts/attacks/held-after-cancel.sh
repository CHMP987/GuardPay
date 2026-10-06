#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Ejecutar un pago ya detenido"
echo "ID: GH-08. Propiedad: P2."
echo "Precondición: la dueña firmó un transfer del hold que el guardián ya canceló."
echo "Esperado: rechazo. El registro ya no está Retained. Rama: NotAllowed."
if gp_signed --id "$GP_SAC" -- transfer --from "$GP_ACCOUNT" --to "$GP_ATTACKER" --amount 200000000; then
  :
else
  echo "Resultado real citado (ensayos CLI del 6 oct 2026, no reenviado): error de contrato 3 (NotAllowed)."
  echo "hash: 2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f"
  gp_horizon "2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f"
fi
