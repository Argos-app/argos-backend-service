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

switch ($Command) {
    "up" {
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
    }
}
