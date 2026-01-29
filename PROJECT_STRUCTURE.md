# 项目结构说明（清理后）

```
e:/travle/
├── backend/                 # Flask 后端主目录
│   ├── app/                 # 应用核心
│   │   ├── routes/          # API 路由（auth.py, guides.py, search.py）
│   │   ├── services/        # 业务逻辑（auth, scraper, vector, database, model）
│   │   └── utils/           # 工具类（config, validation, monitoring 等）
│   ├── api_specs/           # OpenAPI 规范文件
│   ├── data/                # 原始与处理数据
│   ├── modular_api/         # 模块化重构后的主程序
│   ├── scripts/             # 部署与启动脚本
│   ├── tests/               # 测试文件
│   ├── requirements.txt     # Python 依赖
│   ├── .env.example         # 环境变量示例
│   └── Dockerfile / docker-compose.yml
├── docs/                    # 项目文档
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── DEPLOYMENT_GUIDE.md
│   ├── PROJECT_ROADMAP.md   # 四阶段开发计划
│   └── ...
├── chroma_db/               # ChromaDB 向量库数据（运行时生成）
├── NEXT_STEPS.md            # 后续步骤提示
├── openapi-generator-cli.jar
├── preferences.db           # 主数据库文件
├── PROJECT_STRUCTURE.md
├── README.md
└── scripts/                 # 全局辅助脚本（deploy.bat/sh, init.bat 等）
```

> 已清理：`.opencode/`, `.pytest_cache/`, `android-app/build_errors_full.txt`, `build/`, `logs/`, `backend/generated_server/`, `backend/new_generated_server/`, `backend/venv/`, 旧备份 DB 文件。

此结构便于云端部署与持续开发。