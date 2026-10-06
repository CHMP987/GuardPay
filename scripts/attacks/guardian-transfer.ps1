. "$PSScriptRoot\common.ps1"
Write-GpHeader "El guardian intenta transfer desde la cuenta de la duena hacia si mismo"
Write-Host "ID: GH-09. Propiedad: P3. Escena 5."
if (Test-GpState) {
    $env:GUARDPAY_SIGNER = "guardian"
    & node (Join-Path $GpRoot "scripts\sign-delegated.mjs") --invoke --id $GpSac -- transfer --from $GpAccount --to $GpGuardian --amount 10000000
} else {
    Write-Host "Resultado real citado: UnauthorizedSigner 3016."
    Write-Host "hash: ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de"
    Show-GpHorizon "ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de"
    Write-Host "Reejecucion firmada: no corrido. No esta scripts/.testnet/state.json."
}
