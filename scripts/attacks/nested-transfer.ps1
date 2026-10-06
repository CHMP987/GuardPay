# Un contrato intermedio llama al SAC con la cuenta como from.
. "$PSScriptRoot/lib.ps1"
Write-Host "Contrato intermedio. El token es un SAC de prueba, no el USDC de Circle."
Invoke-GpNode "nested-transfer"
