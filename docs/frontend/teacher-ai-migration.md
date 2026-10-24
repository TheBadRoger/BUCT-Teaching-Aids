# 教师 AI 助手前端迁移

## 新入口

统一前端 `apps/web/admin/` 的侧栏新增「AI 教学工具」：

| 页面 | 前端路由 | 原服务接口 |
| --- | --- | --- |
| 知识点手册 | `/ai/knowledge` | `POST /api/knowledge/generate` |
| 智能教案 | `/ai/lesson-plan` | `POST /api/lesson-plan/generate`、`POST /api/lesson-plan/generate-stream` |
| 学科与年级选项 | 两个页面共享 | `GET /api/knowledge/subjects` |
| AI 服务状态 | 两个页面共享 | `GET /api/health` |

页面使用 Vue、TypeScript 和 Element Plus，沿用统一前端的登录守卫与布局。表单支持学科、年级、主题、重点关注、课型、课时、教学风格及额外要求。知识点和教案完整展示原有结构化内容，并提供复制和纯文本导出。

旧助手 `apps/web/teacher-ai-assistant/` 的 Express 服务、API、提示词和静态页面全部保持不变，原入口继续兼容。新页面验证及部署切换完成后，再单独评估移除旧静态界面；这次不删除仍被后端静态资源服务引用的文件。

## 开发连接

先按教师助手原说明启动现有 Express 服务，再启动统一前端：

```bash
# 第一个终端；密钥使用助手原有 .env，只在服务端配置
cd apps/web/teacher-ai-assistant
npm ci
npm run dev

# 第二个终端，从仓库根目录进入
cd apps/web/admin
npm ci
npm run dev
```

登录后访问 `/ai/knowledge` 或 `/ai/lesson-plan`。管理登录仍需原 Java 服务；教师 AI 接口由独立 Express 服务提供。

开发代理将 `/teacher-ai-api/*` 转发至 `http://localhost:3000/api/*`，与 Java 的 `/api` 以及 Python 的同名知识点接口隔离。若 Express 使用其他端口，调整 Vite 的代理目标。

生产构建不会运行 Vite 开发代理。部署时应为统一前端配置可访问的教师服务公开地址，再重新构建。例如在 `apps/web/admin/.env` 设置：

```dotenv
VITE_TEACHER_AI_BASE_URL=https://ai.example.edu/api
```

或者由现有部署入口转发 `/teacher-ai-api/`。本次不新增 Nginx 或修改部署配置。`VITE_` 变量会进入浏览器产物，只能放公开地址，不能放 DeepSeek 密钥。

## 流式行为

智能教案默认使用原有 SSE 接口，也可关闭「逐步显示生成内容」改用原来的非流式接口。取消或离开页面会终止浏览器读取；由于后端没有取消任务接口，不能保证上游模型计算同时终止。

流式工具按 SSE 帧而非网络分块处理，支持跨块 UTF-8、JSON、CRLF、多行 data 及心跳。只有收到 `done` 事件才将连接视为完成；连接提前关闭、错误事件或无效数据会显示错误并允许重试。收到完整输出后解析模型 JSON，也支持外围 JSON 代码围栏。解析失败时保留已收到的原文，仍可复制或导出。

AI 生成内容通过 Vue 文本插值展示，不使用 `v-html`。结果字段先规范化；字符串/数组形式的作业、对象形式的重点和难点均兼容原接口输出。

## 验证与后续

在统一前端运行 `npm run check`，覆盖既有管理功能及本次新增的 SSE、内容规范化、导出完整性和接口协议测试。测试不会调用真实模型或消耗额度。真实模型效果、API 密钥和生产跨域连接还需部署环境联调。

此阶段完成教师 AI 两个工具的前端迁移。Java 内嵌学生页面、AI 背诵手册和 Python 课堂页面保持原状，后续逐模块迁移。
