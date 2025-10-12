@echo off
REM Simple batch script to run the Java sample
REM This script searches for Maven in common locations and runs the sample

echo Searching for Maven installation...

REM Check common Maven installation paths
set MAVEN_PATH=
if exist "C:\Program Files\Apache\Maven" (
    for /d %%i in ("C:\Program Files\Apache\Maven\apache-maven-*") do set "MAVEN_PATH=%%i\bin"
)
if exist "C:\Program Files (x86)\Apache\Maven" (
    for /d %%i in ("C:\Program Files (x86)\Apache\Maven\apache-maven-*") do set "MAVEN_PATH=%%i\bin"
)
if exist "C:\maven" (
    for /d %%i in ("C:\maven\apache-maven-*") do set "MAVEN_PATH=%%i\bin"
)
if exist "C:\tools" (
    for /d %%i in ("C:\tools\apache-maven-*") do set "MAVEN_PATH=%%i\bin"
)

if defined MAVEN_PATH (
    echo Found Maven at: %MAVEN_PATH%
    echo Running Java sample...
    "%MAVEN_PATH%\mvn.cmd" -q exec:java -Dexec.mainClass=Main
) else (
    echo Maven not found in common locations.
    echo.
    echo Please install Maven or add it to your PATH using one of these methods:
    echo.
    echo 1. Download Maven from https://maven.apache.org/download.cgi
    echo 2. Extract to C:\Program Files\Apache\Maven
    echo 3. Add to PATH using PowerShell:
    echo    $mavenPath = "C:\Program Files\Apache\apache-maven-x.x.x\bin"
    echo    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    echo    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$mavenPath", "User")
    echo.
    echo See MAVEN_PATH_SETUP.md for detailed instructions.
    exit /b 1
)
