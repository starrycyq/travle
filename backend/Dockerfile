# 使用官方Python运行时作为父镜像
FROM python:3.10-slim

# 设置工作目录
WORKDIR /app

# 复制requirements文件
COPY requirements.txt .

# 安装依赖包
RUN pip install --no-cache-dir -r requirements.txt

# 安装中文语言模型
RUN python -m spacy download zh_core_web_sm

# 复制应用程序代码
COPY . .

# 使端口可供外部访问
EXPOSE 5000

# 定义环境变量
ENV FLASK_APP=app.py
ENV CHROMA_PATH=/data/chroma_db
ENV DATABASE_PATH=/data/preferences.db

# 创建数据目录
RUN mkdir -p /data/chroma_db

# 运行应用程序
CMD ["python", "app.py"]