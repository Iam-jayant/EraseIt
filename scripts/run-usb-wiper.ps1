# EraseIt - USB Drive Wiper
# Safe script to build and run the USB drive wiper

Write-Host "======================================" -ForegroundColor Green
Write-Host "    EraseIt - USB Drive Wiper        " -ForegroundColor Green  
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "🔒 SAFETY FEATURES ENABLED:" -ForegroundColor Yellow
Write-Host "✅ Only USB removable drives can be wiped" -ForegroundColor Green
Write-Host "✅ System drives (C:\\) are completely blocked" -ForegroundColor Green
Write-Host "✅ Multiple confirmation dialogs required" -ForegroundColor Green
Write-Host "✅ Must type 'WIPE' to confirm operation" -ForegroundColor Green
Write-Host "✅ Drive larger than 2TB are blocked" -ForegroundColor Green
Write-Host "✅ Comprehensive safety assessment shown" -ForegroundColor Green
Write-Host ""

# Check if Java is available
Write-Host "Checking system requirements..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java not found. Please install Java 11+ first." -ForegroundColor Red
    Write-Host "   Download from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check Maven
Write-Host "Checking Maven..." -ForegroundColor Cyan
try {
    mvn -version | Out-Null
    Write-Host "✅ Maven found and working" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Maven not found, but we can try with Java directly" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "🔧 Building EraseIt USB Wiper..." -ForegroundColor Cyan
Write-Host ""

# Try Maven build first
$buildSuccess = $false
try {
    mvn clean compile -q
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Maven build successful!" -ForegroundColor Green
        $buildSuccess = $true
    }
} catch {
    Write-Host "⚠️  Maven build failed, trying alternative..." -ForegroundColor Yellow
}

if (-not $buildSuccess) {
    Write-Host "⚠️  Please ensure Maven is installed and try:" -ForegroundColor Yellow
    Write-Host "   mvn clean compile" -ForegroundColor White
    Write-Host "   mvn javafx:run" -ForegroundColor White
    Write-Host ""
}

Write-Host ""
Write-Host "🚀 Starting EraseIt USB Wiper..." -ForegroundColor Green
Write-Host ""
Write-Host "IMPORTANT USAGE INSTRUCTIONS:" -ForegroundColor Red
Write-Host "1. Connect your USB drive that you want to wipe" -ForegroundColor White
Write-Host "2. Click 'Refresh Devices' to detect drives" -ForegroundColor White
Write-Host "3. Select ONLY the USB drive you want to wipe" -ForegroundColor White
Write-Host "4. Verify the safety status shows 'SAFE - Removable USB drive'" -ForegroundColor White
Write-Host "5. Choose your wipe method (DoD 3-Pass recommended)" -ForegroundColor White
Write-Host "6. Click 'Start Secure Wipe' and follow confirmations" -ForegroundColor White
Write-Host "7. Type 'WIPE' exactly when prompted" -ForegroundColor White
Write-Host "8. Save the certificate when complete" -ForegroundColor White
Write-Host ""

Write-Host "⚠️  FINAL WARNINGS:" -ForegroundColor Red
Write-Host "• This will PERMANENTLY destroy all data on the selected USB drive" -ForegroundColor Red
Write-Host "• Make sure you have backups of any important data" -ForegroundColor Red
Write-Host "• Double-check you're selecting the correct drive" -ForegroundColor Red
Write-Host "• Your SSD/HDD are protected and cannot be wiped" -ForegroundColor Red
Write-Host ""

$confirmation = Read-Host "Type 'START' to launch EraseIt (or press Enter to cancel)"

if ($confirmation -eq "START") {
    Write-Host ""
    Write-Host "🚀 Launching EraseIt..." -ForegroundColor Green
    Write-Host "Close the application window when done." -ForegroundColor Yellow
    Write-Host ""
    
    try {
        # Try to run with Maven
        mvn javafx:run
    } catch {
        Write-Host ""
        Write-Host "❌ Failed to start with Maven." -ForegroundColor Red
        Write-Host "Please run manually: mvn javafx:run" -ForegroundColor Yellow
    }
} else {
    Write-Host ""
    Write-Host "Operation cancelled. Stay safe! 🛡️" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Thank you for using EraseIt responsibly!" -ForegroundColor Green
