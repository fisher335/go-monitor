@echo off
setlocal enabledelayedexpansion

echo === Go-Monitor Python Backend ===
set "ROOT=%~dp0"
cd /d "%ROOT%"

REM 1. check venv
if not exist ".venv\Scripts\python.exe" (
    echo [1/3] Creating venv...
    python -m venv .venv
    if !errorlevel! neq 0 (
        echo Failed to create venv - need Python installed
        pause
        exit /b 1
    )
) else (
    echo [1/3] venv exists
)

REM 2. install deps
echo [2/3] Installing dependencies...
".venv\Scripts\python.exe" -m pip install -r requirements.txt -q
if !errorlevel! neq 0 ( echo pip install failed & pause & exit /b 1 )

REM 3. start
echo [3/3] Starting server...
echo.
echo Open browser: http://localhost:9502
echo Press Ctrl+C to stop
echo.
title Go-Monitor (Python)
".venv\Scripts\python.exe" -m uvicorn main:app --host 0.0.0.0 --port 9502

echo Server exited with code !errorlevel!
pause
endlocal
