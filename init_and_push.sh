#!/bin/bash

echo "开始初始化Git仓库并推送代码..."

# 检查是否已在Git仓库中
if [ -d ".git" ]; then
  echo "目录已经是Git仓库"
else
  echo "初始化Git仓库..."
  git init
fi

# 添加所有文件到暂存区
echo "添加所有文件到暂存区..."
git add .

# 检查是否有远程仓库设置
remote_url=$(git remote get-url origin 2>/dev/null)
if [ -z "$remote_url" ]; then
  echo "请输入您的GitHub仓库地址（例如：https://github.com/username/repo-name.git）:"
  read repo_url
  
  if [ -z "$repo_url" ]; then
    echo "错误：必须提供仓库URL才能继续"
    exit 1
  fi
  
  echo "添加远程仓库..."
  git remote add origin "$repo_url"
fi

# 创建初始提交
echo "创建初始提交..."
git commit -m "Initial commit: Add CI/CD workflow and test framework

- Added comprehensive test framework for both backend and Android app
- Implemented GitHub Actions CI/CD workflow
- Created test configuration files for pytest, tox, coverage
- Added Docker Compose configuration for test environments"

# 推送到远程仓库
echo "推送到远程仓库..."
git branch -M main
git push -u origin main

echo "代码已成功推送至远程仓库！"
echo ""
echo "接下来请按照以下步骤启用CI/CD："
echo "1. 在GitHub仓库中，进入 Settings > Actions > General"
echo "2. 确保允许工作流运行"
echo "3. 如果需要访问外部API（如阿里云API），请在GitHub Secrets中添加所需密钥："
echo "   - Settings > Secrets and variables > Actions"
echo "   - 添加 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET 等密钥"