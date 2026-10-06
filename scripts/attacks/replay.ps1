# Segunda firma del mismo pago ya ejecutado.
. "$PSScriptRoot/lib.ps1"
Write-Host "Replay. Puede esperar 120 s. SAC de prueba."
Invoke-GpNode "replay"
