"""
Flask应用初始化模块
采用应用工厂模式创建应用实例
"""

from flask import Flask
from flask_cors import CORS
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from flask_compress import Compress
from .utils.config import Config

# 全局limiter实例，将在create_app中初始化
limiter = None

def create_app(config_name='default'):
    """应用工厂函数"""
    app = Flask(__name__)

    # 加载配置
    app.config.from_object(Config)

    # 启用CORS
    CORS(app)

    # 初始化请求限流
    global limiter
    if Config.RATE_LIMIT_ENABLED:
        limiter = Limiter(
            get_remote_address,
            app=app,
            default_limits=[Config.RATE_LIMIT_DEFAULT],
            storage_uri=f"redis://{Config.REDIS_HOST}:{Config.REDIS_PORT}/{Config.REDIS_DB}"
        )
        # 存储limiter实例以便在蓝图中使用
        app.limiter = limiter
        print(f"[限流] 启用请求限流，默认限制: {Config.RATE_LIMIT_DEFAULT}")
    else:
        limiter = None
        print("[限流] 请求限流已禁用")

    # 初始化响应压缩
    if Config.COMPRESS_ENABLED:
        compress = Compress(app)
        print(f"[压缩] 启用响应压缩，最小大小: {Config.COMPRESS_MIN_SIZE}字节")
    else:
        print("[压缩] 响应压缩已禁用")

    # 初始化缓存服务
    from .services.cache import cache
    cache.init_app(app)

    # 初始化监控系统
    from .utils.monitoring import init_monitoring
    init_monitoring(app)

    # 注册蓝图
    register_blueprints(app)

    # 注册错误处理器
    register_error_handlers(app)

    return app

def register_blueprints(app):
    """注册蓝图"""
    from .routes.main import bp as main_bp
    from .routes.preference import bp as preference_bp
    from .routes.guide import bp as guide_bp
    from .routes.community import bp as community_bp
    from .routes.auth import bp as auth_bp
    from .routes.search import bp as search_bp
    from .routes.chat import bp as chat_bp
    from .routes.roadtrip import bp as roadtrip_bp

    app.register_blueprint(main_bp)
    app.register_blueprint(preference_bp)
    app.register_blueprint(guide_bp)
    app.register_blueprint(community_bp)
    app.register_blueprint(auth_bp, url_prefix='/api/auth')
    app.register_blueprint(search_bp, url_prefix='/api/search')
    app.register_blueprint(chat_bp, url_prefix='/api')
    app.register_blueprint(roadtrip_bp, url_prefix='/api')

def register_error_handlers(app):
    """注册错误处理器"""
    from flask import jsonify

    @app.errorhandler(400)
    def bad_request(error):
        return jsonify({
            'status': 'error',
            'message': 'Bad Request',
            'error_code': 'BAD_REQUEST'
        }), 400

    @app.errorhandler(401)
    def unauthorized(error):
        return jsonify({
            'status': 'error',
            'message': 'Unauthorized',
            'error_code': 'UNAUTHORIZED'
        }), 401

    @app.errorhandler(404)
    def not_found(error):
        return jsonify({
            'status': 'error',
            'message': 'Not Found',
            'error_code': 'NOT_FOUND'
        }), 404

    @app.errorhandler(500)
    def internal_error(error):
        return jsonify({
            'status': 'error',
            'message': 'Internal Server Error',
            'error_code': 'INTERNAL_ERROR'
        }), 500