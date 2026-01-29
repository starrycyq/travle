"""
自驾游路由模块
处理路线规划和自驾游相关的请求
"""

from flask import Blueprint, request, jsonify
import uuid
import logging
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from services.auth import auth_required, optional_auth
from services.cache import api_response_cache
from services.model import get_model_service
from utils.monitoring import performance_monitor
from utils.config import Config

try:
    from .. import limiter
except ImportError:
    limiter = None

logger = logging.getLogger(__name__)

bp = Blueprint('roadtrip', __name__)

ROUTE_CACHE_FILE = './data/route_cache.json'

def load_route_cache():
    """加载路线缓存"""
    try:
        import json
        data_dir = os.path.dirname(ROUTE_CACHE_FILE)
        if not os.path.exists(data_dir):
            os.makedirs(data_dir, exist_ok=True)
        if os.path.exists(ROUTE_CACHE_FILE):
            with open(ROUTE_CACHE_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}
    except Exception as e:
        logger.error(f"加载路线缓存失败: {e}")
        return {}

def save_route_cache(route_id, route_data):
    """保存路线缓存"""
    try:
        import json
        cache = load_route_cache()
        cache[route_id] = {
            'data': route_data,
            'timestamp': str(uuid.uuid4())
        }
        with open(ROUTE_CACHE_FILE, 'w', encoding='utf-8') as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except Exception as e:
        logger.error(f"保存路线缓存失败: {e}")

def generate_route_waypoints(start, destination, route_type):
    """生成路线途经点"""
    waypoints = []
    start_clean = start.strip()
    dest_clean = destination.strip()

    common_waypoints = {
        ('北京', '上海'): ['天津', '济南', '南京'],
        ('上海', '北京'): ['南京', '济南', '天津'],
        ('北京', '杭州'): ['天津', '济南', '南京', '苏州'],
        ('杭州', '北京'): ['苏州', '南京', '济南', '天津'],
        ('广州', '深圳'): ['东莞', '惠州'],
        ('深圳', '广州'): ['惠州', '东莞'],
        ('成都', '重庆'): ['德阳', '绵阳'],
        ('重庆', '成都'): ['绵阳', '德阳'],
    }

    key = (start_clean, dest_clean)
    reverse_key = (dest_clean, start_clean)

    if key in common_waypoints:
        waypoints = common_waypoints[key]
    elif reverse_key in common_waypoints:
        waypoints = common_waypoints[reverse_key]
    else:
        waypoints = [f"途经点1（{start_clean}→{dest_clean}途中）"]

    if route_type == 'scenic':
        waypoints.extend(['风景区', '观景点'])
    elif route_type == 'balanced':
        waypoints.append('休息区')

    return waypoints

def calculate_route_info(start, destination, route_type, waypoints):
    """计算路线信息"""
    distance_factors = {
        'fastest': 0.8,
        'scenic': 1.3,
        'balanced': 1.0
    }

    base_distance = 100 + hash(f"{start}{destination}") % 1000

    total_distance = base_distance * distance_factors.get(route_type, 1.0)
    estimated_time = total_distance / 80

    if route_type == 'fastest':
        time_factor = 0.85
        description = f"从{start}到{destination}的最快路线，途经{len(waypoints)}个主要节点，预计行驶{estimated_time*time_factor:.1f}小时。"
    elif route_type == 'scenic':
        time_factor = 1.4
        description = f"从{start}到{destination}的风景路线，穿越多个风景区，预计行驶{estimated_time*time_factor:.1f}小时，风景优美但路程较长。"
    else:
        time_factor = 1.0
        description = f"从{start}到{destination}的平衡路线，兼顾速度和风景，预计行驶{estimated_time:.1f}小时。"

    return {
        'start': start,
        'destination': destination,
        'waypoints': waypoints,
        'total_distance_km': round(total_distance, 1),
        'estimated_time_hours': round(estimated_time * time_factor, 1),
        'route_description': description
    }

def generate_roadtrip_guide(start, destination, preferences, route_type, route_info):
    """生成自驾游攻略"""
    model_service = get_model_service()

    prompt = f"""
生成一份从{start}到{destination}的自驾游攻略。

路线信息：
- 路线类型：{route_type}
- 总距离：{route_info['total_distance_km']}公里
- 预计时间：{route_info['estimated_time_hours']}小时
- 途经点：{', '.join(route_info['waypoints'])}

用户偏好：{preferences}

请生成一份实用的自驾游攻略，包括：
1. 行前准备清单
2. 沿途亮点推荐
3. 住宿和餐饮建议
4. 驾驶注意事项
5. 景点游玩顺序建议

回复格式要求简洁实用，便于旅途查看。
"""

    try:
        guide = model_service.generate_response(prompt)
    except Exception as model_error:
        logger.warning(f"模型生成失败，使用模拟攻略: {model_error}")
        guide = generate_mock_roadtrip_guide(start, destination, preferences, route_type, route_info)

    return guide

