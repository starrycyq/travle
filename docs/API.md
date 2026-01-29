# 旅游助手 API 文档

## 基础信息

- **Base URL**: `http://121.43.58.117:5000` (生产环境)
- **Base URL**: `http://localhost:5000` (本地开发)
- **API Version**: v1.1.0
- **Content-Type**: `application/json`

## 认证方式

### JWT Token 认证

本API使用JWT (JSON Web Token) 进行身份认证。除了特殊的公开端点外，所有API请求都需要在Header中包含有效的JWT Token：

```
Authorization: Bearer {JWT_TOKEN}
```

### 获取Token

使用以下认证端点获取JWT Token：
- `POST /api/auth/login` - 用户登录获取Token
- `POST /api/auth/register` - 用户注册并获取Token
- `POST /api/auth/refresh` - 刷新过期Token

### Token 有效期

- **Access Token**: 24小时有效期
- **Token过期**: 返回401状态码，需使用refresh token获取新token
- **自动刷新**: Android客户端支持自动token刷新

### 认证状态说明

- 🔒 **必需认证**: 必须提供有效的JWT Token
- 🔓 **可选认证**: 可以提供Token以获取个性化数据
- ⚪ **公开端点**: 无需认证即可访问

## 通用响应格式

### 成功响应
```json
{
  "status": "success",
  "data": { ... }
}
```

或

```json
{
  "code": 200,
  "msg": "消息内容"
}
```

### 错误响应
```json
{
  "status": "error",
  "message": "错误描述",
  "error_code": "ERROR_CODE"
}
```

或

```json
{
  "code": 400,
  "msg": "错误描述"
}
```

### 认证错误响应
```json
{
  "status": "error",
  "message": "Token已过期",
  "error_code": "TOKEN_EXPIRED"
}
```

```json
{
  "status": "error",
  "message": "无效的认证信息",
  "error_code": "INVALID_TOKEN"
}
```

## API 端点

### 0. 认证管理 (Authentication)

#### 0.1 用户登录 ⚪

**端点**: `POST /api/auth/login`

**请求体**:
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**响应**:
```json
{
  "status": "success",
  "message": "登录成功",
  "data": {
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "token_type": "Bearer",
    "expires_in": 86400,
    "user": {
      "id": 1,
      "username": "testuser",
      "email": "test@example.com"
    }
  }
}
```

#### 0.2 用户注册 ⚪

**端点**: `POST /api/auth/register`

**请求体**:
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com"
}
```

**响应**:
```json
{
  "status": "success",
  "message": "注册成功",
  "data": {
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "token_type": "Bearer",
    "expires_in": 86400,
    "user": {
      "id": 2,
      "username": "newuser",
      "email": "newuser@example.com"
    }
  }
}
```

#### 0.3 Token刷新 🔒

**端点**: `POST /api/auth/refresh`

**请求体**:
```json
{
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**响应**:
```json
{
  "status": "success",
  "message": "Token刷新成功",
  "data": {
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "token_type": "Bearer",
    "expires_in": 86400
  }
}
```

#### 0.4 Token验证 🔒

**端点**: `POST /api/auth/verify`

**请求体**:
```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**响应**:
```json
{
  "status": "success",
  "message": "Token有效",
  "data": {
    "valid": true,
    "user_id": 1,
    "username": "testuser",
    "expires_in": 3600
  }
}
```

#### 0.5 获取当前用户信息 🔒

**端点**: `GET /api/auth/me`

**响应**:
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "created_at": "2024-01-28T10:00:00Z"
  }
}
```

#### 0.6 小红书授权信息 🔒

**端点**: `POST /api/auth/xiaohongshu`

**请求体**:
```json
{
  "auth_token": "小红书授权token",
  "user_id": "小红书用户ID",
  "expires_in": 3600
}
```

**响应**:
```json
{
  "status": "success",
  "message": "授权信息已接收，开始爬取"
}
```

### 1. 系统信息

#### 1.1 获取API信息 ⚪

**端点**: `GET /`

**响应**:
```json
{
  "status": "running",
  "apis": [
    {"path": "/input-preference", "method": "POST", "description": "记录用户偏好"},
    {"path": "/generate-guide", "method": "POST", "description": "生成旅游攻略"},
    {"path": "/upload-guide", "method": "POST", "description": "上传攻略"}
  ]
}
```

#### 1.2 获取模型信息 🔓

**端点**: `GET /model-info`

**响应**:
```json
{
  "status": "success",
  "model_info": {
    "use_real_model": true,
    "nlp_available": true,
    "embedding_model_type": "real"
  }
}
```

### 2. 用户偏好管理

#### 2.1 记录用户偏好 🔒

**端点**: `POST /input-preference`

**请求体**:
```json
{
  "destination": "杭州",
  "preferences": "喜欢自然风光，想要体验当地美食"
}
```

**响应**:
```json
{
  "status": "success",
  "message": "偏好已记录"
}
```

### 3. 攻略生成

#### 3.1 生成旅游攻略 🔒

**端点**: `POST /generate-guide`

**请求体**:
```json
{
  "destination": "杭州",
  "preferences": "喜欢自然风光，想要体验当地美食"
}
```

**响应**:
```json
{
  "status": "success",
  "guide": "生成的攻略内容",
  "images": [],
  "context_length": 1234,
  "retrieved_docs": 5
}
```

#### 3.2 上传攻略 🔒

**端点**: `POST /upload-guide`

**请求体**:
```json
{
  "text": "攻略内容",
  "images": ["图片URL1", "图片URL2"],
  "destination": "杭州"
}
```

**响应**:
```json
{
  "status": "success",
  "message": "攻略已上传",
  "id": "uuid",
  "model_info": {
    "use_real_model": true,
    "nlp_available": true,
    "embedding_model_type": "real"
  }
}
```

### 4. 社区功能

#### 4.1 获取社区动态列表 🔓

**端点**: `GET /community/list`

**查询参数**:
- `page`: 页码（默认1）
- `limit`: 每页数量（默认20）

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "content": "动态内容",
      "like_count": 10,
      "create_time": "2024-01-01 12:00:00"
    }
  ]
}
```

#### 4.2 发布动态 🔒

**端点**: `POST /community/publish`

**请求体**:
```json
{
  "content": "动态内容",
  "destination": "杭州",
  "images": ["图片URL1", "图片URL2"]
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "发布成功",
  "data": {
    "post_id": 1,
    "anonymous_id": "xxx"
  }
}
```

#### 4.3 点赞动态 🔒

**端点**: `POST /community/like`

**请求体**:
```json
{
  "post_id": 1
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "点赞成功"
}
```

#### 4.4 获取评论列表 🔓

**端点**: `GET /community/{post_id}/comments`

**查询参数**:
- `page`: 页码（默认1）
- `limit`: 每页数量（默认20）

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "post_id": 1,
      "content": "评论内容",
      "author_name": "匿名用户",
      "create_time": "2024-01-01 12:00:00",
      "like_count": 5
    }
  ]
}
```

