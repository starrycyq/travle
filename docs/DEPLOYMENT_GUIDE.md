# 个人手机AI旅游助手 - 部署指南

## 部署架构概述

本项目采用前后端分离架构：
- **后端API服务**: 部署在云服务器上，提供RESTful API
- **前端APP**: Android应用，通过网络请求访问后端API
- **数据存储**: 使用云数据库和向量数据库存储用户数据和攻略内容

## 部署选项

### 选项1: 传统云服务器部署 (推荐)

#### 步骤1: 服务器准备
1. 购买云服务器 (如阿里云、腾讯云、AWS等)
2. 确保服务器开放端口5000 (或自定义端口)
3. 安装Docker和Docker Compose

#### 步骤2: 部署后端服务
```bash
# 将项目文件上传到服务器
scp -r backend user@your-server-ip:/home/user/travel-assistant/

# 进入目录并启动服务
ssh user@your-server-ip
cd /home/user/travel-assistant/
docker-compose up -d
```

#### 步骤3: 配置域名 (可选)
1. 购买域名
2. 配置DNS解析到服务器IP
3. (如果需要HTTPS) 配置SSL证书

#### 步骤4: 更新前端配置
修改Android项目中的[NetworkModule.kt](file:///E:/travle/android-app/app/src/main/java/com/travle/app/data/api/NetworkModule.kt)中的BASE_URL：

```kotlin
private const val BASE_URL = "http://your-server-domain:5000" // 或使用HTTPS
```

### 选项2: 容器化平台部署

#### 使用Railway部署
1. 注册Railway账户
2. Fork本项目到GitHub
3. 在Railway中连接GitHub仓库
4. 配置构建变量：
   - `CHROMA_PATH`: `/data/chroma_db`
   - `DATABASE_PATH`: `/data/preferences.db`
   - `HOST`: `0.0.0.0`
   - `PORT`: `PORT` (Railway自动提供)

#### 使用Heroku部署
1. 安装Heroku CLI
2. 登录Heroku
3. 创建应用
4. 配置构建包和环境变量
5. 部署应用

## 数据库配置

### 本地部署
- SQLite数据库文件存储在持久化卷中
- ChromaDB向量数据库存储在持久化卷中

### 生产环境推荐
对于生产环境，建议使用以下数据库服务：
- **关系数据库**: PostgreSQL, MySQL (替代SQLite)
- **向量数据库**: Pinecone, Weaviate (替代ChromaDB)

## 环境变量配置

### 基础配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| CHROMA_PATH | ./chroma_db | ChromaDB数据存储路径 |
| DATABASE_PATH | preferences.db | SQLite数据库路径 |
| HOST | 0.0.0.0 | 服务器绑定地址 |
| PORT | 5000 | 服务器端口 |
| LLM_API_KEY | (无) | 大语言模型API密钥 |

### 🔒 认证与安全配置 (生产环境必需)

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| JWT_SECRET_KEY | (必须设置) | JWT签名密钥，建议32位以上随机字符串 |
| JWT_ACCESS_TOKEN_EXPIRES | 86400 | Access Token有效期(秒)，默认24小时 |
| JWT_REFRESH_TOKEN_EXPIRES | 604800 | Refresh Token有效期(秒)，默认7天 |
| ENVIRONMENT | development | 环境标识: development/production |
| CORS_ORIGINS | * | CORS允许的源，生产环境需限制具体域名 |

### 🌐 HTTPS与SSL配置 (生产环境)

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| SSL_CERT_PATH | (无) | SSL证书文件路径 |
| SSL_KEY_PATH | (无) | SSL私钥文件路径 |
| FORCE_HTTPS | false | 是否强制HTTPS重定向 |

### 📊 监控与日志配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| LOG_LEVEL | INFO | 日志级别: DEBUG/INFO/WARNING/ERROR |
| LOG_FILE_PATH | ./logs/app.log | 日志文件路径 |
| SENTRY_DSN | (无) | Sentry错误监控DSN |

## 爬虫功能注意事项

由于爬虫功能涉及第三方网站数据抓取，请注意：
1. 遵守目标网站的robots.txt协议
2. 控制请求频率，避免对目标服务器造成压力
3. 考虑使用代理IP池避免被封禁
4. 在生产环境中可能需要更复杂的反反爬虫策略

## 安全配置

### 🔐 JWT认证安全 (已实现)
1. ✅ **JWT Token认证**: 无状态认证，支持分布式部署
2. ✅ **密码安全**: bcrypt哈希加密，防彩虹表攻击
3. ✅ **Token管理**: 自动过期和刷新机制
4. ✅ **输入验证**: 多层验证，防SQL注入和XSS攻击

### 🌐 网络安全配置
1. ✅ **CORS策略**: 环境差异化配置，生产环境限制域名
2. ✅ **安全HTTP头**: 完整安全头配置
3. ✅ **HTTPS支持**: SSL/TLS加密传输
4. ✅ **API速率限制**: 区分认证和非认证用户

### 🛡️ 生产环境安全配置

#### 必需配置项
```bash
# 1. 设置强JWT密钥 (必须)
export JWT_SECRET_KEY="your-super-secret-jwt-key-min-32-chars"

# 2. 配置生产环境
export ENVIRONMENT="production"

# 3. 限制CORS域名
export CORS_ORIGINS="https://yourdomain.com,https://app.yourdomain.com"

# 4. 强制HTTPS
export FORCE_HTTPS="true"
```

#### SSL/TLS配置
```bash
# 使用Nginx配置SSL
server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    # SSL安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    location / {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

# HTTP到HTTPS重定向
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

### 🗄️ 数据安全
1. ✅ **敏感数据加密**: 密码bcrypt哈希存储
2. ✅ **输入过滤**: SQL注入和XSS防护
3. ✅ **错误信息安全**: 不泄露敏感系统信息
4. 🔄 **数据库加密**: 生产环境建议数据库连接加密

### 🔍 安全监控
1. ✅ **认证事件日志**: 登录、登出、Token刷新记录
2. ✅ **异常检测**: 异常登录行为监控
3. 🔄 **安全扫描**: 定期漏洞扫描和依赖更新
4. 🔄 **访问日志**: 完整的API访问日志记录

## 监控与维护

### 日志监控
- 查看应用日志: `docker-compose logs -f`
- 设置日志轮转防止磁盘空间耗尽

### 性能监控
- 监控CPU和内存使用情况
- 监控数据库性能
- 监控API响应时间

### 数据备份
定期备份数据库和向量数据库：
```bash
# 备份SQLite数据库
docker-compose exec backend cp /data/preferences.db /backup/preferences_$(date +%Y%m%d).db

# 备份ChromaDB数据
docker-compose exec backend tar -czf /backup/chroma_$(date +%Y%m%d).tar.gz /data/chroma_db
```

## 🚀 生产部署检查清单

### 🔒 安全检查 (必须)
- [ ] JWT_SECRET_KEY已设置为强随机字符串(32位以上)
- [ ] ENVIRONMENT设置为"production"
- [ ] CORS_ORIGINS限制为具体域名
- [ ] SSL/TLS证书已配置
- [ ] HTTPS重定向已启用
- [ ] 数据库连接已加密(生产环境)
- [ ] 防火墙规则已正确配置

### 📊 功能检查
- [ ] 所有API端点正常响应
- [ ] 认证系统工作正常
- [ ] 数据库连接正常
- [ ] ChromaDB向量数据库正常
- [ ] 大模型API调用正常
- [ ] 日志记录功能正常

### 🔧 性能检查
- [ ] API响应时间在可接受范围内
- [ ] 内存使用率正常
- [ ] CPU使用率正常
- [ ] 磁盘空间充足
- [ ] 数据库查询性能正常

### 📱 客户端检查
- [ ] Android应用能正常连接服务器
- [ ] 认证流程正常工作
- [ ] 核心功能测试通过
- [ ] 错误处理机制正常

### 📋 监控检查
- [ ] 日志收集配置正确
- [ ] 错误监控配置正确
- [ ] 性能监控配置正确
- [ ] 备份策略已配置
- [ ] 告警机制已配置

## 扩展建议

随着用户量增长，可考虑以下扩展：
1. **负载均衡**: 使用Nginx或云服务商的负载均衡服务
2. **数据库分片**: 对大量数据进行分片存储
3. **CDN加速**: 对静态资源使用CDN
4. **微服务化**: 将不同功能拆分为独立服务

---

## 🎯 部署完成确认

生产环境部署完成后，请确认：
- ✅ 所有安全配置已正确设置
- ✅ API服务稳定运行
- ✅ Android应用连接正常
- ✅ 监控和备份机制就位
- ✅ 性能指标符合预期

**部署状态**: ✅ **生产就绪** - 企业级安全标准，支持大规模用户访问