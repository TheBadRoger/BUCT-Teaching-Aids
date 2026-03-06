# 后端控制层接口参考（供前端参考）

说明：本文档列出项目中已实现的控制器接口（未实现或空实现的控制器已跳过）。包含调用方法、路由、请求参数（类型/是否必需）、返回值、返回值字段类型，以及接口作用与注意事项。

---

## 通用说明

- 所有成功/失败响应均使用 ApiResponse<T> 包裹，结构如下：
  - `code` (int)：业务状态码（例如 2000 表示成功）
  - `msg` (string)：提示信息或错误描述
  - `timestamp` (long)：服务器时间戳（毫秒）
  - `data` (T)：实际返回的数据（成功时）

- 常见分页对象（Spring Data `Page<T>`）常见序列化字段：
  - `content`: T[] — 当前页内容
  - `totalElements`: long — 总元素数
  - `totalPages`: int — 总页数
  - `number`: int — 当前页索引（通常从 0 开始）
  - `size`: int — 每页大小
  - `numberOfElements`: int — 本页元素数量

- 若接口使用 `@RequestBody`，请使用 `Content-Type: application/json`。
- 若接口使用 `@RequestParam List<String>`（如列表参数），前端可使用重复参数或 multipart/form-data 的同名字段。
- SSE（Server-Sent Events）接口直接返回 `SseEmitter`，不使用 ApiResponse 包裹，前端建议使用 `EventSource` 订阅。
- 标注“需登录”或“管理员”的接口需确保前端已经通过登录建立 session/cookie 或其他鉴权机制。

---

## 目录（按控制器）
- `AdminUserLoginCtrl` (/api/admin)
- `CourseCalls` (/api/course)
- `CoursePopularityController` (/api/course/popularity)
- `CourseViewController` (/api/course/view)
- `ExternalAIGenerateCtrl` (/api/ai)
- `FileDownloadCtrl` (/api/generate)
- `FileExtractCtrl` (/api/fileextract)
- `JudgeUserLoginCtrl` (/api/aijudegment)
- `StudentCourseCtrl` (/api/student-courses)
- `StudentCtrl` (/api/students)
- `TeacherListCtrl` (/api/teacher)
- `UserAuthCtrl` (/api/user/auth)
- `UserBindingCtrl` (/api/user/binding)

> 注：`StudentLoginCtrl` 在源码中为空，已跳过。

---

## `AdminUserLoginCtrl` — 路径前缀 `/api/admin`

### POST /api/admin/login
- 描述：管理员登录。
- 请求方式：POST（表单或 query param）
- 请求参数：
  - `username`: String (required)
  - `password`: String (required)
- 返回：`ApiResponse<AdminUser>`
  - AdminUser 字段：`id` (long), `username` (String), `password` (String)

### POST /api/admin/register
- 描述：管理员注册。
- 请求方式：POST，JSON body（AdminUser）
- 请求体字段：`username` (String), `password` (String)
- 返回：`ApiResponse<AdminUser>`（返回时 password 被掩码为固定字符串）

---

## `CourseCalls` — 路径前缀 `/api/course`

实体：`Course`（主要字段）
- `id`: long
- `courseName`: String
- `courseNumber`: String
- `courseIntroduction`: String
- `startDate`: String
- `teachingObjectives`: String
- `duration`: String
- `teachingTeachers`: String
- `teachingClasses`: String
- `targetAudience`: String
- `classAddress`: String
- `coursePrice`: Double
- `courseStatus`: String
- `courseTags`: String
- `courseOutline`: String
- `courseImage`: String
- `viewCount`: Long

### POST /api/course/add
- 描述：新增课程。
- 请求方式：POST，JSON body（Course）
- 返回：`ApiResponse<Course>`（成功返回创建的 Course）
- 错误：若课程已存在，返回 `BusinessStatus.ENTITY_EXISTS`。

### GET /api/course/search
- 描述：按条件搜索课程（支持分页、排序）。
- 请求方式：GET
- 查询参数：
  - `courseName`: String (optional)
  - `courseNumber`: String (optional)
  - `teachingTeachers`: String (optional)
  - `courseStatus`: String (optional)
  - `courseTags`: String (optional)
  - `startDate`: String (optional)
  - `page`: int (default 0)
  - `size`: int (default 10)
  - `sort`: String (default "id")
- 返回：`ApiResponse<Page<Course>>`

---

## `CoursePopularityController` — 路径前缀 `/api/course/popularity`