#### 4.5 添加评论 🔒

**端点**: `POST /community/{post_id}/comments`

**请求体**:
```json
{
  "content": "评论内容",
  "author_name": "用户名"
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "评论成功",
  "data": {
    "comment_id": 1
  }
}
```

#### 4.6 评论点赞 🔒

**端点**: `POST /community/comments/{comment_id}/like`

**响应**:
```json
{
  "code": 200,
  "msg": "评论点赞成功"
}
```

### 5. 小红书授权与爬取

#### 5.1 接收小红书授权信息 🔒

**端点**: `POST /api/auth/xiaohongshu`

**请求体**:
```json
{
  "auth_token": "授权token",
  "user_id": "用户ID",
  "expires_in": 3600
}
```

**响应**:
```json
{
  "status": "success",
  "message": "授权信息已接收，开始爬取"
}
```

### 6. 旅游攻略搜索

#### 6.1 搜索旅游攻略 🔒

**端点**: `POST /api/search/guides`

**请求体**:
```json
{
  "query": "杭州三日游攻略",
  "filters": {
    "destination": "杭州",
    "duration": "3天",
    "budget": "中等"
  },
  "limit": 5
}
```

**响应**:
```json
{
  "status": "success",
  "data": {
    "query": "杭州三日游攻略",
    "results": []
  }
}
```

## 错误码

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或Token无效 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

### 认证错误码

| 错误码 | 说明 |
|--------|------|
| TOKEN_EXPIRED | Token已过期 |
| INVALID_TOKEN | 无效的Token |
| MISSING_TOKEN | 缺少认证Token |
| INVALID_CREDENTIALS | 用户名或密码错误 |
| USER_EXISTS | 用户已存在 |
| USER_NOT_FOUND | 用户不存在 |
| REFRESH_TOKEN_EXPIRED | 刷新Token已过期 |
| INVALID_REFRESH_TOKEN | 无效的刷新Token |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 授权信息无效 |
| 1002 | 爬取任务失败 |
| 1003 | 向量数据库错误 |
| 1004 | 大模型API调用失败 |
| 1005 | 输入验证失败 |
| 1006 | 数据格式错误 |

## 速率限制

### 未认证请求
- 每个IP地址每分钟最多请求30次
- 超过限制将返回429状态码

### 已认证请求
- 每个用户每分钟最多请求120次
- 超过限制将返回429状态码

### 特殊端点限制
- 登录/注册端点：每IP每分钟最多5次
- Token刷新端点：每用户每分钟最多10次
- 文件上传端点：每用户每分钟最多20次

## 注意事项

1. 所有时间戳格式为 `YYYY-MM-DD HH:mm:ss`
2. post_id, comment_id为整数类型
3. 攻略id为字符串类型（UUID格式）
4. 图片URL必须是完整的HTTP/HTTPS地址
5. 文本字段建议限制在10000字符以内
6. 分页查询默认返回20条记录，最大100条

## 更新日志

### v1.2.0 (2024-01-28) - 🔒 安全与认证
- ✅ **JWT认证系统**：完整的用户认证和授权
- ✅ **6个认证端点**：login, register, refresh, verify, me, xiaohongshu
- ✅ **API安全保护**：100%核心端点需要认证
- ✅ **输入验证系统**：多层验证，防注入攻击
- ✅ **Token管理**：24小时有效期，自动刷新机制
- ✅ **安全配置**：CORS、安全头、HTTPS支持
- ✅ **速率限制**：区分认证和非认证用户
- ✅ **错误处理**：标准化认证错误响应

### v1.1.0 (2024-01-27) - 🚀 功能增强
- ✅ 新增 `GET /model-info` 端点，获取模型状态信息
- ✅ 新增评论功能：`GET /community/{post_id}/comments`, `POST /community/{post_id}/comments`
- ✅ 新增评论点赞：`POST /community/comments/{comment_id}/like`
- ✅ 攻略生成响应新增 `retrieved_docs` 字段
- ✅ 攻略上传响应新增 `model_info` 字段
- ✅ 统一响应格式，规范错误处理

### v1.0.0 (2024-01-01)
- ✅ 基础功能实现
- ✅ 核心API端点
- ✅ 社区功能
- ✅ 攻略生成和搜索