# Travle 项目一键部署指南（云端 API 转发架构）

## 架构说明
- **云端服务器**（阿里云 `121.43.58.117`）：仅部署轻量 API 网关，接收 APP 请求并转发到本地后端。
- **本地后端**：运行爬虫、清洗、向量化、大模型调用等核心业务，保持在内网。
- **通信方式**：通过本地公网 IP 直连（若网络可达），否则用内网穿透（ngrok / frp）。

---

## 第一步：本地后端准备

1. 确保本地后端可在 `http://<本地内网或公网IP>:5000` 访问（例如 `192.168.1.100:5000` 或公网 IP）。
2. 在 `backend/modular_api/utils/config.py` 或主入口添加 IP 白名单，允许云服务器 IP：
```python
ALLOWED_IPS = {"127.0.0.1", "::1", "121.43.58.117"}

@app.before_request
def limit_remote_addr():
    if request.remote_addr not in ALLOWED_IPS:
        abort(403)
```
3. 启动本地后端：
```bash
cd e:/travle/backend
source venv/bin/activate   # 若用虚拟环境
python -m modular_api.run_app
```

---

## 第二步：云端 API 网关部署（root 用户）

### 2.1 上传网关代码
```bash
ssh root@121.43.58.117
mkdir -p /root/api_gateway
cd /root/api_gateway
# 用 scp 上传 gateway.py、requirements.txt、.env.example、start.sh、travle_gateway.service
```

### 2.2 安装依赖
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 2.3 配置环境变量
```bash
cp .env.example .env
nano .env
```
修改：
```
LOCAL_BACKEND_URL=http://<本地内网或公网IP>:5000
```
例如：
```
LOCAL_BACKEND_URL=http://123.45.67.89:5000
```

### 2.4 配置 Systemd 保活
将 `travle_gateway.service` 复制到系统目录：
```bash
cp travle_gateway.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable travle_gateway
systemctl start travle_gateway
```

### 2.5 验证网关
```bash
curl http://121.43.58.117:5000/
# 返回 Gateway OK 表示正常
```

---

## 第三步：网络直连（优先，无内网穿透）
若本地有公网 IP 或路由器已映射 5000 端口，则云端直接访问该 IP:5000，无需内网穿透。

仅在本地无公网 IP 时才考虑：
### 方案 A：ngrok（快速测试）
1. 本地运行：
```cmd
ngrok http 5000
```
2. 获取 https 公网地址，例如 `https://abc123.ngrok.io`。
3. 在云端 `.env` 中修改：
```
LOCAL_BACKEND_URL=https://abc123.ngrok.io
```

### 方案 B：frp（稳定生产）
1. 云端运行 frps（配置文件 `frps.ini`）：
```ini
[common]
bind_port = 7000
```
2. 本地运行 frpc（配置文件 `frpc.ini`）：
```ini
[common]
server_addr = 121.43.58.117
server_port = 7000

[backend_http]
type = http
local_ip = 127.0.0.1
local_port = 5000
custom_domains = travle.local
```
3. 云端 `.env`：
```
LOCAL_BACKEND_URL=http://travle.local:5000
```

---

## 第四步：测试完整链路

1. APP 或 Postman 请求：
```
POST http://121.43.58.117:5000/api/auth/xiaohongshu/send_code
```
2. 应返回正常，且本地后端收到请求并完成验证码发送。
3. 后续登录、爬取等同理测试。

---

## 常见问题
- **连接超时**：检查本地防火墙、路由器端口映射、云服务器安全组是否开放 5000 端口。
- **403 拒绝**：确认 `ALLOWED_IPS` 包含云服务器 IP。
- **权限问题**：所有路径与服务均已适配 root 用户。

---

完成以上步骤后，即实现 **云端 API 转发 + 本地业务处理** 的稳定架构，可按阶段二进行真实业务流程测试。