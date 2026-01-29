"""
输入验证工具模块
处理API请求的输入验证、清洗和过滤
"""

import re
import html
from functools import wraps
from flask import request, jsonify
import logging

logger = logging.getLogger(__name__)

class ValidationError(Exception):
    """验证错误异常"""
    def __init__(self, message, error_code='VALIDATION_ERROR', field=None):
        self.message = message
        self.error_code = error_code
        self.field = field
        super().__init__(self.message)

class ValidationRule:
    """验证规则类"""
    
    def __init__(self, name, required=False, min_length=None, max_length=None, 
                 pattern=None, custom_validator=None, error_message=None):
        self.name = name
        self.required = required
        self.min_length = min_length
        self.max_length = max_length
        self.pattern = pattern
        self.custom_validator = custom_validator
        self.error_message = error_message

    def validate(self, value):
        """执行验证"""
        # 检查必填字段
        if self.required and (value is None or value == ''):
            raise ValidationError(
                f"{self.name}不能为空",
                'REQUIRED_FIELD',
                self.name
            )
        
        # 如果值为空且不是必填，跳过其他验证
        if value is None or value == '':
            return value
        
        # 类型转换
        if isinstance(value, str):
            # 防止XSS攻击
            value = html.escape(value.strip())
            # 限制特殊字符
            value = re.sub(r'[<>"\']', '', value)
        
        # 长度验证
        if self.min_length is not None and len(str(value)) < self.min_length:
            raise ValidationError(
                f"{self.name}长度不能少于{self.min_length}个字符",
                'MIN_LENGTH',
                self.name
            )
        
        if self.max_length is not None and len(str(value)) > self.max_length:
            raise ValidationError(
                f"{self.name}长度不能超过{self.max_length}个字符",
                'MAX_LENGTH',
                self.name
            )
        
        # 正则表达式验证
        if self.pattern and isinstance(value, str):
            if not re.match(self.pattern, value):
                raise ValidationError(
                    self.error_message or f"{self.name}格式不正确",
                    'INVALID_FORMAT',
                    self.name
                )
        
        # 自定义验证器
        if self.custom_validator:
            try:
                result = self.custom_validator(value)
                if result is False:
                    raise ValidationError(
                        self.error_message or f"{self.name}验证失败",
                        'CUSTOM_VALIDATION',
                        self.name
                    )
            except Exception as e:
                raise ValidationError(
                    str(e),
                    'CUSTOM_VALIDATION',
                    self.name
                )
        
        return value

def validate_request(rules):
    """
    请求验证装饰器
    
    Args:
        rules: 验证规则字典
        格式: {
            'field_name': ValidationRule(...),
            ...
        }
    """
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            try:
                data = request.get_json() if request.method in ['POST', 'PUT'] else request.args
                if not data:
                    data = {}
                
                validated_data = {}
                errors = []
                
                # 验证每个字段
                for field_name, rule in rules.items():
                    try:
                        value = data.get(field_name)
                        validated_value = rule.validate(value)
                        validated_data[field_name] = validated_value
                    except ValidationError as e:
                        errors.append({
                            'field': e.field,
                            'message': e.message,
                            'error_code': e.error_code
                        })
                    except Exception as e:
                        logger.error(f"验证字段 {field_name} 时发生异常: {e}")
                        errors.append({
                            'field': field_name,
                            'message': f"{field_name}验证失败",
                            'error_code': 'VALIDATION_EXCEPTION'
                        })
                
                # 如果有验证错误，返回错误响应
                if errors:
                    return jsonify({
                        'status': 'error',
                        'message': '输入验证失败',
                        'error_code': 'VALIDATION_FAILED',
                        'errors': errors
                    }), 400
                
                # 将验证后的数据添加到请求上下文
                request.validated_data = validated_data
                
                return f(*args, **kwargs)
                
            except Exception as e:
                logger.error(f"验证装饰器异常: {e}")
                return jsonify({
                    'status': 'error',
                    'message': '验证过程发生异常',
                    'error_code': 'VALIDATION_EXCEPTION'
                }), 500
        
        return decorated_function
    return decorator

