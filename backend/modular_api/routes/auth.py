"""
认证路由模块
处理用户登录、注册和小红书授权等认证相关的请求
"""

from flask import Blueprint, request, jsonify, current_app
import logging
import sys
import os
from datetime import datetime

# 导入认证服务和验证工具
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from services.auth import auth_service, user_db, auth_required, optional_auth
from services.scraper import get_scraper_service
from utils.validation import validate_request, CommonRules
from utils.monitoring import performance_monitor
from utils.config import Config
try:
    from .. import limiter
except ImportError:
    limiter = None

logger = logging.getLogger(__name__)

bp = Blueprint('auth', __name__)

def rate_limit_if_enabled(limit_value):
    """条件限流装饰器，仅在限流启用时生效"""
    def decorator(func):
        if Config.RATE_LIMIT_ENABLED and limiter:
            return limiter.limit(limit_value)(func)
        return func
    return decorator

@bp.route('/login', methods=['POST'])
@rate_limit_if_enabled(Config.RATE_LIMIT_AUTH)
@validate_request({
    'username': CommonRules.username_rule(),
    'password': CommonRules.password_rule()
})
@performance_monitor
def login():
    """
    用户登录
    验证用户凭据并返回JWT token
    """
    try:
        # 获取验证后的数据
        data = request.validated_data
        username = data['username']
        password = data['password']

        # 验证用户凭据
        user = user_db.authenticate_user(username, password)
        if not user:
            logger.warning(f"登录失败: username={username}")
            return jsonify({
                'status': 'error',
                'message': '用户名或密码错误',
                'error_code': 'INVALID_CREDENTIALS'
            }), 401

        # 生成token
        user_data = {
            'username': user['username'],
            'email': user['email'],
            'role': user['role']
        }
        
        token_info = auth_service.generate_token(user['user_id'], user_data)
        
        logger.info(f"用户登录成功: username={username}, user_id={user['user_id']}")
        
        return jsonify({
            'status': 'success',
            'message': '登录成功',
            'data': {
                'access_token': token_info['access_token'],
                'token_type': token_info['token_type'],
                'expires_in': token_info['expires_in'],
                'user_info': {
                    'user_id': user['user_id'],
                    'username': user['username'],
                    'email': user['email'],
                    'role': user['role']
                }
            }
        })

    except Exception as e:
        logger.error(f"登录处理失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': '登录失败',
            'error_code': 'LOGIN_ERROR'
        }), 500

@bp.route('/register', methods=['POST'])
@rate_limit_if_enabled(Config.RATE_LIMIT_AUTH)
@validate_request({
    'username': CommonRules.username_rule(),
    'password': CommonRules.password_rule(),
    'email': CommonRules.email_rule()
})
@performance_monitor
def register():
    """
    用户注册
    创建新用户账号
    """
    try:
        # 获取验证后的数据
        data = request.validated_data
        username = data['username']
        password = data['password']
        email = data.get('email')

        # 检查用户名是否已存在
        if user_db.get_user(username):
            return jsonify({
                'status': 'error',
                'message': '用户名已存在',
                'error_code': 'USERNAME_EXISTS'
            }), 409

        # 创建新用户
        user_id = f"user_{len(user_db.users) + 1}"
        new_user = {
            'user_id': user_id,
            'username': username,
            'password_hash': auth_service.hash_password(password),
            'email': email or f"{username}@travle.com",
            'role': 'user'
        }
        
        user_db.users[username] = new_user
        
        logger.info(f"新用户注册成功: username={username}, user_id={user_id}")
        
        return jsonify({
            'status': 'success',
            'message': '注册成功',
            'data': {
                'user_id': user_id,
                'username': username,
                'email': new_user['email']
            }
        })

    except Exception as e:
        logger.error(f"注册处理失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': '注册失败',
            'error_code': 'REGISTER_ERROR'
        }), 500

@bp.route('/refresh', methods=['POST'])
@rate_limit_if_enabled(Config.RATE_LIMIT_AUTH)
@auth_required
@performance_monitor
def refresh_token():
    """
    刷新token
    使用当前token生成新的token
    """
    try:
        # 从请求中获取当前用户信息
        user_id = request.user_id
        user_data = request.user_data
        
        # 生成新token
        new_token_info = auth_service.generate_token(user_id, user_data)
        
        return jsonify({
            'status': 'success',
            'message': 'Token刷新成功',
            'data': {
                'access_token': new_token_info['access_token'],
                'token_type': new_token_info['token_type'],
                'expires_in': new_token_info['expires_in']
            }
        })

    except Exception as e:
        logger.error(f"Token刷新失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': 'Token刷新失败',
            'error_code': 'TOKEN_REFRESH_ERROR'
        }), 500

