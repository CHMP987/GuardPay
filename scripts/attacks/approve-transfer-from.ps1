$ErrorActionPreference = "Continue"
. "$PSScriptRoot\common.ps1"
Write-GpHeader "approve y transfer_from"
Write-Host "ID: GH-16 y GH-15. Propiedad: P4."
Write-Host "hash approve: 25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434"
Show-GpHorizon "25b91a394c032af67050b0315e57ccaba1fe77b1d391b2aae32363297dcab434"
$stellar = Get-GpStellar
& $stellar contract invoke --network testnet --id $GpSac --source-account $GpAttacker --send no -- transfer_from --spender $GpAttacker --from $GpAccount --to $GpContact1 --amount 10000000
Write-Host "hash transfer_from: no hay tx"
if (Test-GpState) {
    & node (Join-Path $GpRoot "scripts\sign-delegated.mjs") --invoke --id $GpSac -- approve --from $GpAccount --spender $GpAttacker --amount 10000000 --live_until_ledger 6000000
}
