"""
向量数据库服务模块
封装ChromaDB操作，提供向量检索功能
"""

import os
import logging
import chromadb
from typing import List, Dict, Any, Optional, Tuple
import numpy as np

from .model import get_model_service
from modular_api.utils.config import Config

logger = logging.getLogger(__name__)

class VectorService:
    """向量数据库服务类"""
    
    def __init__(self):
        self.client = None
        self.collection = None
        self.model_service = None
        self._initialize()
    
    def _initialize(self):
        """初始化向量数据库服务"""
        try:
            # 初始化ChromaDB客户端
            self.client = chromadb.PersistentClient(path=Config.CHROMA_PATH)
            logger.info(f"ChromaDB客户端初始化成功，路径: {Config.CHROMA_PATH}")
            
            # 获取或创建集合
            self.collection = self.client.get_or_create_collection(
                name="travel_guides",
                metadata={"description": "旅游攻略向量数据库"}
            )
            logger.info(f"ChromaDB集合获取成功: travel_guides")
            
            # 获取模型服务
            self.model_service = get_model_service()
            logger.info("模型服务获取成功")
            
        except Exception as e:
            logger.error(f"向量数据库服务初始化失败: {str(e)}")
            raise
    
    def search_similar(self, query: str, limit: int = 5, filters: Optional[Dict] = None) -> Dict[str, Any]:
        """
        搜索相似的旅游攻略
        
        Args:
            query: 搜索查询文本
            limit: 返回结果数量
            filters: 过滤条件
        
        Returns:
            搜索结果字典
        """
        try:
            # 向量化查询文本
            query_embedding = self.model_service.encode_text([query])[0].tolist()
            
            # 构建查询参数
            query_params = {
                "query_embeddings": [query_embedding],
                "n_results": limit
            }
            
            # 添加过滤条件
            if filters:
                where_clause = {}
                for key, value in filters.items():
                    if value:  # 只添加非空过滤条件
                        where_clause[key] = value
                
                if where_clause:
                    query_params["where"] = where_clause
            
            # 执行查询
            results = self.collection.query(**query_params)
            
            # 格式化结果
            formatted_results = []
            if results['documents'] and len(results['documents'][0]) > 0:
                for i in range(len(results['documents'][0])):
                    doc = results['documents'][0][i]
                    metadata = results['metadatas'][0][i] if results['metadatas'] else {}
                    distance = results['distances'][0][i] if results['distances'] else None
                    
                    # 解析元数据中的images字段
                    images = []
                    if metadata and 'images' in metadata:
                        try:
                            import json
                            images = json.loads(metadata['images'])
                        except:
                            images = metadata.get('images', [])
                    
                    formatted_results.append({
                        "id": results['ids'][0][i],
                        "content": doc,
                        "score": 1.0 - (distance if distance else 0),  # 将距离转换为相似度分数
                        "metadata": {
                            "destination": metadata.get('destination', ''),
                            "images": images,
                            "original_text_length": metadata.get('original_text_length', 0)
                        }
                    })
            
            return {
                "query": query,
                "results": formatted_results,
                "total": len(formatted_results)
            }
            
        except Exception as e:
            logger.error(f"向量搜索失败: {str(e)}")
            raise
    
    def add_document(self, text: str, metadata: Dict[str, Any]) -> str:
        """
        添加文档到向量数据库
        
        Args:
            text: 文档文本
            metadata: 文档元数据
        
        Returns:
            文档ID
        """
        try:
            # 清洗文本
            clean_text = self.model_service.clean_text(text)
            
            # 向量化文本
            embedding = self.model_service.encode_text([clean_text])[0].tolist()
            
            # 生成唯一ID
            import uuid
            doc_id = str(uuid.uuid4())
            
            # 存储到ChromaDB
            self.collection.add(
                documents=[clean_text],
                embeddings=[embedding],
                metadatas=[metadata],
                ids=[doc_id]
            )
            
            logger.info(f"文档添加成功: ID={doc_id}, 长度={len(clean_text)}")
            return doc_id
            
        except Exception as e:
            logger.error(f"添加文档失败: {str(e)}")
            raise
    
    def get_document(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """
        根据ID获取文档
        
        Args:
            doc_id: 文档ID
        
        Returns:
            文档信息或None
        """
        try:
            # 从ChromaDB获取文档
            result = self.collection.get(ids=[doc_id])
            
            if not result['documents'] or len(result['documents']) == 0:
                return None
            
            # 解析元数据
            metadata = result['metadatas'][0] if result['metadatas'] else {}
            images = []
            if metadata and 'images' in metadata:
                try:
                    import json
                    images = json.loads(metadata['images'])
                except:
                    images = metadata.get('images', [])
            
            return {
                "id": doc_id,
                "content": result['documents'][0],
                "metadata": {
                    "destination": metadata.get('destination', ''),
                    "images": images,
                    "original_text_length": metadata.get('original_text_length', 0)
                }
            }
            
        except Exception as e:
            logger.error(f"获取文档失败: {str(e)}")
            raise
    
    def get_collection_info(self) -> Dict[str, Any]:
        """获取集合信息"""
        try:
            count = self.collection.count()
            return {
                "name": "travel_guides",
                "count": count,
                "model_info": self.model_service.get_model_info()
            }
        except Exception as e:
            logger.error(f"获取集合信息失败: {str(e)}")
            raise


# 全局向量服务实例
vector_service = None

def get_vector_service():
    """获取向量服务实例（单例模式）"""
    global vector_service
    if vector_service is None:
        vector_service = VectorService()
    return vector_service