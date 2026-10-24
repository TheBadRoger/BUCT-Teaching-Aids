# 教学平台统一前端

Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios；学习、管理和教师 AI 页面共用一个工程。

## 开发与检查

使用 Node.js 22.13+（22 系列）或 24+，在 `apps/web/` 执行：

```bash
npm ci
npm run dev
```

`/api` 默认代理到 Java 的 `http://localhost:80`，真实业务操作需要后端与数据库。教师 AI 使用同源 `/api/teacher-ai`，DeepSeek 密钥只配置在 Java 服务端。

| 命令 | 用途 |
| --- | --- |
| `npm run check` | Lint、Vitest、类型检查和生产构建，CI 使用此命令 |
| `npm run lint` / `npm run test` / `npm run typecheck` | 单独执行对应检查 |
| `npm run build` | 类型检查后生成 `dist/` |
| `npm run build:static` | 本地联调：输出到 Java 的 `static/js/app/` |

## 结构与约定

- 页面位于 `src/views/`，使用 `<script setup lang="ts">` 和严格 TypeScript；接口位于 `src/api/`，类型位于 `src/types/`。
- Pinia 的 `session` 只保存账号展示字段，认证和权限由后端 Cookie 会话决定。学习端学生身份由服务端绑定接口获取。
- `http.ts` 处理 Java 业务封装，`request.ts` 接入提示与登录跳转；教师 AI 保留自己的 `success/data` 协议。
- `usePagedList` 处理分页、删除及请求竞态，`useFormSubmit` 防止重复提交并在失败时保留输入；`useTeachingGeneration` 管理生成和取消。离开页面后旧响应不得覆盖当前状态。
- 主题位于 `style.css`，基础布局位于 `assets/css/base.css`。登录背景与蓝色 Logo 通过 `@shared-images` 复用 Java 原图。
- 用户内容通过文本插值展示，不将其作为 HTML 执行。本机草稿、演示数据和未接入服务必须明确标注。

## 构建与入口

路由使用 Hash 模式。部署后学习入口为 `/js/app/index.html#/learn`，管理登录为 `/js/app/index.html#/login`。

Docker 从源码编译 Vue，产物写入 Java JAR 的 `static/js/app/`，同时生成 `static/index.html`。不提交编译产物；无需独立前端运行容器。`build:static` 仅用于本地联调。

页面与数据来源见[管理页面](ADMIN-PAGES.md)，教师 AI 见[Java 接口手册](../../docs/api/java_apidoc.md#教师-ai-apiteacher-ai)。完整导航见[根目录 README](../../README.md#文档导航)，Agent 约定见[AGENTS.md](../../AGENTS.md)。
