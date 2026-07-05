@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   ASOPokeTool Local Setup and Run Tool
echo ========================================================
echo.

set "PROJECT_DIR=%~dp0"
set "LOCAL_ENV_DIR=%PROJECT_DIR%.local-env"
set "JDK_DIR=%LOCAL_ENV_DIR%\jdk"
set "MAVEN_DIR=%LOCAL_ENV_DIR%\maven"

mkdir "%LOCAL_ENV_DIR%" 2>NUL
mkdir "%JDK_DIR%" 2>NUL
mkdir "%MAVEN_DIR%" 2>NUL

echo [1/3] Checking Java 17 Setup...
if not exist "%JDK_DIR%\jdk-17.0.9+9" (
    echo Java 17 not found. Downloading OpenJDK 17 (160MB)...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip' -OutFile '%LOCAL_ENV_DIR%\jdk.zip'"
    echo Extracting Java 17...
    powershell -Command "Expand-Archive -Path '%LOCAL_ENV_DIR%\jdk.zip' -DestinationPath '%JDK_DIR%'"
    del "%LOCAL_ENV_DIR%\jdk.zip"
    echo Java 17 download completed.
) else (
    echo Java 17 is already set up.
)
echo.

echo [2/3] Checking Maven Setup...
if not exist "%MAVEN_DIR%\apache-maven-3.9.5" (
    echo Maven not found. Downloading Apache Maven (9MB)...
    powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip' -OutFile '%LOCAL_ENV_DIR%\maven.zip'"
    echo Extracting Maven...
    powershell -Command "Expand-Archive -Path '%LOCAL_ENV_DIR%\maven.zip' -DestinationPath '%MAVEN_DIR%'"
    del "%LOCAL_ENV_DIR%\maven.zip"
    echo Maven download completed.
) else (
    echo Maven is already set up.
)
echo.

echo [3/3] Checking local database 'asopoketool'...
where mysql >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo MySQL command found. Trying to create database if not exists...
    mysql -u root -p"Abcc123.#" -e "CREATE DATABASE IF NOT EXISTS asopoketool CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>NUL
    if %ERRORLEVEL% equ 0 (
        echo Database 'asopoketool' checked successfully.
    ) else (
        echo [Warning] Failed to auto create DB. If MySQL is running, please manually create a database named 'asopoketool'.
    )
) else (
    echo [Info] MySQL command not found in Path. Please make sure MySQL is installed and running with DB name 'asopoketool'.
)
echo.

echo ========================================================
echo   Starting Application...
echo ========================================================
echo.

set "JAVA_HOME=%JDK_DIR%\jdk-17.0.9+9"
set "M2_HOME=%MAVEN_DIR%\apache-maven-3.9.5"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

echo Java Version:
java -version
echo.
echo Maven Version:
mvn -version
echo.
echo Running App (mvn spring-boot:run)...
echo (First time boot may take a few minutes to download libraries)
echo.

mvn spring-boot:run
