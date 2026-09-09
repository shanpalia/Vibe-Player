@echo off
setlocal
set "GRADLE_VERSION=9.3.1"
set "WRAPPER_DIR=%USERPROFILE%\.gradle\wrapper\dists\vibe-player-gradle-%GRADLE_VERSION%"
set "GRADLE_HOME=%WRAPPER_DIR%\gradle-%GRADLE_VERSION%"
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
set "ZIP=%WRAPPER_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
if not exist "%ZIP%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%WRAPPER_DIR%'"
:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
