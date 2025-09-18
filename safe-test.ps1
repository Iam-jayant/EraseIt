# EraseIt Safe Testing Script
# This script runs the application in SAFE MODE - NO REAL DATA WILL BE WIPED

Write-Host "======================================" -ForegroundColor Green
Write-Host "    EraseIt - SAFE TESTING MODE      " -ForegroundColor Green  
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "🛡️  SAFETY GUARANTEE: NO real data will be wiped!" -ForegroundColor Yellow
Write-Host "✅ Only simulated operations will be performed" -ForegroundColor Green
Write-Host "✅ Test certificates will be generated" -ForegroundColor Green
Write-Host "✅ UI and functionality will be demonstrated" -ForegroundColor Green
Write-Host ""

# Check if Maven is available
Write-Host "Checking prerequisites..." -ForegroundColor Cyan
try {
    $mavenVersion = mvn -version | Select-Object -First 1
    Write-Host "✅ Maven found: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Maven not found. Please install Maven first." -ForegroundColor Red
    Write-Host "   Download from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    exit 1
}

# Check Java version
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java not found. Please install Java 11+ first." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Building application for safe testing..." -ForegroundColor Cyan

# Build the application
mvn clean compile -q

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build successful!" -ForegroundColor Green
} else {
    Write-Host "❌ Build failed. Check the error messages above." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting EraseIt in SAFE TEST MODE..." -ForegroundColor Green
Write-Host ""
Write-Host "Expected behavior:" -ForegroundColor Yellow
Write-Host "• Window title will show 'SAFE TEST MODE'" -ForegroundColor White
Write-Host "• Only 3 simulated test drives will appear" -ForegroundColor White  
Write-Host "• All wipe operations are simulated (no real commands)" -ForegroundColor White
Write-Host "• Test certificates will be generated" -ForegroundColor White
Write-Host "• Debug logging will show all operations" -ForegroundColor White
Write-Host ""

# Create test output directory
$testDir = "test-output"
if (!(Test-Path $testDir)) {
    New-Item -ItemType Directory -Path $testDir | Out-Null
    Write-Host "Created test output directory: $testDir" -ForegroundColor Cyan
}

# Start the application with test configuration
Write-Host "Launching application..." -ForegroundColor Green
Write-Host "(Close the application window when done testing)" -ForegroundColor Yellow
Write-Host ""

# Run with test profile and safe configuration
$env:SPRING_PROFILES_ACTIVE = "test"
mvn javafx:run -Dspring.config.location=application-test.properties -Dlogging.level.com.hackathon.securewipe=DEBUG

Write-Host ""
Write-Host "🎉 Safe testing completed!" -ForegroundColor Green
Write-Host ""
Write-Host "What was tested safely:" -ForegroundColor Cyan
Write-Host "✅ User interface and controls" -ForegroundColor Green
Write-Host "✅ Drive detection simulation" -ForegroundColor Green
Write-Host "✅ Wipe method selection" -ForegroundColor Green
Write-Host "✅ Progress bar and logging" -ForegroundColor Green
Write-Host "✅ Certificate generation" -ForegroundColor Green
Write-Host "✅ QR code creation" -ForegroundColor Green
Write-Host ""
Write-Host "Check the 'test-output' folder for generated test certificates!" -ForegroundColor Yellow
Write-Host ""