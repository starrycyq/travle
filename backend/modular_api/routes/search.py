"""
搜索路由模块
处理旅游攻略搜索相关的请求
"""

from flask import Blueprint, request, jsonify
import logging
import os
import sys

# 导入认证装饰器
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from services.auth import auth_required, optional_auth
from services.vector import get_vector_service

logger = logging.getLogger(__name__)

bp = Blueprint('search', __name__)

@bp.route('/guides', methods=['POST'])
@auth_required
def search_guides():
    """
    搜索旅游攻略
    根据关键词和过滤条件搜索旅游攻略
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求体为空',
                'error_code': 'EMPTY_REQUEST'
            }), 400

        query = data.get('query')
        filters = data.get('filters', {})
        limit = data.get('limit', 5)

        if not query:
            return jsonify({
                'status': 'error',
                'message': '缺少搜索关键词',
                'error_code': 'MISSING_QUERY'
            }), 400

        # 使用向量服务搜索相似攻略
        vector_service = get_vector_service()
        search_results = vector_service.search_similar(
            query=query,
            limit=limit,
            filters=filters
        )
        
        logger.info(f"搜索攻略: query={query}, filters={filters}, limit={limit}, 结果数={len(search_results['results'])}")
        
        return jsonify({
            'status': 'success',
            'data': search_results
        })

    except Exception as e:
        logger.error(f"搜索攻略失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': '搜索攻略失败',
            'error_code': 'SEARCH_ERROR'
        }), 500