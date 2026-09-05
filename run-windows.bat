@echo off
setlocal
if "%1"=="backend" goto backend
if "%1"=="frontend" goto frontend

echo NEXUS
 echo.
echo 1. Start backend:  run-windows.bat backend
 echo 2. Start frontend: run-windows.bat frontend
 echo 3. Or use Docker: docker compose up --build
 goto end

:backend
cd /d "%~dp0backend"
call mvn clean spring-boot:run
goto end

:frontend
cd /d "%~dp0frontend"
call npm install
call npm run dev
goto end
:end
