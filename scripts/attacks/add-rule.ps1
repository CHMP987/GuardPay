# Anadir una segunda context rule. La cuenta no exporta esa funcion.
. "$PSScriptRoot/lib.ps1"
Invoke-GpMissingFn "add-rule" "add_context_rule"