返回对象：`CoursePopularityResponse`（见 utils）
- `items`: CoursePopularityItem[]（包含 ranking, courseId, courseName, viewCount, courseStatus, courseTags, courseImage）
- `stats`: PopularityStats（包含 totalCourses, totalViews, averageViews, maxViews）
- `timestamp`: Long

### GET /api/course/popularity/ranking
- 描述：获取热门课程排行（详细版）。
- 请求参数：`limit`: int (default 10, max 100)
- 返回：`ApiResponse<CoursePopularityResponse>`

### GET /api/course/popularity/top10-simple
- 描述：获取前 10 简化版热门课程。
- 返回：`ApiResponse<List<CoursePopularityItem>>`

### GET /api/course/popularity/range
- 描述：获取指定排名区间的课程（start..end）。
- 请求参数：`start`: Integer (default 1), `end`: Integer (default 10)
- 返回：`ApiResponse<List<CoursePopularityItem>>`

### GET /api/course/popularity/{courseId}/is-popular
- 描述：检查某课程是否位于前 N 热门课程中。
- 路径参数：`courseId`: Long
- 查询参数：`limit`: int (default 10)
- 返回：`ApiResponse<Boolean>`

### GET /api/course/popularity/{courseId}/ranking
- 描述：获取某课程总体排名（找不到则返回 0）。
- 路径参数：`courseId`: Long
- 返回：`ApiResponse<Integer>`（1-based 排名或 0）

---

## `CourseViewController` — 路径前缀 `/api/course/view`

### GET /api/course/view/popular
- 描述：获取热门课程（按 viewCount 降序）。
- 查询参数：`limit`: int (default 10, max 100)
- 返回：`ApiResponse<List<Course>>`

### GET /api/course/view/{courseId}/count
- 描述：获取指定课程的访问量。
- 路径参数：`courseId`: Long
- 返回：`ApiResponse<Long>`

### POST /api/course/view/{courseId}/record
- 描述：记录一次课程访问（前端在用户打开课程时可调用）。
- 路径参数：`courseId`: Long
- 返回：`ApiResponse<String>`（成功消息）

### GET /api/course/view/top10
- 描述：便捷接口，返回前 10 热门课程。
- 返回：`ApiResponse<List<Course>>`

### POST /api/course/view/admin/sync
- 描述：手动触发 Redis -> MySQL 数据同步（管理员操作）。
- 返回：`ApiResponse<String>`
- 注意：应加权限检查，仅管理员可调用（代码注释提示）。

### POST /api/course/view/admin/rebuild-cache
- 描述：手动重建缓存（管理员）。
- 返回：`ApiResponse<String>`

### DELETE /api/course/view/admin/clear/{courseId}
- 描述：清除某课程的访问记录（管理员）。
- 路径参数：`courseId`: Long
- 返回：`ApiResponse<String>`

---

## `ExternalAIGenerateCtrl` — 路径前缀 `/api/ai`

### POST /api/ai/generate/start
- 描述：提交 AI 判题任务（异步），返回任务 id。
- 请求方式：POST（使用 @RequestParam 接收多个同名字段或 form-data）
- 请求参数：
  - `extractedTexts`: List<String>
  - `fileNames`: List<String>
  - `counts`: int
- 返回：`ApiResponse<Map<String, String>>`，示例 data: `{ "id": "<taskId>", "status": "started" }`
- 说明：若 counts == 1，代码会把多个 extractedTexts 合并为一个条目再提交。

### GET /api/ai/generate/stream/{id}
- 描述：使用 SSE 推送任务执行/结果流。
- 路径参数：`id`: String
- 返回：SseEmitter（非 ApiResponse）
- 前端建议：使用 `EventSource` 订阅（例如 `new EventSource('/api/ai/generate/stream/' + id)`）。

---

## `FileDownloadCtrl` — 路径前缀 `/api/generate`

### GET /api/generate/download/{timeStamp}
- 描述：生成并下载 Excel 报告（在内存中生成，不写磁盘）。
- 路径参数：`timeStamp`: String
- 查询参数：`text`: String（要写入 Excel 的文本）
- 返回：二进制文件（`ResponseEntity<byte[]>`），Content-Type: application/octet-stream，文件名 `judgereport_{timeStamp}.xlsx`
- 前端调用示例：使用 `fetch` 获取二进制，然后 `URL.createObjectURL` + `<a download>` 触发下载。

---

## `FileExtractCtrl` — 路径前缀 `/api/fileextract`

