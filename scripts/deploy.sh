#!/bin/bash

# 云端部署脚本
# 用于在云端服务器上部署后端服务

set -e

echo "开始部署云端服务..."

# 更新系统包
echo "更新系统包..."
apt update && apt upgrade -y

# 安装必要依赖
echo "安装必要依赖..."
apt install -y python3-pip python3-venv git nginx certbot docker.io docker-compose

# 创建项目目录
PROJECT_DIR="/opt/travel-assistant"
if [ ! -d "$PROJECT_DIR" ]; then
    mkdir -p "$PROJECT_DIR"
fi

# 复制项目文件
echo "复制项目文件..."
cp -r backend/* "$PROJECT_DIR/"

# 创建虚拟环境
echo "创建Python虚拟环境..."
cd "$PROJECT_DIR"
python3 -m venv venv
source venv/bin/activate

# 安装依赖
echo "安装Python依赖..."
pip install -r requirements.txt

# 创建.env文件
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "请编辑 $PROJECT_DIR/.env 文件，填入必要的配置"
fi

# 创建必要的目录
mkdir -p logs data/raw data/processed

# 配置Nginx
echo "配置Nginx..."
cat > /etc/nginx/sites-available/travel-assistant <<EOF
server {
    listen 80;
    server_name 121.43.58.117;

    location / {
        proxy_pass http://localhost:5000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

ln -sf /etc/nginx/sites-available/travel-assistant /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx

# 创建systemd服务
echo "创建systemd服务..."
cat > /etc/systemd/system/travel-assistant.service <<EOF
[Unit]
Description=Travel Assistant Backend Service
After=network.target

[Service]
User=root
WorkingDirectory=$PROJECT_DIR
Environment="PATH=$PROJECT_DIR/venv/bin"
ExecStart=$PROJECT_DIR/venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
echo "启动服务..."
systemctl daemon-reload
systemctl enable travel-assistant
systemctl start travel-assistant

echo "部署完成！"
echo "服务状态："
systemctl status travel-assistant
