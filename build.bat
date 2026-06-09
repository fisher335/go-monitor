@echo off
chcp 65001 >nul
echo ============================
echo Go-Monitor 构建脚本
echo ============================

REM 1. 构建前端
echo [1/4] 构建 Vue 前端...
cd frontend
call npm install --silent
call npm run build
cd ..

REM 2. 复制到 Go 后端
echo [2/4] 复制到 Go 嵌入目录...
if exist go-backend\static rmdir /s /q go-backend\static
xcopy frontend\dist go-backend\static\ /e /i /q >nul

REM 3. 复制到 Python 后端
echo [3/4] 复制到 Python 静态目录...
if exist python-backend\static rmdir /s /q python-backend\static
xcopy frontend\dist python-backend\static\ /e /i /q >nul

REM 4. 编译 Go 后端
echo [4/4] 编译 Go 二进制...
cd go-backend
call go mod tidy
go build -o ..\bin\go-monitor.exe .
cd ..

echo ============================
echo 构建完成!
echo.
echo Go:     bin\go-monitor.exe       :9501
echo Python: python-backend\main.py   :9502
echo Java:   java-backend\go-monitor.jar :9500
echo.
echo 打开: http://localhost:9501 或 http://localhost:9502
echo ============================
