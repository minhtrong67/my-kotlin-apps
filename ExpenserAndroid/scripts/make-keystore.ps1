# Generates a release keystore (keystore\expenser-release.jks) and writes the signing settings into .env.
# Needs keytool (ships with the JDK / Android Studio:  <Android Studio>\jbr\bin\keytool.exe).
param([string]$Alias = "expenser", [string]$Password = "", [string]$Dname = "CN=Expenser, O=Expenser, C=VN")
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root "keystore"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$ks = Join-Path $dir "expenser-release.jks"
if (Test-Path $ks) { Write-Error "keystore already exists: $ks (delete it first if you really want a new one)"; exit 1 }
if ($Password -eq "") {
    $bytes = New-Object byte[] 18; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $Password = [Convert]::ToBase64String($bytes).Replace("+","A").Replace("/","B").Replace("=","")
}
& keytool -genkeypair -v -keystore $ks -alias $Alias -keyalg RSA -keysize 2048 -validity 10000 -storepass $Password -keypass $Password -dname $Dname
if ($LASTEXITCODE -ne 0) { Write-Error "keytool failed"; exit 1 }
$envf = Join-Path $root ".env"
$keep = @()
if (Test-Path $envf) { $keep = Get-Content $envf | Where-Object { $_ -notmatch '^(KEYSTORE_FILE|KEYSTORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD)=' } }
$keep += "KEYSTORE_FILE=keystore/expenser-release.jks"
$keep += "KEYSTORE_PASSWORD=$Password"
$keep += "KEY_ALIAS=$Alias"
$keep += "KEY_PASSWORD=$Password"
Set-Content -Path $envf -Value $keep -Encoding UTF8
Write-Host ""
Write-Host "Done. BACK UP keystore\expenser-release.jks and .env - losing them means you can never update the app."
Write-Host "SHA-1 to add in Firebase (Project settings > Your apps > Add fingerprint):"
& keytool -list -v -keystore $ks -alias $Alias -storepass $Password | Select-String "SHA1|SHA-1"
