# GH-28. 30, luego 20, luego 1 contra el tope de 50.
. "$PSScriptRoot/lib.ps1"
Write-Host "Tope 30 + 20, y despues 1. SAC de prueba."
Invoke-GpNode "cap-sequence"
