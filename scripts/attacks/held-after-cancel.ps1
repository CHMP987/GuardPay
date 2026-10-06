# Escena 4. Ejecutar un pago que el guardian ya detuvo.
. "$PSScriptRoot/lib.ps1"
Write-Host "Escena 4. Pago detenido. El activo es un SAC de prueba, no el USDC de Circle."
Invoke-GpNode "held-after-cancel"
