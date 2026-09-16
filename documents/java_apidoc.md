# Java 后端接口参考（供前端参考）

说明：本文档列出 BUCT 教学辅助系统 Java 后端（Spring Boot）已实现的全部控制层接口。包含请求方法、路由、接受的参数与类型、返回的响应体字段与含义，以及接口功能的简要描述。

---

## 通用说明

- **基础地址**：`http://<服务器IP>:80`（默认端口，由 `application.properties` 中 `server.port` 定义，默认 `80`，可通过环境变量 `SERVER_PORT` 修改）
- **统一响应结构**（JSON）：

  | 字段 | 类型 | 说明 |
  |------|------|------|
  | `code` | int | 业务状态码，`2000` 表示成功，其他值见下表 |
  | `msg` | string | 操作结果描述 |
  | `timestamp` | long | 服务器响应时间戳（毫秒） |
  | `data` | object \| array \| null | 实际返回数据（成功时） |

- **常用业务状态码**：

  | code | 说明 |
  |------|------|
  | 2000 | 成功 |
  | 4001 | 缺少必要参数 |
  | 4002 | 参数类型错误 |
  | 4003 | 参数格式错误 |
  | 4004 | 上传文件过大 |
  | 4005 | 不支持的上传内容类型 |
  | 4006 | 图片/媒体地址不合法 |
  | 4011 | 用户名或密码错误 |
  | 4012 | 登录已过期 |
  | 4013 | 无效的 Token / 未登录 |
  | 4014 | 验证码无效或已过期 |
  | 4015 | 验证码发送失败 |
  | 4031 | 无权限 |
  | 4041 | 用户不存在 |
  | 4042 | 资源不存在 |
  | 4091 | 实体已存在 |
  | 4092 | 用户名已存在 |
  | 4093 | 邮箱已存在 |
  | 4094 | 手机号已存在 |
  | 4095 | 该用户已绑定身份 |
  | 4096 | 该身份已被其他用户绑定 |
  | 4097 | 用户尚未绑定任何身份 |
  | 4098 | 绑定身份类型冲突 |
  | 4099 | 已关注该用户 |
  | 4100 | 尚未关注该用户 |
  | 4101 | 实名认证失败 |
  | 5000 | 服务器内部错误 |

- **请求体格式**：含 JSON body 的接口需设置 `Content-Type: application/json`
- **Session 认证**：登录接口成功后，认证信息保存在服务端 Session 中，前端需通过 Cookie 携带 `JSESSIONID` 进行后续请求。Session 默认超时时间为 **5 分钟**（由 `server.servlet.session.timeout` 配置，生产环境建议调整为更合理的值，如 30 分钟）。

---

## 目录（按模块）

