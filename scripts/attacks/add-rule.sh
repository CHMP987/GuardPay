#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Añadir una segunda context rule"
echo "ID: GH-13 y GH-25. Propiedad: P5."
echo "Esperado: la cuenta no exporta add_context_rule. No hay transacción."
gp_missing_export "add_context_rule"
