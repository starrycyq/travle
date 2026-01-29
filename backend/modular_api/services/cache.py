"""
Redis缓存服务
提供统一的缓存接口，支持API响应缓存、查询缓存等
"""

import json
import logging
import pickle
from typing import Any, Optional, Union
from functools import wraps
import hashlib
import time

logger = logging.getLogger(__name__)

class CacheService:
    """Redis缓存管理器"""
    
    def __init__(self):
        self._redis_client = None
        self._enabled = False
        
    def init_app(self, app):
        """初始化Redis连接"""
        try:
            import redis
            from ..utils.config import config
            
            cfg = config[app.config.get('ENV', 'default')]
            
            # Redis配置
            redis_host = getattr(cfg, 'REDIS_HOST', 'localhost')
            redis_port = getattr(cfg, 'REDIS_PORT', 6379)
            redis_password = getattr(cfg, 'REDIS_PASSWORD', None)
            redis_db = getattr(cfg, 'REDIS_DB', 0)
            
            self._redis_client = redis.Redis(
                host=redis_host,
                port=redis_port,
                password=redis_password,
                db=redis_db,
                decode_responses=False,  # 保持原始字节数据
                socket_connect_timeout=5,
                socket_timeout=5,
                retry_on_timeout=True
            )
            
            # 测试连接
            self._redis_client.ping()
            self._enabled = True
            logger.info(f"Redis缓存已连接: {redis_host}:{redis_port}")
            
        except ImportError:
            logger.warning("redis模块未安装，缓存功能将被禁用")
            self._enabled = False
        except Exception as e:
            logger.warning(f"Redis连接失败: {e}，缓存功能将被禁用")
            self._enabled = False
    
    def is_enabled(self):
        """检查缓存是否可用"""
        return self._enabled and self._redis_client is not None
    
    def get(self, key: str) -> Optional[Any]:
        """获取缓存值"""
        if not self.is_enabled():
            return None
            
        try:
            data = self._redis_client.get(key)
            if data:
                return pickle.loads(data)
        except Exception as e:
            logger.warning(f"获取缓存失败 {key}: {e}")
        return None
    
    def set(self, key: str, value: Any, ttl: int = 3600) -> bool:
        """设置缓存值，TTL单位：秒"""
        if not self.is_enabled():
            return False
            
        try:
            data = pickle.dumps(value)
            result = self._redis_client.setex(key, ttl, data)
            return result is True
        except Exception as e:
            logger.warning(f"设置缓存失败 {key}: {e}")
            return False
    
    def delete(self, key: str) -> bool:
        """删除缓存"""
        if not self.is_enabled():
            return False
            
        try:
            result = self._redis_client.delete(key)
            return result > 0
        except Exception as e:
            logger.warning(f"删除缓存失败 {key}: {e}")
            return False
    
    def clear_pattern(self, pattern: str) -> int:
        """清除匹配模式的缓存"""
        if not self.is_enabled():
            return 0
            
        try:
            keys = self._redis_client.keys(pattern)
            if keys:
                count = self._redis_client.delete(*keys)
                logger.info(f"清除缓存模式 {pattern}: 删除 {count} 个键")
                return count
        except Exception as e:
            logger.warning(f"清除缓存模式失败 {pattern}: {e}")
        return 0
    
    def get_or_set(self, key: str, func: callable, ttl: int = 3600) -> Any:
        """获取缓存，如果不存在则调用函数生成并缓存"""
        value = self.get(key)
        if value is not None:
            return value
            
        value = func()
        if value is not None:
            self.set(key, value, ttl)
        return value

# 全局缓存实例
cache = CacheService()

def get_cache_service():
    """获取缓存服务实例（单例模式）"""
    return cache

def cache_key_builder(prefix: str = "cache", **kwargs) -> str:
    """构建缓存键"""
    if not kwargs:
        return prefix
    
    # 对参数进行排序以确保一致性
    sorted_items = sorted(kwargs.items())
    param_str = "&".join(f"{k}={v}" for k, v in sorted_items)
    key_hash = hashlib.md5(param_str.encode()).hexdigest()[:8]
    return f"{prefix}:{key_hash}"

def api_response_cache(ttl: int = 300, prefix: str = "api"):
    """
    API响应缓存装饰器
    :param ttl: 缓存时间（秒）
    :param prefix: 缓存键前缀
    """
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            # 从请求中提取参数
            from flask import request
            
            # 构建缓存键
            cache_params = {
                "path": request.path,
                "method": request.method,
                "query": dict(request.args),
                "data": request.get_json() if request.is_json else {}
            }
            
            # 如果已认证，包含用户ID
            if hasattr(request, 'user_id'):
                cache_params['user_id'] = request.user_id
            
            key = cache_key_builder(prefix, **cache_params)
            
            # 尝试从缓存获取
            cached_response = cache.get(key)
            if cached_response is not None:
                logger.debug(f"缓存命中: {key}")
                return cached_response
            
            # 执行函数
            response = func(*args, **kwargs)
            
            # 缓存成功响应（仅缓存2xx状态码）
            if response.status_code >= 200 and response.status_code < 300:
                try:
                    # 复制响应对象
                    from copy import deepcopy
                    cached_response = deepcopy(response)
                    cache.set(key, cached_response, ttl)
                    logger.debug(f"缓存设置: {key}, TTL={ttl}s")
                except Exception as e:
                    logger.warning(f"缓存响应失败: {e}")
            
            return response
        return wrapper
    return decorator

def query_cache(ttl: int = 600, key_prefix: str = "query"):
    """
    查询缓存装饰器
    :param ttl: 缓存时间（秒）
    :param key_prefix: 缓存键前缀
    """
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            # 构建缓存键
            key = cache_key_builder(key_prefix, 
                                  func_name=func.__name__,
                                  args=args,
                                  kwargs=kwargs)
            
            # 尝试从缓存获取
            cached_result = cache.get(key)
            if cached_result is not None:
                logger.debug(f"查询缓存命中: {key}")
                return cached_result
            
            # 执行函数
            result = func(*args, **kwargs)
            
            # 缓存结果
            if result is not None:
                cache.set(key, result, ttl)
                logger.debug(f"查询缓存设置: {key}, TTL={ttl}s")
            
            return result
        return wrapper
    return decorator

def clear_related_cache(pattern: str = "api:*"):
    """清除相关缓存"""
    return cache.clear_pattern(pattern)