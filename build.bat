@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAVA_HOME=%SCRIPT_DIR%jdk-21"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAREXE=%JAVA_HOME%\bin\jar.exe"

set "SRC=%SCRIPT_DIR%src\main\java"
set "OUT=%SCRIPT_DIR%target\classes"
set "TARGET=%SCRIPT_DIR%target"

if not exist "%JAVAC%" (
    echo [ERROR] javac not found: %JAVAC%
    pause
    exit /b 1
)

echo [INFO] Cleaning...
if exist "%TARGET%" rmdir /s /q "%TARGET%"
mkdir "%OUT%"

echo [INFO] Compiling...
"%JAVAC%" -source 21 -target 21 -encoding UTF-8 -d "%OUT%" "%SRC%\com\sonar\tools\App.java" "%SRC%\com\sonar\tools\model\SonarProject.java" "%SRC%\com\sonar\tools\model\ExportConfig.java" "%SRC%\com\sonar\tools\util\SettingsManager.java" "%SRC%\com\sonar\tools\service\SonarApiClient.java" "%SRC%\com\sonar\tools\service\ReportExporter.java" "%SRC%\com\sonar\tools\ui\MainFrame.java" "%SRC%\com\sonar\tools\ui\ServerPanel.java" "%SRC%\com\sonar\tools\ui\ProjectPanel.java" "%SRC%\com\sonar\tools\ui\ExportPanel.java"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compile failed!
    pause
    exit /b 1
)

echo [INFO] Packaging JAR...
"%JAREXE%" cfe "%TARGET%\sonar-report-tool.jar" com.sonar.tools.App -C "%OUT%" .

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Package failed!
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Build complete: target\sonar-report-tool.jar
echo [INFO] Run start.bat to launch the tool
echo.
pause
