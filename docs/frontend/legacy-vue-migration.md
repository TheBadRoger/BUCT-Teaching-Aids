# 旧静态页面迁移到 Vue

## 目标与约束

迁移全部旧页面，按模块推进。旧 HTML 不作为兼容页面长期保留：完成模块迁移后删除原 HTML、专用 JS/CSS，并将其他页面链接改为 Vue 路由。尚未迁移页面仅作为后续迁移输入，不将 iframe 或 v-html 注入旧页面算作 Vue 迁移。

统一 Vue 源码继续放在 `apps/web/`，同时承载学习端和管理端；沿用 Vue 3、TypeScript、Vite、Pinia、Element Plus，不改 Java/Python 业务逻辑和数据库。

## 本轮完成：学生端课程核心流程

| 原页面（已删除） | Vue 路由 | 已迁移功能 |
| --- | --- | --- |
| `index.html` | `/learn` | 首页搜索、热门课程、推荐课程、真实身份查询 |
| `course-list.html` | `/learn/courses` | 课程筛选、排序、分页、URL 查询条件、空/错误/重试状态 |
| `course-info.html` | `/learn/course?courseNumber=...` | 按编号精确定位、课程资料、大纲、选课、已学状态、课程笔记 |
| `course-play.html` | `/learn/play?courseNumber=...&lesson=...` | 课时解析、视频播放、前后课时、URL 分享、播放结束标记已学、保存笔记 |

相应专用脚本与样式已删除。课程展示用 Vue 模板转义文本，不继续拼接 innerHTML。资源请求取消旧查询并忽略迟到结果；学生身份从服务端 Cookie 与绑定信息获取，不读取管理员本地会话。课程编号不再直接使用模糊搜索第一条结果，避免定位到另一门课程。

学习页导航放行不代表 API 免认证，接口仍使用原后端认证规则。未登录、无绑定、接口失败均有提示，不使用假数据。当前登录和个人绑定页在后续账号模块迁移，现阶段学习页仍提供前往这些页面的链接。

课程目录沿用旧页面的大纲约定：每行一个课时，可写 `标题|视频地址`；未配置地址时显示说明。笔记默认私有，保存成功后才清空输入。页面离开后不再执行尚未完成校验的表单请求。

## 只提交源码，Docker 现场编译

根目录 `Dockerfile` 的 `java` 目标 的 Node 阶段执行 `npm ci` 和 Vue 构建，Maven 阶段把 `dist/` 放入 `static/js/app/`，并将编译出的入口放入 `static/index.html`，最终与 Java 一起打包。`/index.html` 和 `/js/app/index.html` 均为部署产物，默认 Vue 页面进入 `/learn`，管理路由保持 `/teacher/list` 等原路径。

Git 忽略 `dist/`、`node_modules/`、`static/js/app/`、`static/index.html`；Docker 忽略本地产物，每次从源码构建。前端 CI 检查这些目录/入口没有被 Git 跟踪。禁止提交生成的 HTML、JS 和 CSS 包；保留在旧页面目录的文件属于尚未迁移的源码。

本地开发用 `npm run dev`，完整检查用 `npm run check`。`npm run build:static` 仅供本地联调，产物同样不提交。实际部署仍使用 Docker 构建，不依赖本地 dist。

## 后续模块

1. 账号登录、注册、身份绑定、个人中心；完成后移除本轮仍链接的旧账号页面。
2. 管理页面差异核对，移除已被 Vue 取代的教师/学生/课程管理 HTML。
3. 我的课程、学习记录、笔记管理、问答、关注、动态、成长报告、教参。
4. 教师课堂与互动、成绩、录制、AI 评判、背诵工具。
5. Python 视觉和课堂页面；最后清理不再引用的公共脚本和样式。

以下是本轮结束时的剩余来源清单；迁移每个模块时需核对表单、接口、设备权限、下载和页面跳转，再删除对应源文件。

