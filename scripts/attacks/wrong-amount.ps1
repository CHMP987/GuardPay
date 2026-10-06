# Cambiar el monto de un hold maduro.
. "$PSScriptRoot/lib.ps1"
Write-Host "Monto distinto al del hold. Puede esperar 120 s. SAC de prueba."
Invoke-GpNode "wrong-amount"