### POST /api/fileextract/temp
- 描述：上传文件（.docx 或 .pdf）并解析文本。
- 请求方式：POST，multipart/form-data
- 表单字段：`files`（MultipartFile[]，可多文件）
- 返回：`ApiResponse<List<DocResult>>`
  - DocResult：
    - `fileName`: String
    - `success`: boolean
    - `msg`: String
    - `content`: String | null
- 错误：若未上传文件或 files 长度为 0，返回 `BusinessStatus.RESOURCE_NOT_FOUND`。

---

## `JudgeUserLoginCtrl` — 路径前缀 `/api/aijudegment`
> 注：路径使用源码中的拼写 `/api/aijudegment`。

### POST /api/aijudegment/login
- 描述：AI 判题系统用户登录，会将认证信息写入 SecurityContext，并保存到 Session。
- 请求参数：`username`, `password`（均为 String）
- 返回：`ApiResponse<JudgementUser>`（字段：id, username, password）

### POST /api/aijudegment/register
- 描述：注册 AI 判题用户。
- 请求体：JudgementUser JSON（username, password）
- 返回：`ApiResponse<JudgementUser>`（返回时 password 被掩码）

---

## `StudentCourseCtrl` — 路径前缀 `/api/student-courses`

实体：`StudentCourse`（主要字段）
- `id`: StudentCourseId { studentId: Long, courseId: Long }
- `student`: Student
- `course`: Course
- `isViewed`: Boolean

### POST /api/student-courses/select
- 描述：学生选课（创建关系）。
- 请求参数：`studentId`: Long, `courseId`: Long
- 返回：`ApiResponse<StudentCourse>`

### PUT /api/student-courses/update-viewed
- 描述：更新学生对课程的已查看标记。
- 请求参数：`studentId`: Long, `courseId`: Long, `isViewed`: Boolean
- 返回：`ApiResponse<StudentCourse>`

### GET /api/student-courses/all-courses
- 描述：获取学生的所有课程（分页）。
- 查询参数：`studentId`: Long, `page`: int (default 0), `size`: int (default 10), `sort`: String (default "createdAt"), `direction`: String (default "desc")
- 返回：`ApiResponse<Page<StudentCourse>>`

### GET /api/student-courses/viewed-courses
- 描述：获取已查看课程（分页）。参数同上（默认 sort: "id"）。
- 返回：`ApiResponse<Page<StudentCourse>>`

### GET /api/student-courses/not-viewed-courses
- 描述：获取未查看课程（分页）。参数同上。
- 返回：`ApiResponse<Page<StudentCourse>>`

### DELETE /api/student-courses/drop
- 描述：退选某课程。
- 请求参数：`studentId`: Long, `courseId`: Long
- 返回：`ApiResponse<Void>`（data 为 null）

---

## `StudentCtrl` — 路径前缀 `/api/students`

实体：`Student`（主要字段）
- `id`: Long
- `studentNumber`: String
- `name`: String
- `className`: String
- `gender`: String
- `admissionDate`: LocalDate (序列化为 ISO 字符串)

### POST /api/students/add
- 描述：新增学生。
- 请求体：Student JSON
- 返回：`ApiResponse<Student>`
- 错误：已存在返回 `BusinessStatus.ENTITY_EXISTS`

### GET /api/students/search
- 描述：搜索学生（支持分页）。
- 查询参数（可选）：`name`, `studentNumber`, `className`, `gender`, `telephone`, `email`, `page` (default 0), `size` (default 10), `sort` (default "id")
- 返回：`ApiResponse<Page<Student>>`

---

## `TeacherListCtrl` — 路径前缀 `/api/teacher`

实体：`Teacher`（主要字段）
- `id`: long
- `name`: String
- `organization`: String
- `gender`: String
- `education`: String
- `jointime`: String

### POST /api/teacher/add
- 描述：新增教师。
- 请求体：Teacher JSON
- 返回：`ApiResponse<Teacher>`

### POST /api/teacher/search
- 描述：搜索教师（注：实现使用 POST + @RequestParam 接收查询参数）。
- 参数：`name`, `organization`, `jointime`, `gender`, `education`, `page` (default 0), `size` (default 10), `sort` (default "id")
- 返回：`ApiResponse<Page<Teacher>>`

---

## `UserAuthCtrl` — 路径前缀 `/api/user/auth`

实体：`User`（主要字段）
- `id`: Long
- `username`: String
- `telephone`: String
- `email`: String
- `password`: String
- `userType`: enum ("TEACHER" | "STUDENT")
- `teacher`: Teacher | null
- `student`: Student | null

