"""
模型服务模块
处理所有AI模型的初始化和管理
"""

import os
import logging
import numpy as np
from sentence_transformers import SentenceTransformer
import spacy

logger = logging.getLogger(__name__)

class ModelService:
    """模型服务类，管理所有AI模型"""
    
    def __init__(self):
        self.model = None
        self.nlp = None
        self.use_real_model = False
        self._initialize_models()
    
    def _initialize_models(self):
        """初始化所有模型"""
        self._initialize_embedding_model()
        self._initialize_nlp_model()
    
    def _initialize_embedding_model(self):
        """初始化embedding模型"""
        # 设置环境变量以避免网络检查
        os.environ["CHROMA_TELEMETRY"] = "false"
        os.environ["TRANSFORMERS_OFFLINE"] = "1"
        os.environ["HF_HUB_OFFLINE"] = "1"
        
        # 默认模型名称
        model_name = 'all-MiniLM-L6-v2'
        cache_path = r'C:\Users\Administrator\.cache\huggingface\hub\models--sentence-transformers--all-MiniLM-L6-v2'
        
        try:
            # 尝试从配置获取模型路径
            try:
                from utils.config import Config
                cache_path = Config.EMBEDDING_MODEL_PATH
                model_name = Config.EMBEDDING_MODEL_NAME
            except ImportError:
                logger.warning("无法导入配置，使用默认模型路径")
            
            # 尝试直接缓存路径（用户确认可用的路径）
            logger.info("尝试从缓存路径加载模型...")
            self.model = SentenceTransformer(cache_path)
            logger.info("模型加载成功（直接缓存路径）")
            self.use_real_model = True
            
        except Exception as e1:
            logger.warning(f"直接缓存路径加载失败，尝试标准模型名称: {e1}")
            try:
                # 尝试标准模型名称（应该从缓存加载）
                self.model = SentenceTransformer(model_name)
                logger.info("模型加载成功（从HuggingFace缓存）")
                self.use_real_model = True
                
            except Exception as e2:
                logger.warning(f"标准模型加载失败，尝试ONNX模型: {e2}")
                try:
                    # 最后尝试本地ONNX模型
                    local_onnx_path = r'C:\Users\Administrator\.cache\chroma\onnx_models\all-MiniLM-L6-v2\onnx'
                    self.model = SentenceTransformer(local_onnx_path)
                    logger.info("模型加载成功（本地ONNX模型）")
                    self.use_real_model = True
                    
                except Exception as e3:
                    logger.warning(f"所有模型加载失败，使用模拟模式: {e3}")
                    self.use_real_model = False
                    self.model = MockModel()
    
    def _initialize_nlp_model(self):
        """初始化NLP模型"""
        try:
            self.nlp = spacy.load("zh_core_web_sm")
            logger.info("NLP模型加载成功")
        except Exception as e:
            logger.error(f"NLP模型加载失败: {e}")
            self.nlp = None
    
    def encode_text(self, texts):
        """
        将文本编码为向量
        
        Args:
            texts: 文本或文本列表
            
        Returns:
            向量或向量列表
        """
        if isinstance(texts, str):
            texts = [texts]
        
        if self.model is None:
            raise RuntimeError("模型未正确初始化")
            
        return self.model.encode(texts)
    
    def clean_text(self, text):
        """
        清洗文本
        
        Args:
            text: 输入文本
            
        Returns:
            清洗后的文本
        """
        if not self.nlp:
            logger.warning("NLP模型未初始化，返回原文本")
            return text
            
        try:
            doc = self.nlp(text)
            clean_text = " ".join([token.lemma_ for token in doc if not token.is_stop and not token.is_punct])
            # 如果清洗后为空或只包含空格，返回原始文本
            if not clean_text.strip():
                return text
            return clean_text
        except Exception as e:
            logger.error(f"文本清洗失败: {e}")
            return text
    
    def get_model_info(self):
        """获取模型信息"""
        return {
            "use_real_model": self.use_real_model,
            "nlp_available": self.nlp is not None,
            "embedding_model_type": "real" if self.use_real_model else "mock"
        }


class MockModel:
    """模拟模型类，当真实模型加载失败时使用"""
    
    def encode(self, texts):
        """生成模拟的向量"""
        import numpy as np
        # 返回随机向量（384维）
        return np.random.rand(len(texts), 384)


# 全局模型服务实例
model_service = None

def get_model_service():
    """获取模型服务实例（单例模式）"""
    global model_service
    if model_service is None:
        model_service = ModelService()
    return model_service