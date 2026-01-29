"""
数据库服务模块
处理数据库连接和初始化
"""

import sqlite3
import os
try:
    from utils.config import Config
    USE_CONFIG = True
except ImportError:
    USE_CONFIG = False

def get_db_connection():
    """根据环境变量决定数据库连接方式"""
    # 优先使用Config.DATABASE_PATH，否则使用环境变量，最后使用默认值
    if USE_CONFIG and hasattr(Config, 'DATABASE_PATH'):
        database_path = Config.DATABASE_PATH
    else:
        database_path = os.getenv('DATABASE_PATH', './preferences.db')
    
    if os.getenv('DATABASE_URL'):  # 如果设置了DATABASE_URL，认为是在云环境中
        # 这里可以连接到云数据库，如PostgreSQL
        import psycopg2
        conn = psycopg2.connect(os.getenv('DATABASE_URL'))
    else:
        # 本地SQLite数据库
        conn = sqlite3.connect(database_path)
        conn.row_factory = sqlite3.Row
    return conn

def init_db():
    """初始化数据库表"""
    conn = get_db_connection()
    c = conn.cursor()
    # 用户偏好表
    c.execute('''CREATE TABLE IF NOT EXISTS preferences
                 (id INTEGER PRIMARY KEY, destination TEXT, preferences TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)''')
    # 社区动态表（预留扩展字段）
    c.execute('''CREATE TABLE IF NOT EXISTS community_post
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  content TEXT NOT NULL,
                  destination TEXT,
                  like_count INTEGER DEFAULT 0,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  anonymous_id TEXT,
                  images TEXT)''')
    # 社区评论表
    c.execute('''CREATE TABLE IF NOT EXISTS community_comment
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  post_id INTEGER NOT NULL,
                  content TEXT NOT NULL,
                  author_name TEXT DEFAULT '匿名用户',
                  like_count INTEGER DEFAULT 0,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY (post_id) REFERENCES community_post(id))''')
    # 小红书授权信息表
    c.execute('''CREATE TABLE IF NOT EXISTS xiaohongshu_auth
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id TEXT NOT NULL,
                  auth_token TEXT NOT NULL,
                  scraper_user_id TEXT NOT NULL,
                  expires_at DATETIME,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE(user_id, scraper_user_id))''')
    # 爬虫任务表
    c.execute('''CREATE TABLE IF NOT EXISTS scraper_tasks
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  task_id TEXT UNIQUE NOT NULL,
                  user_id TEXT NOT NULL,
                  keywords TEXT,  -- JSON数组
                  max_posts INTEGER DEFAULT 10,
                  status TEXT DEFAULT 'pending',
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  started_at DATETIME,
                  completed_at DATETIME,
                  results TEXT,   -- JSON对象
                  error TEXT)''')
    conn.commit()
    conn.close()