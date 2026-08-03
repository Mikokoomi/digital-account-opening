@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\.mvn\wrapper\maven-wrapper.ps1" %*
exit /b %ERRORLEVEL%
