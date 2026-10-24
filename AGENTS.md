# Agent 项目约定

## 工作范围与结构

| 位置 | 职责 |
| --- | --- |
| `apps/web/` | 唯一 Vue 前端，包含学习、管理和教师 AI 页面 |
| `apps/api/java/` | Java 主业务、教师 AI 与前端静态产物托管 |
| `apps/api/python/` | Flask 课堂互动、PBL、视觉识别和学情分析 |
| `database/init/` / `database/manual/` | 容器初始化 SQL / 人工执行 SQL |
| `compose.yaml` / `Dockerfile` | 唯一整体部署编排 / 多阶段构建 |
| `.agents/skills/` | 项目内技能参考及上游许可 |

- 改动范围以用户当前授权为准；前端任务不附带修改后端业务或数据库。
- 修改现有文件前检查工作区，保留与任务无关的用户改动。
- 技能是工作参考，不扩大授权范围；实际依赖版本和用户要求优先于技能示例。
- 技能只在项目内管理，保留来源元信息和许可证。版本相关 API 以当前锁文件为准。

## 实现约定

| 范围 | 约定 |
| --- | --- |
| Vue | Vue 3、严格 TypeScript、Composition API、`<script setup lang="ts">`；沿用 Element Plus 与蓝色主题 |
| 状态与接口 | Pinia 保存展示状态；认证与权限由后端 Cookie 会话决定；学习端身份来自服务端绑定信息 |
| 数据展示 | 用户内容使用文本插值；本机数据、演示数据和待接入接口明确标注，不伪造服务端成功 |
| 请求生命周期 | 防止重复提交和迟到响应覆盖；离开页面后不再写入失效状态；失败保留用户输入 |
| Java / Python | JDK 25、Spring Boot 4 / Python 3.11、Flask；保持现有模块与接口约定 |
| 部署 | 只维护根目录整体 Compose；Vue 在 Docker 构建时编译到 Java static；不引入 Nginx、Caddy 或独立前端运行容器 |
| 文件 | 文本统一 LF；不提交密钥、依赖、虚拟环境、模型、日志、备份或编译产物 |

Vue 源码位于 `apps/web/`；Java static 和 Python templates 中的 HTML 也是页面源码。处理这些页面时核对接口、资源和跳转，功能完整后再删除对应源码，不使用 iframe 或 HTML 注入代替 Vue 实现。

## 开发与验证

| 工作目录 | 命令 | 用途 |
| --- | --- | --- |
| `apps/web/` | `npm ci`、`npm run dev` | Node.js 22.13+（22 系列）或 24+，开发代理连接 Java `localhost:80` |
| `apps/web/` | `npm run check` | Lint、Vitest、类型检查与生产构建 |
| `apps/api/java/` | `mvn spring-boot:run` / `mvn test` | 本地开发 / Java 回归 |
| `apps/api/java/` | `mvn -Dtest=TeacherAICtrlTest test` | 无真实模型调用的教师 AI 接口测试 |
| `apps/api/python/` | `python app.py` / `pytest` | 本地开发 / Python 测试 |
| 仓库根目录 | `docker compose config --quiet` | 使用已配置的 `.env` 验证整体编排 |

- 根据实际改动运行必要检查；纯文档改动检查链接、路径和格式，无须重复运行应用测试。
- 模型测试使用模拟服务，不默认调用付费模型。验证结论区分配置解析、镜像构建、容器运行和真实业务联调。
- Git 忽略 `dist/`、`node_modules/`、Java `static/js/app/` 和生成的 `static/index.html`。
- 统计工作流由推送 main 触发；README 的统计区由脚本管理，保留 `STATS_SECTION_START/END` 标记。

## 文档规范

| 类别 | 位置与内容 |
| --- | --- |
| 正式文档 | 根 `README.md` 为唯一导航；`docs/` 为部署、接口和架构；应用 README 为当前开发与功能说明 |
| Agent 参考 | 本文件统一保存协作、实现、技能使用、验证和文档约定；技能正文位于 `.agents/skills/` |
| 项目历史 | 通过 Git commit 信息记录问题、最终改动、必要原因和验证结果，不在正式文档保存操作流水 |

- 正式文档只描述项目当前有效状态，不记录迁移过程、版本对照、改动清单、安装技能过程或仓库瘦身过程。
- 使用陈述句表达职责、接口、配置、限制和数据来源，避免“本次新增”“原先”“现已迁移”等过程叙述。
- 优先用表格表达配置与映射、代码块表达命令与示例、图表表达关系和流程；文字用于必要解释，避免大段铺陈或重复正文。
- 同一事实维护一处，其他位置链接引用；文件较少时直接放入所属目录，不建立仅容纳一个文件的分类子目录。
- 文档与实现同步；修改路径、合并或删除文档时更新所有引用，检查本地链接和锚点。
- 保留必要接口参数、错误行为、部署变量、持久化和功能边界；不为压缩篇幅删除实际使用信息。
- Commit 信息围绕最终改动组织，复杂变更注明原因和验证；不将调试流水复制到文档或提交描述中。
