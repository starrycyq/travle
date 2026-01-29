"""
监控系统模块
提供结构化日志、性能监控和告警功能
"""

import logging
import time
import json
from datetime import datetime
from typing import Dict, Any, Optional
from functools import wraps
from .config import Config

# 配置结构化日志格式
class StructuredLogFormatter(logging.Formatter):
    """结构化日志格式化器"""
    
    def format(self, record):
        """格式化日志记录"""
        log_entry = {
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'level': record.levelname,
            'logger': record.name,
            'message': record.getMessage(),
            'module': record.module,
            'function': record.funcName,
            'line': record.lineno
        }
        
        # 添加额外字段
        if hasattr(record, 'extra_fields'):
            log_entry.update(record.extra_fields)
        
        # 添加异常信息
        if record.exc_info:
            log_entry['exception'] = self.formatException(record.exc_info)
        
        return json.dumps(log_entry, ensure_ascii=False)

class APIMetricsCollector:
    """API指标收集器"""
    
    def __init__(self):
        self.metrics = {
            'request_count': 0,
            'error_count': 0,
            'response_times': [],
            'endpoint_stats': {},
            'start_time': time.time()
        }
    
    def record_request(self, endpoint: str, method: str, response_time: float, 
                      status_code: int, user_id: Optional[str] = None):
        """记录API请求指标"""
        self.metrics['request_count'] += 1
        
        if status_code >= 400:
            self.metrics['error_count'] += 1
        
        # 记录响应时间
        self.metrics['response_times'].append(response_time)
        
        # 维护最近100个响应时间
        if len(self.metrics['response_times']) > 100:
            self.metrics['response_times'] = self.metrics['response_times'][-100:]
        
        # 按端点统计
        endpoint_key = f"{method}:{endpoint}"
        if endpoint_key not in self.metrics['endpoint_stats']:
            self.metrics['endpoint_stats'][endpoint_key] = {
                'count': 0,
                'total_time': 0,
                'error_count': 0,
                'min_time': float('inf'),
                'max_time': 0
            }
        
        stats = self.metrics['endpoint_stats'][endpoint_key]
        stats['count'] += 1
        stats['total_time'] += response_time
        stats['avg_time'] = stats['total_time'] / stats['count']
        
        if response_time < stats['min_time']:
            stats['min_time'] = response_time
        if response_time > stats['max_time']:
            stats['max_time'] = response_time
        
        if status_code >= 400:
            stats['error_count'] += 1
    
    def get_metrics(self) -> Dict[str, Any]:
        """获取当前指标"""
        metrics = self.metrics.copy()
        
        # 计算统计信息
        if metrics['response_times']:
            import statistics
            metrics['avg_response_time'] = statistics.mean(metrics['response_times'])
            metrics['p95_response_time'] = statistics.quantiles(
                metrics['response_times'], n=20
            )[18] if len(metrics['response_times']) >= 20 else max(metrics['response_times'])
            
            metrics['error_rate'] = (
                metrics['error_count'] / metrics['request_count']
                if metrics['request_count'] > 0 else 0
            )
        
        # 计算运行时间
        metrics['uptime'] = time.time() - metrics['start_time']
        
        return metrics
    
    def get_endpoint_performance(self) -> Dict[str, Dict]:
        """获取端点性能报告"""
        performance = {}
        
        for endpoint_key, stats in self.metrics['endpoint_stats'].items():
            if stats['count'] > 0:
                performance[endpoint_key] = {
                    'request_count': stats['count'],
                    'avg_response_time': stats.get('avg_time', 0),
                    'min_response_time': stats.get('min_time', 0),
                    'max_response_time': stats.get('max_time', 0),
                    'error_rate': stats['error_count'] / stats['count']
                }
        
        return performance

