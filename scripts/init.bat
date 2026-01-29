@echo off
REM 项目初始化脚本 (Windows版本)
REM 用于快速设置开发环境

echo 开始初始化项目...

REM 检查Python是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Python，请先安装Python 3.8+
    pause
    exit /b 1
)

REM 创建虚拟环境
echo 创建Python虚拟环境...
if not exist venv (
    python -m venv venv
)

REM 激活虚拟环境
echo 激活虚拟环境...
call venv\Scripts\activate.bat

REM 安装后端依赖
echo 安装后端依赖...
cd backend
pip install -r requirements.txt

REM 创建.env文件
echo 创建环境配置文件...
if not exist .env (
    copy .env.example .env
    echo 请编辑 backend\.env 文件，填入必要的配置
)

REM 创建必要的目录
echo 创建必要的目录...
if not exist logs mkdir logs
if not exist data\raw mkdir data\raw
if not exist data\processed mkdir data\processed
if not exist tests mkdir tests

REM 返回项目根目录
cd ..

echo 初始化完成！
echo.
echo 下一步操作：
echo 1. 编辑 backend\.env 文件，填入必要的配置
echo 2. 运行 backend\app.py 启动后端服务
echo 3. 运行 scripts\ngrok_start.py 启动内网穿透（可选）
echo 4. 使用Android Studio打开 android-app 目录，运行Android应用
echo.
pause
