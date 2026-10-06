# GH-29. Dos transfer de 30. El segundo no debe pasar.
. "$PSScriptRoot/lib.ps1"
Write-Host "Dos transfer de 30. SAC de prueba."
Invoke-GpNode "same-ledger"
