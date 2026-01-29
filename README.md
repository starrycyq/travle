# Travle - 智能旅行助手

一个基于AI的智能旅行助手Android应用，提供旅行攻略、自驾游规划、AI聊天等功能。

## 项目架构

travle/
├── android-app/          # Android前端 (Jetpack Compose)
│   ├── app/              # 应用模块
│   │   ├── src/
│   │   │   ├── main/     # 主代码
│   │   │   │   ├── java/com/travle/app/
│   │   │   │   │   ├── data/           # 数据层
│   │   │   │   │   │   ├── api/        # API接口
│   │   │   │   │   │   ├── auth/       # 认证管理
│   │   │   │   │   │   ├── database/   # 本地数据库
│   │   │   │   │   │   ├── model/      # 数据模型
│   │   │   │   │   │   └── repository/ # 仓库层
│   │   │   │   │   └── ui/             # UI层
│   │   │   │   │       ├── screens/    # 页面
│   │   │   │   │       ├── theme/      # 主题
│   │   │   │   │       └── viewmodel/  # 视图模型
│   │   │   │   └── res/                # 资源
│   │   │   └── test/                   # 测试
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── backend/              # Flask后端 (云端部署)
│   ├── modular_api/      # 主应用
│   │   ├── routes/       # API路由
│   │   │   ├── auth.py           # 认证接口
│   │   │   ├── chat.py           # AI聊天
│   │   │   ├── community.py      # 社区功能
│   │   │   ├── guide.py          # 攻略功能
│   │   │   ├── main.py           # 基础接口
│   │   │   ├── preference.py     # 偏好管理
│   │   │   ├── roadtrip.py       # 自驾游规划
│   │   │   ├── search.py         # 搜索
│   │   │   └── xiaohongshu.py    # 小红书攻略
│   │   ├── services/      # 业务逻辑
│   │   │   ├── auth.py
│   │   │   ├── cache.py
│   │   │   ├── database.py
│   │   │   ├── model.py
│   │   │   ├── scraper.py
│   │   │   └── vector.py
│   │   ├── utils/         # 工具类
│   │   │   ├── config.py
│   │   │   ├── database_optimizer.py
│   │   │   ├── monitoring.py
│   │   │   └── validation.py
│   │   ├── app.py
│   │   ├── run_app.py
│   │   └── .env.example
│   ├── api_specs/         # API文档
│   │   └── travle_api.yaml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── requirements.txt
├── app-debug.apk         # Android安装包
└── zongjie.md            # 项目总结
```

## 技术栈

### 前端 (Android)

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp
- **数据库**: Room
- **构建工具**: Gradle 8.5

### 后端 (Flask)

- **语言**: Python 3.10+
- **Web框架**: Flask
- **数据库**: SQLite + ChromaDB (向量数据库)
- **缓存**: SQLite Cache
- **认证**: JWT
- **部署**: Docker

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