| 剩余源文件 | 页面标题 | 模块 | 状态 |
| --- | --- | --- | --- |
| `apps/api/java/src/main/resources/static/add-student.html` | 新增学生 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/add-teacher.html` | 新增教师 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/addcourse.html` | 新增课程 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/admin-enter.html` | 北化教育后台管理系统 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/ai-agent.html` | AI 智能体 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/AIchat.html` | AI 智能助手 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/AIJudge.html` | 实验报告提交 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/apply-teacher.html` | 申请成为老师 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/course-detail.html` | 课程介绍 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/courselist.html` | 课程列表 - 后台管理 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/distinguishedteacherlist.html` | 名师列表 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/edit-student.html` | 编辑学生 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/edit-teacher.html` | 编辑教师 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/editcourse.html` | 编辑课程 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/enter.html` | 北化教育管理系统 | 账号与个人中心 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/error.html` | BUCT Teaching Aids - 错误页 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/friend-activity.html` | 好友动态 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/gradedetail-list.html` | 名单 - 列表详情 - 北化教学辅助系统 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/gradedetail.html` | 名单 - 详情 - 北化教学辅助系统 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/gradelist.html` | 名单列表 - 北化教学辅助系统 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/growth-map.html` | 成长地图 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/guessyouneed.html` | 学生智慧前台-课程页 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/institution.html` | 机构专区 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/interactive-play.html` | 课时播放 - 互动视频 - 北化教学辅助系统 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/learn-record.html` | 我的学习记录 - 北化教学辅助系统 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/login.html` | 管理员登录 - 北化教学辅助系统 | 账号与个人中心 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/my-class.html` | 我的易课堂 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/my-course-detail.html` | 课程详情与维护 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/my-course.html` | 我开的课 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/my-note.html` | 我的笔记 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/my-teaching-materials.html` | 易班学习平台 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/my.html` | 个人资料页面 | 账号与个人中心 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/mycourse.html` | 我要开课-上传课程 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/myfollow.html` | 我的关注 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/myquestion.html` | 我的提问 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/personal-center.html` | 个人中心 - 北化教学辅助系统 | 账号与个人中心 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/personal-settings.html` | 个人设置 | 账号与个人中心 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/publish-info.html` | 发布信息（机构） - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/questiondetail.html` | 提问详情 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/recitation/index.html` | 知识点背诵手册 - AI智能生成 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/record.html` | 学习平台页面 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/register.html` | 注册 | 账号与个人中心 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/report.html` | 个性化学情报告 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/search.html` | 课程搜索 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/student-detail.html` | 学生详情 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/studentlist.html` | 学生列表 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/study-distribution.html` | 学习分布 - 北化教学辅助系统 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/study.html` | 学习记录页面（二期） | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/teacher-detail.html` | 教师介绍 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/teacherlist.html` | 教师列表 - 后台管理 | 管理端（Vue 已有对应页面，待差异核对） | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/towatchcourse.html` | 课程页面 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/video-record.html` | 在线录制视频 - 北化教学辅助系统 | 课堂互动与教学工具 | 待迁移/核对后删除 |
| `apps/api/java/src/main/resources/static/watchedcourse.html` | 课程页面 | 学习空间、教参及社区 | 待迁移/核对后删除 |
| `apps/api/python/templates/ai_classroom/index.html` | AI智慧课堂系统 | Python 视觉与课堂 | 待迁移/核对后删除 |
| `apps/api/python/templates/ai_classroom/student-view.html` | 学生视图 - AI智慧课堂 | Python 视觉与课堂 | 待迁移/核对后删除 |
| `apps/api/python/templates/ai_classroom/teacher-dashboard.html` | 教师仪表盘 - AI智慧课堂 | Python 视觉与课堂 | 待迁移/核对后删除 |

剩余 HTML 源文件：56。已完成学生课程核心流程的 4 个原页面；重复的教师 AI 旧页面已删除，所有 AI 页面共用统一 Vue 工程。其他模块尚未全部迁移。
