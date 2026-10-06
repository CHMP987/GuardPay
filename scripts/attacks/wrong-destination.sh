#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Cambiar el destino de un hold maduro"
echo "ID: GH-20. Propiedad: P1."
echo "Precondición: un hold Retained y maduro. El transfer usa otro destino, fuera de los contactos."
echo "Esperado: rechazo NotAllowed. Comparación campo a campo."
echo "Resultado real en testnet de este caso exacto: no corrido."
echo "hash: no hay tx"
echo "Nativo gh_20_other_destination_rejected: verde (evidence/policies/tests.md)."
echo "Caso cercano en el ledger, GH-03, destino desconocido sin hold, error de contrato 3."
echo "hash citado GH-03: a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492"
gp_horizon "a8664af0b762a69b1eba145b05ffd46d1aab4f7c135572f5a585f4ffb183f492"
echo "No se reenvía un pago a un contacto: ese carril puede aceptar el monto si cabe en el tope."
