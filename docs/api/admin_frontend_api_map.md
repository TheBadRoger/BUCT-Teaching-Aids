# 管理页面接口映射

页面入口为 `/js/app/index.html#` 加下表路由。Java 使用 Cookie Session 认证，普通业务成功响应的 `code` 为 `2000`。

| 页面 | 路由 | 主要接口 |
| --- | --- | --- |
| 管理登录 | `/login` | `POST /api/admin/login`，表单字段 username、password |
| 教师列表 / 新增 / 详情 | `/teacher/list`、`/teacher/add`、`/teacher/detail` | `POST /api/teacher/search`、`POST /api/teacher/add-with-user`、`GET /api/teacher/{id}` |
| 教师编辑 / 删除 | 详情编辑 / 列表删除 | `PUT /api/teacher/update?id=`、`DELETE /api/teacher/batch` |
| 学生列表 / 新增 / 详情 | `/student/list`、`/student/add`、`/student/detail` | `GET /api/students/search`、`POST /api/students/add-with-user`；详情使用列表缓存或按学号查询 |
| 学生编辑 / 删除 | 详情编辑 / 列表删除 | `PUT /api/students/update?id=`、`DELETE /api/students/batch` |
| 课程列表 / 新增 / 详情 | `/course/list`、`/course/add`、`/course/detail` | `GET /api/course/search`、`POST /api/course/add`；详情按课程编号匹配 |
| 课程编辑 / 删除 | 详情编辑 / 列表删除 | `PUT /api/course/update?id=`、`DELETE /api/course/batch` |
| 机构 | `/organization/list`、`/organization/add`、`/organization/detail` | `/api/organization/**` |
| 教师 AI | `/ai/knowledge`、`/ai/lesson-plan` | `/api/teacher-ai/**`，使用 `success/data` 和 SSE |

## 数据规则

| 项目 | 行为 |
| --- | --- |
| 教师标识 | 使用记录 ID 获取详情 |
| 学生标识 | 学号为唯一业务编号 |
| 课程标识 | 课程编号为唯一业务编号；模糊查询结果需要精确匹配 |
| 分页与删除 | 页面处理查询、错误、重复提交与末页边界 |
| 本机或演示功能 | 资讯、评价、管理员列表、日历、邮件和直播的数据范围见[管理页面说明](../../apps/web/ADMIN-PAGES.md) |

字段、错误码与完整接口见[Java 接口手册](java_apidoc.md)，部署入口见[整体部署](../deploy.md)。
