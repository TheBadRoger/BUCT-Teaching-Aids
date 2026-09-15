# 学生智慧前台 前后端链路说明（前端页面 → Java API）

> 面向「北化教学辅助系统」学生智慧前台。前端为原生静态 HTML + 模块化 JS（位于 `API/JavaAPI/src/main/resources/static/`），后端为 Java Spring Boot（`/api/**`）。
>
> 本次目标：按《前端页面对应表》把尚未实现的页面补齐到 Java 后端静态资源区，并**对齐到后端已实现接口**；对后端暂未提供接口的能力，页面给出明确说明，不使用假数据、不伪造保存成功。

---

## 一、通用约定

- **接口基础路径**：`/api`，前端统一通过 `js/frontend-common.js` 中的 `API_BASE` 拼接。
- **统一响应结构**：`{ code, msg, timestamp, data }`，`code === 2000` 表示成功。
- **认证**：页面本身通过 `SecurityAuthorize` 放行，页面内调用的 `/api/**` 仍需登录（Session + `JSESSIONID` Cookie）。未登录时后端返回 `4013`，前端统一跳转 `enter.html`。
- **公共前端资源（本次新增）**：
  - `css/frontend.css`：学生前台设计系统（设计令牌、按钮、表单、表格、卡片、标签页、模态框、状态占位、响应式栅格）。
  - `js/frontend-common.js`：请求封装（`request / apiGet / apiPost / apiPut / apiDelete`）、`buildQuery / getParam / escapeHtml / toast`、`formatDate / formatDateTime / fromNow`、`pageContent / renderLoading / renderEmpty / renderError / renderPagination`、登录与角色（`getCurrentUser / getBindingInfo / resolveRole / resolveStudent / resolveTeacher`）、页面骨架（`initPageShell`，统一渲染顶部导航、二级导航与侧边栏）、课程卡片（`courseCardHtml / courseDetailUrl / coursePlayUrl`）。
- **设计一致性**：主色沿用首页 `#165DFF`，白底卡片 + `#e1e5eb` 分隔线 + 4/8px 圆角，与 `index.html` / `guessyouneed.html` 保持一致；全部样式使用 CSS 自定义属性，移动优先响应式，无第三方框架与图表库。

---

## 二、页面与接口对照表

### 1. 已存在页面（仅做接口接线，未改动设计）

| 页面名称 | 前端文件 | 后端接口 |
|---|---|---|
| 未登录首页 | `index.html` | `/api/course/popularity/ranking`、`/api/course/search` |
| 搜索 | `search.html` | `/api/course/search`、`/api/teacher/search` |
| 还未看的课 | `towatchcourse.html` | `/api/student-courses/not-viewed-courses` |
| 已经看过的课 | `watchedcourse.html` | `/api/student-courses/viewed-courses` |
| 我的教参 | `my-teaching-materials.html` | `/api/course/search`、`/api/notes/course/{id}` |
| 我的提问 | `myquestion.html` | `/api/notes/student/{studentId}` |
| 提问详情 | `questiondetail.html` | `/api/notes/{id}`、`/api/comments/note/{noteId}` |
| 我要关注 | `myfollow.html` | `/api/teacher/search`、`/api/notes/public` |
| 猜你需要 | `guessyouneed.html` | `/api/course/search`、`/api/course/view/popular` |
| 学习记录 / 学习分布（一期） | `record.html`、`study.html` | `/api/student-courses/*`、`/api/notes/*` |
| 学生/老师/机构-个人资料 | `my.html` | `/api/user/auth/current`、`/api/user/binding/info` |
| 名师-列表详情 | `distinguishedteacherlist.html` | `/api/teacher/search`、`/api/teacher/{id}` |
| 智能体对话页 | `AIchat.html` | `/api/ai/generate/start`、`/api/ai/generate/stream/{id}` |

### 2. 本次新建页面

