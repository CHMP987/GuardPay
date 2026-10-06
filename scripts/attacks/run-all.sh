#!/usr/bin/env bash
set -u
dir="$(cd "$(dirname "$0")" && pwd)"
fail=0
for script in \
  held-after-cancel.sh \
  guardian-transfer.sh \
  wrong-destination.sh \
  wrong-amount.sh \
  muxed-destination.sh \
  approve-transfer-from.sh \
  add-rule.sh \
  execute.sh \
  upgrade.sh \
  direct-enforce.sh \
  queue-without-owner.sh \
  nested-transfer.sh \
  replay.sh
do
  echo
  echo "========== $script =========="
  if ! bash "$dir/$script"; then
    echo "EL SCRIPT TERMINÓ CON ERROR: $script"
    fail=1
  fi
done
exit "$fail"
