# Demo on Stellar testnet. Keys stay in scripts/.testnet/, which git ignores.
# A second run reuses those contracts instead of deploying again.
# Retention is the contract constant 120s. There is no 600s expiry parameter.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$ConfigDir = Join-Path $PSScriptRoot ".testnet"
$StatePath = Join-Path $ConfigDir "state.json"
$LogPath = Join-Path $ConfigDir "deploy.log"
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null
if (Test-Path $LogPath) { Remove-Item $LogPath }

$Scale = 10000000
$DailyCap = 50 * $Scale
$FundAmount = 500 * $Scale
$SaltGh = "0000000000000000000000000000000000000000000000000000000000000001"
$SaltReg = "0000000000000000000000000000000000000000000000000000000000000002"
$SaltAcc = "0000000000000000000000000000000000000000000000000000000000000003"

function Write-Log([string]$Text) {
    Add-Content -Path $LogPath -Value $Text
    Write-Host $Text
}

function Invoke-Stellar {
    param([Parameter(Mandatory = $true)][string[]]$ArgList)
    Write-Log (">> stellar " + ($ArgList -join " "))
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $dash = [array]::IndexOf($ArgList, "--")
    if ($dash -ge 0) {
        $head = @()
        if ($dash -gt 0) { $head = $ArgList[0..($dash - 1)] }
        $tail = $ArgList[$dash..($ArgList.Length - 1)]
        $final = $head + @("--network", "testnet") + $tail
    } else {
        $final = $ArgList + @("--network", "testnet")
    }
    $lines = & stellar --config-dir $ConfigDir @final 2>&1 | ForEach-Object { $_.ToString() }
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous
    $text = ($lines -join "`n")
    Write-Log $text
    return @{ Code = $code; Text = $text }
}

function Assert-Ok($Result, [string]$Label) {
    if ($Result.Code -ne 0) {
        throw "$Label failed with exit $($Result.Code)"
    }
}

function Get-LastContractId([string]$Text) {
    $found = [regex]::Matches($Text, "C[A-Z2-7]{55}")
    if ($found.Count -eq 0) { throw "No contract id in output" }
    return $found[$found.Count - 1].Value
}

function Get-TxHashes([string]$Text) {
    return @([regex]::Matches($Text, "tx/([a-f0-9]{64})") | ForEach-Object { $_.Groups[1].Value })
}

function Ensure-Identity([string]$Name) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $addr = & stellar keys address $Name --config-dir $ConfigDir 2>&1 | Out-String
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous
    if ($code -ne 0) {
        $created = Invoke-Stellar -ArgList @("keys", "generate", $Name, "--fund")
        Assert-Ok $created "keys generate $Name"
        Start-Sleep -Seconds 2
        $addr = & stellar keys address $Name --config-dir $ConfigDir | Out-String
        if ($LASTEXITCODE -ne 0) { throw "no address for $Name" }
    }
    return $addr.Trim()
}

function Address-ScVal([string]$Address) {
    return '{"address":"' + $Address + '"}'
}

