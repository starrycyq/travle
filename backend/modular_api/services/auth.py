"""
认证服务模块
处理用户认证、JWT token生成和验证
"""

import jwt
import bcrypt
import datetime
import uuid
from functools import wraps
from flask import request, jsonify, current_app
import logging

logger = logging.getLogger(__name__)

class AuthService:
    """认证服务类"""
    
    def __init__(self):
        self.secret_key = self._get_secret_key()
        self.token_expiry = 24 * 60 * 60  # 24小时
        
    def _get_secret_key(self):
        """获取JWT密钥"""
        # 从环境变量获取，如果没有则使用默认值
        import os
        secret_key = os.getenv('JWT_SECRET_KEY')
        if not secret_key:
            secret_key = 'travle-jwt-secret-key-change-in-production'
            logger.warning("使用默认JWT密钥，生产环境请设置JWT_SECRET_KEY环境变量")
        return secret_key
    
    def hash_password(self, password: str) -> str:
        """
        密码哈希
        
        Args:
            password: 明文密码
            
        Returns:
            哈希后的密码
        """
        return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
    
    def verify_password(self, password: str, hashed_password: str) -> bool:
        """
        验证密码
        
        Args:
            password: 明文密码
            hashed_password: 哈希密码
            
        Returns:
            验证结果
        """
        try:
            return bcrypt.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))
        except Exception as e:
            logger.error(f"密码验证失败: {e}")
            return False
    
    def generate_token(self, user_id: str, user_data: dict = None) -> dict:
        """
        生成JWT token
        
        Args:
            user_id: 用户ID
            user_data: 额外用户数据
            
        Returns:
            token信息字典
        """
        try:
            payload = {
                'user_id': user_id,
                'jti': str(uuid.uuid4()),  # JWT ID，用于防止重放攻击
                'iat': datetime.datetime.utcnow(),
                'exp': datetime.datetime.utcnow() + datetime.timedelta(seconds=self.token_expiry),
                'iss': 'travle-api',  # 发行者
                'user_data': user_data or {}
            }
            
            token = jwt.encode(payload, self.secret_key, algorithm='HS256')
            
            return {
                'access_token': token,
                'token_type': 'Bearer',
                'expires_in': self.token_expiry,
                'user_id': user_id
            }
        except Exception as e:
            logger.error(f"生成token失败: {e}")
            raise Exception("Token生成失败")
    
    def verify_token(self, token: str) -> dict:
        """
        验证JWT token
        
        Args:
            token: JWT token
            
        Returns:
            解码后的payload
            
        Raises:
            jwt.InvalidTokenError: token无效
        """
        try:
            payload = jwt.decode(
                token, 
                self.secret_key, 
                algorithms=['HS256'],
                options={
                    'verify_signature': True,
                    'verify_exp': True,
                    'verify_iat': True,
                    'verify_iss': True
                }
            )
            
            # 验证发行者
            if payload.get('iss') != 'travle-api':
                raise jwt.InvalidTokenError("无效的发行者")
                
            return payload
            
        except jwt.ExpiredSignatureError:
            raise jwt.InvalidTokenError("Token已过期")
        except jwt.InvalidTokenError as e:
            raise jwt.InvalidTokenError(f"Token无效: {str(e)}")
        except Exception as e:
            logger.error(f"Token验证异常: {e}")
            raise jwt.InvalidTokenError("Token验证失败")
    
    def refresh_token(self, token: str) -> dict:
        """
        刷新token
        
        Args:
            token: 旧token
            
        Returns:
            新token信息
        """
        try:
            payload = self.verify_token(token)
            user_id = payload.get('user_id')
            user_data = payload.get('user_data', {})
            
            # 生成新token
            return self.generate_token(user_id, user_data)
            
        except jwt.InvalidTokenError:
            raise Exception("Token无效，无法刷新")


# 全局认证服务实例
auth_service = AuthService()

def get_auth_service():
    """获取认证服务实例（单例模式）"""
    return auth_service


def auth_required(f):
    """
    认证装饰器
    用于保护需要认证的API端点
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            # 从请求头获取token
            auth_header = request.headers.get('Authorization')
            
            if not auth_header:
                return jsonify({
                    'status': 'error',
                    'message': '缺少Authorization头',
                    'error_code': 'MISSING_AUTH_HEADER'
                }), 401
            
            # 解析Bearer token
            try:
                auth_type, token = auth_header.split(' ')
                if auth_type.lower() != 'bearer':
                    raise ValueError()
            except ValueError:
                return jsonify({
                    'status': 'error',
                    'message': 'Authorization格式错误，应为: Bearer <token>',
                    'error_code': 'INVALID_AUTH_FORMAT'
                }), 401
            
            # 验证token
            try:
                payload = auth_service.verify_token(token)
            except jwt.InvalidTokenError as e:
                return jsonify({
                    'status': 'error',
                    'message': str(e),
                    'error_code': 'INVALID_TOKEN'
                }), 401
            
            # 将用户信息添加到请求上下文
            request.current_user = payload
            request.user_id = payload.get('user_id')
            request.user_data = payload.get('user_data', {})
            
            return f(*args, **kwargs)
            
        except Exception as e:
            logger.error(f"认证装饰器异常: {e}")
            return jsonify({
                'status': 'error',
                'message': '认证失败',
                'error_code': 'AUTH_ERROR'
            }), 500
    
    return decorated_function


def optional_auth(f):
    """
    可选认证装饰器
    支持匿名和认证用户访问的端点
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            # 尝试获取用户信息
            auth_header = request.headers.get('Authorization')
            
            if auth_header:
                try:
                    auth_type, token = auth_header.split(' ')
                    if auth_type.lower() == 'bearer':
                        payload = auth_service.verify_token(token)
                        request.current_user = payload
                        request.user_id = payload.get('user_id')
                        request.user_data = payload.get('user_data', {})
                except Exception:
                    # 认证失败，继续作为匿名用户
                    pass
            
            return f(*args, **kwargs)
            
        except Exception as e:
            logger.error(f"可选认证装饰器异常: {e}")
            # 认证异常，继续作为匿名用户
            return f(*args, **kwargs)
    
    return decorated_function


# 简单的用户数据库（实际项目中应该使用真实数据库）
class SimpleUserDB:
    """简单用户数据库（演示用）"""
    
    def __init__(self):
        # 初始化一个测试用户
        self.users = {
            'admin': {
                'user_id': 'admin',
                'username': 'admin',
                'password_hash': auth_service.hash_password('admin123'),
                'email': 'admin@travle.com',
                'role': 'admin'
            },
            'testuser': {
                'user_id': 'testuser',
                'username': 'testuser',
                'password_hash': auth_service.hash_password('test123'),
                'email': 'test@travle.com',
                'role': 'user'
            }
        }
    
    def get_user(self, username: str) -> dict:
        """获取用户信息"""
        return self.users.get(username)
    
    def authenticate_user(self, username: str, password: str) -> dict:
        """验证用户凭据"""
        user = self.get_user(username)
        if not user:
            return None
            
        if auth_service.verify_password(password, user['password_hash']):
            return user
            
        return None


# 全局用户数据库实例
user_db = SimpleUserDB()