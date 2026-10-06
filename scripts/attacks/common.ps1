# Helpers for scripts/attacks. Public addresses only. Does not print seeds.
$ErrorActionPreference = "Stop"
$GpRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$GpState = Join-Path $GpRoot "scripts\.testnet\state.json"
$GpAccount = "CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX"
$GpPolicy = "CCGIEMIU3D7IEGVZUKZTNSQN4UHPHE2RWXKTAVIS4666MDMIH3DZBJJI"
$GpRegistry = "CBEDM2DZYJ7VEU434J7D44VVQ6ADW4IAG5UHE74PY72KI5GNRTMRU36S"
$GpSac = "CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R"
$GpOwner = "GDJ3EJJG6JCIJF5IGR2EMZG72UHAPAMNFE55WLVFVFSQYGXTBYE7AP2Y"
$GpGuardian = "GA7G4HPJFYRWD6SI5DW7YYXYNAXZD7JXD3Z4L5GFJIMVBSNBNPBF7JZ3"
$GpAttacker = "GA77MSV4XBYMA24E7IEMWPUY63MQ2YYAQWP5U6QL577RCLFHVNMBS4GL"
$GpContact1 = "GDCL4MPCRHK4D4Q5INJV3TFQ5QIDG3S3IJUOS7QR2O52DSVZ2X3HQGYT"

function Get-GpStellar {
    $cmd = Get-Command stellar -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $fallback = Join-Path $env:USERPROFILE ".local\bin\stellar.exe"
    if (Test-Path $fallback) { return $fallback }
    throw "stellar no está en PATH ni en $fallback"
}

function Test-GpState {
    return (Test-Path $GpState)
}

function Write-GpHeader([string]$Title) {
    Write-Host "GuardPay ataque: $Title"
    Write-Host "Activo: SAC de prueba $GpSac. No es el USDC de Circle."
    Write-Host "Cuenta CLI: $GpAccount"
    Write-Host "Cuenta de la demo en emulador: CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6 (hashes aparte)"
}

function Show-GpHorizon([string]$Hash) {
    try {
        $tx = Invoke-RestMethod -Uri "https://horizon-testnet.stellar.org/transactions/$Hash"
        Write-Host "horizon successful=$($tx.successful) ledger=$($tx.ledger) created=$($tx.created_at)"
    } catch {
        Write-Host "horizon no corrido"
    }
}

function Invoke-GpMissingExport([string]$Fn) {
    $stellar = Get-GpStellar
    Write-Host "Acción: invoke $Fn en la cuenta. El spec publicado no tiene esa función."
    & $stellar contract invoke --network testnet --id $GpAccount --source-account $GpOwner --send no -- $Fn
    Write-Host "hash: no hay tx"
}
