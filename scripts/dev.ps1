# Author: @luismede
# Version: 1.0

param (
    [Parameter(Position = 0)]
    [ValidateSet("up", "down", "restart", "logs", "ps", "clean", "help")]
    [string]$Command = "help"
)

function Show-Help {
    Write-Host ""
    Write-Host "Usage: .\dev.ps1 <command>" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Commands:" -ForegroundColor Yellow
    Write-Host "  up       Start all services in background"
    Write-Host "  down     Stop all services"
    Write-Host "  restart  Restart all services"
    Write-Host "  logs     Stream logs from all services (Ctrl+C to exit)"
    Write-Host "  ps       Show status of all containers"
    Write-Host "  clean    Stop and remove containers, volumes and networks"
    Write-Host ""
}

function Check-Java-Version {
    $javaVersion = & java -version 2>&1 | Select-String "version" | ForEach-Object { $_ -match '"(\d+)' | Out-Null; $matches[1] }
    if ([int]$javaVersion -lt 21) {
        Write-Host "Java 21 or higher is required. Current version: $javaVersion" -ForegroundColor Red
        Write-Host "Download in: https://adoptium.net" -ForegroundColor Yellow
        exit 1
    }
}

switch ($Command) {
    "up" {
        Check-Java-Version
        Write-Host "Starting services..." -ForegroundColor Green
        docker compose up -d
    }
    "down" {
        Write-Host "Stopping services..." -ForegroundColor Yellow
        docker compose down
    }
    "restart" {
        Write-Host "Restarting services..." -ForegroundColor Yellow
        docker compose restart
    }
    "logs" {
        Write-Host "Streaming logs (Ctrl+C to exit)..." -ForegroundColor Cyan
        docker compose logs -f
    }
    "ps" {
        docker compose ps
    }
    "clean" {
        $confirm = Read-Host "This will delete all volumes and data. Are you sure? (y/N)"
        if ($confirm -eq "y") {
            Write-Host "Cleaning up..." -ForegroundColor Red
            docker compose down -v --remove-orphans
        } else {
            Write-Host "Aborted." -ForegroundColor Gray
        }
    }
    default {
        Show-Help
        Check-Java-Version
    }
}
