#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Llamar enforce directamente desde fuera"
echo "ID: GH-26. Propiedad: P4."
echo "Precondición: un tercero invoca guardian_hold.enforce con un transfer a un contacto, dentro del tope."
echo "Esperado: el ledger rechaza en account.require_auth(). El gasto del día no cambia."
echo "Resultado real, 2026-10-06T22:31:22Z. Fee payer GC7SNM4SEWEZTPDNXSVUWLTDXVOB7DGKKRQ3OTBTQ75OETUULQZFSFKZ."
echo "La simulación en modo record aceptó la lista blanca. El envío, con la firma de la cuenta vacía, falló:"
echo "auth invalid_action. __check_auth terminó en UnreachableCodeReached. spent_today siguió en 100000000."
echo "hash: 0e46055b18304e54f69a1ec39891bf12fb75ece15e16680ab17393410804678f"
gp_horizon "0e46055b18304e54f69a1ec39891bf12fb75ece15e16680ab17393410804678f"
echo "Nativo gh_26_direct_enforce_rejected: verde."
echo "No es un bypass: no hubo escritura del gasto."
