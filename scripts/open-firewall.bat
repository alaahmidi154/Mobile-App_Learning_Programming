@echo off
net session >nul 2>&1
if errorlevel 1 (
  echo.
  echo   Right-click this file and choose "Run as administrator".
  echo.
  pause
  exit /b 1
)
netsh advfirewall firewall delete rule name="CodeMissions API 5007" >nul 2>&1
netsh advfirewall firewall add rule name="CodeMissions API 5007" dir=in action=allow protocol=TCP localport=5007 profile=any
echo.
echo   Done: phones on the same Wi-Fi can now reach the API (port 5007).
echo.
pause
