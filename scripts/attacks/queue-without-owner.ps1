# queue para la cuenta de la dueña sin su firma.
. "$PSScriptRoot/lib.ps1"
Write-Host "queue sin la firma de la dueña."
Invoke-GpNode "queue-without-owner"