if (Test-Path $StatePath) {
    Write-Log "State file exists. Second run will not deploy new contracts."
    $state = Get-Content $StatePath -Raw | ConvertFrom-Json
} else {
    $owner = Ensure-Identity "owner"
    $guardian = Ensure-Identity "guardian"
    $trusted1 = Ensure-Identity "trusted1"
    $trusted2 = Ensure-Identity "trusted2"
    $trusted3 = Ensure-Identity "trusted3"
    $attacker = Ensure-Identity "attacker"

    $asset = Invoke-Stellar -ArgList @("contract", "id", "asset", "--asset", "USDC:$owner")
    # id asset does not need --network in some builds; retry with the source form if needed
    if ($asset.Code -ne 0) {
        $asset = Invoke-Stellar -ArgList @("contract", "asset", "id", "--asset", "USDC:$owner", "--source", "owner")
    }
    $usdcId = $null
    if ($asset.Code -eq 0) { $usdcId = Get-LastContractId $asset.Text }

    $deployedAsset = Invoke-Stellar -ArgList @("contract", "asset", "deploy", "--asset", "USDC:$owner", "--source", "owner")
    if ($deployedAsset.Code -eq 0) {
        $usdcId = Get-LastContractId $deployedAsset.Text
    } elseif (-not $usdcId) {
        throw "Could not deploy or derive the test SAC"
    } else {
        Write-Log "SAC already on testnet: $usdcId"
    }
    Write-Log "Test SAC (issuer is the owner, not Circle): $usdcId"

    function Deploy-Wasm {
        param([string]$Wasm, [string]$Salt, [string[]]$AfterDash)
        $argList = @("contract", "deploy", "--wasm", $Wasm, "--source", "owner", "--salt", $Salt)
        if ($AfterDash -and $AfterDash.Count -gt 0) {
            $argList += "--"
            $argList += $AfterDash
        }
        $result = Invoke-Stellar -ArgList $argList
        if ($result.Code -eq 0) { return Get-LastContractId $result.Text }
        if ($result.Text -notmatch "exist") { throw "deploy failed for $Wasm`n$($result.Text)" }
        $predicted = Invoke-Stellar -ArgList @("contract", "id", "wasm", "--salt", $Salt, "--source", "owner")
        if ($predicted.Code -eq 0) {
            Write-Log "Deploy already done, using existing id"
            return Get-LastContractId $predicted.Text
        }
        throw "deploy failed for $Wasm"
    }

    $ghId = Deploy-Wasm -Wasm "target/wasm/guardian_hold.wasm" -Salt $SaltGh -AfterDash @()
    $regId = Deploy-Wasm -Wasm "target/wasm/hold_registry.wasm" -Salt $SaltReg -AfterDash @(
        "--guardian", $guardian, "--guardian_hold", $ghId
    )

    $contacts = @(
        (Address-ScVal $trusted1),
        (Address-ScVal $trusted2),
        (Address-ScVal $trusted3)
    ) -join ","
    $params = '{"map":[' +
        '{"key":{"symbol":"daily_cap"},"val":{"i128":"' + $DailyCap + '"}},' +
        '{"key":{"symbol":"owner"},"val":' + (Address-ScVal $owner) + '},' +
        '{"key":{"symbol":"registry"},"val":' + (Address-ScVal $regId) + '},' +
        '{"key":{"symbol":"trusted_contacts"},"val":{"vec":[' + $contacts + ']}},' +
        '{"key":{"symbol":"usdc"},"val":' + (Address-ScVal $usdcId) + '}' +
        ']}'

    $paramsPath = Join-Path $ConfigDir "install-params.json"
    Set-Content -Path $paramsPath -Value $params -Encoding ascii -NoNewline

    $acc = Invoke-Stellar -ArgList @(
        "contract", "deploy",
        "--wasm", "target/wasm/account.wasm",
        "--source", "owner",
        "--salt", $SaltAcc,
        "--",
        "--owner", $owner,
        "--policy", $ghId,
        "--install_params-file-path", $paramsPath
    )
    Assert-Ok $acc "deploy account"
    $accId = Get-LastContractId $acc.Text

    $mint = Invoke-Stellar -ArgList @(
        "contract", "invoke",
        "--id", $usdcId,
        "--source", "owner",
        "--",
        "mint",
        "--to", $accId,
        "--amount", "$FundAmount"
    )
    Assert-Ok $mint "mint test token"

    $state = [ordered]@{
        network = "testnet"
        usdc_kind = "SAC de prueba. Emisor = dueña. No es el USDC de Circle."
        owner = $owner
        guardian = $guardian
        trusted = @($trusted1, $trusted2, $trusted3)
        attacker = $attacker
        usdc = $usdcId
        guardian_hold = $ghId
        registry = $regId
        account = $accId
        daily_cap_raw = $DailyCap
        funded_raw = $FundAmount
        scale = $Scale
        hold_seconds = 120
        expiry_seconds = $null
        wasm = [ordered]@{
            account = (Get-FileHash "target/wasm/account.wasm" -Algorithm SHA256).Hash.ToLower()
            guardian_hold = (Get-FileHash "target/wasm/guardian_hold.wasm" -Algorithm SHA256).Hash.ToLower()
            hold_registry = (Get-FileHash "target/wasm/hold_registry.wasm" -Algorithm SHA256).Hash.ToLower()
        }
    }
    $state | ConvertTo-Json -Depth 6 | Set-Content -Path $StatePath -Encoding utf8
}

Write-Log "owner $($state.owner)"
Write-Log "guardian $($state.guardian)"
Write-Log "account $($state.account)"
Write-Log "guardian_hold $($state.guardian_hold)"
Write-Log "registry $($state.registry)"
Write-Log "usdc $($state.usdc)"
Write-Log "Second run is safe: contracts are loaded from scripts/.testnet/state.json."
