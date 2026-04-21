@echo off

REM ========================================
REM  SonarQube Report Tool Launcher
REM  Priority: embedded JDK > JAVA_HOME > PATH
REM ========================================

set "SCRIPT_DIR=%~dp0"
set "TOOL_JAR=%SCRIPT_DIR%target\sonar-report-tool.jar"

if not defined TOOL_JAR (
    echo [ERROR] sonar-report-tool.jar not found
    echo Please run build.bat first
    pause
    exit /b 1
)

REM 1. Try embedded JDK
set "JAVAW_CMD="
if exist "%SCRIPT_DIR%jdk-21\bin\javaw.exe" (
    set "JAVAW_CMD=%SCRIPT_DIR%jdk-21\bin\javaw.exe"
)

REM 2. Try JAVA_HOME
if not defined JAVAW_CMD (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\javaw.exe" (
            set "JAVAW_CMD=%JAVA_HOME%\bin\javaw.exe"
        )
    )
)

REM 3. Fallback to system PATH
if not defined JAVAW_CMD (
    where javaw >nul 2>&1
    if %ERRORLEVEL%==0 (
        set "JAVAW_CMD=javaw"
    ) else (
        echo [ERROR] Java not found!
        echo.
        echo Please do one of the following:
        echo   1. Extract JDK 21 to %SCRIPT_DIR%jdk-21\
        echo   2. Set JAVA_HOME environment variable
        echo   3. Add Java to system PATH
        echo.
        echo Download: https://adoptium.net/temurin/releases/?version=21
        pause
        exit /b 1
    )
)

REM Launch GUI with javaw (no console window)
start "" "%JAVAW_CMD%" -jar "%TOOL_JAR%"
