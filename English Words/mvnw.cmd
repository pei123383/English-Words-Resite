@echo off
setlocal

set MAVEN_VERSION=3.9.9
set MAVEN_DIR=%~dp0.mvn\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%~dp0.mvn\wrapper\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  if not exist "%MAVEN_ZIP%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%~dp0.mvn' -Force"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_DIR%\bin\mvn.cmd" %*
