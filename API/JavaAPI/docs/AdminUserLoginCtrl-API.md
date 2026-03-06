# 管理员用户接口文档

**基础路径（Base Path）**：`/api/admin`

---

## 接口列表

| 接口名称 | 请求方法 | 路由 |
|----------|----------|------|
| 管理员登录 | POST | `/api/admin/login` |
| 管理员注册 | POST | `/api/admin/register` |

---

## 1. 管理员登录

### 接口描述

管理员通过用户名和密码进行身份验证，登录成功后返回管理员用户信息。若用户名或密码错误，则返回认证失败的错误响应。

### 请求信息

| 属性 | 值 |
|------|----|
| **请求方法** | `POST` |
| **请求路由** | `/api/admin/login` |
| **参数位置** | Query Parameter（URL 查询参数） |
| **Content-Type** | `application/x-www-form-urlencoded` 或直接拼接在 URL 中 |

### 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| `username` | `String` | ✅ 必填 | 管理员用户名 |
| `password` | `String` | ✅ 必填 | 管理员密码（明文） |

### 请求示例

```
POST /api/admin/login?username=admin&password=123456
```

### 返回值

返回统一响应体 `ApiResponse<AdminUser>`。

#### 响应体结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `code` | `int` | 业务状态码 |
| `msg` | `String` | 响应描述信息 |
| `timestamp` | `long` | 服务器响应时间戳（毫秒） |
| `data` | `AdminUser` \| `null` | 登录成功时返回管理员用户对象，失败时为 `null` |

#### `data` 字段结构（`AdminUser`）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | `long` | 管理员用户 ID（主键，自增） |
| `username` | `String` | 管理员用户名 |
| `password` | `String` | 管理员密码（返回时包含原始密码，建议后续处理脱敏） |

### 响应示例

**登录成功（HTTP 200）**

```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1741234567890,
  "data": {
    "id": 1,
    "username": "admin",
    "password": "123456"
  }
}
```

**登录失败 - 用户名或密码错误（HTTP 200）**

```json
{
  "code": 4011,
  "msg": "Incorrect password or name.",
  "timestamp": 1741234567890,
  "data": null
}
```

### 业务状态码说明

| 状态码 | 含义 |
|--------|------|
| `2000` | 登录成功 |
| `4011` | 用户名或密码错误 |

---

## 2. 管理员注册

### 接口描述

注册一个新的管理员账户。请求体中需包含用户名和密码信息。注册成功后返回新建的管理员用户信息（密码字段将被脱敏为 `***************`）；若用户名已存在，则返回冲突错误响应。

### 请求信息

| 属性 | 值 |
|------|----|
| **请求方法** | `POST` |
| **请求路由** | `/api/admin/register` |
| **参数位置** | Request Body（请求体） |
| **Content-Type** | `application/json` |

### 请求参数（Request Body）

请求体为 `AdminUser` 对象的 JSON 表示。

| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| `username` | `String` | ✅ 必填 | 管理员用户名，需全局唯一 |
| `password` | `String` | ✅ 必填 | 管理员密码（明文） |

> `id` 字段由数据库自动生成，无需在请求中提供。

### 请求示例

```json
{
  "username": "newadmin",
  "password": "securepassword"
}
```

### 返回值

返回统一响应体 `ApiResponse<AdminUser>`。

#### 响应体结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `code` | `int` | 业务状态码 |
| `msg` | `String` | 响应描述信息 |
| `timestamp` | `long` | 服务器响应时间戳（毫秒） |
| `data` | `AdminUser` \| `null` | 注册成功时返回新建的管理员用户对象，失败时为 `null` |

#### `data` 字段结构（`AdminUser`）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | `long` | 新建的管理员用户 ID（主键，自增） |
| `username` | `String` | 管理员用户名 |
| `password` | `String` | 固定为 `***************`（已脱敏） |

### 响应示例

**注册成功（HTTP 200）**

```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1741234567890,
  "data": {
    "id": 2,
    "username": "newadmin",
    "password": "***************"
  }
}
```

**注册失败 - 用户名已存在（HTTP 200）**

```json
{
  "code": 4092,
  "msg": "Username already exists.",
  "timestamp": 1741234567890,
  "data": null
}
```

### 业务状态码说明

| 状态码 | 含义 |
|--------|------|
| `2000` | 注册成功 |
| `4092` | 用户名已存在 |

---

## 附录：通用业务状态码

| 状态码 | 枚举值 | 含义 |
|--------|--------|------|
| `2000` | `SUCCESS` | 操作成功 |
| `4001` | `PARAM_MISSING` | 缺少必要参数 |
| `4002` | `PARAM_TYPE_ERROR` | 参数类型错误 |
| `4011` | `ACCOUNT_PASSWORD_ERROR` | 用户名或密码错误 |
| `4092` | `USERNAME_EXISTS` | 用户名已存在 |
| `5000` | `INTERNAL_ERROR` | 服务器内部错误 |

