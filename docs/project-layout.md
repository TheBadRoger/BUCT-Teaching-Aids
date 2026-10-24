# 项目目录说明

应用统一放在 `apps/`，其下以 `api/` 和 `web/` 区分服务端应用和 Web 应用。部署、数据脚本、文档及仓库工具分别放在根目录对应文件夹中。

| 原路径 | 新路径 |
| --- | --- |
| `API/JavaAPI/` | `apps/api/java/` |
| `API/PythonAPI/` | `apps/api/python/` |
| `front-end/` | `apps/web/` |
| `teacher-ai-assistant/` | `apps/api/teacher-ai/` |
| `documents/*apidoc.md`、页面接口映射文档 | `docs/api/` |
| `documents/*api_intro.md` | `docs/architecture/` |
| `documents/*deploy*.md` | `docs/deployment/` |
| `ddl_Note.sql` | `database/manual/ddl_Note.sql` |

Docker 仅保留根目录 `compose.yaml` 和多目标 `Dockerfile`；初始化脚本放在 `database/init/`，Python 容器启动辅助文件放在 `apps/api/python/deployment/`。`scripts/`、`.github/` 和 `.agents/` 保持原位。IDE 项目设置保留，相关路径随目录迁移调整。

## 迁移边界

- Java 的 `src/`、Maven 工程文件和 Python 的应用、模块、测试、依赖文件只迁移，不修改内容。
- Java/Python 的旧页面按前端迁移清单逐模块迁入统一 Vue。已迁移的页面直接删除，不保留兼容界面；Java static 中的 Vue 产物由 Docker 构建生成，不提交。
- 教师 AI 的生成路由、提示词与模型调用放在 `apps/api/teacher-ai/src/`；旧 `public/` 已删除，不再独立托管页面。
- 不改包名、导入、HTTP 路由、接口、数据库结构或技术栈。
- 只调整 Docker、CI、统计工具、IDE 的路径配置和文档说明。
- Git 暂存沿用仓库现有 LF 规则；原有 CRLF 文件可能显示换行差异，代码和 SQL 内容不变。
- `database/manual/` 中的 SQL 是原有独立文件，不自动挂载到数据库初始化目录，也不自动执行。

## 开发入口

从仓库根目录进入相应应用，再运行原有命令：

| 应用 | 目录 | 命令 |
| --- | --- | --- |
| Java | `apps/api/java` | `mvn spring-boot:run` |
| Python | `apps/api/python` | `python app.py` |
| 唯一 Vue 前端 | `apps/web` | `npm ci`、`npm run dev` |
| 教师 AI 接口 | `apps/api/teacher-ai` | `npm ci`、`npm run dev` |

依赖、环境变量、数据库和端口要求继续以各应用及部署文档为准。迁移未引入跨应用依赖或新的启动器。

## 部署影响

Docker 命令从仓库根目录执行，唯一入口为 `docker compose up -d --build --wait --wait-timeout 300`，同时启动 Java（含 Vue）、Python、教师 AI、MySQL、Redis。原独立部署 Compose 和 Java 应用内的 Compose 已删除，Java 不再自动管理容器。根目录 `.env` 自动读取，`FACE_DATABASE_DIR` 默认 `./apps/api/python/face_database`，Compose 项目名固定为 `buct-teaching-aids`。

独立部署的 systemd `WorkingDirectory`、`ExecStart`、IDE 项目根目录及服务器脚本也需使用新路径。已经创建的虚拟环境如果包含旧的绝对路径，应在新目录重新创建；不要修改业务源码来适配本地环境。

统一 Vue 工程的源码、依赖清单、路由与构建入口均位于 `apps/web/`。该目录不再包含 admin 或 teacher-ai-assistant 子工程；学习、管理和 AI 页面按 `src/views/` 模块组织，只有一套前端依赖与构建产物。
