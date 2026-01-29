"""
聊天路由模块
处理AI对话相关的请求
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
from utils.database_optimizer import db_operation

try:
    from .. import limiter
except ImportError:
    limiter = None

logger = logging.getLogger(__name__)

bp = Blueprint('chat', __name__)

CONVERSATION_HISTORY_FILE = './data/conversation_history.json'

def load_conversation_history():
    """加载对话历史"""
    try:
        import json
        data_dir = os.path.dirname(CONVERSATION_HISTORY_FILE)
        if not os.path.exists(data_dir):
            os.makedirs(data_dir, exist_ok=True)
        if os.path.exists(CONVERSATION_HISTORY_FILE):
            with open(CONVERSATION_HISTORY_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}
    except Exception as e:
        logger.error(f"加载对话历史失败: {e}")
        return {}

def save_conversation_history(history):
    """保存对话历史"""
    try:
        import json
        with open(CONVERSATION_HISTORY_FILE, 'w', encoding='utf-8') as f:
            json.dump(history, f, ensure_ascii=False, indent=2)
    except Exception as e:
        logger.error(f"保存对话历史失败: {e}")

def get_conversation_messages(conversation_id, max_messages=20):
    """获取对话消息列表"""
    history = load_conversation_history()
    messages = history.get(conversation_id, [])
    return messages[-max_messages:] if len(messages) > max_messages else messages

def add_message_to_conversation(conversation_id, role, content):
    """添加消息到对话"""
    history = load_conversation_history()
    if conversation_id not in history:
        history[conversation_id] = []
    history[conversation_id].append({
        'role': role,
        'content': content,
        'timestamp': str(uuid.uuid4())
    })
    if len(history[conversation_id]) > 100:
        history[conversation_id] = history[conversation_id][-100:]
    save_conversation_history(history)

@bp.route('/chat', methods=['POST'])
@optional_auth
@api_response_cache(ttl=60)
@performance_monitor
def chat():
    """
    AI聊天接口
    处理用户消息并返回AI响应
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求体为空'
            }), 400

        message = data.get('message')
        conversation_id = data.get('conversation_id')
        context = data.get('context', {})

        if not message:
            return jsonify({
                'status': 'error',
                'message': '消息内容不能为空'
            }), 400

        if not conversation_id:
            conversation_id = str(uuid.uuid4())

        conversation_history = get_conversation_messages(conversation_id)

        user_context = ""
        if context:
            user_context = f"用户额外信息: {context}。"

        if conversation_history:
            history_text = ""
            for msg in conversation_history[-10:]:
                role = "用户" if msg['role'] == 'user' else "助手"
                history_text += f"{role}: {msg['content']}\n"
            user_context += f"\n历史对话:\n{history_text}"

        model_service = get_model_service()
        prompt = f"""
你是一个智能旅行助手，帮助用户规划旅行、推荐景点、解答旅行相关问题。

用户消息: {message}
{user_context}

请给出有用、友好的回复，如果是旅行相关问题，尽量提供具体实用的建议。
"""
        try:
            response = model_service.generate_response(prompt)
        except Exception as model_error:
            logger.warning(f"模型生成失败，使用模拟响应: {model_error}")
            response = generate_mock_travel_response(message)

        add_message_to_conversation(conversation_id, 'user', message)
        add_message_to_conversation(conversation_id, 'assistant', response)

        logger.info(f"聊天请求处理完成: conversation_id={conversation_id}, message_len={len(message)}")

        return jsonify({
            'status': 'success',
            'data': {
                'response': response,
                'conversation_id': conversation_id
            }
        })

    except Exception as e:
        logger.error(f"聊天处理失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': f'处理消息时出错: {str(e)}'
        }), 500

@bp.route('/chat/history', methods=['POST'])
@optional_auth
@performance_monitor
def chat_history():
    """
    获取聊天历史
    返回指定会话的消息历史
    """
    try:
        data = request.get_json() or {}
        conversation_id = data.get('conversation_id')
        limit = data.get('limit', 50)

        if not conversation_id:
            return jsonify({
                'status': 'error',
                'message': '会话ID不能为空'
            }), 400

        history = load_conversation_history()
        messages = history.get(conversation_id, [])

        messages = messages[-limit:] if len(messages) > limit else messages

        return jsonify({
            'status': 'success',
            'data': {
                'conversation_id': conversation_id,
                'messages': messages,
                'total_count': len(messages)
            }
        })

    except Exception as e:
        logger.error(f"获取聊天历史失败: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': f'获取历史记录时出错: {str(e)}'
        }), 500

def generate_mock_travel_response(message):
    """生成模拟的旅行相关回复"""
    message_lower = message.lower()

    if any(word in message_lower for word in ['北京', '北京景点', '北京旅游']):
        return """推荐北京经典景点：
1. 故宫 - 世界最大的宫殿建筑群
2. 长城 - 八达岭或慕田峪段
3. 天安门广场 - 世界最大的城市广场
4. 颐和园 - 中国清朝时期皇家园林
5. 天坛 - 古代皇帝祭天、祈谷的圣地

建议游玩天数：4-5天
最佳季节：春秋两季"""

    elif any(word in message_lower for word in ['上海', '上海景点', '上海旅游']):
        return """推荐上海热门景点：
1. 外滩 - 万国建筑博览群
2. 东方明珠 - 上海标志性建筑
3. 田子坊 - 文艺小资聚集地
4. 上海迪士尼乐园
5. 南京路步行街 - 中国第一商业街

建议游玩天数：3-4天"""

    elif any(word in message_lower for word in ['杭州', '西湖']):
        return """杭州西湖必游景点：
1. 苏堤春晓 - 苏轼主持修建
2. 断桥残雪 - 白娘子传说发生地
3. 雷峰塔 - 可俯瞰西湖全景
4. 三潭印月 - 人民币一元纸币背面图案
5. 灵隐寺 - 千年古刹

建议环湖骑行或步行，感受江南水乡之美"""

    elif any(word in message_lower for word in ['美食', '好吃', '推荐美食']):
        return """中国各地美食推荐：
1. 北京 - 烤鸭、铜锅涮肉
2. 成都 - 火锅、串串香、担担面
3. 广州 - 早茶、烧腊、煲仔饭
4. 西安 - 肉夹馍、羊肉泡馍、凉皮
5. 上海 - 生煎包、小笼包、本帮菜

想了解更多特色美食，可以告诉我具体城市！"""

    elif any(word in message_lower for word in ['住宿', '酒店', '订房']):
        return """旅行住宿建议：
1. 旺季提前预订，景点附近价格较高
2. 交通便利比位置更重要
3. 连锁酒店性价比较高
4. 民宿体验当地生活是不错的选择
5. 查看评价时重点关注卫生和位置

您需要推荐具体城市的住宿吗？"""

    else:
        return f"""您好！我收到您的消息：「{message[:50]}...」

作为您的智能旅行助手，我可以帮您：
- 推荐热门景点和玩法
- 规划旅行路线
- 介绍当地美食
- 提供住宿建议
- 解答旅行相关问题

请告诉我您想去哪里旅行，或者有什么具体问题？"""
