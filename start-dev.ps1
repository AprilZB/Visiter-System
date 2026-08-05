param (
    [string]$mysqlHost = "localhost",
    [string]$mysqlPort = "3306",
    [string]$mysqlUser = "root",
    [string]$mysqlPass = "admin@123",
    [string]$mysqlDb   = "visitor_system_db"
)


$ErrorActionPreference = "Stop"

try {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "  Starting Visitor System (MySQL Mode)..." -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan

    $WorkspaceDir = Get-Location

    # Set MySQL Datasource Environment Variables
    $env:SPRING_DATASOURCE_URL = "jdbc:mysql://${mysqlHost}:${mysqlPort}/${mysqlDb}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
    $env:SPRING_DATASOURCE_USERNAME = $mysqlUser
    $env:SPRING_DATASOURCE_PASSWORD = $mysqlPass

    Write-Host "[MySQL] Target DB: ${mysqlHost}:${mysqlPort}/${mysqlDb} (User: ${mysqlUser})" -ForegroundColor Cyan

    # 1. Check Backend Jar
    $JarPath = "$WorkspaceDir\visitor-backend\target\visitor-backend-1.0.0.jar"
    if (-not (Test-Path $JarPath)) {
        Write-Host "[Build] Backend jar not found, building with Maven..." -ForegroundColor Yellow
        Set-Location "$WorkspaceDir\visitor-backend"
        mvn clean package -DskipTests
        Set-Location $WorkspaceDir
    }

    # 2. Start Backend Server (Port 8096)
    $Port8096 = Get-NetTCPConnection -LocalPort 8096 -ErrorAction SilentlyContinue
    if ($Port8096) {
        Write-Host "[Backend] Port 8096 is already running." -ForegroundColor Green
    } else {
        Write-Host "[Backend] Launching Spring Boot Backend with MySQL (Port 8096)..." -ForegroundColor Green
        Start-Process -FilePath "java" -ArgumentList "-jar", "`"$JarPath`"" -WorkingDirectory "$WorkspaceDir\visitor-backend" -WindowStyle Normal
        Start-Sleep -Seconds 5
    }



    # 3. Check and clean Port 8097 for Frontend
    Set-Location "$WorkspaceDir\visitor-frontend"
    $Port8097 = Get-NetTCPConnection -LocalPort 8097 -ErrorAction SilentlyContinue
    if ($Port8097) {
        Write-Host "[Frontend] Cleaning occupying process on port 8097..." -ForegroundColor Yellow
        $Port8097 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 1
    }

    if (-not (Test-Path "$WorkspaceDir\visitor-frontend\node_modules")) {
        Write-Host "[Frontend] Installing npm dependencies..." -ForegroundColor Yellow
        npm install --registry=https://registry.npmmirror.com
    }


    Write-Host "`n==========================================================" -ForegroundColor Green
    Write-Host "  Visitor System Started Successfully!" -ForegroundColor Green
    Write-Host " --------------------------------------------------------" -ForegroundColor Gray
    Write-Host "  1. Visitor H5     : http://localhost:8097/visitor" -ForegroundColor White
    Write-Host "  2. DingTalk Host  : http://localhost:8097/host" -ForegroundColor White
    Write-Host "  3. Security H5    : http://localhost:8097/security" -ForegroundColor White
    Write-Host "  4. Admin Console  : http://localhost:8097/admin" -ForegroundColor White
    Write-Host "     - Admin User   : admin" -ForegroundColor Yellow
    Write-Host "     - Admin Pass   : Accupath@0723" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan

    # Launch Frontend Dev Server
    npm run dev
} catch {
    Write-Host "`n[ERROR] Start-up failed: $_" -ForegroundColor Red
    Read-Host "Press Enter to exit..."
}
