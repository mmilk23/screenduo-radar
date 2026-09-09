@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
pushd "%PROJECT_DIR%" > nul

if not "%GRAALVM_HOME%"=="" set "JAVA_HOME=%GRAALVM_HOME%"
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\dev\graalvm-jdk-24+36.1"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo java.exe was not found at "%JAVA_HOME%\bin\java.exe".
    echo Set JAVA_HOME to a JDK 24 installation and try again.
    popd > nul
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
call mvn package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\service\lib %*
if errorlevel 1 (
    popd > nul
    exit /b %errorlevel%
)

if not exist target\service mkdir target\service
copy /Y target\screenduo-radar-0.1.0-SNAPSHOT.jar target\service\screenduo-radar.jar > nul
if errorlevel 1 (
    popd > nul
    exit /b %errorlevel%
)
copy /Y deployment\windows\screenduo-radar-service.xml target\service\screenduo-radar-service.xml > nul
if errorlevel 1 (
    popd > nul
    exit /b %errorlevel%
)
if exist config.ini copy /Y config.ini target\service\config.ini > nul

echo Service payload prepared in target\service.
echo Add WinSW as target\service\screenduo-radar-service.exe before installing the service.
popd > nul
