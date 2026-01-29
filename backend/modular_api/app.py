#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Flask应用主入口
使用模块化架构
"""

import os
import logging
from . import create_app
from .services.database import init_db

# 配置日志
def setup_logging():
    """配置日志系统"""
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

# 创建应用实例
app = create_app()

# 初始化数据库
init_db()

if __name__ == '__main__':
    setup_logging()
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