- [管理员认证模块](#管理员认证模块-apiadmin)
- [AI评判用户模块](#ai评判用户模块-apiaijudegment)
- [用户认证模块](#用户认证模块-apiuserauth)
- [用户身份绑定模块](#用户身份绑定模块-apiuserbinding)
- [用户资料模块](#用户资料模块-apiuserprofile)
- [课程管理模块](#课程管理模块-apicourse)
- [课程访问量模块](#课程访问量模块-apicourseview)
- [课程热门排行模块](#课程热门排行模块-apicoursepopularity)
- [教师管理模块](#教师管理模块-apiteacher)
- [学生管理模块](#学生管理模块-apistudents)
- [学生选课模块](#学生选课模块-apistudent-courses)
- [笔记模块](#笔记模块-apinotes)
- [评论模块](#评论模块-apicomments)
- [文件提取模块](#文件提取模块-apifileextract)
- [AI报告生成模块](#ai报告生成模块)
- [机构管理模块](#机构管理模块-apiorganization)
- [媒体上传模块](#媒体上传模块-apimedia)
- [课程视频模块](#课程视频模块-apicoursevideo)
- [关注关系模块](#关注关系模块-apiuserfollow)
- [教参模块](#教参模块-apiteaching-materials)
- [学习统计模块](#学习统计模块-apilearning)
---

## 管理员认证模块 `/api/admin`

### POST /api/admin/login
- **描述**：管理员登录接口，使用用户名和密码进行认证。
- **请求方式**：`POST`，`application/x-www-form-urlencoded` 或 `Query Params`
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `username` | string | 是 | 管理员用户名 |
  | `password` | string | 是 | 管理员密码 |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": 1,
      "username": "admin",
      "password": "***************"
    }
  }
  ```
- **返回**（失败）：`code: 4011`，用户名或密码错误

---

### POST /api/admin/register
- **描述**：注册新管理员账号。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `username` | string | 是 | 管理员用户名 |
  | `password` | string | 是 | 管理员密码（明文，后端加密存储） |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": 2,
      "username": "newadmin",
      "password": "***************"
    }
  }
  ```
  > 注意：返回的 `password` 字段已脱敏为星号。
- **返回**（失败）：`code: 4092`，用户名已存在

---

## AI评判用户模块 `/api/aijudegment`

> 该模块为作业/报告 AI 批改功能提供独立的用户系统。

### POST /api/aijudegment/login
- **描述**：AI评判用户登录，登录成功后将认证信息持久化至 Session。
- **请求方式**：`POST`，`application/x-www-form-urlencoded` 或 `Query Params`
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `username` | string | 是 | 用户名 |
  | `password` | string | 是 | 密码 |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": 1,
      "username": "judgeuser",
      "password": "***"
    }
  }
  ```
- **返回**（失败）：`code: 4011`，用户名或密码错误

---

### POST /api/aijudegment/register
- **描述**：注册新 AI评判用户账号。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `username` | string | 是 | 用户名 |
  | `password` | string | 是 | 密码（明文，后端加密存储） |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": 2,
      "username": "newjudgeuser",
      "password": "****************"
    }
  }
  ```
  > 注意：返回的 `password` 字段已脱敏。
- **返回**（失败）：`code: 4092`，用户名已存在

---

## 用户认证模块 `/api/user/auth`

> 面向普通用户（学生/教师）的统一认证模块，支持密码登录、手机验证码登录和邮箱验证码登录。

### POST /api/user/auth/login
- **描述**：统一登录接口，支持三种登录方式：用户名密码、手机验证码、邮箱验证码。登录成功后认证信息写入 Session。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `loginType` | string (enum) | 是 | 登录方式：`PASSWORD` / `SMS_CODE` / `EMAIL_CODE` |
  | `username` | string | 条件必填 | 用户名（`PASSWORD` 登录时必填） |
  | `password` | string | 条件必填 | 密码（`PASSWORD` 登录时必填） |
  | `telephone` | string | 条件必填 | 手机号（`SMS_CODE` 登录时必填） |
  | `email` | string | 条件必填 | 邮箱（`EMAIL_CODE` 登录时必填） |
  | `code` | string | 条件必填 | 验证码（`SMS_CODE` 或 `EMAIL_CODE` 登录时必填） |

- **请求示例（密码登录）**：
  ```json
  {
    "loginType": "PASSWORD",
    "username": "zhangsan",
    "password": "mypassword"
  }
  ```
- **请求示例（手机验证码登录）**：
  ```json
  {
    "loginType": "SMS_CODE",
    "telephone": "13800138000",
    "code": "123456"
  }
  ```
- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": 1,
      "username": "zhangsan",
      "telephone": "13800138000",
      "email": "zhangsan@example.com",
      "password": "...",
      "userType": "STUDENT",
      "teacher": null,
      "student": { ... }
    }
  }
  ```

  | 返回字段 | 类型 | 说明 |
  |----------|------|------|
  | `id` | long | 用户ID |
  | `username` | string | 用户名 |
  | `telephone` | string | 手机号 |
  | `email` | string | 邮箱 |
  | `password` | string | 密码（已加密，不可用） |
  | `userType` | string | 用户类型：`TEACHER` 或 `STUDENT`（绑定身份后设置，未绑定时为 null） |
  | `teacher` | object \| null | 关联的教师信息（`userType` 为 `TEACHER` 时有值，结构见下表） |
  | `teacher.id` | long | 教师数据库ID |
  | `teacher.name` | string | 教师姓名 |
  | `teacher.organization` | string | 所属单位/院系 |
  | `teacher.gender` | string | 性别 |
  | `teacher.education` | string | 最高学历 |
  | `teacher.jointime` | string | 入职时间 |
  | `student` | object \| null | 关联的学生信息（`userType` 为 `STUDENT` 时有值，结构见下表） |
  | `student.id` | long | 学生数据库ID |
  | `student.studentNumber` | string | 学号 |
  | `student.name` | string | 学生姓名 |
  | `student.className` | string | 班级 |
  | `student.gender` | string | 性别 |
  | `student.admissionDate` | string | 入学日期（格式 `YYYY-MM-DD`） |

- **返回**（失败）：`code: 4011`，账号或密码错误；`code: 4001`，缺少必要参数

---

### POST /api/user/auth/register
- **描述**：统一注册接口，支持手机验证码和邮箱验证码两种注册方式。注册时不绑定身份，后续通过绑定接口设置用户类型。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `registerType` | string (enum) | 是 | 注册方式：`SMS_CODE` / `EMAIL_CODE` |
  | `username` | string | 是 | 用户名 |
  | `password` | string | 是 | 密码（明文，后端加密存储） |
  | `telephone` | string | 条件必填 | 手机号（`SMS_CODE` 注册时必填） |
  | `email` | string | 条件必填 | 邮箱（`EMAIL_CODE` 注册时必填） |
  | `code` | string | 条件必填 | 验证码 |

- **返回**（成功）：同登录接口，返回新建的 User 对象（`userType` 为 null）
- **返回**（失败）：
  - `code: 4014`：验证码无效或已过期
  - `code: 4094`：手机号已被注册
  - `code: 4093`：邮箱已被注册
  - `code: 4092`：用户名已存在

---

### POST /api/user/auth/send-code
- **描述**：发送验证码，支持发送短信验证码和邮箱验证码，用于注册或登录前调用。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `sendType` | string (enum) | 是 | 发送方式：`SMS` / `EMAIL` |
  | `telephone` | string | 条件必填 | 手机号（`SMS` 类型时必填） |
  | `email` | string | 条件必填 | 邮箱（`EMAIL` 类型时必填） |

- **返回**（成功）：
  ```json
  { "code": 2000, "msg": "Ok.", "timestamp": 1700000000000, "data": null }
  ```
- **返回**（失败）：`code: 4015`，验证码发送失败

---

### POST /api/user/auth/logout
- **描述**：登出当前用户，使 Session 失效并清除服务端安全上下文。
- **请求方式**：`POST`，无请求体
- **返回**（成功）：
  ```json
  { "code": 2000, "msg": "Ok.", "timestamp": 1700000000000, "data": null }
  ```

---

### GET /api/user/auth/current
- **描述**：获取当前已登录的用户信息（依赖 Session）。
- **请求方式**：`GET`，无请求参数
- **返回**（成功）：同登录接口 `data` 字段，返回当前 User 对象；其中 `avatar` 为头像地址（站内路径或 `http(s)` 外链），未设置时为 `null`
- **说明**：前端导航栏头像读取的正是该字段的 `avatar`。若需要学生/教师详情与机构资料，
  请改用 `/api/user/profile/me`（本接口返回的是 Session 中的游离实体，不含这些信息）
- **返回**（失败）：`code: 4013`，未登录或 Token 无效

---

## 用户身份绑定模块 `/api/user/binding`

> 用于将已注册的用户账号绑定到学生或教师实体，绑定后 `userType` 将自动设置。需要先登录。

### POST /api/user/binding/bind
- **描述**：绑定学生或教师身份。需要携带 Session 凭证（已登录）。系统将通过姓名和身份证号进行实名认证。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `bindingType` | string (enum) | 是 | 绑定类型：`STUDENT` / `TEACHER` |
  | `name` | string | 是 | 真实姓名 |
  | `idCard` | string | 是 | 身份证号，用于实名认证 |
  | `studentNumber` | string | 条件必填 | 学号（`STUDENT` 绑定时必填） |
  | `employeeNumber` | string | 条件必填 | 工号（`TEACHER` 绑定时必填） |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "userId": 1,
      "username": "zhangsan",
      "userType": "STUDENT",
      "studentInfo": {
        "id": 10,
        "studentNumber": "2024001",
        "name": "张三",
        "className": "计算机2401",
        "gender": "男",
        "admissionDate": "2024-09-01"
      },
      "teacherInfo": null
    }
  }
  ```

  | 返回字段 | 类型 | 说明 |
  |----------|------|------|
  | `userId` | long | 用户ID |
  | `username` | string | 用户名 |
  | `userType` | string | 绑定后的用户类型：`STUDENT` 或 `TEACHER` |
  | `studentInfo` | object \| null | 绑定的学生信息（`STUDENT` 类型时有值） |
  | `studentInfo.id` | long | 学生数据库ID |
  | `studentInfo.studentNumber` | string | 学号 |
  | `studentInfo.name` | string | 姓名 |
  | `studentInfo.className` | string | 班级 |
  | `studentInfo.gender` | string | 性别 |
  | `studentInfo.admissionDate` | string | 入学日期（格式 `YYYY-MM-DD`） |
  | `teacherInfo` | object \| null | 绑定的教师信息（`TEACHER` 类型时有值） |
  | `teacherInfo.id` | long | 教师数据库ID |
  | `teacherInfo.name` | string | 姓名 |
  | `teacherInfo.organization` | string | 所属单位/院系 |
  | `teacherInfo.gender` | string | 性别 |
  | `teacherInfo.education` | string | 学历 |
  | `teacherInfo.jointime` | string | 入职时间 |

- **返回**（失败）：
  - `code: 4013`：未登录
  - `code: 4101`：实名认证失败
  - `code: 4095`：该用户已绑定身份
  - `code: 4096`：该身份已被其他用户绑定
  - `code: 4098`：绑定类型与已有身份冲突

---

### POST /api/user/binding/unbind
- **描述**：解绑当前用户的学生/教师身份。需要先登录。
- **请求方式**：`POST`，无请求体
- **返回**（成功）：
  ```json
  { "code": 2000, "msg": "Ok.", "timestamp": 1700000000000, "data": null }
  ```
- **返回**（失败）：
  - `code: 4013`：未登录
  - `code: 4097`：用户尚未绑定任何身份

---

### GET /api/user/binding/info
- **描述**：获取当前已登录用户的身份绑定信息。需要先登录。
- **请求方式**：`GET`，无请求参数
- **返回**（成功）：同 `/bind` 接口的 `data` 字段结构，未绑定时 `userType` 为 null，`studentInfo` 和 `teacherInfo` 均为 null
- **返回**（失败）：`code: 4013`，未登录

---

## 用户资料模块 `/api/user/profile`

> **只读**接口（不含资料修改）。与 `/api/user/auth/current` 的区别：后者返回登录时放入 Session
> 的游离 User 实体，不含学生/教师详情，且其关联为懒加载，会话外读取会抛异常；
> 本模块在事务内重新加载，返回扁平可用的完整资料。

### 资料结构（UserProfileDTO）

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | long | 用户 ID |
| `username` | string | 登录名 |
| `telephone` / `email` | string | 联系方式 |
| `avatar` | string | 头像地址，可为 null |
| `role` | string | `STUDENT` / `TEACHER` / `UNBOUND` |
| `student` | object | 学生资料（未绑定时为 null） |
| `teacher` | object | 教师资料（未绑定时为 null） |
| `organization` | object | 所属机构资料，未绑定教师或机构未登记时为 null |

- `student` 子对象：`id`、`studentNumber`、`name`、`className`、`gender`、`admissionDate`
- `teacher` 子对象：`id`、`name`、`organization`、`gender`、`education`、`jointime`
- `organization` 子对象：`id`、`name`、`logo`、`bannerUrl`、`info`、`honorCertUrl`

> **机构关联方式**：按字符串匹配 `teacher.organization` ↔ `organization.name`（既有设计，
> 绑定身份时写入），未另建外键。机构未发布资料时 `organization` 为 null，**不影响**教师资料返回。

### GET /api/user/profile/me

- **描述**：当前登录用户的完整资料。
- **请求方式**：`GET`，无参数
- **返回**（成功）：`data` 为 UserProfileDTO
- **返回**（失败）：`code: 4013`（未登录）、`4041`（账号存在但用户记录已清理）

### GET /api/user/profile/{userId}

- **描述**：按用户 ID 读取资料，用于查看他人主页。
- **请求方式**：`GET`
- **路径参数**：`userId` (long) — 用户 ID
- **返回**（成功）：`data` 为 UserProfileDTO
- **返回**（失败）：`code: 4041`，用户不存在

---

## 学生资料模块 `/api/student/profile`

### GET /api/student/profile/{studentId}

- **描述**：一次返回「学生基础资料 + 学习统计」，供「学生-个人资料」页使用，
  避免前端为渲染一页分别请求资料与统计两个接口。
- **请求方式**：`GET`
- **路径参数**：`studentId` (long) — 学生 ID
- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "profile": { "...": "UserProfileDTO，见用户资料模块" },
    "stats": { "...": "LearningStatsDTO，见学习统计模块" }
  }
}
```

- **说明**：学生记录未绑定登录账号时，`profile.userId` 为 `null`，其余基础资料仍正常返回
- **返回**（失败）：`code: 4042`，学生不存在

---

## 课程管理模块 `/api/course`

### POST /api/course/add
- **描述**：新增一门课程记录。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON，Course 对象）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `courseName` | string | 是 | 课程名称 |
  | `courseNumber` | string | 是 | 课程编号（唯一） |
  | `courseIntroduction` | string | 否 | 课程简介（支持长文本） |
  | `startDate` | string | 否 | 开课日期 |
  | `teachingObjectives` | string | 否 | 教学目标 |
  | `duration` | string | 否 | 课程时长/学时 |
  | `teachingTeachers` | string | 否 | 授课教师 |
  | `teachingClasses` | string | 否 | 开课班级 |
  | `targetAudience` | string | 否 | 适用对象 |
  | `classAddress` | string | 否 | 上课地点 |
  | `coursePrice` | double | 否 | 课程价格 |
  | `courseStatus` | string | 否 | 课程状态（如"进行中"、"已结课"） |
  | `courseTags` | string | 否 | 课程标签 |
  | `courseOutline` | string | 否 | 课程大纲（支持超长文本） |
  | `courseImage` | string | 否 | 课程封面图片 URL |

- **返回**（成功）：`data` 字段为新增的 Course 对象（含自动生成的 `id` 和 `viewCount: 0`）
- **返回**（失败）：`code: 4091`，课程编号已存在

---

### GET /api/course/search
- **描述**：多条件分页搜索课程，所有条件均为可选，支持模糊匹配。
- **请求方式**：`GET`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `courseName` | string | 否 | 按课程名称模糊搜索 |
  | `courseNumber` | string | 否 | 按课程编号模糊搜索 |
  | `teachingTeachers` | string | 否 | 按授课教师模糊搜索 |
  | `courseStatus` | string | 否 | 按课程状态过滤 |
  | `courseTags` | string | 否 | 按课程标签过滤 |
  | `startDate` | string | 否 | 按开课日期过滤 |
  | `page` | int | 否 | 页码，从 0 开始，默认 `0` |
  | `size` | int | 否 | 每页条数，默认 `10` |
  | `sort` | string | 否 | 排序字段，默认 `id` |

- **返回**（成功）：`data` 字段为 Spring Data 分页对象（`Page<Course>`），包含：

  | 字段 | 类型 | 说明 |
  |------|------|------|
  | `content` | array | 当前页的课程列表 |
  | `totalElements` | long | 总记录数 |
  | `totalPages` | int | 总页数 |
  | `size` | int | 每页条数 |
  | `number` | int | 当前页码（从0开始） |

---

### PUT /api/course/update
- **描述**：编辑已有课程的信息，根据 ID 进行更新。仅更新请求中提供的非 null 字段。

- **请求方式**：PUT，application/json

- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | id | long | 是 | 要更新的课程ID |

- **请求体（JSON，Course 对象，所有字段均可选）**：

  | 字段 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | courseName | string | 否 | 课程名称 |
  | courseNumber | string | 否 | 课程编号（唯一） |
  | courseIntroduction | string | 否 | 课程简介（支持长文本） |
  | startDate | string | 否 | 开课日期 |
  | teachingObjectives | string | 否 | 教学目标 |
  | duration | string | 否 | 课程时长/学时 |
  | teachingTeachers | string | 否 | 授课教师 |
  | teachingClasses | string | 否 | 开课班级 |
  | targetAudience | string | 否 | 适用对象 |
  | classAddress | string | 否 | 上课地点 |
  | coursePrice | double | 否 | 课程价格 |
  | courseStatus | string | 否 | 课程状态（如"进行中"、"已结课"） |
  | courseTags | string | 否 | 课程标签 |
  | courseOutline | string | 否 | 课程大纲（支持超长文本） |
  | courseImage | string | 否 | 课程封面图片 URL |

- **返回**（成功）：data 字段为更新后的 Course 对象（结构同现有课程实体）

- **返回**（失败）：code: 4042，课程不存在；code: 4091，课程编号已存在

### PUT /api/course/{id}/publish

- **描述**：**发布 / 取消发布课程**。教师可直接发布自己授课的课程，**无需任何审批**。
- **请求方式**：`PUT`
- **路径参数**：`id` (long) — 课程 ID
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `published` | boolean | 否 | 目标状态，默认 `true`（发布）；传 `false` 取消发布 |

- **返回**（成功）：`data` 为更新后的 Course 对象，`published` 字段已变更
- **返回**（失败）：

  | code | 触发条件 |
  |------|------|
  | 4001 | 课程 ID 为空 |
  | 4031 | 调用者不是该课程的授课教师，且没有可用的教师身份 |
  | 4042 | 课程不存在 |


### DELETE /api/course/batch
- **描述**：批量删除课程记录（物理删除）。接收课程 ID 列表。

- **请求方式**：DELETE，application/json

- **请求体（JSON，ID 列表）**：

  | 字段 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | [0..n] | long | 是 | 课程ID数组 |

- **请求示例**：

```json
[1, 2, 3]
```
- **返回**（成功）：

```json
{
"code": 2000,
"msg": "批量删除成功",
"timestamp": 1700000000000,
"data": "批量删除成功"
}
```
- **返回**（失败）：code: 5000，删除失败

### GET /api/course/export
- **描述**：导出全部课程数据为 Excel（.xlsx）文件。返回文件流，浏览器自动触发下载。

- **请求方式**：GET

- **响应类型**：application/vnd.openxmlformats-officedocument.spreadsheetml.sheet（文件流）

- **响应头**：Content-Disposition: attachment; filename=courses.xlsx

- **Excel 列结构**：

  | 列名 | 说明 |
  |--------|------|
  | ID | 课程ID |
  | 课程名称 | 课程名称 |
  | 课程编号 | 课程编号 |
  | 授课教师 | 授课教师 |
  | 状态 | 课程状态 |

- **注意**：该接口直接响应文件下载，无需解析 JSON。



## 课程访问量模块 `/api/course/view`

> 基于 Redis 实现的课程访问量统计，定期同步到 MySQL。

### GET /api/course/view/popular
- **描述**：获取按访问量降序排列的热门课程列表。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `limit` | int | 否 | 返回数量，默认 `10`，范围 1-100 |

- **返回**（成功）：`data` 字段为 Course 对象数组，已按访问量降序排列
- **返回**（失败）：`code: 4003`，`limit` 参数不合法

---

### GET /api/course/view/{courseId}/count
- **描述**：查询指定课程的当前访问量（从 Redis 读取）。
- **路径参数**：`courseId` (long) — 课程ID
- **返回**（成功）：
  ```json
  { "code": 2000, "msg": "Ok.", "timestamp": 1700000000000, "data": 128 }
  ```
- **返回**（失败）：`code: 4003`，courseId 不合法

---

### POST /api/course/view/{courseId}/record
- **描述**：记录一次课程访问（浏览量 +1），由前端在用户进入课程页面时调用。
- **路径参数**：`courseId` (long) — 课程ID
- **请求体**：无
- **返回**（成功）：`data: "View recorded successfully"`
- **返回**（失败）：`code: 4003`，courseId 不合法

---

### GET /api/course/view/top10
- **描述**：获取访问量前 10 的热门课程（`/popular?limit=10` 的快捷接口）。
- **返回**（成功）：`data` 字段为包含最多 10 条 Course 对象的数组

---

### POST /api/course/view/admin/sync
- **描述**：手动触发将 Redis 中的访问量数据同步到 MySQL 数据库。通常由定时任务自动执行，管理员可手动调用。
- **请求体**：无
- **返回**（成功）：`data: "Data synced successfully"`

---

### POST /api/course/view/admin/rebuild-cache
- **描述**：手动触发从 MySQL 重建 Redis 访问量缓存，用于 Redis 数据异常时恢复。
- **请求体**：无
- **返回**（成功）：`data: "Cache rebuilt successfully"`

---

### DELETE /api/course/view/admin/clear/{courseId}
- **描述**：清除指定课程的访问量记录（Redis 及 MySQL）。
- **路径参数**：`courseId` (long) — 课程ID
- **返回**（成功）：`data: "View count cleared successfully"`
- **返回**（失败）：`code: 4003`，courseId 不合法

---

## 课程热门排行模块 `/api/course/popularity`

> 提供比访问量模块更详细的热门课程统计与排行数据。

### GET /api/course/popularity/ranking
- **描述**：获取热门课程排行榜（详细版），附带全局统计信息。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `limit` | int | 否 | 返回数量，默认 `10`，范围 1-100 |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "items": [
        {
          "ranking": 1,
          "courseId": 5,
          "courseName": "高等数学（上）",
          "viewCount": 1024,
          "courseStatus": "进行中",
          "courseTags": "数学,基础",
          "courseImage": "http://..."
        }
      ],
      "stats": {
        "totalCourses": 50,
        "totalViews": 8800,
        "averageViews": 176,
        "maxViews": 1024
      },
      "timestamp": 1700000000000
    }
  }
  ```

  | 返回字段 | 类型 | 说明 |
  |----------|------|------|
  | `items` | array | 排行课程列表 |
  | `items[].ranking` | int | 名次（从1开始） |
  | `items[].courseId` | long | 课程ID |
  | `items[].courseName` | string | 课程名称 |
  | `items[].viewCount` | long | 访问量 |
  | `items[].courseStatus` | string | 课程状态 |
  | `items[].courseTags` | string | 课程标签 |
  | `items[].courseImage` | string | 课程封面图片 URL |
  | `stats.totalCourses` | int | Redis 中有访问量记录的课程总数 |
  | `stats.totalViews` | long | 所有课程总访问量 |
  | `stats.averageViews` | long | 平均每课程访问量 |
  | `stats.maxViews` | long | 最高单课访问量 |
  | `timestamp` | long | 数据生成时间戳 |

---

### GET /api/course/popularity/top10-simple
- **描述**：获取访问量前 10 的热门课程简化版排行，不含全局统计信息。
- **返回**（成功）：`data` 字段为简化版 CoursePopularityItem 数组（字段同上 `items`，但不含 `courseTags` 和 `courseImage`）

---

### GET /api/course/popularity/range
- **描述**：获取指定排名区间内的热门课程。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `start` | int | 否 | 起始排名（从1开始），默认 `1` |
  | `end` | int | 否 | 结束排名，默认 `10`，区间长度不超过100 |

- **返回**（成功）：`data` 字段为 CoursePopularityItem 数组（含 `ranking`、`courseId`、`courseName`、`viewCount`、`courseStatus`、`courseImage`）
- **返回**（失败）：`code: 4003`，参数不合法（`start < 1`，或 `end < start`，或区间超过100）

---

### GET /api/course/popularity/{courseId}/is-popular
- **描述**：检查指定课程是否在热门课程前 N 名中。
- **路径参数**：`courseId` (long) — 课程ID
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `limit` | int | 否 | 检查范围（前N名），默认 `10` |

- **返回**（成功）：`data: true` 或 `data: false`
- **返回**（失败）：`code: 4003`，参数不合法

---

### GET /api/course/popularity/{courseId}/ranking
- **描述**：查询指定课程在全部有访问量课程中的排名。
- **路径参数**：`courseId` (long) — 课程ID
- **返回**（成功）：`data` 为排名数字（int，从1开始）；若该课程无访问量则返回 `0`
- **返回**（失败）：`code: 4003`，courseId 不合法

---

## 教师管理模块 `/api/teacher`

### POST /api/teacher/add
- **描述**：新增一名教师记录。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON，Teacher 对象）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `name` | string | 是 | 教师姓名 |
  | `organization` | string | 否 | 所属单位/院系 |
  | `gender` | string | 否 | 性别 |
  | `education` | string | 否 | 最高学历 |
  | `jointime` | string | 否 | 入职时间 |

- **返回**（成功）：`data` 字段为新增的 Teacher 对象（含自动生成的 `id`）
- **返回**（失败）：`code: 4091`，教师实体已存在

---

### POST /api/teacher/search
- **描述**：多条件分页搜索教师，所有条件均为可选。支持通过关联的 User 字段进行跨表过滤。
- **请求方式**： POST
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `name` | string | 否 | 按姓名模糊搜索 |
  | `organization` | string | 否 | 按单位模糊搜索 |
  | `jointime` | string | 否 | 按入职时间过滤 |
  | `gender` | string | 否 | 按性别过滤 |
  | `education` | string | 否 | 按学历过滤 |
  | `username` | string | 否 | 按关联用户名模糊搜索（User 表） |
  | `telephone` | string | 否 | 按关联电话模糊搜索（User 表） |
  | `email` | string | 否 | 按关联邮箱模糊搜索（User 表） |
  | `userType` | string | 否 | 按关联用户类型过滤（User 表，枚举值：TEACHER / STUDENT） |
  | `page` | int | 否 | 页码，从 0 开始，默认 `0` |
  | `size` | int | 否 | 每页条数，默认 `10` |
  | `sort` | string | 否 | 排序字段，默认 `id` |
返回（成功）： data 字段为分页对象（Page），content 数组中每条记录为 TeacherDTO 对象，已将关联的 User 字段展开为平铺结构。
返回示例：
```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1700000000000,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "张教授",
        "organization": "信息科学与技术学院",
        "gender": "男",
        "education": "博士",
        "jointime": "2020-09-01",
        "username": "zhang_prof",
        "telephone": "13800138000",
        "email": "zhang@buct.edu.cn",
        "userType": "TEACHER"
      }
    ],
    "pageable": { ... },
    "totalElements": 50,
    "totalPages": 5
  }
}
```
---

### PUT /api/teacher/update
- **描述**：编辑已有教师的信息，根据 ID 进行更新。仅更新请求中提供的非 null 字段。

- **请求方式**：PUT，application/json

- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | id | long | 是 | 要更新的教师ID |

- **请求体（JSON，Teacher 对象，所有字段均可选）**：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | name | string | 否 | 教师姓名 |
  | organization | string | 否 | 所属单位/院系 |
  | gender | string | 否 | 性别 |
  | education | string | 否 | 最高学历 |
  | jointime | string | 否 | 入职时间 |

- **返回**（成功）：
```json
{
"code": 2000,
"msg": "Ok.",
"timestamp": 1700000000000,
"data": {
"id": 1,
"name": "张教授",
"organization": "信息科学与技术学院",
"gender": "男",
"education": "博士",
"jointime": "2020-09-01"
}
}
```
- **返回**（失败）：code: 4042，教师不存在；code: 4091，教师姓名已存在
### DELETE /api/teacher/batch
- **描述**：批量删除教师记录（物理删除）。接收教师 ID 列表，一次删除多条记录。

- **请求方式**：DELETE，application/json

- **请求体（JSON，ID 列表）**：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | [0..n] | long | 是 | 教师ID数组 |

- **请求示例**：

```json
[1, 2, 3]
```
- **返回（成功）**：

```json
{
  "code": 2000,
  "msg": "批量删除成功",
  "timestamp": 1700000000000,
  "data": "批量删除成功"
}
```
- **返回（失败）**：code: 5000，删除失败

### GET /api/teacher/export
- **描述**：导出教师数据为 Excel（.xlsx）文件。支持通过可选查询参数按条件过滤导出，不传任何参数则导出全部教师。返回文件流，浏览器自动触发下载。
- **请求方式**：GET
- **查询参数**（均为可选）：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | name | string | 否 | 按姓名过滤 |
  | organization | string | 否 | 按单位过滤 |
  | jointime | string | 否 | 按入职时间过滤 |
  | gender | string | 否 | 按性别过滤 |
  | education | string | 否 | 按学历过滤 |
  | username | string | 否 | 按关联用户名过滤（User 表） |
  | telephone | string | 否 | 按关联电话过滤（User 表） |
  | email | string | 否 | 按关联邮箱过滤（User 表） |
  | userType | string | 否 | 按关联用户类型过滤（User 表，枚举值：TEACHER / STUDENT） |

- **响应类型**：application/vnd.openxmlformats-officedocument.spreadsheetml.sheet（文件流）

- **响应头**：Content-Disposition: attachment; filename=teachers.xlsx
- **Excel 列结构**：

  | 列名 | 说明 |
  |------|------|
  | ID | 教师 ID |
  | 姓名 | 教师姓名 |
  | 单位 | 所属单位/院系 |
  | 性别 | 性别 |
  | 学历 | 最高学历 |
  | 入职时间 | 入职时间 |
  | 用户名 | 关联用户名（来自 User 表） |
  | 电话 | 关联电话（来自 User 表） |
  | 邮箱 | 关联邮箱（来自 User 表） |
  | 用户类型 | 关联用户类型（来自 User 表） |
- **注意**：该接口直接响应文件下载，前端可通过 window.open() 或 \<a\> 标签 download 属性触发下载。导出的数据会根据传入的查询参数进行过滤，不传参数时默认导出全部教师 

### POST /api/teacher/add-with-user

- **描述**：新增一名教师记录，同时自动创建一个系统登录用户并绑定该教师身份，用户可直接使用分配的用户名和密码登录。
- **请求方式**：POST，application/json
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | teacher | object | 是 | 教师基本信息对象 |
  | teacher.name | string | 是 | 教师姓名 |
  | teacher.organization | string | 否 | 所属单位/院系 |
  | teacher.gender | string | 否 | 性别 |
  | teacher.education | string | 否 | 最高学历 |
  | teacher.jointime | string | 否 | 入职时间 |
  | username | string | 否 | 登录用户名，不传则自动生成（格式：T + 时间戳） |
  | password | string | 否 | 登录密码，不传则默认为 123456 |
  | telephone | string | 否 | 手机号 |
  | email | string | 否 | 邮箱 |
- **返回**（成功）：data 字段包含新创建的 teacher 和 user 对象，其中 user 不返回密码字段。
- **返回**（失败）：code: 4091，实体已存在（如教师姓名、用户名、手机号、邮箱重复等）。

## 学生管理模块 `/api/students`

### POST /api/students/add
- **描述**：新增一名学生记录。
- **请求方式**：`POST`，`application/json`
- **请求体**（JSON，Student 对象）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `studentNumber` | string | 是 | 学号（唯一） |
  | `name` | string | 是 | 学生姓名 |
  | `className` | string | 否 | 班级名称 |
  | `gender` | string | 否 | 性别 |
  | `admissionDate` | string | 否 | 入学日期（格式 `YYYY-MM-DD`） |

- **返回**（成功）：`data` 字段为新增的 Student 对象（含自动生成的 `id`，`user` 字段为 null）
- **返回**（失败）：`code: 4091`，学号已存在

---

### GET /api/students/search
- **描述**：多条件分页搜索学生，所有条件均为可选。支持通过关联的 User 字段进行跨表过滤。
- **请求方式**： GET
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | name | string | 否 | 按姓名模糊搜索 |
  | studentNumber | string | 否 | 按学号模糊搜索 |
  | className | string | 否 | 按班级过滤 |
  | gender | string | 否 | 按性别过滤 |
  | telephone | string | 否 | 按关联电话模糊搜索（User 表） |
  | email | string | 否 | 按关联邮箱模糊搜索（User 表） |
  | page | int | 否 | 页码，从 0 开始，默认 0 |
  | size | int | 否 | 每页条数，默认 10 |
  | sort | string | 否 | 排序字段，默认 id |
- **返回（成功）**： data 字段为分页对象（Page），content 数组中每条记录为 StudentDTO 对象，已将关联的 User 字段展开为平铺结构。
- **返回示例**：
```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1700000000000,
  "data": {
    "content": [
      {
        "id": 1,
        "studentNumber": "2024001",
        "name": "张三",
        "className": "计算机2401",
        "gender": "男",
        "admissionDate": "2024-09-01",
        "username": "zhangsan",
        "telephone": "13900139000",
        "email": "zhangsan@buct.edu.cn",
        "userType": "STUDENT"
      }
    ],
    "pageable": { ... },
    "totalElements": 120,
    "totalPages": 12
  }
}
```

---

### PUT /api/students/update
- **描述**：编辑已有学生的信息，根据 ID 进行更新。仅更新请求中提供的非 null 字段。

- **请求方式**：PUT，application/json

  - **查询参数**：

    | 参数名 | 类型 | 必填 | 说明 |
    |--------|------|------|------|
    | id | long | 是 | 要更新的学生ID |

  - **请求体（JSON，Student 对象，所有字段均可选）**：

  | 字段 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | name | string | 否 | 学生姓名 |
  | studentNumber | string | 否 | 学号（唯一） |
  | className | string | 否 | 班级名称 |
  | gender | string | 否 | 性别 |
  | admissionDate | string | 否 | 入学日期（格式 YYYY-MM-DD） |

- **返回（成功）**：

```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1700000000000,
  "data": {
  "id": 1,
  "studentNumber": "2024001",
  "name": "张三",
  "className": "计算机2401",
  "gender": "男",
  "admissionDate": "2024-09-01"
  }
}
```
- **返回（失败）**：code: 4042，学生不存在；code: 4091，学号已存在

### DELETE /api/students/batch
- **描述**：批量删除学生记录（逻辑删除，将 deleted 字段标记为 true）。

- **请求方式**：DELETE，application/json

- **请求体（JSON，ID 列表）**：

  | 字段 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | [0..n] | long | 是 | 学生ID数组 |

- **请求示例**：

```json
[1, 2, 3]
```
- **返回（成功）**：

```json
{
  "code": 2000,
  "msg": "批量删除成功",
  "timestamp": 1700000000000,
  "data": "批量删除成功"
}
```
- **返回（失败）**：code: 5000，删除失败
---
### GET /api/students/export
- **描述**：导出学生数据为 Excel（.xlsx）文件。支持通过可选查询参数按条件过滤导出，不传任何参数则导出全部学生。返回文件流，浏览器自动触发下载。
- **请求方式**：GET
- **查询参数（均为可选）**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | name | string | 否 | 按姓名过滤 |
  | studentNumber | string | 否 | 按学号过滤 |
  | className | string | 否 | 按班级过滤 |
  | gender | string | 否 | 按性别过滤 |
  | telephone | string | 否 | 按关联电话过滤（User 表） |
  | email | string | 否 | 按关联邮箱过滤（User 表） |
- **响应类型**：application/vnd.openxmlformats-officedocument.spreadsheetml.sheet（文件流）
- **响应头**：Content-Disposition: attachment; filename=students.xlsx
- **Excel 列结构**：

  | 列名 | 说明 |
  |------|------|
  | ID | 学生 ID |
  | 姓名 | 学生姓名 |
  | 学号 | 学生学号 |
  | 班级 | 所在班级 |
  | 性别 | 性别 |
  | 用户名 | 关联用户名（来自 User 表） |
  | 电话 | 关联电话（来自 User 表） |
  | 邮箱 | 关联邮箱（来自 User 表） |
  | 用户类型 | 关联用户类型（来自 User 表） |

- **注意**：该接口直接响应文件下载，无需解析 JSON。导出的数据会根据传入的查询参数进行过滤，不传参数时默认导出全部学生。

### POST /api/students/add-with-user

- **描述**：新增一名学生记录，同时自动创建一个系统登录用户并绑定该学生身份，用户可直接使用分配的用户名和密码登录。
  - **请求方式**：POST，application/json
    **请求体**（JSON）：

    | 字段 | 类型 | 必填 | 说明 |
          |------|------|----|------|
    | student | object | 是  | 学生基本信息对象 |
    | student.name | string | 是  | 学生姓名 |
    | student.studentNumber | string | 是  | 学号 |
    | student.className | string | 否  | 班级 |
    | student.gender | string | 否  | 性别 |
    | student.admissionDate | string | 否  | 入学日期（格式：yyyy-MM-dd） |
    | username | string | 否  | 登录用户名，不传则默认使用学号 |
    | password | string | 否  | 登录密码，不传则默认为 123456 |
    | telephone | string | 否  | 手机号 |
    | email | string | 否  | 邮箱 |

- **返回**（成功）：data 字段包含新创建的 student 和 user 对象，其中 user 不返回密码字段。
- **返回**（失败）：code: 4091，实体已存在（如学号、用户名、手机号、邮箱重复等）。
---

## 学生选课模块 `/api/student-courses`

> 管理学生与课程之间的多对多关系，并记录查看状态。

### POST /api/student-courses/select
- **描述**：学生选择一门课程，建立学生-课程关联，初始查看状态为未查看。
- **请求方式**：`POST`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `studentId` | long | 是 | 学生ID |
  | `courseId` | long | 是 | 课程ID |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": { "studentId": 1, "courseId": 5 },
      "student": { ... },
      "course": { ... },
      "isViewed": false
    }
  }
  ```

  | 返回字段 | 类型 | 说明 |
  |----------|------|------|
  | `id.studentId` | long | 学生ID（复合主键） |
  | `id.courseId` | long | 课程ID（复合主键） |
  | `student` | object | 关联的学生信息 |
  | `course` | object | 关联的课程信息 |
  | `isViewed` | boolean | 是否已查看，初始为 `false` |

- **返回**（失败）：`code: 5000`，选课失败（如已选过该课程）

---

### PUT /api/student-courses/update-viewed
- **描述**：更新学生对某门课程的查看状态。
- **请求方式**：`PUT`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `studentId` | long | 是 | 学生ID |
  | `courseId` | long | 是 | 课程ID |
  | `isViewed` | boolean | 是 | 是否已查看：`true` 或 `false` |

- **返回**（成功）：`data` 字段为更新后的 StudentCourse 对象（结构同上）
- **返回**（失败）：`code: 5000`，更新失败

---

### GET /api/student-courses/all-courses
- **描述**：分页获取指定学生已选的所有课程。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `studentId` | long | 是 | 学生ID |
  | `page` | int | 否 | 页码，从 0 开始，默认 `0` |
  | `size` | int | 否 | 每页条数，默认 `10` |
  | `sort` | string | 否 | 排序字段，默认 `createdAt` |
  | `direction` | string | 否 | 排序方向：`asc` / `desc`，默认 `desc` |

- **返回**（成功）：`data` 字段为分页对象（`Page<StudentCourse>`）

---

### GET /api/student-courses/viewed-courses
- **描述**：分页获取指定学生已查看的课程（`isViewed = true`）。
- **查询参数**：同 `/all-courses`，`sort` 默认 `id`

- **返回**（成功）：`data` 字段为分页对象（`Page<StudentCourse>`）

---

### GET /api/student-courses/not-viewed-courses
- **描述**：分页获取指定学生尚未查看的课程（`isViewed = false`）。
- **查询参数**：同 `/viewed-courses`

- **返回**（成功）：`data` 字段为分页对象（`Page<StudentCourse>`）

---

### DELETE /api/student-courses/drop
- **描述**：学生退选一门课程，删除学生-课程关联记录。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `studentId` | long | 是 | 学生ID |
  | `courseId` | long | 是 | 课程ID |

- **返回**（成功）：`data: null`
- **返回**（失败）：`code: 5000`，退课失败（如选课记录不存在）

---

## 笔记模块 `/api/notes`

> 提供课程笔记的创建、查询、点赞、搜索等功能。笔记关联学生与课程，支持公开/私有、点赞计数、评论计数。

### Note 实体字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 笔记ID |
| `title` | string | 标题 |
| `content` | string | Markdown 内容 |
| `htmlContent` | string \| null | 渲染后的 HTML（可选） |
| `isPublic` | boolean | 是否公开，默认 false |
| `likeCount` | int | 点赞数 |
| `commentCount` | int | 评论数 |
| `student` | object | 关联学生 |
| `course` | object | 关联课程 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

### POST /api/notes/create
- **描述**：创建笔记。
- **请求方式**：`POST`，`application/json`
- **请求体**（Note 对象）：`title`（必填）、`content`（必填）、`isPublic`、`student`（含 id）、`course`（含 id）等字段
- **返回**（成功）：`data` 为新建的 Note 对象
- **返回**（失败）：`code: 4042`，关联实体不存在；`code: 5000`，服务器错误

### PUT /api/notes/update/{id}
- **描述**：更新笔记（仅作者可操作）。
- **路径参数**：`id` (long) — 笔记ID
- **请求体**（Note 对象，字段可选）
- **返回**（成功）：`data` 为更新后的 Note 对象
- **返回**（失败）：`code: 4042`，笔记不存在；`code: 4031`，无权限；`code: 5000`，服务器错误

### GET /api/notes/{id}
- **描述**：获取笔记详情。
- **路径参数**：`id` (long)
- **返回**（成功）：`data` 为 Note 对象
- **返回**（失败）：`code: 4042`，笔记不存在

### DELETE /api/notes/{noteId}
- **描述**：删除笔记（仅作者可操作）。
- **路径参数**：`noteId` (long)
- **查询参数**：`studentId` (long, 必填) — 操作学生ID，用于权限校验
- **返回**（成功）：`data: true`
- **返回**（失败）：`code: 4042`，笔记不存在；`code: 4031`，无权限

### GET /api/notes/course/{courseId}
- **描述**：分页查询某课程下的笔记。
- **路径参数**：`courseId` (long)
- **查询参数**：`page`（默认 0）、`size`（默认 10）、`sort`（默认 `createdAt`）、`direction`（默认 `desc`）
- **返回**（成功）：`data` 为 `Page<Note>`

### GET /api/notes/student/{studentId}
- **描述**：分页查询某学生写的笔记（按创建时间倒序）。
- **路径参数**：`studentId` (long)
- **查询参数**：`page`（默认 0）、`size`（默认 10）
- **返回**（成功）：`data` 为 `Page<Note>`

### GET /api/notes/public
- **描述**：分页获取所有公开笔记。
- **查询参数**：`page`（默认 0）、`size`（默认 10）、`sort`（默认 `createdAt`）、`direction`（默认 `desc`）
- **返回**（成功）：`data` 为 `Page<Note>`

### GET /api/notes/search
- **描述**：搜索公开笔记（按标题/内容匹配）。
- **查询参数**：`keyword`（可选）、`page`（默认 0）、`size`（默认 10）
- **返回**（成功）：`data` 为 `Page<Note>`

### POST /api/notes/{noteId}/like
- **描述**：点赞/取消点赞（切换状态）。
- **路径参数**：`noteId` (long)
- **查询参数**：`studentId` (long, 必填)
- **返回**（成功）：`data: true`（已点赞）/ `data: false`（已取消）
- **返回**（失败）：`code: 4042`，笔记不存在

### GET /api/notes/{noteId}/liked
- **描述**：检查当前学生是否已点赞该笔记。
- **路径参数**：`noteId` (long)
- **查询参数**：`studentId` (long, 必填)
- **返回**（成功）：`data: true` / `data: false`

### GET /api/notes/popular
- **描述**：获取热门笔记（按点赞数降序）。
- **查询参数**：`page`（默认 0）、`size`（默认 10）
- **返回**（成功）：`data` 为 `Page<Note>`

---

## 评论模块 `/api/comments`

> 提供笔记评论的添加、回复、删除、查询等功能，支持两级评论结构（一级评论 + 回复）。

### Comment 实体字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 评论ID |
| `content` | string | 评论内容 |
| `note` | object | 关联笔记 |
| `student` | object | 关联评论者（学生） |
| `parentComment` | object \| null | 父评论（回复时设置） |
| `createdAt` | string | 创建时间 |

### POST /api/comments/add
- **描述**：添加一级评论。
- **请求方式**：`POST`，`application/json`
- **请求体**（Comment 对象）：`content`（必填）、`note`（含 id）、`student`（含 id）
- **返回**（成功）：`data` 为新建的 Comment 对象
- **返回**（失败）：`code: 4042`，关联实体不存在；`code: 5000`，服务器错误

### POST /api/comments/reply/{parentId}
- **描述**：回复指定父评论。
- **路径参数**：`parentId` (long) — 父评论ID
- **请求体**（Comment 对象）：`content`（必填）、`note`（含 id）、`student`（含 id）
- **返回**（成功）：`data` 为新建的回复 Comment 对象
- **返回**（失败）：`code: 4042`，父评论不存在

### DELETE /api/comments/{commentId}
- **描述**：删除评论（仅作者可操作）。
- **路径参数**：`commentId` (long)
- **查询参数**：`studentId` (long, 必填) — 操作学生ID，用于权限校验
- **返回**（成功）：`data: true`
- **返回**（失败）：`code: 4042`，评论不存在；`code: 4031`，无权限

### GET /api/comments/note/{noteId}
- **描述**：获取指定笔记的所有评论（含回复，平铺列表，树形结构由前端处理）。
- **路径参数**：`noteId` (long)
- **返回**（成功）：`data` 为 Comment 列表

### GET /api/comments/note/{noteId}/page
- **描述**：分页获取笔记的一级评论（不包含回复，按创建时间升序）。
- **路径参数**：`noteId` (long)
- **查询参数**：`page`（默认 0）、`size`（默认 10）
- **返回**（成功）：`data` 为 `Page<Comment>`

### GET /api/comments/{id}
- **描述**：获取单条评论详情。
- **路径参数**：`id` (long)
- **返回**（成功）：`data` 为 Comment 对象
- **返回**（失败）：`code: 4042`，评论不存在

### PUT /api/comments/{id}
- **描述**：更新评论（仅作者可操作）。
- **路径参数**：`id` (long)
- **请求体**（Comment 对象，字段可选）
- **返回**（成功）：`data` 为更新后的 Comment 对象
- **返回**（失败）：`code: 4042`，评论不存在；`code: 4031`，无权限

### GET /api/comments/{parentId}/replies
- **描述**：获取某条评论下的所有回复。
- **路径参数**：`parentId` (long)
- **返回**（成功）：`data` 为 Comment 列表

---

## 文件提取模块 `/api/fileextract`

### POST /api/fileextract/temp
- **描述**：批量上传 `.docx` 或 `.pdf` 文件，提取文本内容并返回，文件不会持久化存储。支持一次上传多个文件，逐个处理，单个文件失败不影响其他文件。
- **请求方式**：`POST`，`multipart/form-data`
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `files` | file[] | 是 | 待提取文件，支持 `.docx` 和 `.pdf` 格式，可多选，单文件最大 256 MB |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": [
      {
        "fileName": "report1.docx",
        "success": true,
        "msg": "解析成功",
        "content": "提取到的文本内容..."
      },
      {
        "fileName": "invalid.txt",
        "success": false,
        "msg": "仅支持 .docx / .pdf",
        "content": null
      }
    ]
  }
  ```

  | 返回字段 | 类型 | 说明 |
  |----------|------|------|
  | `data[]` | array | 每个文件的处理结果列表 |
  | `data[].fileName` | string | 原始文件名 |
  | `data[].success` | boolean | 是否解析成功 |
  | `data[].msg` | string | 处理结果描述（成功为"解析成功"，失败为错误原因） |
  | `data[].content` | string \| null | 提取的文本内容（失败时为 null） |

- **返回**（无文件）：`code: 4042`，资源不存在

---

## AI报告生成模块

> 提供 AI 批改作业/报告的异步任务接口，基于 SSE（Server-Sent Events）推送生成结果，最终可下载为 Excel 报告。

### POST /api/ai/generate/start
- **描述**：提交 AI 批改任务。将提取好的文本内容和文件名提交给 AI 进行批改，返回任务 ID 用于后续流式获取结果。
- **请求方式**：`POST`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `extractedTexts` | string[] | 是 | 各文件提取出的文本内容列表（与 `fileNames` 一一对应） |
  | `fileNames` | string[] | 是 | 各文件的原始文件名列表 |
  | `counts` | int | 是 | 批改模式：`1` 表示将所有文本合并为一份批改；其他正整数值表示逐文件分别批改（注意：该参数名为代码中的实际参数名，其语义为批改模式选择而非数量） |

- **返回**（成功）：
  ```json
  {
    "code": 2000,
    "msg": "Ok.",
    "timestamp": 1700000000000,
    "data": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "status": "started"
    }
  }
  ```

  | 返回字段 | 类型 | 说明 |
  |----------|------|------|
  | `id` | string | 任务唯一ID（UUID），用于订阅 SSE 流 |
  | `status` | string | 任务初始状态，固定为 `"started"` |

---

### GET /api/ai/generate/stream/{id}
- **描述**：通过 SSE 订阅指定 AI 批改任务的实时生成结果流。前端通过 `EventSource` 连接该接口，AI 生成内容将实时推送，任务完成后流关闭。
- **请求方式**：`GET`
- **路径参数**：`id` (string) — 由 `/start` 接口返回的任务 ID
- **响应类型**：`text/event-stream`（SSE）
- **用法**：
  ```javascript
  const evtSource = new EventSource(`/api/ai/generate/stream/${taskId}`);
  evtSource.onmessage = (event) => {
    console.log("收到片段：", event.data);
  };
  evtSource.onerror = () => {
    evtSource.close();
  };
  ```
- **推送数据**：每个 SSE 事件的 `data` 字段包含 AI 生成的文本片段，所有片段按顺序拼接即为完整批改报告（Markdown 格式）。

---

### GET /api/generate/download/{timeStamp}
- **描述**：根据 AI 批改生成的文本内容，生成并下载 Excel（`.xlsx`）格式的批改报告。同时支持别名路径 `/api/generate/judgereport/{timeStamp}`。
- **请求方式**：`GET`（也可使用 `POST`，请求体为 `text/plain` 纯文本，适用于批改结果较长、超出 URL 长度限制的场景）
- **路径参数**：`timeStamp` (string) — 文件名中的时间戳标识
- **查询参数**（GET 方式）：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `text` | string | 是 | 完整的 AI 批改结果文本（由 SSE 流拼接得到） |

- **请求体**（POST 方式）：`text/plain`，内容为完整的 AI 批改结果文本
- **响应类型**：`application/octet-stream`（二进制文件流）
- **响应头**：
  - `Content-Disposition: attachment; filename*=utf-8''judgereport_{timeStamp}.xlsx`
- **Excel 报告列结构**：

  | 列名 | 说明 |
  |------|------|
  | 姓名 | 学生姓名 |
  | 学号 | 学生学号 |
  | 班级 | 所在班级 |
  | 日期 | 报告日期 |
  | 报告名称 | 作业/报告标题 |
  | 分数 | AI评判分数 |
  | 评判依据 | AI评判理由说明 |

- **注意**：该接口直接响应文件下载，无需解析 JSON，前端可直接通过 `<a href="...">` 触发下载。当批改文本较长时建议使用 POST 方式提交。

## 机构管理模块 `/api/organization`
### POST /api/organization/add

- **描述**：新增一条机构记录。
- **请求方式**：POST，application/json
- **请求体**（JSON，Organization 对象）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 机构名称 |
| logo | string | 否 | 机构 logo 图片地址。只允许站内路径（以 `/` 开头）或 `http(s)` 地址 |
| bannerUrl | string | 否 | 机构自定义 banner 图片地址。限制同 logo |
| info | string | 否 | 机构信息描述（支持长文本） |
| honorCertUrl | string | 否 | 荣誉证书图片地址。限制同 logo |

> 三个图片地址字段会直接用于 `<img src>` / CSS 背景，因此服务端限制协议：仅接受
> 站内路径（`/...`）或 `http://` / `https://` 绝对地址。`javascript:`、`data:` 等其他协议
> 一律拒绝，返回 code 4006。通过 `/api/media/upload` 上传得到的 `/api/media/{id}/content`
> 属于站内路径，可直接使用。

- **返回**（成功）：

```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1700000000000,
  "data": {
    "id": 1,
    "name": "信息科学与技术学院",
    "logo": "/static/logo.png",
    "bannerUrl": "/static/banner.png",
    "info": "学院简介...",
    "honorCertUrl": "/static/cert.png",
    "createdTime": "2026-05-06T10:00:00",
    "updatedTime": "2026-05-06T10:00:00"
  }
}
```

- **返回**（失败）：code: 4091（机构名称已存在）、4006（图片地址不合法）、5000（保存失败）

### DELETE /api/organization/{id}

- **描述**：根据 ID 删除单条机构记录（物理删除）。
- **路径参数**：id (long) — 机构ID
- **返回**（成功）：data 字段为 null，msg 为 "机构删除成功"
- **返回**（失败）：code: 4042，机构不存在
### PUT /api/organization/{id}

- **描述**：编辑已有机构的信息，根据 ID 进行更新。仅更新请求中提供的非 null 字段。
- **路径参数**：id (long) — 要更新的机构ID
- **请求体**（JSON，Organization 对象，所有字段均可选）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 机构名称 |
| logo | string | 否 | 机构 logo 图片地址。只允许站内路径（以 `/` 开头）或 `http(s)` 地址 |
| bannerUrl | string | 否 | 机构自定义 banner 图片地址。限制同 logo |
| info | string | 否 | 机构信息描述 |
| honorCertUrl | string | 否 | 荣誉证书图片地址。限制同 logo |

- **返回**（成功）：data 字段为更新后的 Organization 对象（结构同新增）
- **返回**（失败）：code: 4042（机构不存在）、4091（机构名称已被其他机构使用）、4006（图片地址不合法）、5000（更新失败）

### GET /api/organization/{id}

- **描述**：根据 ID 获取指定机构的详细信息。
- **路径参数**：id (long) — 机构ID
- **返回**（成功）：data 字段为 Organization 对象（结构同新增，含 createdTime 和 updatedTime）
- **返回**（失败）：code: 4042，机构不存在

### GET /api/organization/search

- **描述**：按机构名称模糊搜索，分页返回结果。
- **查询参数**：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 按机构名称模糊搜索 |
| page | int | 否 | 页码，从 0 开始，默认 0 |
| size | int | 否 | 每页条数，默认 10 |
| sort | string | 否 | 排序字段，默认 id |

- **返回**（成功）：data 字段为 Spring Data 分页对象（Page<Organization>），包含 content、totalElements、totalPages、size、number 等字段

---

## 媒体上传模块 `/api/media`

本模块提供**上传文件取代手填 URL** 的能力：上传成功后，服务端会把访问地址**自动回填到对应业务字段**，前端无需自行拼接地址，也无需再调用一次更新接口。

### 上传用途（purpose）对照表

`purpose` 参数决定校验规则（允许的文件种类、扩展名白名单、体积上限）以及上传成功后回填的目标字段。

| purpose | 含义 | 要求的文件种类 | 回填字段 | 需要 ownerId |
|------|------|------|------|------|
| `COURSE_COVER` | 课程封面图 | 图片 | `course_list.course_image` | 是，传课程 ID |
| `ORG_LOGO` | 机构 logo | 图片 | `organization_list.logo` | 是，传机构 ID |
| `ORG_BANNER` | 机构 banner | 图片 | `organization_list.banner_url` | 是，传机构 ID |
| `ORG_HONOR_CERT` | 机构荣誉证书 | 图片 | `organization_list.honor_cert_url` | 是，传机构 ID |
| `VIDEO` | 课程视频本体 | 视频 | 无（由课程视频模块建立目录条目） | 否 |

- **支持的扩展名**：图片 `jpg` / `jpeg` / `png` / `gif` / `webp`；视频 `mp4` / `webm` / `mov` / `mkv`
- **体积上限**：图片 20MB，视频 2GB（可用 `app.media.upload.max-image-size` / `app.media.upload.max-video-size` 调整）
- **类型校验**：以**文件头魔术字节**判定真实类型，扩展名仅用于白名单与 mp4/mov 消歧，因此把任意文件改名成 `.mp4` 无法绕过校验

### ⚠️ 新增与编辑场景的差异（前端必读）

除 `VIDEO` 之外的**四个图片用途**（`COURSE_COVER`、`ORG_LOGO`、`ORG_BANNER`、`ORG_HONOR_CERT`）
**都必须传 `ownerId`**，因为服务端要在上传成功后立刻把地址回填到该记录的字段上；
缺失时返回 4001。由此产生一个约束：**记录尚不存在时（新增场景）没有 `ownerId` 可用，
无法直接调用绑定式上传。**

| 场景 | 能否直接上传绑定 | 推荐做法 |
|------|------|------|
| **编辑已有课程 / 机构**（能拿到 ID） | ✅ 可以 | 选图即调 `/api/media/upload` 传 `ownerId`，服务端直接回填字段，保存时无需再传图片地址 |
| **新增课程 / 机构**（记录还不存在） | ❌ 不行 | 见下方两种方案，二选一 |

新增场景的两种处理方式：

1. **先保存记录，再上传绑定**：表单提交成功后拿到新记录 ID，再进入"编辑"态允许选图上传。
   优点是完全复用现有接口、不产生无主文件；缺点是用户在提交前看不到图片预览。
2. **把图片地址当普通字符串一起提交**：新增页暂不提供上传控件（保留手填 URL 输入框），
   或允许用户先上传到别处再粘贴地址。提交时地址随 `organization/add`、`course/add` 一起入库。
   注意地址仍受协议校验限制（见机构模块说明），非 `/` 开头且非 `http(s)` 的值会被拒绝并返回 4006。

> 服务端目前**没有**"上传暂存、稍后绑定"的用途（即不要求 `ownerId` 的图片用途），
> 所以方案 2 里若想让用户从本地上传，需要先扩展后端：新增一个不绑定业务字段的 purpose，
> 上传后仅返回地址，由前端在提交时写入业务字段。

### POST /api/media/upload

- **描述**：通用文件上传接口，按 `purpose` 确定校验规则与回填目标。
- **请求方式**：POST，`multipart/form-data`
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `file` | file | 是 | 文件本体，表单字段名固定为 `file` |
  | `purpose` | string | 是 | 上传用途，取值见上方对照表，大小写不敏感 |
  | `ownerId` | long | 视用途 | 业务主体 ID：`COURSE_COVER` 传课程 ID，`ORG_*` 传机构 ID，`VIDEO` 可省略 |

- **返回**（成功）：

```json
{
  "code": 2000,
  "msg": "Ok.",
  "timestamp": 1700000000000,
  "data": {
    "purpose": "ORG_BANNER",
    "targetField": "bannerUrl",
    "file": {
      "fileId": 12,
      "fileName": "banner.png",
      "url": "/api/media/12/content",
      "contentType": "image/png",
      "size": 20480,
      "kind": "IMAGE",
      "purpose": "ORG_BANNER",
      "targetField": "bannerUrl",
      "storedValue": "/api/media/12/content"
    }
  }
}
```

- `data.file.url` 为可直接使用的访问地址（相对路径，同源直接可用），可放入 `<img src>`、CSS 背景或 `<video src>`
- `data.targetField` 表示服务端回填的业务字段；`VIDEO` 用途下该字段为 `null`
- 业务字段此时已被更新，前端刷新业务对象即可取到新地址

- **返回**（失败）：

  | code | 触发条件 |
  |------|------|
  | 4001 | 缺少 `purpose`；该用途需要 `ownerId` 但未传 |
  | 4002 | `ownerId` 不是数字 |
  | 4003 | `purpose` 取值无法识别；文件名缺少扩展名 |
  | 4004 | 文件体积超过对应用途上限 |
  | 4005 | 文件种类与用途不符、扩展名不在白名单、或文件头无法识别 |
  | 4042 | `ownerId` 对应的课程 / 机构不存在 |

### POST /api/media/upload/image

- **描述**：图片上传便捷入口，参数与行为同 `/api/media/upload`，仅显式声明了 multipart 消费类型，便于前端在只传图片时使用。
- **请求方式**：POST，`multipart/form-data`
- **请求参数**：同 `/api/media/upload`
- **返回**：同 `/api/media/upload`

### GET /api/media/{id}/content

- **描述**：读取媒体内容。图片可直接展示；视频支持 **HTTP Range**，用于拖动进度条与断点续播，浏览器播放器会自动携带 `Range` 请求头。
- **请求方式**：GET
- **路径参数**：`id` (long) — 媒体文件 ID
- **请求头**（可选）：`Range` — 支持三种写法：`bytes=0-499`（前 500 字节）、`bytes=500-`（从 500 到末尾）、`bytes=-500`（末尾 500 字节）。多区间写法（`bytes=0-9,20-29`）暂不支持，会退化为全量响应。
- **返回**：

  | 场景 | 状态码 | 响应头 |
  |------|------|------|
  | 不带 `Range` | 200 | `Accept-Ranges: bytes`、`Content-Type`、`Content-Length` |
  | 带合法 `Range` | 206 | `Content-Range: bytes a-b/total`、`Content-Length` 为区间长度 |
  | 区间越界或格式错误 | 416 | `Content-Range: bytes */total` |
  | 媒体记录不存在 | 200 | body 为 `{"code": 4042, ...}` |
  | 记录存在但磁盘文件缺失 | 404 | 无 body |

- **说明**：响应体为文件二进制流，不是统一 JSON 结构

### HEAD /api/media/{id}/content

- **描述**：仅返回响应头，用于播放器探测文件大小与是否支持 Range。
- **请求方式**：HEAD
- **路径参数**：`id` (long) — 媒体文件 ID
- **返回**（成功）：状态码 200，响应头含 `Content-Length`（文件大小）、`Content-Type`、`Accept-Ranges: bytes`，无响应体

### GET /api/media/{id}

- **描述**：查询媒体文件元数据。
- **请求方式**：GET
- **路径参数**：`id` (long) — 媒体文件 ID
- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "fileId": 12,
    "fileName": "banner.png",
    "url": "/api/media/12/content",
    "contentType": "image/png",
    "size": 20480,
    "kind": "IMAGE"
  }
}
```

- **返回**（失败）：code: 4042，媒体文件不存在

### DELETE /api/media/{id}

- **描述**：删除媒体文件记录与磁盘文件。
- **请求方式**：DELETE
- **路径参数**：`id` (long) — 媒体文件 ID
- **返回**（成功）：code 2000，msg 为 "媒体文件已删除，ID: x"
- **返回**（失败）：code: 4042，媒体文件不存在
- **注意**：本接口**不会清空业务字段中已写入的地址**。课程封面 / 机构图片请优先"重新上传覆盖"，或在删除后同步更新业务字段。

---

## 课程视频模块 `/api/course/video`

一门课程可挂载**多个视频**（课程 → 视频一对多），不设章节层级。上传接口返回的 `videoUrl` 指向 `/api/media/{id}/content`，服务端支持 HTTP Range，前端将其交给 `<video src>` 即可拖动进度条与断点续播。

### 课程视频字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 视频 ID |
| `courseId` | long | 所属课程 ID |
| `title` | string | 视频标题 |
| `description` | string | 视频简介 |
| `sortOrder` | int | 课程内播放顺序，越小越靠前 |
| `durationSeconds` | int | 视频时长（秒） |
| `videoUrl` | string | 播放地址，指向 `/api/media/{id}/content` |
| `coverUrl` | string | 封面地址，未设置时为 null |
| `videoFileId` | long | 视频文件对应的媒体 ID |
| `coverFileId` | long | 封面文件对应的媒体 ID |
| `videoSize` | long | 视频文件字节数 |
| `videoContentType` | string | 视频 MIME 类型 |
| `playableInBrowser` | boolean | 是否属于浏览器通常可直接播放的容器；`mkv` 等为 `false`，前端可据此提示转码 |

### POST /api/course/video/upload

- **描述**：上传视频并挂载到指定课程下。
- **请求方式**：POST，`multipart/form-data`
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `courseId` | long | 是 | 课程 ID，课程必须存在 |
  | `video` | file | 是 | 视频文件，表单字段名 `video` |
  | `title` | string | 否 | 视频标题，省略时取原始文件名（去扩展名） |
  | `description` | string | 否 | 视频简介 |
  | `sortOrder` | int | 否 | 课程内顺序，省略时追加到末尾 |
  | `durationSeconds` | int | 否 | 时长（秒），建议前端读取媒体元数据后回填 |
  | `cover` | file | 否 | 封面图 |

- **返回**（成功）：data 为课程视频对象，字段见上方说明

```json
{
  "code": 2000,
  "data": {
    "id": 3,
    "courseId": 7,
    "title": "第一讲：绪论",
    "description": null,
    "sortOrder": 0,
    "durationSeconds": 600,
    "videoUrl": "/api/media/21/content",
    "coverUrl": "/api/media/22/content",
    "videoFileId": 21,
    "coverFileId": 22,
    "videoSize": 104857600,
    "videoContentType": "video/mp4",
    "playableInBrowser": true
  }
}
```

- **返回**（失败）：code: 4042（课程不存在）、4001（`courseId` 或视频文件为空）、4005（文件不是可识别的视频）

### GET /api/course/video/list

- **描述**：查询某门课程的视频目录，按 `sortOrder` 升序排列（相同顺序按创建先后）。
- **请求方式**：GET
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `courseId` | long | 是 | 课程 ID |

- **返回**（成功）：data 为课程视频对象数组；课程存在但无视频时为空数组
- **返回**（失败）：code: 4042，课程不存在

### GET /api/course/video/{videoId}

- **描述**：查询单个视频详情。
- **请求方式**：GET
- **路径参数**：`videoId` (long) — 视频 ID
- **返回**（成功）：data 为课程视频对象
- **返回**（失败）：code: 4042，课程视频不存在

### PUT /api/course/video/{videoId}

- **描述**：修改视频信息。传入 `cover` 时会替换封面，并自动清理旧封面文件。
- **请求方式**：PUT，`multipart/form-data`
- **路径参数**：`videoId` (long) — 视频 ID
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `title` | string | 否 | 新标题 |
  | `description` | string | 否 | 新简介 |
  | `sortOrder` | int | 否 | 新顺序 |
  | `durationSeconds` | int | 否 | 新时长（秒） |
  | `cover` | file | 否 | 新封面；不传则保留原封面 |

- **返回**（成功）：data 为更新后的课程视频对象
- **返回**（失败）：code: 4042（视频不存在）、4001（声明替换封面但文件为空）

### DELETE /api/course/video/{videoId}

- **描述**：删除视频，**视频文件与封面文件一并清理**。
- **请求方式**：DELETE
- **路径参数**：`videoId` (long) — 视频 ID
- **返回**（成功）：code 2000，msg 为 "视频已删除，ID: x"
- **返回**（失败）：code: 4042，课程视频不存在

### POST /api/course/video/progress

- **描述**：上报播放进度，用于断点续播。学生尚未选该课时会**自动补建选课记录**；播放位置达到总时长 **97%** 时自动把课程标记为已观看，并把续播位置归零。
- **请求方式**：POST，`application/x-www-form-urlencoded` 或 `Query Params`
- **请求参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `courseId` | long | 是 | 课程 ID |
  | `positionSeconds` | int | 是 | 当前播放位置（秒），非负整数 |
  | `studentId` | long | 否 | 省略时取当前登录用户绑定的学生身份 |
  | `videoId` | long | 否 | 正在播放的视频，必须属于该课程 |
  | `durationSeconds` | int | 否 | 视频总时长（秒），用于判断是否看完 |

- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "success": true,
    "exists": true,
    "studentCourse": { "...": "选课记录对象" },
    "errorCode": null,
    "message": "进度已更新"
  }
}
```

- `exists` 为 `false` 表示该学生此前未选此课，本次自动补建了选课记录
- **返回**（失败）：code 4001（`studentId` 省略且非学生登录态）、5000（视频不属于该课程、学生或课程不存在）

### GET /api/course/video/progress

- **描述**：查询续播位置。
- **请求方式**：GET
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `courseId` | long | 是 | 课程 ID |
  | `studentId` | long | 否 | 省略时取当前登录用户绑定的学生身份 |

- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "videoId": 3,
    "positionSeconds": 120,
    "viewed": false
  }
}
```

- 无进度记录时 `data` 为 `null`
- **返回**（失败）：code: 4001，`studentId` 省略且非学生登录态

### 课程级进度 vs 视频级学习记录

这两者分工不同，别混用：

| | 课程级（`/progress`） | 视频级（`/learning-records` 等） |
|------|------|------|
| 存储 | `student_course` 一行 | `student_video_progress` 每视频一行 |
| 回答的问题 | *这门课*最近看到哪个视频、哪一秒 | *每个视频*看到哪、看了多久、是否看完 |
| 局限 | 只能记住一个位置 | —— |

上报 `/progress` 时若带了 `videoId`，服务端会在同一事务内**同时**更新课程级指针与视频级记录；
视频级写入失败只记警告日志，不影响课程级进度与播放本身。

### GET /api/course/video/{videoId}/learning-record

- **描述**：查询**单个视频**的学习记录。
- **请求方式**：`GET`
- **路径参数**：`videoId` (long) — 视频 ID
- **查询参数**：`studentId` (long，否) — 省略时取当前登录用户绑定的学生身份
- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "videoId": 21,
    "courseId": 7,
    "watchedSeconds": 300,
    "lastPosition": 300,
    "completed": false,
    "firstWatchedAt": "2026-09-15T10:00:00",
    "updatedTime": "2026-09-15T10:05:00"
  }
}
```

- **未开始学习的视频返回 `data: null`**（不报错，属正常情况）
- `watchedSeconds` 的语义是**到达过的最大位置**，不是去重后的真实观看时长；
  因此来回拖动进度条不会虚增，但直接拖到结尾也会被计为看完整段
- **返回**（失败）：code: 4001，`studentId` 省略且非学生登录态

### GET /api/course/video/learning-records

- **描述**：查询某学生在某门课下的**逐视频轨迹**。
- **请求方式**：`GET`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `courseId` | long | 是 | 课程 ID |
  | `studentId` | long | 否 | 省略时取当前登录用户绑定的学生身份 |

- **返回**（成功）：`data` 为学习记录数组（结构同单视频接口），无记录时为空数组

### GET /api/course/video/learning-summary

- **描述**：某学生的整体学习记录摘要（跨课程）。
- **请求方式**：`GET`
- **查询参数**：`studentId` (long，否) — 省略时取当前登录用户绑定的学生身份
- **返回**（成功）：

```json
{
  "code": 2000,
  "data": { "totalVideos": 12, "completedVideos": 5, "totalWatchedSeconds": 8460 }
}
```

- `totalVideos` 为**有学习记录**的视频数，未开始学习的不计入

### 前端播放示例

```html
<video id="player" controls preload="metadata"
       src="/api/media/21/content"
       poster="/api/media/22/content"></video>
```

```js
// 上传课程视频
async function uploadCourseVideo(courseId, file, title) {
  const form = new FormData();
  form.append("video", file);
  form.append("courseId", String(courseId));
  form.append("title", title);
  const res = await fetch("/api/course/video/upload", { method: "POST", body: form });
  return (await res.json()).data;   // 含 videoUrl，可直接交给 <video src>
}

// 节流上报进度（例如每 10 秒或暂停/离开页面时）
function reportProgress(courseId, videoId, positionSeconds, durationSeconds) {
  const body = new URLSearchParams({
    courseId, videoId, positionSeconds, durationSeconds
  });
  return fetch("/api/course/video/progress", { method: "POST", body });
}
```

### 上传相关配置项

| 配置 | 环境变量 | 默认值 | 说明 |
|------|------|------|------|
| `app.media.root` | `MEDIA_ROOT` | `data/uploads` | 上传根目录，生产建议配置为挂载卷的绝对路径 |
| `app.media.storage` | `MEDIA_STORAGE` | `media` | 媒体子目录，实际媒体目录为 `<root>/<storage>` |
| `app.media.upload.max-video-size` | `MEDIA_MAX_VIDEO_SIZE` | `2147483648` | 视频体积上限（字节） |
| `app.media.upload.max-image-size` | `MEDIA_MAX_IMAGE_SIZE` | `20971520` | 图片体积上限（字节） |
| `app.media.public-url-prefix` | `MEDIA_PUBLIC_URL_PREFIX` | 空 | 播放地址前缀，留空时返回相对地址 `/api/media/<id>/content` |

> 单文件上传上限已由 `spring.servlet.multipart.max-file-size` / `max-request-size` 放宽至 `2048MB`。
> 若前置 nginx 等反向代理，需同步放宽 `client_max_body_size`，并建议对 `/api/media/` 关闭 `proxy_request_buffering`，避免大视频占用代理内存。

---

## 关注关系模块 `/api/user/follow`

> 为「好友动态」提供关注范围。**关注动作的操作者一律取自当前登录态**，不接受前端传入自己的 ID，
> 避免越权替他人建立关注关系。关注是**单向**的，A 关注 B 不代表 B 关注 A。

### 用户摘要（UserSummary）

所有列表接口返回该结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 用户 ID |
| `username` | string | 登录名 |
| `displayName` | string | 展示名：已绑定学生/教师时取真实姓名，未绑定时退回用户名 |
| `avatar` | string | 头像地址，可为 null |
| `userType` | string | `STUDENT` / `TEACHER`，未绑定为 null |
| `studentId` / `teacherId` | long | 绑定的学生 / 教师实体 ID，未绑定为 null |
| `followedByMe` | boolean | 当前登录用户是否已关注该用户 |

### POST /api/user/follow/{followeeId}

- **描述**：关注指定用户。
- **请求方式**：`POST`，无请求体
- **路径参数**：`followeeId` (long) — 被关注用户 ID
- **返回**（成功）：`data` 为提示文案，如 `"关注成功"`
- **返回**（失败）：

  | code | 触发条件 |
  |------|------|
  | 4013 | 未登录 |
  | 4003 | 试图关注自己 |
  | 4041 | 被关注用户不存在 |
  | 4099 | 已经关注过该用户 |

### DELETE /api/user/follow/{followeeId}

- **描述**：取消关注。
- **请求方式**：`DELETE`
- **路径参数**：`followeeId` (long) — 被取消关注的用户 ID
- **返回**（成功）：`data` 为 `"已取消关注"`
- **返回**（失败）：`code: 4013`（未登录）、`4100`（尚未关注该用户）

### GET /api/user/follow/following

- **描述**：查询我关注的人。
- **请求方式**：`GET`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `page` | int | 否 | 页码，从 0 开始，默认 0 |
  | `size` | int | 否 | 每页条数，默认 10，上限 100 |

- **返回**（成功）：`data` 为 `Page<UserSummary>`（含 `content`、`totalElements` 等）
- **返回**（失败）：`code: 4013`，未登录

### GET /api/user/follow/followers

- **描述**：查询关注我的人（粉丝）。参数与返回同 `/following`。
- **请求方式**：`GET`

### GET /api/user/follow/stats

- **描述**：关注统计，用于个人主页/动态页初始化。
- **请求方式**：`GET`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `userId` | long | 否 | 目标用户；省略时取当前登录用户 |

- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "userId": 12,
    "following": 8,
    "followers": 3,
    "followedByMe": true
  }
}
```

- `followedByMe`：当前登录用户是否已关注该用户；未登录时为 `null`
- **返回**（失败）：`code: 4001`，未登录且未提供 `userId`

### GET /api/user/follow/following-ids

- **描述**：返回我关注的人的 ID 数组，供动态流按关注范围过滤。
- **请求方式**：`GET`
- **返回**（成功）：`data` 为 long 数组，如 `[3, 7, 9]`
- **说明**：我未关注任何人时返回**空数组**（而不是退回全站内容），前端据此展示空动态
- **返回**（失败）：`code: 4013`，未登录

---

## 教参模块 `/api/teaching-materials`

> 教参（教学参考资料）是**独立于笔记**的实体：笔记是学生个人的学习沉淀，教参是教师按课程发布的资料，
> 二者权限与生命周期不同，因此不共用 `notes` 表。

### 教参字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 教参 ID |
| `title` | string | 标题，必填 |
| `content` | string | 正文（LONGTEXT） |
| `courseId` | long | 所属课程 ID，可为 null |
| `teacherId` | long | 发布教师 ID（`teacher_list.id`），可为 null |
| `materialType` | string | 类型：讲义 / 习题 / 实验指导 / 参考书目 等，自由文本 |
| `attachmentUrl` | string | 附件地址，通常由 `/api/media/upload` 上传后回填 |
| `isPublic` | boolean | 是否公开（学生可见），默认 `true` |
| `viewCount` | int | 浏览量，默认 0 |
| `createdTime` / `updatedTime` | datetime | 创建 / 更新时间 |

> `attachmentUrl` 会进入 `href` / `src`，因此与机构图片一样受协议校验：
> 仅接受站内路径（`/...`）或 `http(s)` 地址，其他协议返回 `4006`。

### POST /api/teaching-materials/create

- **描述**：新建教参。
- **请求方式**：`POST`，`application/json`
- **请求体**：教参对象（`title` 必填，其余可选；`id` 会被忽略并重新生成）
- **返回**（成功）：`data` 为创建后的教参对象
- **返回**（失败）：`code: 4001`（`title` 为空）、`4006`（附件地址不合法）

### GET /api/teaching-materials/{id}

- **描述**：按 ID 读取教参。
- **请求方式**：`GET`
- **路径参数**：`id` (long) — 教参 ID
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `countView` | boolean | 否 | 是否累加浏览量，默认 `false` |

- **说明**：`countView` 默认 false——GET 带副作用容易被预取/重试放大浏览量，请在真实阅读场景显式传 `true`
- **返回**（失败）：`code: 4042`，教参不存在

### GET /api/teaching-materials/teacher/{teacherId}

- **描述**：按发布教师分页查询（「我的教参」使用该接口）。
- **请求方式**：`GET`
- **查询参数**：`page`（默认 0）、`size`（默认 10，上限 100）
- **返回**（成功）：`data` 为 `Page<TeachingMaterial>`

### GET /api/teaching-materials/course/{courseId}

- **描述**：按课程分页查询。
- **请求方式**：`GET`
- **返回**（成功）：`data` 为 `Page<TeachingMaterial>`

### GET /api/teaching-materials/public

- **描述**：公开教参分页，供学生浏览。
- **请求方式**：`GET`
- **返回**（成功）：`data` 为 `Page<TeachingMaterial>`，仅含 `isPublic = true` 的记录

### PUT /api/teaching-materials/update/{id}

- **描述**：更新教参，**仅覆盖请求中提供的非 null 字段**。
- **请求方式**：`PUT`，`application/json`
- **路径参数**：`id` (long) — 教参 ID
- **返回**（成功）：`data` 为更新后的教参对象
- **返回**（失败）：`code: 4042`（不存在）、`4006`（附件地址不合法）

### DELETE /api/teaching-materials/{id}

- **描述**：删除教参。
- **请求方式**：`DELETE`
- **路径参数**：`id` (long) — 教参 ID
- **返回**（成功）：`data` 为 `"教参已删除，ID: x"`
- **返回**（失败）：`code: 4042`，教参不存在

---

## 学习统计模块 `/api/learning`

> 把原先散落在前端的折算规则（完成率、**成长等级**、标签分布、活跃度趋势）收到后端一处，
> 同时服务「成长地图」与「学习分布」两个页面，保证口径一致、可追溯。

### GET /api/learning/stats

- **描述**：取学习统计聚合数据。
- **请求方式**：`GET`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `studentId` | long | 否 | 学生 ID；省略时取当前登录用户绑定的学生身份 |
  | `teacherId` | long | 否 | 教师 ID；省略时取当前登录用户绑定的教师身份 |
  | `role` | string | 否 | 当登录用户可解析出两种身份时消歧：`STUDENT` / `TEACHER`，默认按学生处理 |

- **返回**（成功）：

```json
{
  "code": 2000,
  "data": {
    "role": "STUDENT",
    "studentId": 12,
    "teacherId": null,
    "displayName": "张三",
    "courses": { "enrolled": 6, "completed": 3, "completionRate": 0.5 },
    "notes": { "total": 15, "publicCount": 9, "totalLikes": 12, "totalComments": 4 },
    "follows": { "following": 8, "followers": 3 },
    "growth": {
      "key": "steady",
      "title": "稳固",
      "description": "完成 3 门课程并沉淀 3 篇笔记",
      "achieved": 2,
      "nextTitle": "深耕",
      "progressToNext": 0.5
    },
    "tagDistribution": [
      { "label": "编程", "count": 3, "ratio": 0.6 },
      { "label": "数学", "count": 2, "ratio": 0.4 }
    ],
    "activityByMonth": [
      { "month": "2026-08", "count": 4 },
      { "month": "2026-09", "count": 11 }
    ],
    "notesByCourse": [
      { "label": "数据结构", "count": 9, "ratio": 0.6 }
    ]
  }
}
```

### 成长等级口径（后端唯一规则）

各等级要求**全部条件同时满足**（不是满足其一），取满足的最高级：

| key | 名称 | 达成条件 |
|------|------|------|
| `start` | 启程 | 加入第一门课程 |
| `steady` | 稳固 | 完成 **3** 门课程 **且** 沉淀 **3** 篇笔记 |
| `deep` | 深耕 | 完成 **6** 门课程 **且** 笔记获 **10** 次点赞 |
| `expert` | 领航 | 完成 **10** 门课程 **且** 保持 **15** 篇以上笔记 |

- `achieved`：当前达到的阶段序号，1~4
- `progressToNext`：距离下一阶段的完成度 0~1，**取各项条件完成比例的最小值**
  （避免"课程够了但笔记不够"却显示满格）；已是最高阶段时为 `1`
- 前端无需再自行折算等级，直接展示 `title` 与 `description` 即可

### 说明与边界

- 教师维度：`courses` / `notes` 置零、`growth` 为 `null`，仅返回 `follows` 等可用指标；
  教师的开课分布仍走 `/api/course/search?teachingTeachers=`，不在本接口重复统计
- `notes.total` 受后端扫描上限（500 条）影响，个人笔记量远小于该值，可忽略
- 学生维度的学习数据属公开统计口径，当前**允许查询任意学生**；如需收紧可在此接口加权限判断
- **返回**（失败）：`code: 4001`（无法确定统计对象）、`4042`（学生/教师不存在）

---