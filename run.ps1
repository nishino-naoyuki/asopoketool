# ASOPokeTool Local Run Script (PowerShell Version)
$ErrorActionPreference = "Stop"

$ProjectDir = $PSScriptRoot
$LocalEnvDir = Join-Path $ProjectDir ".local-env"
$JdkDir = Join-Path $LocalEnvDir "jdk"
$MavenDir = Join-Path $LocalEnvDir "maven"

# Create directories
New-Item -ItemType Directory -Force -Path $LocalEnvDir | Out-Null
New-Item -ItemType Directory -Force -Path $JdkDir | Out-Null
New-Item -ItemType Directory -Force -Path $MavenDir | Out-Null

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  ASOPokeTool Local Environment Setup" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# 1. Setup Java 17
$JdkTarget = $null
if (Test-Path $JdkDir) {
    $subdirs = Get-ChildItem -Path $JdkDir -Directory
    if ($subdirs.Count -gt 0) {
        $JdkTarget = $subdirs[0].FullName
    }
}

if ($null -eq $JdkTarget) {
    Write-Host "[1/3] Java 17 not found. Downloading OpenJDK 17 (160MB)..." -ForegroundColor Yellow
    $JdkZip = Join-Path $LocalEnvDir "jdk.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" -OutFile $JdkZip
    Write-Host "Extracting Java 17..." -ForegroundColor Yellow
    Expand-Archive -Path $JdkZip -DestinationPath $JdkDir -Force
    Remove-Item $JdkZip -Force
    
    $subdirs = Get-ChildItem -Path $JdkDir -Directory
    $JdkTarget = $subdirs[0].FullName
    Write-Host "Java 17 setup complete." -ForegroundColor Green
} else {
    Write-Host "[1/3] Java 17 is already set up at $JdkTarget." -ForegroundColor Green
}
Write-Host ""

# 2. Setup Maven
$MavenTarget = Join-Path $MavenDir "apache-maven-3.9.5"
if (-not (Test-Path $MavenTarget)) {
    Write-Host "[2/3] Maven not found. Downloading Apache Maven (9MB)..." -ForegroundColor Yellow
    $MavenZip = Join-Path $LocalEnvDir "maven.zip"
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip" -OutFile $MavenZip
    Write-Host "Extracting Maven..." -ForegroundColor Yellow
    Expand-Archive -Path $MavenZip -DestinationPath $MavenDir -Force
    Remove-Item $MavenZip -Force
    Write-Host "Maven setup complete." -ForegroundColor Green
} else {
    Write-Host "[2/3] Maven is already set up." -ForegroundColor Green
}
Write-Host ""

# MariaDB startup and check block skipped for H2 Database local testing
<#
# 3. Setup Portable Database (MariaDB/MySQL compatible)
$DbDir = Join-Path $LocalEnvDir "mariadb"
$DbTarget = Join-Path $DbDir "mariadb-10.11.5-winx64"

# Check if port 3306 is already active (another MySQL/MariaDB might be running)
$portActive = $null
if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
    $portActive = Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue
}

if ($null -ne $portActive) {
    Write-Host "[3/3] Port 3306 is already active. Assuming existing database server is running." -ForegroundColor Green
} else {
    if (-not (Test-Path $DbTarget)) {
        Write-Host "[3/3] Database not found. Downloading Portable MariaDB (65MB)..." -ForegroundColor Yellow
        $DbZip = Join-Path $LocalEnvDir "mariadb.zip"
        Invoke-WebRequest -Uri "https://archive.mariadb.org/mariadb-10.11.5/winx64-packages/mariadb-10.11.5-winx64.zip" -OutFile $DbZip
        Write-Host "Extracting MariaDB..." -ForegroundColor Yellow
        Expand-Archive -Path $DbZip -DestinationPath $DbDir -Force
        Remove-Item $DbZip -Force
        
        Write-Host "Initializing MariaDB database engine..." -ForegroundColor Yellow
        # Initialize data directory
        $mysqlInstallDb = Join-Path $DbTarget "bin\mysql_install_db.exe"
        Start-Process -FilePath $mysqlInstallDb -ArgumentList "--datadir=""$DbTarget\data""" -NoNewWindow -Wait
        
        # Set root password to Abcc123.#
        # By default, MariaDB has no root password. We will set it during start or leave it empty, 
        # but to match application.properties we will set password for root
        Write-Host "MariaDB initialized." -ForegroundColor Green
    } else {
        Write-Host "[3/3] Portable MariaDB is already configured." -ForegroundColor Green
    }

    # Start MariaDB service in the background
    Write-Host "Starting Portable MariaDB server in the background (Port 3306)..." -ForegroundColor Yellow
    $mysqld = Join-Path $DbTarget "bin\mysqld.exe"
    Start-Process -FilePath $mysqld -ArgumentList "--datadir=""$DbTarget\data"" --port=3306" -NoNewWindow
    
    # Wait for DB to startup
    Start-Sleep -Seconds 12
}

# Create 'asopoketool' database if not exists
$mysqlCli = Join-Path $DbTarget "bin\mysql.exe"
if (Test-Path $mysqlCli) {
    Write-Host "Creating database 'asopoketool' inside local MariaDB..." -ForegroundColor Yellow
    # First set root password to Abcc123.# if it's new
    Start-Process -FilePath $mysqlCli -ArgumentList "-u root -e ""ALTER USER 'root'@'localhost' IDENTIFIED BY 'Abcc123.#'; CREATE DATABASE IF NOT EXISTS asopoketool CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;""" -NoNewWindow -Wait 2>$null
    # Fallback attempt in case password was already set
    Start-Process -FilePath $mysqlCli -ArgumentList "-u root -p""Abcc123.#"" -e ""CREATE DATABASE IF NOT EXISTS asopoketool CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;""" -NoNewWindow -Wait 2>$null
} else {
    if (Get-Command mysql -ErrorAction SilentlyContinue) {
        mysql -u root -p"Abcc123.#" -e "CREATE DATABASE IF NOT EXISTS asopoketool CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>$null
    }
}
Write-Host ""
#>

# Set Environment Variables temporarily
$env:JAVA_HOME = $JdkTarget
$env:M2_HOME = $MavenTarget
$env:PATH = "$JdkTarget\bin;$MavenTarget\bin;" + $env:PATH

Write-Host "---------------------------------------------" -ForegroundColor Cyan
Write-Host "Starting Application..." -ForegroundColor Cyan
Write-Host "---------------------------------------------" -ForegroundColor Cyan
Write-Host ""

# Running Application
mvn spring-boot:run
