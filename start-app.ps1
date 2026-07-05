# ASOPokeTool App Start (MySQL mode)
$env:JAVA_HOME = "c:\repo\asopoketool\.local-env\jdk\jdk-17.0.19+10"
$env:M2_HOME   = "c:\repo\asopoketool\.local-env\maven\apache-maven-3.9.5"
$env:PATH      = "$env:JAVA_HOME\bin;$env:M2_HOME\bin;" + $env:PATH

Write-Host "Java: $(java -version 2>&1 | Select-Object -First 1)" -ForegroundColor Cyan
Write-Host "Starting ASOPokeTool with MySQL..." -ForegroundColor Cyan
mvn spring-boot:run
