# 管理前端技术栈升级

## 范围

本阶段升级 `apps/web/admin/`。Vue 单文件组件、入口、路由、接口模块、请求工具和 Vite 配置迁入 TypeScript；保留 Vue 3、Vue Router、Element Plus 和现有页面风格。未修改 Java、Python 后端或数据库，也未将其内嵌静态页面迁出。

`apps/web/teacher-ai-assistant/` 保持原状。后续可逐模块迁移教师 AI 工作台、学生学习端和课堂互动页面，无需在本阶段重新实现全部功能。

## 数据与会话

- `src/types/api.ts` 描述管理员、教师、学生、课程、机构及分页数据；`src/types/application.ts` 描述本地日程和直播计划。
- `src/api/*.ts` 保留原有 URL、HTTP 方法与参数位置。登录使用标准 `URLSearchParams` 编码，移除仅用于登录序列化的 `qs` 依赖。
- `src/utils/http.ts` 校验业务响应封装并返回数据，统一处理业务失败、HTTP 401 和现有登录过期码。`request.ts` 负责接入错误提示及登录跳转。
- Pinia 只保存登录展示状态。Cookie 认证、权限判断及会话有效期仍由后端决定，客户端状态不能作为授权依据。
- 兼容原来的 `isLogin`、`user` 缓存键。读取旧账号时校验字段，只保留 `id`、`username`；账号密码等额外字段不会继续持久化。
- 详情缓存增加读取校验，损坏缓存返回空状态。现有本地日历、邮件草稿和直播计划继续采用原来的数据存储方式。

## 开发约定

业务页面使用 `<script setup lang="ts">`。新增接口应声明返回类型；表单使用 Element Plus 的 `FormInstance`、`FormRules`，列表、详情和编辑状态使用对应实体类型。使用 `useSessionStore()` 获取当前账号，不再各自解析登录缓存。

TypeScript 使用严格模式，不启用 JavaScript 兼容兜底，也不使用 `@ts-ignore` 跳过页面检查。Vite 本身只转译，因此 `build` 明确先执行 `vue-tsc --noEmit`。参见 [Vue TypeScript 文档](https://vuejs.org/guide/typescript/overview)。路由守卫和请求回调通过共享 Pinia 实例访问状态，避免在插件安装前依赖隐式实例。参见 [Pinia 组件外用法](https://pinia.vuejs.org/core-concepts/outside-component-usage.html)。

## 列表与表单重构

- `style.css` 只定义全站字体和蓝色主题；`assets/css/base.css` 负责基础布局。移除 Vite 示例样式、固定根容器宽度及自动深色模式，页面使用可用宽度，正文列表保留原生项目符号。
- 教师、学生、课程列表共用 `usePagedList`：查询、重置、分页、批量删除及错误状态集中处理。新查询取消前一次请求，并以请求序号防止迟到响应覆盖当前结果；离开页面后不再写入结果。取消请求不会弹出网络错误。
- 重新查询清空旧选择，删除操作防止重复确认与提交；删除末页记录后按返回总数回到有效页。加载失败清空旧数据并提供重新加载入口。
- 新增和编辑页面共用 `useFormSubmit`。提交锁覆盖异步校验阶段，提交期间禁用表单；失败保留输入并显示错误，允许修改后重试。覆盖教师、学生、课程新增/编辑及机构、管理员新增，接口地址、请求参数和后端逻辑保持原样。
- 回归测试覆盖请求竞态、取消、离开页面、分页重置、末页删除、重复提交、校验失败和错误后重试。
- 教师 AI 的生成任务由 `useTeachingGeneration` 管理，页面保留输入、连接状态、渲染及导出。任务在校验前建立取消标记；取消、切换工具或离开页面后，旧校验不再发起请求，旧响应及流式片段不再修改结果或新任务的加载状态。主动取消保留已收到的文本，便于复制与导出；这只停止浏览器接收，不承诺终止服务端模型任务。
- 列表在删除确认后再次检查页面是否已离开，避免确认框延迟完成时发送用户已离开页面的删除操作。

## 自动检查

在 `apps/web/admin/` 执行 `npm run check`。Vitest 覆盖旧缓存脱敏、损坏缓存、禁止浏览器存储、会话清理、业务响应解包、业务过期码、HTTP 401、HTML 错误响应和课程编号冲突提示。测试不连接真实后端。

依赖锁文件已更新；本次兼容升级后的 `npm audit` 报告为 0 个已知漏洞。CI 对管理端的推送和 PR 执行安装、Lint、测试、类型检查及生产构建。生产构建仍可能报告 Element Plus 全量注册带来的大体积代码块提示；本次保留现有组件注册方式。

默认生产产物为 `dist/`；`npm run build:static` 可直接输出到 Java 的 `static/js/admin/`。Docker 构建自动将前端打入 Java JAR，使用 Hash 路由兼容静态托管；不增加 Nginx、Caddy 或前端运行服务。实际登录、增删改查及部署访问还需在已有后端环境联调。
