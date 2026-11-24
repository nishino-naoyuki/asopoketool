# ASOPokeTool Local Start Script
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$JdkDir   = Join-Path $ScriptDir ".local-env\jdk\jdk-17.0.19+10"
$MavenDir = Join-Path $ScriptDir ".local-env\maven\apache-maven-3.9.5"
$MvnCmd   = Join-Path $MavenDir "bin\mvn.cmd"
$Mysqld   = Join-Path $ScriptDir ".local-env\mariadb\mariadb-10.11.5-winx64\bin\mysqld.exe"
$DataDir  = Join-Path $ScriptDir ".local-env\mariadb\mariadb-10.11.5-winx64\data"

# 1. Check & Start MariaDB if port 3306 is not open
$port3306 = $null
if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
    $port3306 = Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue
}

if ($null -eq $port3306) {
    Write-Output "Starting local MariaDB on port 3306..."
    Start-Process -FilePath $Mysqld -ArgumentList "--datadir=`"$DataDir`" --port=3306" -WindowStyle Hidden
    Start-Sleep -Seconds 4
    Write-Output "MariaDB startup triggered."
} else {
    Write-Output "Database port 3306 is already active."
}

# 2. Set environment variables
$env:JAVA_HOME = $JdkDir
$env:M2_HOME   = $MavenDir
$env:PATH      = "$JdkDir\bin;$MavenDir\bin;" + $env:PATH

Write-Output "JAVA_HOME: $env:JAVA_HOME"
Write-Output "M2_HOME:   $env:M2_HOME"
Write-Output "Starting ASOPokeTool Spring Boot application on http://localhost:8080/asopoketool ..."

# 3. Start Spring Boot
& $MvnCmd spring-boot:run
