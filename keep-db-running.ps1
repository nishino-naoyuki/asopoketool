# Keep MariaDB running as a persistent background process
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysqld = Join-Path $ScriptDir ".local-env\mariadb\mariadb-10.11.5-winx64\bin\mysqld.exe"
$datadir = Join-Path $ScriptDir ".local-env\mariadb\mariadb-10.11.5-winx64\data"

Write-Host "Starting MariaDB..." -ForegroundColor Cyan
$proc = Start-Process -FilePath $mysqld `
    -ArgumentList "--datadir=`"$datadir`" --port=3306" `
    -PassThru -WindowStyle Hidden

Write-Host "MariaDB started with PID: $($proc.Id)" -ForegroundColor Green
Write-Host "Keeping alive. Do not close this process."
$proc | Wait-Process
Write-Host "MariaDB process ended."
