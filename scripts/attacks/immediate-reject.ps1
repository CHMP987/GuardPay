# Escena 2, parte de terminal: enviar ya, sin hold.
. "$PSScriptRoot/lib.ps1"
Write-Host "Rechazo inmediato. SAC de prueba, no el USDC de Circle."
Invoke-GpNode "immediate-reject"
