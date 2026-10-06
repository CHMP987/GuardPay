#!/usr/bin/env bash
# Linux port of the attack entry points. LF line endings.
# The jury scripts are the .ps1 files. This calls the same runner.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
export PATH="${HOME}/.local/bin:${PATH}"
exec node scripts/attacks/run.mjs "$@"
