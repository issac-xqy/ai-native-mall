# AI-Native Mall Startup Script (Windows)
# Author: xqy
# Date: 2026-04-15

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AI-Native Smart Mall Startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Set Node.js path (use built-in node.js)
$nodePath = "$PSScriptRoot\node.js"
if (Test-Path $nodePath) {
    $env:Path = "$nodePath;" + $env:Path
    Write-Host "[OK] Node.js path configured: $nodePath" -ForegroundColor Green
} else {
    Write-Host "[WARN] Built-in Node.js not found, using system PATH" -ForegroundColor Yellow
}

# Check Java environment
$javaPath = "D:\java\develop\jdk-17\bin\java.exe"
if (-not (Test-Path $javaPath)) {
    Write-Host "[ERROR] Java 17 not found at: $javaPath" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Java 17 found" -ForegroundColor Green
Write-Host ""

# Build backend first (avoid classpath length issue)
Write-Host "[BUILD] Compiling and packaging backend..." -ForegroundColor Yellow
$buildResult = Start-Process -FilePath ".\mvnw.cmd" `
    -ArgumentList "clean", "package", "-DskipTests" `
    -NoNewWindow `
    -Wait `
    -PassThru `
    -WorkingDirectory $PSScriptRoot

if ($buildResult.ExitCode -ne 0) {
    Write-Host "[ERROR] Build failed, please check error messages" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Build successful" -ForegroundColor Green
Write-Host ""

# Find generated jar file
$jarFile = Get-ChildItem -Path "target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1

if (-not $jarFile) {
    Write-Host "[ERROR] No jar file found in target directory" -ForegroundColor Red
    exit 1
}

Write-Host "[START] Starting backend service..." -ForegroundColor Yellow
Write-Host "   Jar file: $($jarFile.Name)" -ForegroundColor Gray

$backendProcess = Start-Process -FilePath $javaPath `
    -ArgumentList "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", $jarFile.FullName `
    -NoNewWindow `
    -PassThru `
    -WorkingDirectory $PSScriptRoot

Start-Sleep -Seconds 5

# Start admin frontend
Write-Host "[START] Starting admin panel..." -ForegroundColor Yellow
$adminProcess = Start-Process -FilePath ".\node.js\npm.cmd" `
    -ArgumentList "run", "dev" `
    -NoNewWindow `
    -PassThru `
    -WorkingDirectory "$PSScriptRoot\web-admin"

Start-Sleep -Seconds 2

# Start client frontend
Write-Host "[START] Starting client app..." -ForegroundColor Yellow
$clientProcess = Start-Process -FilePath ".\node.js\npm.cmd" `
    -ArgumentList "run", "dev" `
    -NoNewWindow `
    -PassThru `
    -WorkingDirectory "$PSScriptRoot\web-client"

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  [OK] All services started!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Admin Panel: http://localhost:3000" -ForegroundColor Cyan
Write-Host "Client App:  http://localhost:3001" -ForegroundColor Cyan
Write-Host "Backend API: http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop all services" -ForegroundColor Yellow
Write-Host ""

# Wait for user interrupt
try {
    Wait-Process -Id $backendProcess.Id, $adminProcess.Id, $clientProcess.Id -ErrorAction SilentlyContinue
} catch {
    Write-Host ""
    Write-Host "[STOP] Stopping services..." -ForegroundColor Yellow
    
    # Stop all processes
    Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $adminProcess.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $clientProcess.Id -Force -ErrorAction SilentlyContinue
    
    Write-Host "[OK] All services stopped" -ForegroundColor Green
}
