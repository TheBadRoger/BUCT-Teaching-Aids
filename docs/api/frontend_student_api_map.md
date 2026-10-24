# 学习页面接口映射

学习入口为 `/js/app/index.html#/learn`。页面允许访客打开，受保护接口仍要求后端登录会话和学生绑定。

| 页面 | Vue 路由 | 主要数据 |
| --- | --- | --- |
| 学习首页 | `/learn` | `GET /api/course/view/popular`、`GET /api/course/view/top10` |
| 课程列表 | `/learn/courses` | `GET /api/course/search`，分页与筛选 |
| 课程详情 | `/learn/course?courseNumber=...` | 按课程编号精确匹配，展示课程与笔记 |
| 课时播放 | `/learn/play?courseNumber=...&lesson=...` | 课程大纲、课时地址、已学状态和笔记 |

| 功能 | 接口 |
| --- | --- |
| 当前账号 / 学生绑定 | `GET /api/user/auth/current`、`GET /api/user/binding/info` |
| 已选课程 | `GET /api/student-courses/all-courses` |
| 选课 | `POST /api/student-courses/select` |
| 已学状态 | `PUT /api/student-courses/update-viewed` |
| 课程访问记录 | `POST /api/course/view/{courseId}/record` |
| 课程笔记 | `GET /api/notes/course/{courseId}`、`POST /api/notes/create` |

## 页面行为

| 项目 | 规则 |
| --- | --- |
| 身份 | 从后端 Cookie 与学生绑定信息获取，不以管理员本地缓存代替学生身份 |
| 课程定位 | 分页搜索后按 `courseNumber` 精确匹配，不直接使用第一条模糊查询结果 |
| 大纲格式 | 每行一个课时，格式为 `标题` 或 `标题|视频地址`；无地址时显示说明 |
| 笔记 | 默认私有，保存成功后清空输入 |
| 异常状态 | 登录、绑定、请求失败和资源不存在均提供明确提示 |
| 其他入口 | 账号绑定、个人中心和其他教学工具由 Java static 或 Python templates 页面提供 |

参数与返回结构见[Java 接口手册](java_apidoc.md)，前端命令见[前端说明](../../apps/web/README.md)。
