@echo off
set VERSION=8.10.2
set DIST=%USERPROFILE%\.gradle\wrapper\dists\gradle-%VERSION%-bin
set INSTALL=%DIST%\gradle-%VERSION%
if not exist "%INSTALL%\bin\gradle.bat" (
  if not exist "%DIST%" mkdir "%DIST%"
  powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%DIST%\gradle.zip'"
  powershell -NoProfile -Command "Expand-Archive -Force '%DIST%\gradle.zip' '%DIST%'"
)
call "%INSTALL%\bin\gradle.bat" %*
