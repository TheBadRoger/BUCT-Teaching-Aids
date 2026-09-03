# 后台管理系统 前后端链路说明（表2 页面 → API）

> 面向「北化教学辅助系统」后台管理系统。前端为静态 HTML+JS（位于 `API/JavaAPI/src/main/resources/static/`），后端为 Java Spring Boot（`/api/**`）。
>
> 本次目标：把表2 中列出的 13 个后台管理页面，从前端真正接到后端接口上，形成「列表 → 查看 → 新增 → 编辑 → 删除」的完整业务链路，替换掉原先的 mock 数据与 `alert()` 占位逻辑。

---

## 一、通用约定

- **接口基础路径**：`/api`（前端统一通过 `js/admin-common.js` 中的 `API_BASE` 拼接）。
- **统一响应结构**：`{ code, msg, timestamp, data }`，`code === 2000` 表示成功。
- **认证**：整个后台受 Spring Security 保护（`anyRequest().authenticated()`）。管理员登录成功后，后端把认证信息写入 Session（`JSESSIONID`，由 Cookie 自动携带），后续请求即可通过校验。
- **公共前端资源**：
  - `css/admin.css`：后台公共样式（导航、表单、表格、分页、详情）。
  - `js/admin-common.js`：请求封装（`request / apiGet / apiPost / apiPostForm / apiPut / apiDelete`）、`buildQuery`、`getParam`、`toast`、`escapeHtml`、`renderAdminNav`、`renderPagination`、`requireAdminLogin`、`adminLogout`。

---

## 二、页面与接口对照表

| 表中序号 | 页面 | 前端文件 | 后端接口 | 方法/类型 |
|------|------|----------|----------|-----------|
| 1 | 登录 | `login.html` | `/api/admin/login` | POST `application/x-www-form-urlencoded`（`username`、`password`） |
| 2 | 教师列表 | `teacherlist.html` | `/api/teacher/search` | POST form-urlencoded（`name`/`organization`/`gender`/`education`/`jointime`/`page`/`size`/`sort`，返回分页 `TeacherDTO`） |
| 2 | 教师列表（删除） | `teacherlist.html` | `/api/teacher/batch` | DELETE JSON（ID 数组） |
| 2 | 教师列表（导出） | `teacherlist.html` | `/api/teacher/export` | GET（返回 `.xlsx` 文件流） |
| 3 | 编辑教师 | `edit-teacher.html` | `/api/teacher/{id}`（读取）、`/api/teacher/update?id=`（保存） | GET；PUT JSON |
| 4 | 新增教师 | `add-teacher.html` | `/api/teacher/add-with-user` | POST JSON（`teacher` + `username`/`password`/`telephone`/`email`） |
| 5 | 教师介绍 | `teacher-detail.html` | `/api/teacher/{id}` | GET |
| 6 | 学生列表 | `studentlist.html` | `/api/students/search` | GET query（`name`/`studentNumber`/`className`/`gender`/`page`/`size`/`sort`，返回分页 `StudentDTO`） |
| 6 | 学生列表（删除） | `studentlist.html` | `/api/students/batch` | DELETE JSON（ID 数组） |
| 6 | 学生列表（导出） | `studentlist.html` | `/api/students/export` | GET（`name`/`studentNumber`/`className`/`gender`，返回 `.xlsx`） |
| 7 | 编辑学生 | `edit-student.html` | `/api/students/search?studentNumber=`（读取）、`/api/students/update?id=`（保存） | GET；PUT JSON |
| 8 | 新增学生 | `add-student.html` | `/api/students/add-with-user` | POST JSON（`student` + `username`/`password`/`telephone`/`email`） |
| 9 | 学生详情 | `student-detail.html` | `/api/students/search?studentNumber=` | GET |
| 10 | 课程列表 | `courselist.html` | `/api/course/search` | GET query（`courseName`/`courseNumber`/`teachingTeachers`/`courseStatus`/`page`/`size`/`sort`，返回分页 `Course`） |
| 10 | 课程列表（删除） | `courselist.html` | `/api/course/batch` | DELETE JSON（ID 数组） |
| 10 | 课程列表（导出） | `courselist.html` | `/api/course/export` | GET（返回 `.xlsx`） |
| 11 | 编辑课程 | `editcourse.html` | `/api/course/search?courseNumber=`（读取）、`/api/course/update?id=`（保存） | GET；PUT JSON |
| 12 | 新增课程 | `addcourse.html` | `/api/course/add` | POST JSON（`Course` 对象） |
| 13 | 课程介绍 | `course-detail.html` | `/api/course/search?courseNumber=` | GET |

