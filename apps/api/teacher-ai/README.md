# 教师 AI 接口服务

这是 Node.js / Express 后端，不再包含独立前端。所有教学页面都位于唯一的 Vue 工程 `apps/web/`。本目录只保留现有知识点、教案、流式生成接口及 DeepSeek 调用逻辑。

## 启动

在本目录复制 `.env.example` 为 `.env`，设置 `DEEPSEEK_API_KEY`，然后执行：

```bash
npm ci
npm run dev
```

默认端口 3000。生产启动使用 `npm start`；Docker 配置见[部署说明](../../../docs/deployment/docker_deploy.md)。网页由 Java 服务托管 Vue 编译产物，本服务的 `/`、`/index.html` 和其他未知路径返回 JSON 404。

## 接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/health` | 公开状态，不返回密钥 |
| GET | `/api/knowledge/subjects` | 学科、年级和课型目录 |
| POST | `/api/knowledge/generate` | 知识点手册 |
| POST | `/api/lesson-plan/generate` | 智能教案 |
| POST | `/api/lesson-plan/generate-stream` | SSE 流式教案 |

生成请求沿用 `subject`、`grade`、`topic` 等原参数和 `success/data/error` 响应；普通生成与流式协议均保持原样。密钥只由服务端读取。`DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL`、`REQUEST_TIMEOUT` 的含义不变。

前端使用方法见[AI 页面接入说明](../../../docs/frontend/teacher-ai-migration.md)。运行 `npm test` 会启动临时本地服务，检查健康、目录、输入校验和旧网页入口移除，不发送有效生成请求或调用付费模型。
