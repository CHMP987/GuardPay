$ErrorActionPreference = "Continue"
. "$PSScriptRoot\common.ps1"
Write-GpHeader "Replay de una transaccion ya ejecutada"
Write-Host "ID: GH-19. Propiedad: P2."
$hash = "35b807f0d3256aef671d4dff7621377fa1459daeacb64648430b2b5e4360d573"
Write-Host "hash original: $hash"
$tx = Invoke-RestMethod "https://horizon-testnet.stellar.org/transactions/$hash"
$envPath = Join-Path $env:TEMP "guardpay-replay.xdr"
Set-Content -Path $envPath -Value $tx.envelope_xdr -NoNewline
$stellar = Get-GpStellar
Get-Content $envPath -Raw | & $stellar tx send --network testnet
Write-Host "hash de una tx nueva: no hay tx"
Remove-Item $envPath -ErrorAction SilentlyContinue
