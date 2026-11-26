@echo off
title CodeMissions - demo server
cd /d "%~dp0.."

echo.
echo [1/3] Checking Docker...
docker info >nul 2>&1
if errorlevel 1 (
  echo.
  echo   Docker Desktop is not running.
  echo   Open Docker Desktop, wait until it says "Engine running", then run this file again.
  echo.
  pause
  exit /b 1
)
docker image inspect py-grader >nul 2>&1
if errorlevel 1 (
  echo   Building the grader image (only the first time, takes a minute)...
  docker build -t py-grader sandbox
)

echo.
echo [2/3] Type ONE of these addresses in the app (login screen, bottom link "serveur"):
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do echo        %%a
echo.
echo [3/3] Starting the API. Keep this window open during the demo.
echo.
cd api\CodeQuest.Api
dotnet run --urls http://0.0.0.0:5007
pause
