# Llamar enforce desde fuera de __check_auth.
. "$PSScriptRoot/lib.ps1"
Write-Host "enforce directo. SAC de prueba."
Invoke-GpNode "direct-enforce"
