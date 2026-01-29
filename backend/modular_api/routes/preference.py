"""
偏好路由模块
处理用户偏好相关的请求
"""

from flask import Blueprint, request, jsonify
import sqlite3
import logging
import os

# 直接导入模块
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from services.database import get_db_connection
from services.auth import auth_required
from utils.monitoring import performance_monitor

logger = logging.getLogger(__name__)

bp = Blueprint('preference', __name__)

@bp.route('/input-preference', methods=['POST'])
@auth_required
@performance_monitor
def input_preference():
    """记录用户偏好
    记录用户的旅游目的地和偏好信息
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"status": "error", "message": "请求体为空"}), 400
            
        destination = data.get('destination')
        preferences = data.get('preferences')
        
        if not destination or not preferences:
            return jsonify({"status": "error", "message": "缺少目的地或偏好参数"}), 400
        
        conn = get_db_connection()
        c = conn.cursor()
        c.execute("INSERT INTO preferences (destination, preferences) VALUES (?, ?)", (destination, preferences))
        conn.commit()
        conn.close()
        
        return jsonify({"status": "success", "message": "偏好已记录"})
    except Exception as e:
        logger.error(f"记录用户偏好失败: {str(e)}")
        return jsonify({"status": "error", "message": str(e)}), 500