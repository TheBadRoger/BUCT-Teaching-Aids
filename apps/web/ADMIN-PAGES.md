# 管理页面

沿用 Vue 3、Vue Router 和 Element Plus，以及现有蓝色侧栏、查询表单和表格样式。页面与数据来源如下：

| 模块 | 路径 | 数据来源 |
| --- | --- | --- |
| 资讯列表、新增、详情 | `/news/list`、`/news/add`、`/news/detail?id=草稿编号` | 本机资讯草稿，支持搜索、分类筛选、删除和刷新后查看；不发布到服务器 |
| 评价列表、详情 | `/review/list`、`/review/detail?id=演示编号` | 课程评价接口待接入；明确标注的演示数据，支持搜索、评分筛选和详情 |
| 机构列表、新增、介绍 | `/organization/list`、`/organization/add`、`/organization/detail?id=机构编号` | `/api/organization` 真实接口 |
| 管理员列表、新增 | `/admin/list`、`/admin/add` | 新增使用 `/api/admin/register`；列表仅展示当前账号和当前页面会话内新增账号 |
| 小部件 | `/application/widgets` | 本地便签、日程数量、快捷入口 |
| 电子邮件 | `/application/email` | 本地草稿；使用系统邮件客户端确认发送 |
| 日历 | `/application/calendar` | 本地日程增删与月视图 |
| 新增直播、直播间 | `/application/live/add`、`/application/live/room` | 本地直播计划及详情，无推流或播放服务 |

本地功能明确展示未接入服务的提示。浏览器存储按当前浏览器共享，不作为服务端数据或权限依据；不要填写敏感内容。管理员密码仅用于注册请求，不写入浏览器存储。机构介绍使用文本插值，避免将用户内容当 HTML 执行。

资讯与评价页面均沿用管理端登录守卫。详情入口未提供编号或编号无效时显示明确的空状态，并提供返回列表入口。资讯正文使用纯文本插值，不执行 HTML；本机存储失败时保留表单且不显示保存成功，损坏缓存不会被新草稿覆盖。资讯和课程评价接口待接入；学习笔记评论与课程评价是不同功能。