def generate_mock_roadtrip_guide(start, destination, preferences, route_type, route_info):
    """生成模拟的自驾游攻略"""
    scenic_note = "（此路线风景优美，建议多安排拍照时间）" if route_type == 'scenic' else ""
    fastest_note = "（此路线为最快路线，建议保持安全车速）" if route_type == 'fastest' else ""

    guide = f"""
🚗 自驾游攻略：从{start}到{destination}
{'='*50}

📊 路线概览
• 起点：{start}
• 终点：{destination}
• 总里程：{route_info['total_distance_km']}公里
• 预计用时：{route_info['estimated_time_hours']}小时
• 途经城市：{', '.join(route_info['waypoints'])}
{scenic_note}{fastest_note}

🧳 行前准备
• 检查车辆状况（机油、刹车、轮胎）
• 携带驾驶证、行驶证、保险单
• 准备应急工具（三角警示牌、灭火器、补胎工具）
• 下载离线地图备用
• 准备零食和饮用水

🏔️ 沿途亮点
"""

    if '北京' in start or '北京' in destination:
        guide += """• 天津：意式风情街、古文化街
• 济南：趵突泉、大明湖
"""
    elif '上海' in start or '上海' in destination:
        guide += """• 苏州：拙政园、周庄水乡
• 无锡：太湖鼋头渚
• 南京：中山陵、夫子庙
"""
    elif '杭州' in start or '杭州' in destination:
        guide += """• 苏州：拙政园、平江路
• 嘉兴：南湖革命纪念馆
"""
    else:
        guide += f"""• 途经{route_info['waypoints'][0] if route_info['waypoints'] else '主要城市'}
• 当地特色景点
• 沿途自然风光
"""

    guide += f"""
🍜 餐饮推荐
• 途中可选择服务区就餐或提前规划特色餐厅
• 建议品尝当地特色美食
• 准备些零食防止路上饿肚子

🏨 住宿建议
• 可选择在途经城市过夜，分段行驶
• 旺季提前预订酒店
• 选择交通便利的住宿地点

⚠️ 驾驶注意事项
• 保持安全车距，注意限速
• 疲劳驾驶时及时休息
• 关注天气预报，避免恶劣天气出行
• 高速费用预计：{(route_info['total_distance_km'] * 0.5):.0f}元左右

祝您旅途愉快！平安到达！
"""

    if preferences:
        guide += f"""
💡 根据您的偏好「{preferences}」，特别建议：
• 提前查询沿途相关景点和餐厅
• 根据偏好调整停留时间
"""

    return guide

@bp.route('/roadtrip', methods=['POST'])
@optional_auth
@api_response_cache(ttl=300)
@performance_monitor
def plan_roadtrip():
    """
    自驾游路线规划
    根据起点、终点和偏好生成路线规划和攻略
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求体为空'
            }), 400

        start = data.get('start')
        destination = data.get('destination')
        preferences = data.get('preferences', '')
        route_type = data.get('route_type', 'balanced')

        if not start or not destination:
            return jsonify({
                'status': 'error',
                'message': '起点和终点不能为空'
            }), 400

        if route_type not in ['fastest', 'scenic', 'balanced']:
            return jsonify({
                'status': 'error',
                'message': '路线类型必须是 fastest/scenic/balanced'
            }), 400

        route_id = str(uuid.uuid4())

        waypoints = generate_route_waypoints(start, destination, route_type)

        route_info = calculate_route_info(start, destination, route_type, waypoints)

        guide = generate_roadtrip_guide(start, destination, preferences, route_type, route_info)

        sample_images = [
            "https://example.com/scenery1.jpg",
            "https://example.com/scenery2.jpg"
        ]

        save_route_cache(route_id, {
            'route': route_info,
            'guide': guide,
            'preferences': preferences,
            'route_type': route_type
        })

        logger.info(f"自驾游路线规划完成: route_id={route_id}, start={start}, destination={destination}")

        return jsonify({
            'status': 'success',
            'route': route_info,
            'guide': guide,
            'images': sample_images,
            'distance_km': route_info['total_distance_km'],
            'estimated_hours': route_info['estimated_time_hours']
        })

    except Exception as e:
        logger.error(f"自驾游路线规划失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': f'路线规划失败: {str(e)}'
        }), 500

@bp.route('/roadtrip/<route_id>', methods=['GET'])
@optional_auth
@performance_monitor
def get_roadtrip(route_id):
    """
    获取已规划的路线
    根据路线ID获取缓存的路线信息
    """
    try:
        cache = load_route_cache()

        if route_id not in cache:
            return jsonify({
                'status': 'error',
                'message': '路线不存在或已过期'
            }), 404

        route_data = cache[route_id]['data']

        return jsonify({
            'status': 'success',
            'data': route_data
        })

    except Exception as e:
        logger.error(f"获取路线失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': f'获取路线失败: {str(e)}'
        }), 500

@bp.route('/roadtrip/nearby', methods=['POST'])
@optional_auth
@performance_monitor
def nearby_places():
    """
    查找附近地点
    根据坐标或城市名查找周边的景点、餐厅、加油站等
    """
    try:
        data = request.get_json() or {}
        location = data.get('location')
        latitude = data.get('latitude')
        longitude = data.get('longitude')
        place_type = data.get('type', 'all')

        if not location and (latitude is None or longitude is None):
            return jsonify({
                'status': 'error',
                'message': '位置信息不能为空'
            }), 400

        if location:
            search_location = location
        else:
            search_location = f"经度{longitude},纬度{latitude}"

        mock_results = {
            'gas_station': [
                {'name': '中石化加油站', 'distance': '1.2km', 'address': '附近主干道'},
                {'name': '中石油加油站', 'distance': '2.5km', 'address': '附近城镇'}
            ],
            'restaurant': [
                {'name': '当地特色餐厅', 'distance': '800m', 'rating': '4.5'},
                {'name': '快餐店', 'distance': '1.5km', 'rating': '4.2'}
            ],
            'attraction': [
                {'name': '当地景点', 'distance': '3km', 'rating': '4.8'}
            ]
        }

        if place_type == 'all':
            results = mock_results
        else:
            results = {place_type: mock_results.get(place_type, [])}

        return jsonify({
            'status': 'success',
            'location': search_location,
            'type': place_type,
            'results': results
        })

    except Exception as e:
        logger.error(f"查找附近地点失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': f'搜索失败: {str(e)}'
        }), 500
