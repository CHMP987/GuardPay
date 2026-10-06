#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Invocar upgrade"
echo "ID: GH-25. Propiedad: P5."
echo "Esperado: unrecognized subcommand. No hay transacción."
gp_missing_export "upgrade"
