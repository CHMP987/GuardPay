# Escena 5. El guardian intenta transfer desde la cuenta de la dueña hacia si mismo.
. "$PSScriptRoot/lib.ps1"
Write-Host "Escena 5. El guardian no mueve dinero. SAC de prueba, no el USDC de Circle."
Invoke-GpNode "guardian-transfer"
