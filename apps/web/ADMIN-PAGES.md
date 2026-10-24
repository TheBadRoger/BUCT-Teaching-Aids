# 管理页面

沿用 Vue 3、Vue Router 和 Element Plus，以及现有蓝色侧栏、查询表单和表格样式。侧栏新增以下入口：

| 模块 | 路径 | 数据来源 |
| --- | --- | --- |
| 机构列表、新增、介绍 | `/organization/list`、`/organization/add`、`/organization/detail?id=机构编号` | `/api/organization` 真实接口 |
| 管理员列表、新增 | `/admin/list`、`/admin/add` | 新增使用 `/api/admin/register`；列表仅展示当前账号和当前页面会话内新增账号 |
| 小部件 | `/application/widgets` | 本地便签、日程数量、快捷入口 |
| 电子邮件 | `/application/email` | 本地草稿；使用系统邮件客户端确认发送 |
| 日历 | `/application/calendar` | 本地日程增删与月视图 |
| 新增直播、直播间 | `/application/live/add`、`/application/live/room` | 本地直播计划及详情，无推流或播放服务 |

本地功能明确展示未接入服务的提示。浏览器存储按当前浏览器共享，不作为服务端数据或权限依据；不要填写敏感内容。管理员密码仅用于注册请求，不写入浏览器存储。机构介绍使用文本插值，避免将用户内容当 HTML 执行。

统计工作流仅在推送至 main 时触发（合并 PR 会产生该推送事件）。统计时间采用最近一次非统计源码提交的时间，并排除生成的统计 Markdown 和 README，避免统计自增长。新增 `.vue` 行数统计。相同源码连续运行应生成完全相同的结果。

仓库 `.gitattributes` 统一文本文件为 LF，兼容 Linux 部署。验证命令：在 `apps/web` 运行 `npm run build`；在仓库根目录连续运行两次 `python scripts/update_project_stats.py` 并比较三份输出的内容。
