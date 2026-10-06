# Shared helpers for the attack scripts. Testnet only.
# Seeds stay in scripts/.testnet/. This file never prints them.
$ErrorActionPreference = "Stop"

$GpAttacks = $PSScriptRoot
$GpRoot = Split-Path -Parent (Split-Path -Parent $GpAttacks)
$GpConfig = Join-Path (Split-Path -Parent $GpAttacks) ".testnet"

$stellarDir = Join-Path $env:USERPROFILE ".local\bin"
if (Test-Path $stellarDir) {
    $env:PATH = "$stellarDir$([IO.Path]::PathSeparator)$env:PATH"
}

function Get-GpState {
    $path = Join-Path $GpConfig "state.json"
    if (-not (Test-Path $path)) {
        throw "Missing $path. Run scripts/deploy-testnet.ps1 first. Seeds are not in git."
    }
    return Get-Content $path -Raw | ConvertFrom-Json
}

function Invoke-GpNode([string]$Scenario) {
    Set-Location $GpRoot
    & node (Join-Path $GpAttacks "run.mjs") $Scenario
    if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Invoke-GpMissingFn([string]$Title, [string]$FunctionName) {
    $state = Get-GpState
    Write-Host "ATAQUE $Title"
    Write-Host "Cuenta $($state.account)"
    Write-Host "Accion: stellar contract invoke $FunctionName"
    Write-Host "Esperado: la CLI rechaza el subcomando. No hay transaccion."
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = & stellar --config-dir $GpConfig contract invoke --id $state.account --source owner --network testnet -- $FunctionName 2>&1 | Out-String
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous
    Write-Host $out.TrimEnd()
    Write-Host "CLI_EXIT $code"
    if ($out -match "unrecognized subcommand") {
        Write-Host "RESULTADO rechazo de la CLI"
        Write-Host "HASH no hay tx"
        Write-Host "ERROR unrecognized subcommand '$FunctionName'"
        return
    }
    Write-Host "RESULTADO inesperado: la funcion existe o el error no es el de la CLI"
    exit 1
}
