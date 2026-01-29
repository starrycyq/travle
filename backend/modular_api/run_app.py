#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Flask应用启动脚本
使用模块化架构
"""

import os
import sys
import logging

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from flask import Flask, request, make_response
from flask_cors import CORS

# 导入配置
from utils.config import Config


def create_app(config_name='default'):
    """应用工厂函数"""
    import logging
    logger = logging.getLogger(__name__)
    logger.info(">>> 进入 create_app")
    app = Flask(__name__)

    # 加载配置
    app.config.from_object(Config)
    logger.info(">>> 配置加载完成")

    # 启用CORS（生产环境安全配置）
    if app.config.get('FLASK_ENV') == 'production':
        # 生产环境：限制特定域名
        CORS(app, 
             resources={
                 r"/*": {
                     "origins": ["https://yourdomain.com", "https://www.yourdomain.com"],
                     "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
                     "allow_headers": ["Content-Type", "Authorization", "X-Requested-With"],
                     "supports_credentials": True,
                     "max_age": 86400  # 24小时
                 }
             })
    else:
        # 开发环境：允许所有域名
        CORS(app, 
             resources={
                 r"/*": {
                     "origins": ["*"],
                     "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
                     "allow_headers": ["Content-Type", "Authorization", "X-Requested-With"],
                     "supports_credentials": True,
                     "max_age": 86400
                 }
             })

    # 注册安全头中间件
    # register_security_middleware(app)  # 暂时禁用，排查问题

    logger.info(">>> 准备注册蓝图")
    register_blueprints(app)
    logger.info(">>> 蓝图注册完成")

    # 注册错误处理器
    register_error_handlers(app)

    return app


def register_blueprints(app):
    """注册蓝图"""
    import logging
    logger = logging.getLogger(__name__)
    logger.info("开始注册蓝图...")

    try:
        import routes.main
        import routes.preference
        import routes.guide
        import routes.community
        import routes.auth
        import routes.search
        import routes.xiaohongshu  # 小红书蓝图
        import routes.chat        # AI聊天蓝图
        import routes.roadtrip    # 自驾游蓝图
    except Exception as e:
        logger.error(f"导入蓝图模块失败: {e}")
        raise

    # 注册蓝图并记录日志
    app.register_blueprint(routes.main.bp)
    app.register_blueprint(routes.preference.bp)
    app.register_blueprint(routes.guide.bp)
    app.register_blueprint(routes.community.bp, url_prefix='/community')
    app.register_blueprint(routes.auth.bp, url_prefix='/api/auth')
    app.register_blueprint(routes.search.bp, url_prefix='/api/search')
    app.register_blueprint(routes.chat.bp, url_prefix='/api')
    app.register_blueprint(routes.roadtrip.bp, url_prefix='/api')

    try:
        app.register_blueprint(routes.xiaohongshu.xiaohongshu_bp, url_prefix='/api/xiaohongshu')
        logger.info("成功注册小红书蓝图 /api/xiaohongshu")
    except Exception as e:
        logger.error(f"注册小红书蓝图失败: {e}")


def register_security_middleware(app):
    """注册安全头中间件"""
    @app.before_request
    def security_headers():
        # 跳过静态文件和健康检查
        if request.endpoint and request.endpoint.startswith('static'):
            return None
        
        response = None
        if request.endpoint:
            response = make_response()
        
        # 添加安全头
        security_headers = {
            'X-Content-Type-Options': 'nosniff',
            'X-Frame-Options': 'DENY',
            'X-XSS-Protection': '1; mode=block',
            'Referrer-Policy': 'strict-origin-when-cross-origin',
            'Content-Security-Policy': (
                "default-src 'self'; "
                "script-src 'self' 'unsafe-inline'; "
                "style-src 'self' 'unsafe-inline'; "
                "img-src 'self' data: https:; "
                "font-src 'self'; "
                "connect-src 'self'"
            ),
            'Strict-Transport-Security': 'max-age=31536000; includeSubDomains'
        }
        
        # 只在响应对象存在时设置头
        if response is not None:
            for header, value in security_headers.items():
                response.headers[header] = value
            return response
        
        return None


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


# 创建应用实例
app = create_app()

# 初始化数据库
from services.database import init_db
init_db()


if __name__ == '__main__':
    # 配置日志
    log_level = getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper())
    log_format = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'

    # 确保日志目录存在
    log_file = os.getenv('LOG_FILE', './logs/app.log')
    log_dir = os.path.dirname(log_file)
    if log_dir and not os.path.exists(log_dir):
        os.makedirs(log_dir)

    # 配置日志处理器
    handlers = [
        logging.StreamHandler(),  # 控制台输出
        logging.FileHandler(log_file, encoding='utf-8')  # 文件输出
    ]

    logging.basicConfig(
        level=log_level,
        format=log_format,
        handlers=handlers
    )

    logger = logging.getLogger(__name__)

    HOST = os.getenv('HOST', '0.0.0.0')
    PORT = int(os.getenv('PORT', 5000))
    
    logger.info(f"启动Flask应用...")
    logger.info(f"环境: {os.getenv('FLASK_ENV', 'development')}")
    logger.info(f"监听地址: {HOST}:{PORT}")

    app.run(
        host=HOST,
        port=PORT,
        debug=os.getenv('FLASK_DEBUG', 'False').lower() == 'true'
    )