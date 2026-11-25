# Creates / updates .env from app\google-services.json, then you can delete the JSON (it is regenerated at build time).
$root = Split-Path -Parent $PSScriptRoot
$json = Join-Path $root "app\google-services.json"
$envf = Join-Path $root ".env"
if (-not (Test-Path $json)) { Write-Error "app\google-services.json not found"; exit 1 }
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($json))
$lines = @()
if (Test-Path $envf) { $lines = Get-Content $envf | Where-Object { $_ -notmatch '^GOOGLE_SERVICES_JSON_BASE64=' } }
$lines = @("GOOGLE_SERVICES_JSON_BASE64=$b64") + $lines
Set-Content -Path $envf -Value $lines -Encoding UTF8
Write-Host ".env updated. You can now delete app\google-services.json."
