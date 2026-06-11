# CircleGuard E2E Test Runner
param(
    [switch]$SkipImageBuild,
    [string]$TestFilter = ""
)

$ErrorActionPreference = "Stop"
$E2E_DIR = $PSScriptRoot

Write-Host "=== CircleGuard E2E Tests ===" -ForegroundColor Cyan
Write-Host "E2E Directory: $E2E_DIR" -ForegroundColor Gray

# Step 1: Build Docker images (unless skipped)
if (-not $SkipImageBuild) {
    Write-Host "`n[1/3] Building service Docker images..." -ForegroundColor Yellow
    & "$E2E_DIR/build-images.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Image build failed!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "`n[1/3] Skipping image build (--SkipImageBuild)" -ForegroundColor Gray
}

# Step 2: Generate Gradle wrapper if missing
$GRADLEW = "$E2E_DIR/gradlew.bat"
if (-not (Test-Path $GRADLEW)) {
    Write-Host "`n[2/3] Generating Gradle wrapper..." -ForegroundColor Yellow
    # Copy from dev repo as fallback
    $DEV_GRADLEW = Resolve-Path "$E2E_DIR/../gradlew.bat"
    if (Test-Path $DEV_GRADLEW) {
        Copy-Item -LiteralPath $DEV_GRADLEW -Destination $GRADLEW
    }
}

# Step 3: Run tests
Write-Host "`n[3/3] Running E2E tests..." -ForegroundColor Yellow
Set-Location $E2E_DIR

if ($TestFilter) {
    & ".\gradlew.bat" test --tests "$TestFilter" --no-daemon
} else {
    & ".\gradlew.bat" test --no-daemon
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=== All E2E tests passed! ===" -ForegroundColor Green
} else {
    Write-Host "`n=== Some E2E tests failed ===" -ForegroundColor Red
    exit 1
}
