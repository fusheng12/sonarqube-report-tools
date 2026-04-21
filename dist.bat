@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "DIST_DIR=%SCRIPT_DIR%dist\sonar-report-tool"

echo ========================================
echo   SonarQube Report Tool - Package
echo ========================================
echo.

REM Find tool jar
set "TOOL_JAR=%SCRIPT_DIR%target\sonar-report-tool.jar"
if not exist "%TOOL_JAR%" (
    echo [ERROR] target\sonar-report-tool.jar not found. Run build.bat first.
    pause
    exit /b 1
)

REM Check required files
if not exist "%SCRIPT_DIR%jdk-21\bin\java.exe" (
    echo [ERROR] jdk-21 not found.
    pause
    exit /b 1
)
if not exist "%SCRIPT_DIR%lib\sonar-cnes-report-5.0.3.jar" (
    echo [ERROR] lib\sonar-cnes-report-5.0.3.jar not found.
    pause
    exit /b 1
)

echo [1/4] Cleaning dist directory...
if exist "%SCRIPT_DIR%dist" rmdir /s /q "%SCRIPT_DIR%dist"
mkdir "%DIST_DIR%"
mkdir "%DIST_DIR%\target"
mkdir "%DIST_DIR%\lib"
mkdir "%DIST_DIR%\output"

echo [2/4] Copying files...
copy "%TOOL_JAR%" "%DIST_DIR%\target\sonar-report-tool.jar" >nul
copy "%SCRIPT_DIR%lib\sonar-cnes-report-5.0.3.jar" "%DIST_DIR%\lib\" >nul
copy "%SCRIPT_DIR%start.bat" "%DIST_DIR%\" >nul

echo [3/4] Copying JDK 21 (this may take a while)...
xcopy "%SCRIPT_DIR%jdk-21" "%DIST_DIR%\jdk-21\" /E /I /Q >nul

echo [4/4] Creating zip archive...
powershell -NoProfile -Command "Compress-Archive -Path '%DIST_DIR%' -DestinationPath '%SCRIPT_DIR%dist\sonar-report-tool.zip' -Force"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to create zip.
    echo The dist folder is ready at: %DIST_DIR%
    echo You can manually zip it.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Done!
echo   Output: dist\sonar-report-tool.zip
for %%F in ("%SCRIPT_DIR%dist\sonar-report-tool.zip") do echo   Size: %%~zF bytes
echo.
echo   Users: unzip and double-click start.bat
echo ========================================
echo.
pause