class AlertManager:
    """告警管理器"""
    
    def __init__(self, config: Config):
        self.config = config
        self.alerts = []
        self.alert_rules = {
            'high_error_rate': {
                'threshold': 0.1,  # 10%错误率
                'window': 100,      # 最近100个请求
                'message': 'API错误率过高'
            },
            'slow_response': {
                'threshold': 1.0,   # 1秒
                'window': 50,       # 最近50个请求
                'message': 'API响应时间过慢'
            },
            'high_concurrent_errors': {
                'threshold': 5,     # 5个连续错误
                'message': '连续API错误'
            }
        }
    
    def check_alerts(self, metrics_collector: APIMetricsCollector):
        """检查告警条件"""
        metrics = metrics_collector.get_metrics()
        alerts = []
        
        # 检查错误率告警
        if metrics.get('error_rate', 0) > self.alert_rules['high_error_rate']['threshold']:
            alerts.append({
                'type': 'high_error_rate',
                'message': self.alert_rules['high_error_rate']['message'],
                'value': metrics['error_rate'],
                'threshold': self.alert_rules['high_error_rate']['threshold'],
                'timestamp': datetime.utcnow().isoformat() + 'Z'
            })
        
        # 检查慢响应告警
        if metrics.get('avg_response_time', 0) > self.alert_rules['slow_response']['threshold']:
            alerts.append({
                'type': 'slow_response',
                'message': self.alert_rules['slow_response']['message'],
                'value': metrics['avg_response_time'],
                'threshold': self.alert_rules['slow_response']['threshold'],
                'timestamp': datetime.utcnow().isoformat() + 'Z'
            })
        
        # 记录新告警
        for alert in alerts:
            if not self._is_duplicate_alert(alert):
                self.alerts.append(alert)
                self._notify_alert(alert)
        
        # 保留最近100个告警
        if len(self.alerts) > 100:
            self.alerts = self.alerts[-100:]
    
    def _is_duplicate_alert(self, new_alert: Dict) -> bool:
        """检查是否为重复告警（最近5分钟内）"""
        five_minutes_ago = time.time() - 300
        
        for alert in reversed(self.alerts):
            if time.time() - five_minutes_ago < 300:  # 检查时间窗口
                continue
            
            if (alert['type'] == new_alert['type'] and 
                abs(alert['value'] - new_alert['value']) < 0.01):  # 值相近
                return True
        
        return False
    
    def _notify_alert(self, alert: Dict):
        """通知告警（可扩展为邮件、短信等）"""
        logger = logging.getLogger('alert')
        logger.warning(f"告警: {alert['message']} - 当前值: {alert['value']:.3f}")
        
        # 这里可以添加邮件、短信等通知方式
        # if self.config.ALERT_EMAIL:
        #     send_email_alert(alert)

def setup_structured_logging(app):
    """设置结构化日志"""
    # 创建日志目录
    import os
    from pathlib import Path
    
    log_file = Config.LOG_FILE
    log_dir = Path(log_file).parent
    log_dir.mkdir(parents=True, exist_ok=True)
    
    # 配置根日志记录器
    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, Config.LOG_LEVEL.upper()))
    
    # 移除现有的处理器
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)
    
    # 添加控制台处理器
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(StructuredLogFormatter())
    root_logger.addHandler(console_handler)
    
    # 添加文件处理器
    file_handler = logging.FileHandler(log_file, encoding='utf-8')
    file_handler.setFormatter(StructuredLogFormatter())
    root_logger.addHandler(file_handler)
    
    # 创建监控记录器
    monitor_logger = logging.getLogger('monitor')
    monitor_logger.setLevel(logging.INFO)
    
    # 创建告警记录器
    alert_logger = logging.getLogger('alert')
    alert_logger.setLevel(logging.WARNING)
    
    return root_logger