---

## 三、字段与数据约束（关键点）

- **教师 `TeacherDTO`**：`id, name, organization, gender, education, jointime, username, telephone, email, userType`。
  - 新增：`POST /api/teacher/add-with-user`，请求体 `{ teacher: {...}, username, password, telephone, email }`。
  - 编辑/详情读取：`GET /api/teacher/{id}`（本次新增接口，见下文）。
- **学生 `StudentDTO`**：`id, studentNumber, name, className, gender, admissionDate, username, telephone, email, userType`。
  - 学号 `studentNumber` 唯一，作为编辑/详情页的查询键（`GET /api/students/search?studentNumber=xx&size=1` 取第一条即可拿到 `id` 用于更新）。
- **课程 `Course`**：`id, courseName, courseNumber, courseIntroduction, startDate, teachingObjectives, duration, teachingTeachers, teachingClasses, targetAudience, classAddress, coursePrice, courseStatus, courseTags, courseOutline, courseImage, viewCount`。
  - 课程编号 `courseNumber` 唯一，作为编辑/详情页的查询键（`GET /api/course/search?courseNumber=xx&size=1`）。

---

## 四、本次后端改动

为支撑「编辑/详情页按 ID 回填」这一链路，对 Java 后端做了最小扩展：

1. **教师**：新增 `GET /api/teacher/{id}` 接口。
   - `TeacherService` 增加 `TeacherDTO getTeacherById(Long id)`。
   - `IMPL_TeacherService` 实现（基于 `teacherReposit.findTeacherListById`，教师 `@OneToOne` 关联默认 EAGER，可直接读取 `user`）。
   - `TeacherListCtrl` 增加 `@GetMapping("/{id}")`，未找到返回 `4042`。
   - > 学生、课程未新增接口：二者都有唯一的业务编号（学号/课程编号），直接用现有 `search` 接口按唯一编号查询即可取回记录。

2. **管理员登录在 Spring Security 下真正生效**：
   - `SecurityAuthorize` 将 `/login.html`、`/api/admin/login`、`/api/admin/register` 加入 `permitAll`。
   - `AdminUserLoginCtrl.login` 登录成功后写入 `SecurityContext` 并持久化到 Session（与 `/api/aijudegment/login`、`/api/user/auth/login` 一致），使后续访问受保护的 `anyRequest().authenticated()` 接口通过校验。

---

## 五、典型业务链路（示例：教师管理）

1. **登录**：`login.html` → `POST /api/admin/login`（username/password）。成功 → 写 `localStorage` 登录态 + 服务端 Session → 跳转 `teacherlist.html`。
2. **列表**：`teacherlist.html` 载入后 → `POST /api/teacher/search`（分页+筛选）→ 渲染表格，支持全选、批量删除、导出。
3. **查看**：点击「查看」→ `GET /api/teacher/{id}` → `teacher-detail.html` 展示教师与关联账号信息。
4. **新增**：`add-teacher.html` → `POST /api/teacher/add-with-user`（同时创建绑定登录用户，默认密码 123456）。
5. **编辑**：点击「编辑」→ `edit-teacher.html?id=` → `GET /api/teacher/{id}` 回填 → `PUT /api/teacher/update?id=` 保存。
6. **删除**：列表勾选 → `DELETE /api/teacher/batch`（ID 数组）。

> 学生、课程模块的链路与之对称，仅查询键与接口不同（学生/课程用唯一编号查询，删除/导出走各自 batch/export 接口）。

---

## 六、运行说明

- 这些页面由 Java 后端静态资源直接托管，无需单独打包前端（无构建步骤）。
- 访问入口：`http://<服务器IP>:80/login.html`（后台登录页），登录后进入 `teacherlist.html`。
- 若本地环境未部署 MySQL/Redis，直接访问页面时列表/保存会出现网络或业务报错提示（`toast`），符合预期。
