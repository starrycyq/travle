# Travle - AI智能旅游攻略生成系统

Travle是一个基于AI的智能旅游攻略生成系统，采用本地+云端协同架构，通过爬取小红书等平台内容并结合大模型能力，为用户提供个性化、高质量的旅游推荐服务。

## 项目结构

```
travle/
├── android-app/              # Android原生应用
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/travle/app/    # 主要源代码
│   │   │   └── res/                    # 资源文件
│   │   └── build.gradle.kts             # 模块构建配置
│   ├── build.gradle.kts                  # 项目构建配置
│   └── settings.gradle.kts               # 项目设置
├── backend/                 # Python后端服务
│   ├── modular_api/         # 模块化API
│   │   ├── routes/          # API路由
│   │   ├── services/        # 业务服务
│   │   └── utils/           # 工具类
│   ├── requirements.txt     # Python依赖
│   ├── Dockerfile          # Docker配置
│   └── tests/              # 测试文件
├── api_gateway/            # API网关
├── scripts/                # 脚本文件
├── .github/workflows/      # CI/CD配置
│   └── full-cicd.yml       # 主要CI/CD流程
├── docs/                   # 文档
├── chroma_db/              # 向量数据库
├── DEPLOYMENT_CONFIG.md    # 部署配置说明
└── README.md               # 项目说明
```

## 技术栈

- **前端**: Jetpack Compose (Android), Retrofit, OkHttp, Hilt, Room
- **后端**: Flask + JWT, Python 3.8+
- **数据库**: ChromaDB（向量）、SQLite（关系型）
- **机器学习**: SentenceTransformer, Transformers, 阿里云通义千问API
- **爬虫**: Selenium + WebDriver
- **基础设施**: Docker, Nginx, ngrok/frp, Docker Compose

## 开发环境

- Python 3.8+
- Android Studio Arctic Fox+
- JDK 11+
- Chrome浏览器（爬虫依赖）
- Docker
- Nginx

## CI/CD

项目使用GitHub Actions实现完整的CI/CD流程，包括：

1. 后端（Python）自动化测试
2. Android前端自动化测试
3. Docker镜像构建
4. 自动部署到阿里云服务器

## 部署

部署到阿里云服务器（Ubuntu 22.04）需要配置以下GitHub Secrets：

- `SERVER_IP`: 服务器IP地址
- `SSH_PRIVATE_KEY`: SSH私钥
- `DEEPSEEK_API_KEY`: DeepSeek API密钥

详情请参考 [DEPLOYMENT_CONFIG.md](DEPLOYMENT_CONFIG.md)。

## 运行

### 后端服务

```bash
cd backend
pip install -r requirements.txt
python -m modular_api.run_app
```

### Android应用

```bash
cd android-app
./gradlew installDebug
```

## 功能特性

### 1. AI智能助手

- 自然语言对话
- 旅行问题解答
- 个性化推荐

### 2. 自驾游规划

- 起点终点路线规划
- 沿途景点推荐
- 路书生成

### 3. 旅行攻略

- 小红书攻略采集
- 攻略搜索与浏览
- 收藏与管理

### 4. 用户系统

- 注册/登录
- 偏好设置
- 收藏夹管理

## API接口

### 基础接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/config` | GET | 获取配置 |

### 认证接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 登出 |
| `/api/auth/profile` | GET | 获取信息 |
| `/api/auth/profile` | PUT | 更新信息 |
| `/api/auth/password` | PUT | 修改密码 |

### AI聊天接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/chat` | POST | 发送消息 |
| `/api/chat/history` | GET | 对话历史 |

### 自驾游接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/roadtrip` | POST | 路线规划 |

### 攻略接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/guide/list` | GET | 攻略列表 |
| `/api/guide/detail` | GET | 攻略详情 |

### 社区接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/community/notes` | GET | 笔记列表 |
| `/api/community/note/<id>` | GET | 笔记详情 |
| `/api/community/like` | POST | 点赞 |
| `/api/community/collect` | POST | 收藏 |
| `/api/community/collections` | GET | 收藏列表 |
| `/api/community/follow` | POST | 关注用户 |

### 搜索接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/search` | GET | 搜索 |

详细API文档: [api_specs/travle_api.yaml](backend/api_specs/travle_api.yaml)

## 快速开始

### 前端开发

1. 环境要求
   - JDK 17+
   - Android Studio
   - Android SDK

2. 构建命令

   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```

3. 安装APK

   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### 后端开发

1. 环境要求
   - Python 3.10+
   - pip

2. 安装依赖

   ```bash
   cd backend
   pip install -r requirements.txt
   ```

3. 配置环境变量

   ```bash
   cp backend/.env.example backend/.env
   # 编辑 .env 文件
   ```

4. 启动服务

   ```bash
   cd backend/modular_api
   python run_app.py
   ```

## Docker部署

### 构建镜像

```bash
cd backend
docker build -t travle-backend .
```

### 使用Compose启动

```bash
cd backend
docker-compose up -d
```

### 环境变量

在 `.env` 文件中配置：

- `SECRET_KEY`: JWT密钥
- `API_KEY`: API密钥
- `MODEL_NAME`: AI模型名称

## 项目历史

- **初始阶段**: 华为快应用开发
- **第一阶段**: Android原生应用 + 本地服务
- **第二阶段**: 云端服务迁移 (放弃内网穿透)
- **当前状态**: Android + 云端Flask完整架构

## 许可证

MIT License
