# Levanta PostgreSQL (contenedor glifo-postgres) para el backend.
# Arranca Docker Desktop si no esta corriendo y espera a que la base acepte conexiones.
$ErrorActionPreference = "Stop"
$backend = Join-Path $PSScriptRoot "..\backend"

function Test-Docker { docker info *> $null; return $LASTEXITCODE -eq 0 }

if (-not (Test-Docker)) {
    Write-Host "Docker no esta corriendo, iniciando Docker Desktop..."
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe",
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
    )
    $exe = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $exe) { throw "No se encontro Docker Desktop." }
    Start-Process $exe
    $deadline = (Get-Date).AddMinutes(3)
    while (-not (Test-Docker)) {
        if ((Get-Date) -gt $deadline) { throw "Docker no arranco en 3 minutos." }
        Start-Sleep -Seconds 2
    }
}
Write-Host "Docker listo."

# Si el contenedor ya existe (aunque lo haya creado otro compose) se reutiliza;
# si no, se crea con el docker-compose.yml del backend.
$exists = docker ps -a --filter "name=^glifo-postgres$" --format "{{.Names}}"
if ($exists) {
    docker start glifo-postgres | Out-Null
} else {
    docker compose -f (Join-Path $backend "docker-compose.yml") up -d
}

$deadline = (Get-Date).AddMinutes(1)
do {
    docker exec glifo-postgres pg_isready -U glifo *> $null
    if ($LASTEXITCODE -eq 0) { break }
    if ((Get-Date) -gt $deadline) { throw "PostgreSQL no respondio a tiempo." }
    Start-Sleep -Seconds 1
} while ($true)
Write-Host "PostgreSQL listo en localhost:5432."
