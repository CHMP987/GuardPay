# Reads the deployed account from testnet. Stops if the rule count is not 1.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$Config = Join-Path $PSScriptRoot ".testnet"
$state = Get-Content (Join-Path $Config "state.json") -Raw | ConvertFrom-Json

function Encode-Key([string]$Json) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $xdr = $Json | & stellar xdr encode --type ScVal --output single-base64
    $ErrorActionPreference = $previous
    if ($LASTEXITCODE -ne 0) { throw "could not encode key" }
    return ($xdr | Out-String).Trim()
}

function Fetch-Data([string]$KeyXdr) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $text = & stellar --config-dir $Config ledger entry fetch contract-data --network testnet --contract $state.account --key-xdr $KeyXdr --durability persistent --output json 2>&1 | Out-String
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous
    return @{ Code = $code; Text = $text }
}

$instanceText = & stellar --config-dir $Config ledger entry fetch contract-data --network testnet --contract $state.account --instance --output json | Out-String
$instance = $instanceText | ConvertFrom-Json
$storage = $instance.entries[0].val.contract_data.val.contract_instance.storage
$count = ($storage | Where-Object { $_.key.vec[0].symbol -eq "Count" }).val.u32
if ($count -ne 1) {
    Write-Host "STOP: rule count is $count"
    exit 2
}

$rule = (Fetch-Data (Encode-Key '{"vec":[{"symbol":"ContextRuleData"},{"u32":0}]}')).Text | ConvertFrom-Json
$signer = (Fetch-Data (Encode-Key '{"vec":[{"symbol":"SignerData"},{"u32":0}]}')).Text | ConvertFrom-Json
$policy = (Fetch-Data (Encode-Key '{"vec":[{"symbol":"PolicyData"},{"u32":0}]}')).Text | ConvertFrom-Json
$second = Fetch-Data (Encode-Key '{"vec":[{"symbol":"ContextRuleData"},{"u32":1}]}')
if ($second.Code -eq 0 -and $second.Text -match "ContextRuleData") {
    Write-Host "STOP: a second context rule is on the account"
    exit 2
}

$ruleMap = $rule.entries[0].val.contract_data.val.map
$signerMap = $signer.entries[0].val.contract_data.val.map
$policyMap = $policy.entries[0].val.contract_data.val.map
$context = ($ruleMap | Where-Object { $_.key.symbol -eq "context_type" }).val.vec[0].symbol
$name = ($ruleMap | Where-Object { $_.key.symbol -eq "name" }).val.string
$signerVec = ($signerMap | Where-Object { $_.key.symbol -eq "signer" }).val.vec
$policyAddress = ($policyMap | Where-Object { $_.key.symbol -eq "policy" }).val.address

Write-Host "rules $count"
Write-Host "context $context"
Write-Host "name $name"
Write-Host "signer $($signerVec[0].symbol) $($signerVec[1].address)"
Write-Host "policy $policyAddress"
if ($policyAddress -ne $state.guardian_hold) {
    Write-Host "STOP: policy is not the deployed GuardianHold"
    exit 2
}
if ($signerVec[1].address -ne $state.owner) {
    Write-Host "STOP: signer is not the owner"
    exit 2
}
Write-Host "one Default rule, signer is the owner, policy is GuardianHold"
