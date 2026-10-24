# 教学平台管理前端

技术栈：Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios。现有页面使用 `<script setup lang="ts">`，接口类型放在 `src/types/`，共享登录状态位于 `src/stores/session.ts`。

在此目录执行：

```bash
npm ci
npm run dev
```

需要 Node.js 22.13+ 的 22 LTS 系列，或 Node.js 24+。开发服务器把 `/api` 代理到 `http://localhost:80`，继续使用后端 Cookie 会话。启动真实后端后才能完成业务操作。

| 命令 | 用途 |
| --- | --- |
| `npm run typecheck` | 检查 TypeScript 和 Vue 模板 |
| `npm run lint` | ESLint，禁止警告通过 |
| `npm run test` | 运行 Vitest 回归测试 |
| `npm run build` | 先做类型检查，再生成 `dist/` |
| `npm run build:static` | 编译到 Java 的 `static/js/admin/`，由后端直接托管 |
| `npm run check` | 依次执行 Lint、测试和生产构建 |

CI 使用 Node.js 22 执行 `npm ci` 和 `npm run check`。IDE 推荐 Vue - Official 扩展。

路由使用 Hash 模式，刷新页面无需服务器回退规则。运行 `build:static` 后访问 Java 服务的 `/js/admin/index.html#/login`；构建命令只清理专用的 `static/js/admin/` 产物目录，保留原有静态页面。Docker 构建会自动完成编译与 JAR 打包，不需要提交编译产物。生产教师 AI 地址通过构建变量 `VITE_TEACHER_AI_BASE_URL` 指定；Docker 使用 `docker/.env` 中的 `TEACHER_AI_PUBLIC_URL`，必须是浏览器可访问的地址。

页面功能范围见 [管理页面说明](ADMIN-PAGES.md)，本次迁移约定见 [前端升级说明](../../../docs/frontend/admin-upgrade.md)。

教师 AI 工具入口为 `/ai/knowledge`、`/ai/lesson-plan`。开发代理默认连接已有的 `localhost:3000` Express 服务，密钥只配置在该服务端。生产连接与迁移范围见 [教师 AI 前端迁移](../../../docs/frontend/teacher-ai-migration.md)。
