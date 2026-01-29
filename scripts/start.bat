@echo off
REM 快速启动脚本
REM 用于一键启动本地开发环境

echo ========================================
echo 旅游助手 - 本地开发环境启动脚本
echo ========================================
echo.

REM 检查虚拟环境
if not exist venv (
    echo 错误: 未找到虚拟环境，请先运行 scripts\init.bat 初始化项目
    pause
    exit /b 1
)

REM 激活虚拟环境
echo 激活虚拟环境...
call venv\Scripts\activate.bat

REM 启动后端服务
echo.
echo 启动后端服务...
cd backend
start "Travel Assistant Backend" python app.py

REM 等待后端服务启动
timeout /t 3 /nobreak >nul

REM 询问是否启动内网穿透
echo.
set /p START_NGROK="是否启动内网穿透? (y/n): "
if /i "%START_NGROK%"=="y" (
    echo 启动内网穿透...
    start "Ngrok Tunnel" python ..\scripts\ngrok_start.py
)

echo.
echo ========================================
echo 本地开发环境已启动！
echo ========================================
echo.
echo 后端服务地址: http://localhost:5000
echo.
echo 按任意键关闭此窗口（不会停止服务）
pause >nul
