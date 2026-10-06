# Cambiar el destino de un hold maduro.
. "$PSScriptRoot/lib.ps1"
Write-Host "Destino distinto al del hold. Puede esperar 120 s. SAC de prueba."
Invoke-GpNode "wrong-destination"
