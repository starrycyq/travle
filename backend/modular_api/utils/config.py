"""
配置管理模块
统一管理环境变量和配置
"""

import os
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

class Config:
    """配置类"""

    # Flask配置
    FLASK_ENV = os.getenv('FLASK_ENV', 'development')
    FLASK_DEBUG = os.getenv('FLASK_DEBUG', '1') == '1'
    HOST = os.getenv('HOST', '0.0.0.0')
    PORT = int(os.getenv('PORT', 5000))

    # 数据库配置
    CHROMA_PATH = os.getenv('CHROMA_PATH', './chroma_db')
    DATABASE_PATH = os.getenv('DATABASE_PATH', './preferences.db')
    DATABASE_URL = os.getenv('DATABASE_URL')

    # 大模型API配置
    DEEPSEEK_API_KEY = os.getenv('DEEPSEEK_API_KEY', '')
    ALIYUN_DASHSCOPE_API_KEY = os.getenv('ALIYUN_DASHSCOPE_API_KEY', '')

    # Embedding模型配置
    EMBEDDING_MODEL_PATH = os.getenv(
        'EMBEDDING_MODEL_PATH',
        r'C:\Users\Administrator\.cache\huggingface\hub\models--sentence-transformers--all-MiniLM-L6-v2'
    )
    EMBEDDING_MODEL_NAME = os.getenv('EMBEDDING_MODEL_NAME', 'all-MiniLM-L6-v2')

    # 内网穿透配置
    NGROK_AUTH_TOKEN = os.getenv('NGROK_AUTH_TOKEN', '')
    NGROK_DOMAIN = os.getenv('NGROK_DOMAIN', '')

    # 安全配置
    SECRET_KEY = os.getenv('SECRET_KEY', 'your-secret-key-here')
    API_KEY = os.getenv('API_KEY', 'your-api-key-here')

    # 日志配置
    LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
    LOG_FILE = os.getenv('LOG_FILE', './logs/app.log')

    # 爬虫配置
    SCRAPER_HEADLESS = os.getenv('SCRAPER_HEADLESS', 'True') == 'True'
    SCRAPER_DELAY_MIN = float(os.getenv('SCRAPER_DELAY_MIN', '2'))
    SCRAPER_DELAY_MAX = float(os.getenv('SCRAPER_DELAY_MAX', '4'))
    SCRAPER_MAX_POSTS = int(os.getenv('SCRAPER_MAX_POSTS', '10'))

    # 小红书OAuth配置
    XIAOHONGSHU_CLIENT_ID = os.getenv('XIAOHONGSHU_CLIENT_ID', '')
    XIAOHONGSHU_CLIENT_SECRET = os.getenv('XIAOHONGSHU_CLIENT_SECRET', '')
    XIAOHONGSHU_OAUTH_URL = os.getenv('XIAOHONGSHU_OAUTH_URL', 'https://open.xiaohongshu.com/oauth/authorize')
    XIAOHONGSHU_TOKEN_URL = os.getenv('XIAOHONGSHU_TOKEN_URL', 'https://open.xiaohongshu.com/oauth/access_token')
    XIAOHONGSHU_VALIDATE_TOKEN_URL = os.getenv('XIAOHONGSHU_VALIDATE_TOKEN_URL', 'https://open.xiaohongshu.com/oauth/token/info')
    XIAOHONGSHU_OAUTH_ENABLED = os.getenv('XIAOHONGSHU_OAUTH_ENABLED', 'False') == 'True'

    # 云端服务器配置
    CLOUD_SERVER_IP = os.getenv('CLOUD_SERVER_IP', '121.43.58.117')
    CLOUD_SERVER_PORT = int(os.getenv('CLOUD_SERVER_PORT', '5000'))
    CLOUD_SERVER_USER = os.getenv('CLOUD_SERVER_USER', 'root')
    CLOUD_SERVER_PASSWORD = os.getenv('CLOUD_SERVER_PASSWORD', '')

    # 本地服务配置（用于转发授权信息）
    LOCAL_SERVICE_URL = os.getenv('LOCAL_SERVICE_URL', 'http://localhost:5001')
    LOCAL_SERVICE_AUTH_ENDPOINT = os.getenv('LOCAL_SERVICE_AUTH_ENDPOINT', '/api/auth/forward')
    LOCAL_SERVICE_TIMEOUT = int(os.getenv('LOCAL_SERVICE_TIMEOUT', '10'))

    # Redis缓存配置
    REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
    REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
    REDIS_PASSWORD = os.getenv('REDIS_PASSWORD', '')
    REDIS_DB = int(os.getenv('REDIS_DB', 0))
    REDIS_CACHE_TTL = int(os.getenv('REDIS_CACHE_TTL', 3600))  # 默认1小时

    # 限流配置
    RATE_LIMIT_DEFAULT = os.getenv('RATE_LIMIT_DEFAULT', '100 per minute')
    RATE_LIMIT_AUTH = os.getenv('RATE_LIMIT_AUTH', '5 per minute')
    RATE_LIMIT_SEARCH = os.getenv('RATE_LIMIT_SEARCH', '20 per minute')
    RATE_LIMIT_GUIDE = os.getenv('RATE_LIMIT_GUIDE', '10 per minute')
    RATE_LIMIT_ENABLED = os.getenv('RATE_LIMIT_ENABLED', 'True') == 'True'

    # 压缩配置
    COMPRESS_ENABLED = os.getenv('COMPRESS_ENABLED', 'True') == 'True'
    COMPRESS_MIN_SIZE = int(os.getenv('COMPRESS_MIN_SIZE', 500))
    COMPRESS_MIMETYPES = ['text/html', 'text/css', 'text/xml', 'application/json', 
                          'application/javascript', 'application/xml']

    @classmethod
    def init_app(cls, app):
        """初始化Flask应用配置"""
        app.config['SECRET_KEY'] = cls.SECRET_KEY
        app.config['DEBUG'] = cls.FLASK_DEBUG

        # 确保必要的目录存在
        Path(cls.LOG_FILE).parent.mkdir(parents=True, exist_ok=True)
        Path(cls.CHROMA_PATH).parent.mkdir(parents=True, exist_ok=True)

        return app

class DevelopmentConfig(Config):
    """开发环境配置"""
    DEBUG = True

class ProductionConfig(Config):
    """生产环境配置"""
    DEBUG = False

class TestingConfig(Config):
    """测试环境配置"""
    TESTING = True
    DATABASE_PATH = ':memory:'

# 配置字典
config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}