| 对应表序号 | 页面名称 | 前端文件（HTML / CSS / JS） | 主要后端接口 |
|---|---|---|---|
| 1 | 个性化学情报告 | `report.html` / `report.css` / `report.js` | `GET /api/user/auth/current`、`GET /api/user/binding/info`、`GET /api/student-courses/all-courses`、`GET /api/student-courses/viewed-courses`、`GET /api/student-courses/not-viewed-courses`、`GET /api/notes/student/{id}`、`GET /api/course/search?teachingTeachers=` |
| 2 | AI 智能体首页 | `ai-agent.html` / `ai-agent.css` / `ai-agent.js` | `GET /api/user/auth/current`、`GET /api/user/binding/info`、`GET /api/course/search`、`GET /api/student-courses/all-courses`、`GET /api/notes/student/{id}`，并跳转 `AIchat.html` / `AIJudge.html` |
| 5 | 申请成为老师 | `apply-teacher.html` / `apply-teacher.css` / `apply-teacher.js` | `GET /api/user/binding/info`、`POST /api/user/binding/bind`（`bindingType=TEACHER`）、`POST /api/user/binding/unbind` |
| 6 | 在线录制视频 | `video-record.html` / `video-record.css` / `video-record.js` | `GET /api/course/search?teachingTeachers=`、`PUT /api/course/update?id=`（写入 `courseOutline` 课时） |
| 7 | 我的易课堂 | `my-class.html` / `my-class.css` / `my-class.js` | `GET /api/student-courses/all-courses`、`viewed-courses`、`not-viewed-courses`、`PUT /api/student-courses/update-viewed`、`DELETE /api/student-courses/drop`、`GET /api/notes/student/{id}` |
| 8 | 我的学习记录（学生、老师） | `learn-record.html` / `learn-record.css` / `learn-record.js` | `GET /api/student-courses/all-courses`、`viewed-courses`、`GET /api/notes/student/{id}`、`GET /api/course/search?teachingTeachers=` |
| 11 | 我的笔记 | `my-note.html` / `my-note.css` / `my-note.js` | `GET /api/notes/student/{id}`、`GET /api/notes/public`、`GET /api/notes/search`、`GET /api/notes/popular`、`GET /api/notes/{id}`、`POST /api/notes/create`、`PUT /api/notes/update/{id}`、`DELETE /api/notes/{id}`、`POST /api/notes/{id}/like`、`GET /api/student-courses/all-courses` |
| 17 | 我开的课（老师、机构） | `my-course.html` / `my-course.css` / `my-course.js` | `GET /api/course/search?teachingTeachers=`、`DELETE /api/course/batch`、`GET /api/course/view/{id}/count` |
| 18 | 我要开课 - 上传课程 | `my-course-detail.html?mode=create` / `my-course-detail.css` / `my-course-detail.js` | `POST /api/course/add` |
| 19 | 我已开的课 - 课程 | `my-class.html`（教师视角） / `my-course.html` | `GET /api/course/search?teachingTeachers=` |
| 20 | 我已开的课 - 课程详情 | `my-course-detail.html?courseNumber=` / `my-course-detail.js` | `GET /api/course/search`、`PUT /api/course/update?id=`、`GET /api/course/view/{id}/count`、`GET /api/course/popularity/{id}/ranking`、`GET /api/students/search?className=` |
| 21 | 发布信息（机构） | `publish-info.html` / `publish-info.css` / `publish-info.js` | `POST /api/organization/add`、`PUT /api/organization/{id}`、`GET /api/organization/search`、`GET /api/organization/{id}`、`DELETE /api/organization/{id}` |
| 22 | 成长地图（学生、老师） | `growth-map.html` / `growth-map.css` / `growth-map.js` | `GET /api/student-courses/all-courses`、`viewed-courses`、`GET /api/notes/student/{id}`、`GET /api/course/search?teachingTeachers=` |
| 23 | 学习记录（二期） | `learn-record.html` | 同序号 8 |
| 24 | 学习分布（二期） | `study-distribution.html` / `study-distribution.css` / `study-distribution.js` | `GET /api/student-courses/all-courses`、`viewed-courses`、`GET /api/notes/student/{id}`、`GET /api/course/search?teachingTeachers=` |
| 25 | 个人中心 | `personal-center.html` / `personal-center.css` / `personal-center.js` | `GET /api/user/auth/current`、`GET /api/user/binding/info`、`POST /api/user/auth/logout`、`POST /api/user/binding/unbind`、`GET /api/student-courses/*`、`GET /api/notes/student/{id}` |
| 29 | 好友动态 | `friend-activity.html` / `friend-activity.css` / `friend-activity.js` | `GET /api/notes/public`、`/api/notes/popular`、`/api/notes/search`、`POST /api/notes/{id}/like`、`GET /api/notes/{id}/liked`、`GET /api/comments/note/{noteId}`、`POST /api/comments/add`、`POST /api/comments/reply/{parentId}` |
| 30 | 课程大全 - 列表 | `course-list.html` / `course-list.css` / `course-list.js` | `GET /api/course/search`、`GET /api/course/popularity/ranking`、`POST /api/student-courses/select` |
| 31 | 课程详情 | `course-info.html` / `course-info.css` / `course-info.js` | `GET /api/course/search?courseNumber=`、`POST /api/course/view/{id}/record`、`GET /api/course/view/{id}/count`、`GET /api/notes/course/{id}`、`POST /api/student-courses/select`、`PUT /api/student-courses/update-viewed` |
| 32 | 课时播放 - 普通视频 | `course-play.html` / `course-play.css` / `course-play.js` | `GET /api/course/search?courseNumber=`、`GET /api/notes/course/{id}`、`POST /api/notes/create`、`PUT /api/student-courses/update-viewed`、`POST /api/course/view/{id}/record` |
| 33 | 课时播放 - 互动视频（二期） | `interactive-play.html` / `interactive-play.css` / `interactive-play.js` | `GET /api/course/search?courseNumber=`、`POST /api/student-courses/select`、`PUT /api/student-courses/update-viewed`、`POST /api/notes/create`（互动点需后端二期字段支持，见第五节） |
| 34 | 名师 - 列表 | `distinguishedteacherlist.html` | `POST /api/teacher/search` |
| 35 | 名单 - 列表 | `gradelist.html` / `gradelist.css` / `gradelist.js` | `GET /api/course/search?teachingTeachers=`、`GET /api/students/search?className=` |
| 35 | 名单 - 列表详情 | `gradedetail-list.html` / `gradedetail-list.css` / `gradedetail-list.js` | `GET /api/students/search`、`GET /api/students/export`、`GET /api/course/search?teachingTeachers=` |
| 35 | 名单 - 详情 | `gradedetail.html` / `gradedetail.css` / `gradedetail.js` | `GET /api/students/search?className=`、`GET /api/course/search?teachingTeachers=`、`GET /api/student-courses/all-courses?studentId=` |
| 36 | 机构专区 | `institution.html` / `institution.css` / `institution.js` | `GET /api/organization/search`、`GET /api/organization/{id}`、`GET /api/teacher/search?organization=`、`GET /api/course/search?teachingTeachers=` |

