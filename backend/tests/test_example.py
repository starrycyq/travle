import pytest
import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

def test_example():
    """一个简单的示例测试"""
    assert 1 == 1


def test_imports():
    """测试能否正确导入项目模块"""
    try:
        from modular_api.app import create_app
        assert True
    except ImportError:
        assert False, "无法导入modular_api.app"