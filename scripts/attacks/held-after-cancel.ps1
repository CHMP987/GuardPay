. "$PSScriptRoot\common.ps1"
Write-GpHeader "Ejecutar un pago ya detenido"
Write-Host "ID: GH-08. Propiedad: P2."
Write-Host "Esperado: rechazo NotAllowed (3)."
if (Test-GpState) {
    & node (Join-Path $GpRoot "scripts\sign-delegated.mjs") --invoke --id $GpSac -- transfer --from $GpAccount --to $GpAttacker --amount 200000000
} else {
    Write-Host "Resultado real citado: error de contrato 3 (NotAllowed)."
    Write-Host "hash: 2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f"
    Show-GpHorizon "2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f"
    Write-Host "Reejecucion firmada: no corrido. No esta scripts/.testnet/state.json."
}