---

## 三、角色与数据口径

前端通过 `GET /api/user/auth/current` + `GET /api/user/binding/info` 判定角色（`student` / `teacher` / `guest`），并据此切换侧边栏与数据来源：

| 角色 | 判定依据 | 数据口径 |
|---|---|---|
| 学生 | `userType === "STUDENT"` 或 `user.student` 存在 | `/api/student-courses/**`（需 `student.id`）、`/api/notes/student/{studentId}` |
| 教师 | `userType === "TEACHER"` 或 `user.teacher` 存在 | `/api/course/search?teachingTeachers={teacher.name}`、`/api/students/search?className=` |
| 未绑定 | `userType === null` | 展示引导（申请成为老师 / 选课），不展示受身份约束的数据 |

> 注意：`/api/notes/create` 会校验 `student.id` 与 `course.id` 必须存在，因此前端在创建笔记时强制要求选择“已加入的课程”，并在未绑定学生身份时明确提示，避免出现 4042。

---

## 四、关键前端约定

1. **课程定位方式**：后端 `/api/course/search` 不支持按主键 `id` 查询，因此**课程详情与课时播放统一使用课程编号 `courseNumber` 作为查询键**（`course-card` 生成的链接均为 `course-info.html?courseNumber=xxx`）。
2. **学习状态**：`isViewed` 通过 `PUT /api/student-courses/update-viewed?studentId=&courseId=&isViewed=` 切换，课程播放页在视频 `ended` 时自动置为已学。
3. **课时与视频**：后端无独立课时实体，课时信息存放于 `Course.courseOutline`，约定格式为每行一个课时，`课时名称|视频地址`（视频地址可省略）；互动视频页扩展为 `课时名称|视频地址|00:30:选择题:题干|选项A;选项B`。
4. **名单与班级**：后端无“班级”实体，班级来源于 `Course.teachingClasses`（支持逗号分隔多班级），学生名单通过 `GET /api/students/search?className=` 获取。
5. **机构归属**：`Course` 未直接关联机构，机构→课程通过“机构下的教师 → 教师姓名 → 授课教师”间接匹配。
6. **图表实现**：全部统计图使用 CSS（`progress` / 柱状 `chart` / 横向 `bar-row` / 环形 `conic-gradient`）绘制，不引入 ECharts 等第三方库。
7. **安全**：所有插入 DOM 的动态文本统一经 `escapeHtml` 转义；交互使用 `addEventListener` + 事件委托，无内联事件处理器；外部链接使用 `rel="noopener noreferrer"`。

