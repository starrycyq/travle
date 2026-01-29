"""
小红书相关路由
"""

from flask import Blueprint, request, jsonify
from services.model import get_model_service

xiaohongshu_bp = Blueprint('xiaohongshu', __name__)

@xiaohongshu_bp.route('/vectorize', methods=['POST'])
def vectorize():
    data = request.get_json(force=True, silent=True) or {}
    text = data.get('text', '')
    if not text:
        return jsonify({"error_code": "INVALID_PARAM", "message": "Missing 'text' field", "status": "error"}), 400
    
    try:
        vectors = get_model_service().encode_text(text)
        # 如果是单条文本，返回一维数组；否则返回二维
        if isinstance(vectors, list) and len(vectors) == 1:
            vectors = vectors[0]
        return jsonify({
            "status": "success",
            "vector": vectors.tolist()
        })
    except Exception as e:
        return jsonify({"error_code": "MODEL_ERROR", "message": str(e), "status": "error"}), 500