param(
    [string]$WsUrl = "ws://localhost:8080/chatHub",
    [switch]$Smoke,
    [int]$MessageIntervalMs = 10000,
    [int]$DeliveryTimeoutMs = 5000,
    [int]$LatencyP95Ms = 1000
)

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$reportDirectory = Join-Path $projectRoot "reports"
$scriptPath = Join-Path $projectRoot "load-tests\websocket-load.js"

function Find-K6Executable {
    $command = Get-Command k6 -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\k6.exe"),
        (Join-Path $env:USERPROFILE "scoop\shims\k6.exe"),
        (Join-Path $env:ProgramFiles "k6\k6.exe")
    )

    if ($env:ChocolateyInstall) {
        $candidates += Join-Path $env:ChocolateyInstall "bin\k6.exe"
    }

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $wingetPackages = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages"
    if (Test-Path -LiteralPath $wingetPackages) {
        $installedExecutable = Get-ChildItem -LiteralPath $wingetPackages -Filter "k6.exe" -File -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
        if ($installedExecutable) {
            return $installedExecutable
        }
    }

    return $null
}

$k6Executable = Find-K6Executable
if (-not $k6Executable) {
    throw "k6 not found in PATH or standard installation folders. Reopen PowerShell after installing it, or run: winget install --id k6.k6 --exact --source winget"
}

try {
    $healthResponse = Invoke-WebRequest -Uri "http://localhost:8080/" -Method Head -TimeoutSec 10
    if ($healthResponse.StatusCode -ge 400) {
        throw "HTTP $($healthResponse.StatusCode)"
    }
} catch {
    throw "The application is not available at http://localhost:8080. Run 'docker compose up -d --build' before the test. Detail: $($_.Exception.Message)"
}

New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null

$profile = if ($Smoke) { "smoke" } else { "full" }
Write-Host "Running k6 profile '$profile' against $WsUrl"
Write-Host "Using executable: $k6Executable"
Write-Host "Reports will be written to $reportDirectory"

$env:WS_URL = $WsUrl
$env:K6_PROFILE = $profile
$env:MESSAGE_INTERVAL_MS = $MessageIntervalMs.ToString()
$env:DELIVERY_TIMEOUT_MS = $DeliveryTimeoutMs.ToString()
$env:LATENCY_P95_MS = $LatencyP95Ms.ToString()
$env:REPORT_DIR = $reportDirectory.Replace('\', '/')

try {
    & $k6Executable run $scriptPath
    $k6ExitCode = $LASTEXITCODE
} finally {
    Remove-Item Env:WS_URL -ErrorAction SilentlyContinue
    Remove-Item Env:K6_PROFILE -ErrorAction SilentlyContinue
    Remove-Item Env:MESSAGE_INTERVAL_MS -ErrorAction SilentlyContinue
    Remove-Item Env:DELIVERY_TIMEOUT_MS -ErrorAction SilentlyContinue
    Remove-Item Env:LATENCY_P95_MS -ErrorAction SilentlyContinue
    Remove-Item Env:REPORT_DIR -ErrorAction SilentlyContinue
}

exit $k6ExitCode
