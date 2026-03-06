# Java 后端接口参考（供前端参考）

说明：本文档列出 BUCT 教学辅助系统 Java 后端（Spring Boot）已实现的全部控制层接口。包含请求方法、路由、接受的参数与类型、返回的响应体字段与含义，以及接口功能的简要描述。

---

## 通用说明

- **基础地址**：`http://<服务器IP>:8080`（默认端口，可通过配置文件修改）
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
- [课程管理模块](#课程管理模块-apicourse)
- [课程访问量模块](#课程访问量模块-apicourseview)
- [课程热门排行模块](#课程热门排行模块-apicoursepopularity)
- [教师管理模块](#教师管理模块-apiteacher)
- [学生管理模块](#学生管理模块-apistudents)
- [学生选课模块](#学生选课模块-apistudent-courses)
- [文件提取模块](#文件提取模块-apifileextract)
- [AI报告生成模块](#ai报告生成模块)

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
- **返回**（成功）：同登录接口 `data` 字段，返回当前 User 对象
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
- **描述**：多条件分页搜索教师，所有条件均为可选。
- **请求方式**：`POST`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `name` | string | 否 | 按姓名模糊搜索 |
  | `organization` | string | 否 | 按单位模糊搜索 |
  | `jointime` | string | 否 | 按入职时间过滤 |
  | `gender` | string | 否 | 按性别过滤 |
  | `education` | string | 否 | 按学历过滤 |
  | `page` | int | 否 | 页码，从 0 开始，默认 `0` |
  | `size` | int | 否 | 每页条数，默认 `10` |
  | `sort` | string | 否 | 排序字段，默认 `id` |

- **返回**（成功）：`data` 字段为分页对象（`Page<Teacher>`），结构同课程搜索

---

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
- **描述**：多条件分页搜索学生，所有条件均为可选。
- **请求方式**：`GET`
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `name` | string | 否 | 按姓名模糊搜索 |
  | `studentNumber` | string | 否 | 按学号模糊搜索 |
  | `className` | string | 否 | 按班级过滤 |
  | `gender` | string | 否 | 按性别过滤 |
  | `telephone` | string | 否 | 按手机号过滤 |
  | `email` | string | 否 | 按邮箱过滤 |
  | `page` | int | 否 | 页码，从 0 开始，默认 `0` |
  | `size` | int | 否 | 每页条数，默认 `10` |
  | `sort` | string | 否 | 排序字段，默认 `id` |

- **返回**（成功）：`data` 字段为分页对象（`Page<Student>`），结构同课程搜索

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
- **描述**：根据 AI 批改生成的文本内容，生成并下载 Excel（`.xlsx`）格式的批改报告。
- **路径参数**：`timeStamp` (string) — 文件名中的时间戳标识
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `text` | string | 是 | 完整的 AI 批改结果文本（由 SSE 流拼接得到） |

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

- **注意**：该接口直接响应文件下载，无需解析 JSON，前端可直接通过 `<a href="...">` 触发下载。
