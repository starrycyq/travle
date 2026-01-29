"""
攻略路由模块
处理攻略生成和上传相关的请求
"""

from flask import Blueprint, request, jsonify
import chromadb
import uuid
import json
import logging
import os
import sys

# 导入服务模块
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from services.model import get_model_service
from services.database import get_db_connection
from services.auth import auth_required, optional_auth
from services.cache import api_response_cache, query_cache
from utils.monitoring import performance_monitor
from utils.config import Config
try:
    from .. import limiter
except ImportError:
    limiter = None

logger = logging.getLogger(__name__)

# 初始化ChromaDB
def get_chroma_collection():
    """获取ChromaDB集合"""
    client = chromadb.PersistentClient(path=os.getenv('CHROMA_PATH', './chroma_db'))
    return client.get_or_create_collection(name="travel_guides")

bp = Blueprint('guide', __name__)

@bp.route('/generate-guide', methods=['POST'])
@auth_required
@api_response_cache(ttl=300)  # 缓存5分钟
@performance_monitor
def generate_guide():
    """
    生成旅游攻略
    根据用户的目的地和偏好生成旅游攻略
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"status": "error", "message": "请求体为空"}), 400
            
        destination = data.get('destination')
        preferences = data.get('preferences')
        
        if not destination or not preferences:
            return jsonify({"status": "error", "message": "缺少目的地或偏好参数"}), 400
        
        # 获取模型服务
        model_service = get_model_service()
        
        # 记录用户偏好到数据库
        conn = get_db_connection()
        c = conn.cursor()
        c.execute("INSERT INTO preferences (destination, preferences) VALUES (?, ?)", 
                  (destination, preferences))
        conn.commit()
        conn.close()
        
        # 向量检索相关攻略
        query_text = f"{preferences} {destination}"
        query_embedding = model_service.encode_text([query_text])[0].tolist()
        
        collection = get_chroma_collection()
        results = collection.query(query_embeddings=[query_embedding], n_results=5)
        
        context = " ".join(results['documents'][0]) if results['documents'] else ""
        
        # 生成攻略（这里可以集成大模型API）
        guide = f"基于{preferences}的{destination}旅游攻略：{context}。建议游览主要景点，品尝当地美食。"
        
        return jsonify({
            "status": "success", 
            "guide": guide, 
            "images": [],
            "context_length": len(context),
            "retrieved_docs": len(results['documents'][0]) if results['documents'] else 0
        })
    except Exception as e:
        logger.error(f"生成攻略失败: {str(e)}")
        return jsonify({"status": "error", "message": str(e)}), 500

@bp.route('/upload-guide', methods=['POST'])
@auth_required
def upload_guide():
    """
    上传攻略
    上传自定义的旅游攻略到系统
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"status": "error", "message": "请求体为空"}), 400
            
        text = data.get('text')
        images = data.get('images', [])
        destination = data.get('destination', '')
        
        if not text:
            return jsonify({"status": "error", "message": "缺少文本内容"}), 400
        
        # 获取模型服务
        model_service = get_model_service()
        
        # 清洗文本
        clean_text = model_service.clean_text(text)
        
        # 向量化
        embedding = model_service.encode_text([clean_text])[0].tolist()
        
        # 存入ChromaDB
        id_ = str(uuid.uuid4())
        metadata = {
            "images": json.dumps(images),
            "destination": destination,
            "original_text_length": len(text)
        }
        
        collection = get_chroma_collection()
        collection.add(
            documents=[clean_text], 
            embeddings=[embedding], 
            metadatas=[metadata], 
            ids=[id_]
        )
        
        return jsonify({
            "status": "success", 
            "message": "攻略已上传", 
            "id": id_,
            "model_info": model_service.get_model_info()
        })
    except Exception as e:
        logger.error(f"上传攻略失败: {str(e)}")
        return jsonify({"status": "error", "message": str(e)}), 500

@bp.route('/model-info', methods=['GET'])
@optional_auth
def get_model_info():
    """
    获取模型信息
    返回当前使用的模型信息
    """
    try:
        model_service = get_model_service()
        return jsonify({
            "status": "success",
            "model_info": model_service.get_model_info()
        })
    except Exception as e:
        logger.error(f"获取模型信息失败: {str(e)}")
        return jsonify({"status": "error", "message": str(e)}), 500