---

## 五、后端待补充能力（页面已给出说明，未使用假数据）

| 页面 | 缺失能力 | 建议接口 |
|---|---|---|
| 课时播放 - 互动视频（二期） | 课件时间轴与互动点（选择题 / 投票 / 分支）无实体与接口 | 新增 `course_lesson_marker` 表与 `GET /api/course/{courseId}/lessons`、`GET /api/lesson/{lessonId}/markers` |
| 在线录制视频 | 无视频文件上传接口，录制结果只能本地下载 | 新增 `POST /api/files/upload`（multipart），返回可访问 URL |
| 个人中心 | 无资料修改 / 密码修改接口，故资料字段为只读展示 | 新增 `PUT /api/user/profile`、`PUT /api/user/password` |
| 好友动态 | 无“关注关系”接口，动态流以公开笔记替代 | 新增 `follow` 表与 `GET /api/follow/feed` |
| 我的易课堂 / 学习记录 | 无学习行为流水（时长、播放进度）接口 | 新增 `course_learning_log` 表与 `GET /api/learning/logs?studentId=` |
| 申请成为老师 | 无独立的教师申请单与审核状态 | 可扩展 `teacher_application` 表与审核接口（当前复用实名绑定接口） |

---

## 六、本次后端改动

仅一处，且为必要放行（页面内调用的接口仍需登录）：

- `SecurityAuthorize` 的 `permitAll` 列表新增学生前台的**公开页面路径**：`/index.html`、`/search.html`、`/course-list.html`、`/course-info.html`、`/course-play.html`、`/interactive-play.html`、`/guessyouneed.html`、`/ai-agent.html`、`/AIchat.html`、`/institution.html`。
  - 说明：此前未登录访问这些页面会被 Spring Security 重定向到 `/enter.html`，导致页面无法作为访客入口；放行后页面可正常打开，页面内的 `/api/**` 数据请求依然受保护（未登录时前端会跳转登录页）。

---

## 七、运行说明

- 这些页面由 Java 后端静态资源直接托管，无需单独打包前端（无构建步骤）。
- 访问入口：`http://<服务器IP>:80/index.html`，课程浏览入口 `course-list.html`，个人中心 `personal-center.html`。
- 若本地环境未部署 MySQL/Redis，页面会展示错误/空状态并给出“重新加载”按钮，属预期行为。
- 浏览器要求：课时播放、录制等使用 `MediaRecorder` / `getUserMedia` 的页面需 Chrome / Edge 最新版；课程与笔记功能无特殊要求。
