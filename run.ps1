# ApexBank Launch Script for PowerShell
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "         ApexBank - Bank Management System Launch Script" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "`n[1/2] Compiling Java source files..." -ForegroundColor Yellow
javac -encoding UTF-8 -cp "lib/*" -d bin src/com/bank/model/*.java src/com/bank/config/*.java src/com/bank/dao/*.java src/com/bank/server/*.java src/com/bank/*.java

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "✅ [1/2] Compilation successful!" -ForegroundColor Green
Write-Host "`n[2/2] Launching ApexBank Server on port 8080..." -ForegroundColor Yellow
Write-Host "👉 Open your browser at: http://localhost:8080`n" -ForegroundColor Green

java -cp "bin;lib/*" com.bank.Main
