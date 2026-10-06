#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "queue para la cuenta de la dueña sin su firma"
echo "ID: GH-27. Propiedad: P4."
echo "Precondición: un tercero llama a hold_registry.queue(cuenta, SAC, destino, monto)."
echo "Esperado: rechazo. queue exige la autorización de la cuenta."
echo "Resultado real, 2026-10-06T22:26:52Z. Fee payer GC7SNM4SEWEZTPDNXSVUWLTDXVOB7DGKKRQ3OTBTQ75OETUULQZFSFKZ."
echo "La firma de la cuenta iba vacía. __check_auth terminó en UnreachableCodeReached."
echo "get_hold del id 2 respondió HoldNotFound (2). list_retained siguió vacío."
echo "hash: 20c5b59814acf635c33a92405c7897c398454a31481585b295983630e499b4c7"
gp_horizon "20c5b59814acf635c33a92405c7897c398454a31481585b295983630e499b4c7"
echo "La semilla del fee payer no está en el repo."
echo "Nativo gh_27_queue_without_owner_rejected: verde."
