$ErrorActionPreference = "Continue"
. "$PSScriptRoot\common.ps1"
Write-GpHeader "Transfer a un destino muxed"
Write-Host "ID: GH-20b. Propiedad: P1. Nativo: verde."
$stellar = Get-GpStellar
& $stellar contract invoke --network testnet --id $GpSac --source-account $GpOwner --send no -- transfer --from $GpAccount --to MAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF --amount 1
Write-Host "hash: no hay tx"
