# JavaAPI 项目 Controller 层接口文档

本项目后端接口基于 Spring Boot 实现，所有接口均以 `/api/` 开头。本文档详细说明各 Controller 层接口的路由、请求方式、参数、返回值及功能，便于前端开发者调用。

---

## 目录
- [用户认证与身份绑定](#用户认证与身份绑定)
- [课程相关接口](#课程相关接口)
- [学生相关接口](#学生相关接口)
- [教师相关接口](#教师相关接口)
- [AI 相关接口](#ai-相关接口)
- [文件处理接口](#文件处理接口)
- [管理员与评测员接口](#管理员与评测员接口)

---

## 用户认证与身份绑定

### 用户认证（UserAuthCtrl）
- **POST /api/user/auth/login**
  - 统一登录（用户名密码/手机验证码/邮箱验证码）
  - 参数：JSON（LoginRequest）
  - 返回：ApiResponse<User>
- **POST /api/user/auth/register**
  - 统一注册（手机/邮箱验证码）
  - 参数：JSON（RegisterRequest）
  - 返回：ApiResponse<User>
- **POST /api/user/auth/send-code**
  - 发送验证码
  - 参数：JSON（SendCodeRequest）
  - 返回：ApiResponse<Void>
- **POST /api/user/auth/logout**
  - 登出
  - 参数：无
  - 返回：ApiResponse<Void>
- **GET /api/user/auth/current**
  - 获取当前登录用户信息
  - 参数：无
  - 返回：ApiResponse<User>

### 用户身份绑定（UserBindingCtrl）
- **POST /api/user/binding/bind-student**
  - 绑定学生身份
  - 参数：JSON（BindingRequest）
  - 返回：ApiResponse<BindingResponse>
- **POST /api/user/binding/bind-teacher**
  - 绑定教师身份
  - 参数：JSON（BindingRequest）
  - 返回：ApiResponse<BindingResponse>
- **POST /api/user/binding/unbind**
  - 解绑身份
  - 参数：无/JSON
  - 返回：ApiResponse<Void>

---

## 课程相关接口

### 课程管理（CourseCalls）
- **POST /api/course/add**
  - 新增课程
  - 参数：JSON（Course）
  - 返回：ApiResponse<Course>
- **GET /api/course/search**
  - 搜索课程（支持多条件、分页、排序）
  - 参数：courseName, courseNumber, teachingTeachers, courseStatus, courseTags, startDate, page, size, sort
  - 返回：ApiResponse<Page<Course>>

### 课程访问量统计（CourseViewController）
- **GET /api/course/view/popular**
  - 获取热门课程列表（按访问量降序，默认10条）
  - 参数：limit（可选）
  - 返回：ApiResponse<List<Course>>
- **GET /api/course/view/{courseId}/count**
  - 获取指定课程访问量
  - 参数：courseId（路径参数）
  - 返回：ApiResponse<Long>
- **POST /api/course/view/{courseId}/record**
  - 记录课程访问
  - 参数：courseId（路径参数）
  - 返回：ApiResponse<String>

### 课程热门排行（CoursePopularityController）
- **GET /api/course/popularity/ranking**
  - 获取热门课程排行（含统计信息）
  - 参数：limit（可选）
  - 返回：ApiResponse<CoursePopularityResponse>
- **GET /api/course/popularity/top10-simple**
  - 获取前10热门课程（简化版）
  - 参数：无
  - 返回：ApiResponse<List<CoursePopularityItem>>

---

## 学生相关接口

### 学生管理（StudentCtrl）
- **POST /api/students/add**
  - 新增学生
  - 参数：JSON（Student）
  - 返回：ApiResponse<Student>
- **GET /api/students/search**
  - 搜索学生（支持分页、排序）
  - 参数：name, studentNumber, page, size, sort
  - 返回：ApiResponse<Page<Student>>

### 学生课程关系（StudentCourseCtrl）
- **GET /api/student-courses/list**
  - 查询学生选课信息
  - 参数：studentId, page, size
  - 返回：ApiResponse<Page<StudentCourse>>
- **POST /api/student-courses/add**
  - 学生选课
  - 参数：studentId, courseId
  - 返回：ApiResponse<StudentCourse>

---

## 教师相关接口

### 教师管理（TeacherListCtrl）
- **GET /api/teacher/list**
  - 获取教师列表（支持分页、排序）
  - 参数：name, teacherNumber, page, size, sort
  - 返回：ApiResponse<Page<Teacher>>
- **POST /api/teacher/add**
  - 新增教师
  - 参数：JSON（Teacher）
  - 返回：ApiResponse<Teacher>

---

## AI 相关接口

### AI 评测与生成（ExternalAIGenerateCtrl）
- **POST /api/ai/generate/start**
  - 启动AI评测
  - 参数：extractedTexts（List<String>），fileNames（List<String>），counts（int）
  - 返回：ApiResponse<Map<String, String>>

---

## 文件处理接口

### 文件上传与内容提取（FileExtractCtrl）
- **POST /api/fileextract/temp**
  - 上传文件并提取内容
  - 参数：files（MultipartFile[]）
  - 返回：ApiResponse<List<DocResult>>

### 文件下载（FileDownloadCtrl）
- **GET /api/generate/download/{timeStamp}**
  - 下载生成的报告文件
  - 参数：timeStamp（路径参数），text（请求参数）
  - 返回：ResponseEntity<byte[]>（Excel文件流）

---

## 管理员与评测员接口

### 管理员登录注册（AdminUserLoginCtrl）
- **POST /api/admin/login**
  - 管理员登录
  - 参数：username, password
  - 返回：ApiResponse<AdminUser>
- **POST /api/admin/register**
  - 管理员注册
  - 参数：JSON（AdminUser）
  - 返回：ApiResponse<AdminUser>

### 评测员登录注册（JudgeUserLoginCtrl）
- **POST /api/aijudegment/login**
  - 评测员登录
  - 参数：username, password
  - 返回：ApiResponse<JudgementUser>
- **POST /api/aijudegment/register**
  - 评测员注册
  - 参数：JSON（JudgementUser）
  - 返回：ApiResponse<JudgementUser>

---

## 说明
- 所有接口均返回统一响应结构 `ApiResponse<T>`，包含 `code`（状态码）、`msg`（消息）、`data`（数据）。
- 需要鉴权的接口请先完成登录。
- 参数类型如无特殊说明，均为JSON对象或常规表单参数。
- 如需详细参数结构或返回值示例，请查阅对应实体类或DTO定义。

