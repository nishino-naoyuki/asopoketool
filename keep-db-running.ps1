# Keep MariaDB running as a persistent background process
$mysqld = "c:\repo\asopoketool\.local-env\mariadb\mariadb-10.11.5-winx64\bin\mysqld.exe"
$datadir = "c:\repo\asopoketool\.local-env\mariadb\mariadb-10.11.5-winx64\data"

Write-Host "Starting MariaDB..."
$proc = Start-Process -FilePath $mysqld `
    -ArgumentList "--datadir=`"$datadir`" --port=3306" `
    -PassThru -WindowStyle Hidden

Write-Host "MariaDB started with PID: $($proc.Id)"
Write-Host "Keeping alive. Do not close this process."

# Wait forever (keeps mysqld alive as long as this script runs)
$proc | Wait-Process
Write-Host "MariaDB process ended."