# 常用验证规则
class CommonRules:
    """常用验证规则集合"""
    
    @staticmethod
    def string_rule(name, required=False, min_length=None, max_length=None):
        """字符串验证规则"""
        return ValidationRule(
            name=name,
            required=required,
            min_length=min_length,
            max_length=max_length,
            pattern=r'^[\w\s\u4e00-\u9fa5\-_.,!?()]+$',
            error_message=f"{name}只能包含字母、数字、中文、空格和基本标点符号"
        )
    
    @staticmethod
    def email_rule(name='email', required=False):
        """邮箱验证规则"""
        return ValidationRule(
            name=name,
            required=required,
            pattern=r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
            error_message="邮箱格式不正确"
        )
    
    @staticmethod
    def username_rule(name='username', required=False):
        """用户名验证规则"""
        return ValidationRule(
            name=name,
            required=required,
            min_length=3,
            max_length=20,
            pattern=r'^[a-zA-Z0-9_]+$',
            error_message="用户名只能包含字母、数字和下划线，长度3-20位"
        )
    
    @staticmethod
    def password_rule(name='password', required=False):
        """密码验证规则"""
        def validate_password(password):
            if len(password) < 6:
                return False
            if not re.search(r'[a-zA-Z]', password):
                return False
            if not re.search(r'[0-9]', password):
                return False
            return True
        
        return ValidationRule(
            name=name,
            required=required,
            min_length=6,
            max_length=50,
            custom_validator=validate_password,
            error_message="密码必须包含字母和数字，长度6-50位"
        )
    
    @staticmethod
    def id_rule(name='id', required=False):
        """ID验证规则"""
        def validate_id(value):
            try:
                return int(value)
            except (ValueError, TypeError):
                raise ValidationError("ID必须是整数")
        
        return ValidationRule(
            name=name,
            required=required,
            custom_validator=validate_id,
            error_message="ID必须是整数"
        )
    
    @staticmethod
    def text_rule(name, required=False, max_length=10000):
        """文本内容验证规则"""
        return ValidationRule(
            name=name,
            required=required,
            max_length=max_length,
            error_message=f"{name}长度不能超过{max_length}个字符"
        )
    
    @staticmethod
    def destination_rule(name='destination', required=False):
        """目的地验证规则"""
        return ValidationRule(
            name=name,
            required=required,
            min_length=2,
            max_length=50,
            pattern=r'^[\w\s\u4e00-\u9fa5\-_]+$',
            error_message="目的地只能包含中文、字母、数字、空格和连字符"
        )
    
    @staticmethod
    def page_rule(name='page', required=False):
        """页码验证规则"""
        def validate_page(value):
            page = int(value)
            if page < 1:
                raise ValidationError("页码必须大于0")
            return page
        
        return ValidationRule(
            name=name,
            required=required,
            custom_validator=validate_page,
            error_message="页码必须是大于0的整数"
        )
    
    @staticmethod
    def limit_rule(name='limit', required=False, default=20, max_limit=100):
        """数量限制验证规则"""
        def validate_limit(value):
            limit = int(value)
            if limit < 1:
                limit = default
            if limit > max_limit:
                limit = max_limit
            return limit
        
        return ValidationRule(
            name=name,
            required=required,
            custom_validator=validate_limit,
            error_message=f"数量必须在1-{max_limit}之间"
        )

def sanitize_input(text, max_length=10000):
    """
    清洗输入文本
    
    Args:
        text: 输入文本
        max_length: 最大长度
        
    Returns:
        清洗后的文本
    """
    if not text:
        return ""
    
    # 转换为字符串
    if not isinstance(text, str):
        text = str(text)
    
    # 截断长度
    if len(text) > max_length:
        text = text[:max_length]
    
    # HTML转义
    text = html.escape(text)
    
    # 移除危险字符
    text = re.sub(r'[<>"\']', '', text)
    
    # 移除多余的空白字符
    text = re.sub(r'\s+', ' ', text).strip()
    
    return text

def validate_json_structure(data, required_keys=None, optional_keys=None):
    """
    验证JSON数据结构
    
    Args:
        data: JSON数据
        required_keys: 必需键列表
        optional_keys: 可选键列表
        
    Returns:
        验证结果
    """
    if not isinstance(data, dict):
        raise ValidationError("请求数据必须是JSON对象")
    
    required_keys = required_keys or []
    optional_keys = optional_keys or []
    
    errors = []
    
    # 检查必需键
    for key in required_keys:
        if key not in data or data[key] is None or data[key] == '':
            errors.append({
                'field': key,
                'message': f"缺少必需字段: {key}",
                'error_code': 'MISSING_REQUIRED_FIELD'
            })
    
    # 检查未知键
    allowed_keys = set(required_keys + optional_keys)
    unknown_keys = set(data.keys()) - allowed_keys
    if unknown_keys:
        errors.append({
            'field': 'unknown_fields',
            'message': f"未知字段: {', '.join(unknown_keys)}",
            'error_code': 'UNKNOWN_FIELDS'
        })
    
    if errors:
        raise ValidationError("JSON结构验证失败", 'INVALID_JSON_STRUCTURE')
    
    return True