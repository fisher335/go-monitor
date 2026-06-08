@echo off
chcp 65001 >nul
echo ============================
echo Go-Monitor 构建脚本
echo ============================

REM 1. 构建前端
echo [1/3] 构建 Vue 前端...
cd frontend
call npm install --silent
call npm run build
cd ..

REM 2. 复制到 Go 静态目录
echo [2/3] 复制到 Go 嵌入目录...
if exist go-backend\static rmdir /s /q go-backend\static
xcopy frontend\dist go-backend\static\ /e /i /q >nul

REM 3. 编译 Go 后端
echo [3/3] 编译 Go 二进制...
cd go-backend
go mod tidy
go build -o ..\bin\go-monitor.exe .
cd ..

echo ============================
echo ✅ 构建完成!
echo 运行: bin\go-monitor.exe
echo 打开: http://localhost:9501
echo ============================
