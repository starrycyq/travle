"""
社区路由模块
处理社区功能相关的请求
"""

from flask import Blueprint, request, jsonify
import sqlite3
import random
import string
import logging

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from services.database import get_db_connection
from services.auth import auth_required, optional_auth
from utils.monitoring import performance_monitor

logger = logging.getLogger(__name__)

bp = Blueprint('community', __name__)

@bp.route('/publish', methods=['POST'])
@auth_required
@performance_monitor
def community_publish():
    """
    发布社区动态
    发布新的社区旅游动态
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"code": 400, "msg": "请求体为空"}), 400
            
        content = data.get('content')
        if not content:
            return jsonify({"code": 400, "msg": "内容不能为空"}), 400
        
        # 预留扩展：目的地字段
        destination = data.get('destination', '')
        
        # 生成匿名标识（预留扩展：后续改为用户ID）
        anonymous_id = ''.join(random.choices(string.ascii_letters + string.digits, k=16))
        
        conn = get_db_connection()
        c = conn.cursor()
        c.execute("INSERT INTO community_post (content, destination, like_count, anonymous_id, images) VALUES (?, ?, ?, ?, ?)",
                  (content, destination, 0, anonymous_id, ''))
        post_id = c.lastrowid
        conn.commit()
        conn.close()
        
        return jsonify({
            "code": 200,
            "msg": "发布成功",
            "data": {
                "post_id": post_id,
                "anonymous_id": anonymous_id
            }
        })
    except Exception as e:
        logger.error(f"发布社区动态失败: {str(e)}")
        return jsonify({"code": 500, "msg": str(e)}), 500

@bp.route('/list', methods=['GET'])
@optional_auth
@performance_monitor
def community_list():
    """
    获取社区动态列表
    获取社区发布的旅游动态列表
    """
    try:
        page = request.args.get('page', 1, type=int)
        limit = request.args.get('limit', 20, type=int)
        
        conn = get_db_connection()
        c = conn.cursor()
        # 按创建时间倒序排序（预留扩展：分页、筛选）
        offset = (page - 1) * limit
        c.execute("SELECT id, content, like_count, create_time FROM community_post ORDER BY create_time DESC LIMIT ? OFFSET ?", (limit, offset))
        rows = c.fetchall()
        conn.close()
        
        posts = []
        for row in rows:
            posts.append({
                "id": row[0],
                "content": row[1],
                "like_count": row[2],
                "create_time": row[3]
            })
        
        return jsonify({
            "code": 200,
            "data": posts
        })
    except Exception as e:
        logger.error(f"获取社区列表失败: {str(e)}")
        return jsonify({"code": 500, "msg": str(e)}), 500

@bp.route('/like', methods=['POST'])
@auth_required
def community_like():
    """
    点赞社区动态
    为指定的社区动态点赞
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"code": 400, "msg": "请求体为空"}), 400
            
        post_id = data.get('post_id')
        if not post_id:
            return jsonify({"code": 400, "msg": "参数错误"}), 400
        
        conn = get_db_connection()
        c = conn.cursor()
        # 预留扩展：重复点赞校验、用户ID记录
        c.execute("UPDATE community_post SET like_count = like_count + 1 WHERE id = ?", (post_id,))
        conn.commit()
        conn.close()
        
        return jsonify({
            "code": 200,
            "msg": "点赞成功"
        })
    except Exception as e:
        logger.error(f"点赞社区动态失败: {str(e)}")
        return jsonify({"code": 500, "msg": str(e)}), 500

# 新增评论功能
@bp.route('/<int:post_id>/comments', methods=['GET'])
@optional_auth
@performance_monitor
def get_comments(post_id):
    """
    获取某个帖子的评论列表
    """
    try:
        page = request.args.get('page', 1, type=int)
        limit = request.args.get('limit', 20, type=int)
        
        conn = get_db_connection()
        c = conn.cursor()
        # 按创建时间倒序排序
        offset = (page - 1) * limit
        c.execute("""SELECT id, post_id, content, author_name, create_time, like_count 
                     FROM community_comment 
                     WHERE post_id = ? 
                     ORDER BY create_time DESC LIMIT ? OFFSET ?""", (post_id, limit, offset))
        rows = c.fetchall()
        conn.close()
        
        comments = []
        for row in rows:
            comments.append({
                "id": row[0],
                "post_id": row[1],
                "content": row[2],
                "author_name": row[3],
                "create_time": row[4],
                "like_count": row[5]
            })
        
        return jsonify({
            "code": 200,
            "data": comments
        })
    except Exception as e:
        logger.error(f"获取评论列表失败: {str(e)}")
        return jsonify({"code": 500, "msg": str(e)}), 500

@bp.route('/<int:post_id>/comments', methods=['POST'])
@auth_required
@performance_monitor
def add_comment(post_id):
    """
    为某个帖子添加评论
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"code": 400, "msg": "请求体为空"}), 400
            
        content = data.get('content')
        author_name = data.get('author_name', '匿名用户')
        
        if not content:
            return jsonify({"code": 400, "msg": "评论内容不能为空"}), 400
        
        conn = get_db_connection()
        c = conn.cursor()
        c.execute("""INSERT INTO community_comment (post_id, content, author_name) 
                     VALUES (?, ?, ?)""", (post_id, content, author_name))
        comment_id = c.lastrowid
        conn.commit()
        conn.close()
        
        return jsonify({
            "code": 200,
            "msg": "评论成功",
            "data": {
                "comment_id": comment_id
            }
        })
    except Exception as e:
        logger.error(f"添加评论失败: {str(e)}")
        return jsonify({"code": 500, "msg": str(e)}), 500

@bp.route('/comments/<int:comment_id>/like', methods=['POST'])
@auth_required
@performance_monitor
def like_comment(comment_id):
    """
    为评论点赞
    """
    try:
        conn = get_db_connection()
        c = conn.cursor()
        c.execute("UPDATE community_comment SET like_count = like_count + 1 WHERE id = ?", (comment_id,))
        conn.commit()
        conn.close()
        
        return jsonify({
            "code": 200,
            "msg": "评论点赞成功"
        })
    except Exception as e:
        logger.error(f"评论点赞失败: {str(e)}")
        return jsonify({"code": 500, "msg": str(e)}), 500