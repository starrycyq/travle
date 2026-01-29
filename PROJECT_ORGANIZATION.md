# 项目结构组织

## 目录结构说明

项目经过整理，现在具有更清晰的结构：

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
├── docs/                   # 文档目录
├── chroma_db/              # 向量数据库
├── PROJECT_ORGANIZATION.md # 项目结构说明
└── README.md               # 项目总览
```

## 部署配置说明

### GitHub Secrets 配置

要在GitHub Actions中部署到您的阿里云服务器，需要配置以下Secrets：

1. 访问您的GitHub仓库
2. 点击 "Settings" 选项卡
3. 在左侧菜单中选择 "Secrets and variables"，然后点击 "Actions"
4. 点击 "New repository secret" 按钮，添加以下密钥：

#### 必需的Secrets

- `SERVER_IP`: `121.43.58.117` (您的阿里云服务器公网IP)
- `SSH_PRIVATE_KEY`: 您的SSH私钥内容
- `DEEPSEEK_API_KEY`: `sk-10de22349e464437b9dd6aac6fb4e803`

#### 如何获取SSH私钥

如果您使用的是Linux/Mac系统：
```bash
cat ~/.ssh/id_rsa
```

如果您使用的是Windows系统（通过Git Bash）：
```bash
cat ~/.ssh/id_rsa
```

或者，您需要将您提供的公钥对应的私钥内容复制到这里。

#### 如何添加Secret

1. 在GitHub仓库页面，点击"Settings"选项卡
2. 在左侧菜单中选择"Secrets and variables"，然后点击"Actions"
3. 点击"New repository secret"按钮
4. 在"Name"字段中输入上面的名称之一（例如：SERVER_IP）
5. 在"Secret"字段中粘贴相应的值
6. 点击"Add secret"按钮

## 部署流程

1. 当代码推送到master分支时，CI/CD流程将：
   - 运行后端和Android测试
   - 构建Docker镜像
   - 将镜像部署到阿里云服务器
   - 重启服务

2. 当代码推送到develop分支时，仅运行测试，不进行部署

## 服务器配置要求

您的阿里云服务器需要预先安装Docker：

```bash
# 更新包列表
sudo apt update

# 安装Docker
sudo apt install docker.io

# 启动Docker服务
sudo systemctl start docker

# 设置Docker开机自启
sudo systemctl enable docker

# 将当前用户添加到docker组（可选，如果不使用root用户）
sudo usermod -aG docker ${USER}
```

## 故障排除

如果部署失败，请检查：
1. 确保GitHub Secrets已正确设置
2. 确保SSH密钥配置正确，可以从GitHub连接到服务器
3. 确保服务器上Docker已正确安装并正在运行
4. 检查服务器防火墙设置，确保端口5000可用