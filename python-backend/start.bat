@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================
echo Go-Monitor Python 启动脚本
echo ============================

set "ROOT=%~dp0"
cd /d "%ROOT%"

REM 1. 检查虚拟环境
if not exist ".venv\Scripts\python.exe" (
    echo [1/3] 创建虚拟环境...
    python -m venv .venv
    if !errorlevel! neq 0 (
        echo 创建失败，请确认 Python 已安装
        pause
        exit /b 1
    )
) else (
    echo [1/3] 虚拟环境已存在
)

REM 2. 安装依赖
echo [2/3] 安装依赖...
".venv\Scripts\python.exe" -m pip install -r requirements.txt -q
if !errorlevel! neq 0 (
    echo 依赖安装失败
    pause
    exit /b 1
)

REM 3. 启动服务
echo [3/3] 启动服务...
echo.
echo 打开浏览器访问: http://localhost:9502
echo 按 Ctrl+C 停止服务
echo.
title Go-Monitor (Python)
".venv\Scripts\python.exe" -m uvicorn main:app --host 0.0.0.0 --port 9502

if !errorlevel! neq 0 (
    echo 服务异常退出，错误码: !errorlevel!
    pause
)

endlocal
