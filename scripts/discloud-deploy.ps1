# Author: @luismede
# Version: 1.0

param(
    [Parameter(Position = 0)]
    [ValidateSet("prod", "homolog")]
    [string]$Environment = "prod"
)

$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $Root

$ConfigSource = if ($Environment -eq "prod") { "discloud.config" } else { "discloud.config.homolog" }
$BackupConfig = "discloud.config.bak"

try {
    Write-Host "==> Building application ($Environment)..." -ForegroundColor Green
    & "$Root\mvnw.cmd" -B clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Build failed with exit code $LASTEXITCODE" }

    Write-Host "==> Staging app.jar for Discloud upload..." -ForegroundColor Green
    Copy-Item "$Root\target\app.jar" "$Root\app.jar" -Force

    if ($ConfigSource -ne "discloud.config") {
        Write-Host "==> Staging $ConfigSource as discloud.config..." -ForegroundColor Green
        Copy-Item "$Root\discloud.config" "$Root\$BackupConfig" -Force
        Copy-Item "$Root\$ConfigSource" "$Root\discloud.config" -Force
    }

    $AppIdLine = Select-String -Path "$Root\discloud.config" -Pattern '^ID=' | Select-Object -First 1
    if (-not $AppIdLine) { throw "ID not found in discloud.config" }
    $AppId = ($AppIdLine.Line -split '=', 2)[1]

    & discloud app info $AppId 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "==> App '$AppId' already exists, committing new build..." -ForegroundColor Cyan
        & discloud app commit $AppId
    } else {
        Write-Host "==> App '$AppId' not found, uploading as a new app..." -ForegroundColor Cyan
        & discloud app up
    }
    if ($LASTEXITCODE -ne 0) { throw "Discloud deploy failed with exit code $LASTEXITCODE" }
}
finally {
    if ($ConfigSource -ne "discloud.config" -and (Test-Path "$Root\$BackupConfig")) {
        Write-Host "==> Restoring discloud.config..." -ForegroundColor Yellow
        Move-Item "$Root\$BackupConfig" "$Root\discloud.config" -Force
    }
    if (Test-Path "$Root\app.jar") {
        Remove-Item "$Root\app.jar" -Force
    }
    Pop-Location
}