def performance_monitor(func):
    """API性能监控装饰器"""
    @wraps(func)
    def wrapper(*args, **kwargs):
        import time
        from flask import request, current_app
        
        start_time = time.perf_counter()
        
        try:
            response = func(*args, **kwargs)
            response_time = time.perf_counter() - start_time
            
            # 记录指标
            if hasattr(current_app, 'metrics_collector'):
                status_code = response.status_code if hasattr(response, 'status_code') else 200
                user_id = getattr(request, 'user_id', None) if hasattr(request, 'user_id') else None
                
                current_app.metrics_collector.record_request(
                    endpoint=request.path,
                    method=request.method,
                    response_time=response_time,
                    status_code=status_code,
                    user_id=user_id
                )
            
            # 记录结构化日志
            logger = logging.getLogger('monitor')
            logger.info('API请求完成', extra={
                'extra_fields': {
                    'endpoint': request.path,
                    'method': request.method,
                    'response_time': response_time,
                    'status_code': status_code,
                    'user_id': user_id,
                    'ip': request.remote_addr
                }
            })
            
            # 检查告警
            if hasattr(current_app, 'alert_manager'):
                current_app.alert_manager.check_alerts(current_app.metrics_collector)
            
            return response
            
        except Exception as e:
            response_time = time.perf_counter() - start_time
            
            # 记录错误指标
            if hasattr(current_app, 'metrics_collector'):
                current_app.metrics_collector.record_request(
                    endpoint=request.path,
                    method=request.method,
                    response_time=response_time,
                    status_code=500,
                    user_id=getattr(request, 'user_id', None) if hasattr(request, 'user_id') else None
                )
            
            # 记录错误日志
            logger = logging.getLogger('monitor')
            logger.error('API请求失败', extra={
                'extra_fields': {
                    'endpoint': request.path,
                    'method': request.method,
                    'response_time': response_time,
                    'error': str(e),
                    'ip': request.remote_addr
                }
            })
            
            raise
    
    return wrapper

def create_monitoring_endpoints(app):
    """创建监控API端点"""
    from flask import Blueprint, jsonify
    
    monitoring_bp = Blueprint('monitoring', __name__)
    
    @monitoring_bp.route('/metrics', methods=['GET'])
    def get_metrics():
        """获取当前系统指标"""
        if hasattr(app, 'metrics_collector'):
            metrics = app.metrics_collector.get_metrics()
            return jsonify({
                'status': 'success',
                'metrics': metrics,
                'timestamp': datetime.utcnow().isoformat() + 'Z'
            })
        return jsonify({'status': 'error', 'message': '监控未启用'}), 503
    
    @monitoring_bp.route('/performance', methods=['GET'])
    def get_performance():
        """获取端点性能报告"""
        if hasattr(app, 'metrics_collector'):
            performance = app.metrics_collector.get_endpoint_performance()
            return jsonify({
                'status': 'success',
                'performance': performance,
                'timestamp': datetime.utcnow().isoformat() + 'Z'
            })
        return jsonify({'status': 'error', 'message': '监控未启用'}), 503
    
    @monitoring_bp.route('/alerts', methods=['GET'])
    def get_alerts():
        """获取当前告警"""
        if hasattr(app, 'alert_manager'):
            return jsonify({
                'status': 'success',
                'alerts': app.alert_manager.alerts[-20:],  # 最近20个告警
                'timestamp': datetime.utcnow().isoformat() + 'Z'
            })
        return jsonify({'status': 'error', 'message': '告警管理器未启用'}), 503
    
    # 注册蓝图
    app.register_blueprint(monitoring_bp, url_prefix='/api/monitoring')
    
    return monitoring_bp

# 全局监控实例
metrics_collector = APIMetricsCollector()
alert_manager = None

def init_monitoring(app):
    """初始化监控系统"""
    global alert_manager
    
    # 设置结构化日志
    setup_structured_logging(app)
    
    # 初始化指标收集器
    app.metrics_collector = metrics_collector
    
    # 初始化告警管理器
    alert_manager = AlertManager(Config)
    app.alert_manager = alert_manager
    
    # 创建监控端点
    create_monitoring_endpoints(app)
    
    logger = logging.getLogger(__name__)
    logger.info('监控系统已初始化')
    
    return app