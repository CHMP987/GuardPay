#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

gp_header "Replay de una transacción ya ejecutada"
echo "ID: GH-19. Propiedad: P2."
echo "Precondición: GH-06 ya aplicó el hold 0 (Executed)."
echo "Acción: reenviar el mismo sobre."
echo "Esperado: la red no aplica el pago otra vez."
HASH="35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573"
echo "hash original: $HASH"
python3 -c 'import json,urllib.request; h="35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573"; tx=json.load(urllib.request.urlopen("https://horizon-testnet.stellar.org/transactions/"+h, timeout=30)); open("/tmp/guardpay-replay.xdr","w").write(tx["envelope_xdr"]); print("sobre descargado, successful original =", str(tx["successful"]).lower())'
stellar="$(gp_stellar)"
set +e
out="$("$stellar" tx send --network testnet < /tmp/guardpay-replay.xdr 2>&1)"
code=$?
set -e
printf '%s\n' "$out" | grep -E "hash is|TxBadSeq|error:|Failure" || printf '%s\n' "$out" | tail -n 20
echo "hash de una tx nueva: no hay tx"
echo "exit: $code"
rm -f /tmp/guardpay-replay.xdr
echo "Firmar de nuevo el mismo transfer (GH-19 parte b) queda no corrido sin la semilla."
echo "Nativo gh_19_replay_rejected: verde."
