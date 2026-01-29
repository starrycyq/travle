@echo off
REM 云端部署脚本 (Windows版本)
REM 用于将本地代码打包并上传到云端服务器

echo 开始部署云端服务...

REM 检查必要工具
echo 检查必要工具...
where scp >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到scp命令，请确保已安装OpenSSH客户端
    pause
    exit /b 1
)

where ssh >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到ssh命令，请确保已安装OpenSSH客户端
    pause
    exit /b 1
)

REM 配置服务器信息
set SERVER_IP=121.43.58.117
set SERVER_USER=root
set SERVER_PASSWORD=cyq197346825@
set REMOTE_DIR=/opt/travel-assistant

REM 打包后端代码
echo 打包后端代码...
if exist deploy_temp rmdir /S /Q deploy_temp
mkdir deploy_temp
xcopy /E /I /Y backend deploy_temp\backend
xcopy /Y scripts deploy_temp\scripts

REM 上传到服务器
echo 上传到服务器...
scp -r deploy_temp\* %SERVER_USER%@%SERVER_IP%:%REMOTE_DIR%/

REM 在服务器上执行部署
echo 在服务器上执行部署...
ssh %SERVER_USER%@%SERVER_IP% "cd %REMOTE_DIR% && bash scripts/deploy.sh"

REM 清理临时文件
echo 清理临时文件...
rmdir /S /Q deploy_temp

echo 部署完成！
pause
