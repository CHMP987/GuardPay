#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Contrato intermedio que llama al transfer del SAC"
echo "ID: GH-18 y GH-18b. Propiedad: P1."
echo "Esperado: la lista blanca rechaza el contrato intermedio (NotAllowed)."
echo "Resultado real en testnet: no corrido."
echo "hash: no hay tx"
echo "No se desplegó un contrato intermedio. Firmar el árbol exige la semilla de la dueña, y no está en esta máquina."
echo "Nativos gh_18_nested_contract_rejected y gh_18b_nested_contract_rejected_with_mature_hold: verde."
