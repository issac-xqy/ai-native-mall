# AI Mall Stop Script (Development Environment)
# Usage: .\stop-dev.ps1

Write-Host ""
Write-Host "================================================" -ForegroundColor Red
Write-Host "   AI-Native Smart Mall Service Stopper        " -ForegroundColor Red
Write-Host "================================================" -ForegroundColor Red
Write-Host ""

$stoppedCount = 0

# Stop Java processes (Spring Boot)
$javaProcesses = Get-Process | Where-Object { 
    $_.ProcessName -eq 'java' -and 
    ($_.CommandLine -like '*ai-native-mall*' -or 
     $_.Path -like '*java_ai*')
}

if ($javaProcesses) {
    Write-Host "[STOP] Stopping Spring Boot backend..." -ForegroundColor Yellow
    $javaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    $stoppedCount++
    Write-Host "[OK] Spring Boot backend stopped" -ForegroundColor Green
} else {
    # Try to find by port
    try {
        $javaPort8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
        if ($javaPort8080) {
            Write-Host "[STOP] Stopping process on port 8080..." -ForegroundColor Yellow
            Stop-Process -Id $javaPort8080 -Force -ErrorAction SilentlyContinue
            $stoppedCount++
            Write-Host "[OK] Backend service stopped (PID: $javaPort8080)" -ForegroundColor Green
        }
    } catch {
        # Port not in use or access denied
    }
}

# Stop Node.js processes (Vite frontend)
$nodeProcesses = Get-Process | Where-Object { 
    $_.ProcessName -eq 'node' 
}

if ($nodeProcesses) {
    Write-Host "[STOP] Stopping frontend services..." -ForegroundColor Yellow
    
    # Find processes using frontend ports
    $portsToCheck = @(5173, 5174, 5175)
    foreach ($port in $portsToCheck) {
        try {
            $processId = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
            if ($processId) {
                Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            }
        } catch {
            # Port not in use
        }
    }
    
    # If there are remaining node processes, stop them all
    Start-Sleep -Milliseconds 500
    $remainingNodes = Get-Process | Where-Object { $_.ProcessName -eq 'node' }
    if ($remainingNodes) {
        $remainingNodes | Stop-Process -Force -ErrorAction SilentlyContinue
    }
    
    $stoppedCount++
    Write-Host "[OK] All frontend services stopped" -ForegroundColor Green
}

Write-Host ""
if ($stoppedCount -gt 0) {
    Write-Host "[SUCCESS] Stopped $stoppedCount service(s)" -ForegroundColor Green
} else {
    Write-Host "[INFO] No running services found" -ForegroundColor Cyan
}
Write-Host ""
