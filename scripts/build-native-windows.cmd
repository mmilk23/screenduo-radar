@echo off
setlocal

if not "%GRAALVM_HOME%"=="" set "JAVA_HOME=%GRAALVM_HOME%"
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\dev\graalvm-jdk-25.0.4+7.1"

if not exist "%JAVA_HOME%\bin\native-image.cmd" (
    echo GraalVM native-image was not found at "%JAVA_HOME%\bin\native-image.cmd".
    echo Set GRAALVM_HOME to a GraalVM JDK 25 installation and try again.
    exit /b 1
)

if "%VCVARS64%"=="" (
    set "VCVARS64=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
)

if not exist "%VCVARS64%" (
    set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
    if exist "%VSWHERE%" (
        for /f "usebackq tokens=*" %%i in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -find VC\Auxiliary\Build\vcvars64.bat`) do set "VCVARS64=%%i"
    )
)

if not exist "%VCVARS64%" (
    echo Visual Studio C++ x64 build tools vcvars64.bat was not found.
    echo Install the Visual Studio C++ x64 build tools or set VCVARS64 to vcvars64.bat.
    exit /b 1
)

call "%VCVARS64%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call mvn -Pnative package %*
