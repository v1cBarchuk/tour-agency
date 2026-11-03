@echo off
rem Maven Wrapper for Windows: downloads Maven on first run, then runs it.
setlocal
set MAVEN_VERSION=3.9.11
set WRAPPER_DIR=%USERPROFILE%\.m2\wrapper
set MAVEN_HOME_DIR=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%

if not exist "%MAVEN_HOME_DIR%\bin\mvn.cmd" (
  echo Downloading Maven %MAVEN_VERSION% ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12;" ^
    "$u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip';" ^
    "$z=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%.zip';" ^
    "Invoke-WebRequest -Uri $u -OutFile $z;" ^
    "New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null;" ^
    "Expand-Archive -Path $z -DestinationPath '%WRAPPER_DIR%' -Force;" ^
    "Remove-Item $z"
  if errorlevel 1 (
    echo Failed to download Maven. Check the internet connection.
    exit /b 1
  )
)

call "%MAVEN_HOME_DIR%\bin\mvn.cmd" %*
