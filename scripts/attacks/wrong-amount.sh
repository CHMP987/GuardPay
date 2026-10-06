#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Cambiar el monto de un hold maduro"
echo "ID: GH-21. Propiedad: P1."
echo "Precondición: hold Retained y maduro. El transfer cambia el monto."
echo "Esperado: rechazo. El hold no coincide campo a campo."
echo "Resultado real en testnet de este caso exacto: no corrido."
echo "hash: no hay tx"
echo "Nativo gh_21_other_amount_rejected: verde."
echo "GH-05 es el mismo monto antes de ready_at, error de contrato 7. No es este caso."
echo "hash citado GH-05: 0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed"
gp_horizon "0ebb4ba4c3db8b2a07dab5ee7d1d25199d0b340495afdcc76fb7e35904e697ed"