### POST /api/user/auth/login
- 描述：统一登录接口，支持三种登录方式：用户名+密码、手机号+短信验证码、邮箱+验证码。
- 请求体：`LoginRequest` JSON
  - `loginType`: enum { PASSWORD, SMS_CODE, EMAIL_CODE } (required)
  - 若 `PASSWORD`：需要 `username` & `password`
  - 若 `SMS_CODE`：需要 `telephone` & `code`
  - 若 `EMAIL_CODE`：需要 `email` & `code`
- 返回：`ApiResponse<User>`（登录成功会写入 SecurityContext 并保存到 session）
- 错误：参数缺失返回 `PARAM_MISSING`；认证失败返回 `ACCOUNT_PASSWORD_ERROR`。

### POST /api/user/auth/register
- 描述：统一注册接口（短信或邮箱验证码）。
- 请求体：`RegisterRequest` JSON
  - `registerType`: enum { SMS_CODE, EMAIL_CODE } (required)
  - `username`, `password` (required)
  - 注册方式对应的 `telephone`/`email` 和 `code`
- 返回：`ApiResponse<User>`（成功）
- 错误：根据 service 返回的 errorCode 映射为不同 BusinessStatus（如 INVALID_CODE、PHONE_EXISTS、EMAIL_EXISTS、USERNAME_EXISTS 等）

### POST /api/user/auth/send-code
- 描述：发送验证码（短信或邮箱）。
- 请求体：`SendCodeRequest` JSON
  - `sendType`: enum { SMS, EMAIL }
  - `telephone` 或 `email` 对应使用场景
- 返回：`ApiResponse<Void>`

### POST /api/user/auth/logout
- 描述：登出，清空 session 与 SecurityContext。
- 返回：`ApiResponse<Void>`

### GET /api/user/auth/current
- 描述：获取当前登录用户（从 SecurityContext 中读取）。
- 返回：`ApiResponse<User>`（未登录返回 `TOKEN_INVALID`）

---

## `UserBindingCtrl` — 路径前缀 `/api/user/binding`

说明：用于用户与学工信息的绑定/解绑，需已登录（接口会从 SecurityContextHolder 获取当前用户）。

DTO：`BindingRequest`（字段）
- `bindingType`: enum { STUDENT, TEACHER } (required)
- `name`: String (required)
- `idCard`: String (required)
- `studentNumber`: String (when STUDENT)
- `employeeNumber`: String (when TEACHER)

DTO：`BindingResponse`（字段）
- `userId`: Long
- `username`: String
- `userType`: User.UserType
- `studentInfo`: { id, studentNumber, name, className, gender, admissionDate } | null
- `teacherInfo`: { id, name, organization, gender, education, jointime } | null

### POST /api/user/binding/bind
- 描述：绑定身份（学生或教师）。
- 请求体：`BindingRequest` JSON
- 返回：`ApiResponse<BindingResponse>`
- 错误：可能返回 `IDENTITY_VERIFY_FAILED`, `ALREADY_BOUND`, `IDENTITY_ALREADY_BOUND`, `BINDING_CONFLICT` 等

### POST /api/user/binding/unbind
- 描述：解绑当前登录用户的身份。
- 返回：`ApiResponse<Void>`

### GET /api/user/binding/info
- 描述：获取当前用户的绑定信息。
- 返回：`ApiResponse<BindingResponse>`

---

## 常见 BusinessStatus（节选）
- `SUCCESS` (2000)
- `PARAM_MISSING` (4001)
- `PARAM_FORMAT_ERROR` (4003)
- `ACCOUNT_PASSWORD_ERROR` (4011)
- `TOKEN_INVALID` (4013)
- `USER_NOT_FOUND` (4041)
- `RESOURCE_NOT_FOUND` (4042)
- `ENTITY_EXISTS` (4091), `USERNAME_EXISTS` (4092)
- `INTERNAL_ERROR` (5000)

---

## 前端调用注意点和值得关注的实现细节
- SSE：`/api/ai/generate/stream/{id}` 返回 SSE，使用 `EventSource`。
- 文件下载：`/api/generate/download/{timeStamp}?text=...` 返回二进制 Excel，建议用 `fetch` + `blob` 或直接打开下载链接。
- `ExternalAIGenerateCtrl.startJudge` 使用 `@RequestParam List<String>`，若前端希望发送 JSON 数组需后端改为 `@RequestBody`。
- 管理员接口（URL 包含 `/admin/`）需要确保具备管理员权限再调用。

---

如果你希望我把该文档放到不同路径、或为每个接口生成示例请求/响应（JSON）或 Postman/Insomnia 导出文件，我可以继续帮你生成并保存。