@bp.route('/verify', methods=['POST'])
@performance_monitor
def verify_token():
    """
    验证token
    检查token是否有效
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求体为空',
                'error_code': 'EMPTY_REQUEST'
            }), 400

        token = data.get('token')
        if not token:
            return jsonify({
                'status': 'error',
                'message': '缺少token',
                'error_code': 'MISSING_TOKEN'
            }), 400

        # 验证token
        payload = auth_service.verify_token(token)
        
        return jsonify({
            'status': 'success',
            'message': 'Token有效',
            'data': {
                'user_id': payload.get('user_id'),
                'user_data': payload.get('user_data', {}),
                'expires_at': payload.get('exp')
            }
        })

    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': 'Token无效',
            'error_code': 'INVALID_TOKEN'
        }), 401

@bp.route('/xiaohongshu', methods=['POST'])
@auth_required
@performance_monitor
def xiaohongshu_login():
    """
    小红书模拟登录
    接收手机号和验证码，模拟浏览器登录小红书
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求体为空',
                'error_code': 'EMPTY_REQUEST'
            }), 400

        mobile = data.get('mobile')  # 手机号
        verification_code = data.get('verification_code')  # 验证码

        if not mobile or not verification_code:
            return jsonify({
                'status': 'error',
                'message': '缺少手机号或验证码',
                'error_code': 'MISSING_PARAMS'
            }), 400

        # 获取爬虫服务
        scraper_service = get_scraper_service()
        
        # 模拟登录
        login_result = scraper_service.simulated_login(mobile, verification_code)
        if not login_result.get('success'):
            return jsonify({
                'status': 'error',
                'message': f'登录失败: {login_result.get("error", "未知错误")}',
                'error_code': 'LOGIN_FAILED'
            }), 400
        
        # 获取系统用户ID
        system_user_id = request.user_id
        
        # 保存登录状态（cookies/session）
        session_id = login_result.get('session_id')
        if not scraper_service.save_login_session(system_user_id, session_id, mobile):
            logger.warning(f"保存登录状态失败，但继续执行爬虫任务: mobile={mobile}")
        
        # 启动爬虫任务
        task_id = scraper_service.create_scraping_task(system_user_id)
        if not task_id:
            return jsonify({
                'status': 'error',
                'message': '创建爬虫任务失败',
                'error_code': 'TASK_CREATION_ERROR'
            }), 500

        logger.info(f"小红书模拟登录成功: system_user_id={system_user_id}, mobile={mobile}, task_id={task_id}")

        return jsonify({
            'status': 'success',
            'message': '登录成功，爬虫任务已启动',
            'data': {
                'task_id': task_id,
                'session_id': session_id,
                'status': 'pending',
                'created_at': datetime.now().isoformat()
            }
        })

    except Exception as e:
        logger.error(f"处理登录请求失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': '处理登录请求失败',
            'error_code': 'LOGIN_ERROR'
        }), 500

@bp.route('/xiaohongshu/send_code', methods=['POST'])
@auth_required
@performance_monitor
def send_xiaohongshu_verification_code():
    """
    发送小红书验证码
    模拟发送验证码请求到小红书
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求体为空',
                'error_code': 'EMPTY_REQUEST'
            }), 400

        mobile = data.get('mobile')  # 手机号
        if not mobile:
            return jsonify({
                'status': 'error',
                'message': '缺少手机号',
                'error_code': 'MISSING_MOBILE'
            }), 400

        # 获取爬虫服务
        scraper_service = get_scraper_service()
        
        # 发送验证码
        result = scraper_service.send_verification_code(mobile)
        if not result.get('success'):
            return jsonify({
                'status': 'error',
                'message': f'发送验证码失败: {result.get("error", "未知错误")}',
                'error_code': 'SEND_CODE_FAILED'
            }), 400

        logger.info(f"小红书验证码发送成功: mobile={mobile}")

        return jsonify({
            'status': 'success',
            'message': '验证码发送成功',
            'data': {
                'mobile': mobile,
                'sent_at': datetime.now().isoformat()
            }
        })

    except Exception as e:
        logger.error(f"发送验证码请求失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': '发送验证码请求失败',
            'error_code': 'SEND_CODE_ERROR'
        }), 500

@bp.route('/me', methods=['GET'])
@auth_required
def get_current_user():
    """
    获取当前用户信息
    """
    try:
        return jsonify({
            'status': 'success',
            'data': {
                'user_id': request.user_id,
                'user_data': request.user_data
            }
        })
    except Exception as e:
        logger.error(f"获取用户信息失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': '获取用户信息失败',
            'error_code': 'GET_USER_ERROR'
        }), 500