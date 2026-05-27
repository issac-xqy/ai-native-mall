# AI Mall One-Click Startup Script (Development Environment)
# Usage: .\start-dev.ps1

# Fix encoding issue - Set PowerShell to UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   AI-Native Smart Mall Dev Environment Launcher" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Check if Node.js is installed (use built-in or system)
$nodeExe = ".\node.js\node.exe"
if (-not (Test-Path $nodeExe)) {
    if (-not (Get-Command "node" -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] Node.js not found. Please install Node.js first." -ForegroundColor Red
        exit 1
    }
    $nodeExe = "node"
}

# Check if Maven is installed (use wrapper or system)
$mavenCmd = ".\mvnw.cmd"
if (-not (Test-Path $mavenCmd)) {
    if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] Maven not found. Please install Maven first." -ForegroundColor Red
        exit 1
    }
    $mavenCmd = "mvn"
}

Write-Host "[OK] Environment check passed" -ForegroundColor Green
Write-Host ""

# Add built-in Node.js to PATH for this session
$nodePath = Resolve-Path ".\node.js"
$env:Path = "$nodePath;$env:Path"
Write-Host "[INFO] Using built-in Node.js from: $nodePath" -ForegroundColor Cyan

# Set JDK 21 environment for Maven
$env:JAVA_HOME = "D:\java\develop\jdkse21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "[INFO] Using JDK 21 from: $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host ""

# Check if root package.json exists
if (-not (Test-Path "package.json")) {
    Write-Host "[INFO] First run, installing dependencies..." -ForegroundColor Yellow
    npm install
    Write-Host ""
}

# Check web-admin dependencies
if (-not (Test-Path "web-admin\node_modules")) {
    Write-Host "[INFO] Installing admin panel dependencies..." -ForegroundColor Yellow
    Push-Location web-admin
    npm install
    Pop-Location
    Write-Host ""
}

# Check web-client dependencies
if (-not (Test-Path "web-client\node_modules")) {
    Write-Host "[INFO] Installing client frontend dependencies..." -ForegroundColor Yellow
    Push-Location web-client
    npm install
    Pop-Location
    Write-Host ""
}

Write-Host "[START] Launching all services..." -ForegroundColor Green
Write-Host ""
Write-Host "TIP: All logs will be displayed here. Press Ctrl+C to stop all services." -ForegroundColor Yellow
Write-Host ""

# Use concurrently to start all services in parallel
npm run dev
