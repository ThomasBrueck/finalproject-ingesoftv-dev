# Build all service Docker images for E2E tests
# Requires: Docker Desktop, Java 21, Gradle

$DEV_REPO = Resolve-Path "$PSScriptRoot/.."
$SERVICES = @(
    "circleguard-auth-service",
    "circleguard-identity-service",
    "circleguard-promotion-service",
    "circleguard-notification-service",
    "circleguard-form-service",
    "circleguard-gateway-service",
    "circleguard-dashboard-service",
    "circleguard-file-service"
)

Write-Host "Building all service images from $DEV_REPO ..." -ForegroundColor Cyan

Set-Location $DEV_REPO

foreach ($service in $SERVICES) {
    Write-Host "`n=== Building $service ===" -ForegroundColor Yellow
    docker build -t "circleguard/e2e/$service:latest" `
        -f "services/$service/Dockerfile" `
        --build-arg GRADLE_OPTS="-Dorg.gradle.daemon=false" `
        .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build $service" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`nAll images built successfully!" -ForegroundColor Green
