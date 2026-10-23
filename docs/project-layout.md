# 项目目录说明

应用统一放在 `apps/`，其下以 `api/` 和 `web/` 区分服务端应用和 Web 应用。部署、数据脚本、文档及仓库工具分别放在根目录对应文件夹中。

| 原路径 | 新路径 |
| --- | --- |
| `API/JavaAPI/` | `apps/api/java/` |
| `API/PythonAPI/` | `apps/api/python/` |
| `front-end/` | `apps/web/admin/` |
| `teacher-ai-assistant/` | `apps/web/teacher-ai-assistant/` |
| `documents/*apidoc.md`、页面接口映射文档 | `docs/api/` |
| `documents/*api_intro.md` | `docs/architecture/` |
| `documents/*deploy*.md` | `docs/deployment/` |
| `ddl_Note.sql` | `database/manual/ddl_Note.sql` |

`docker/`、`scripts/`、`.github/` 和 `.agents/` 保持原位。IDE 项目设置保留，相关路径随目录迁移调整。

## 迁移边界

- Java 的 `src/`、Maven 工程文件和 Python 的应用、模块、测试、依赖文件只迁移，不修改内容。
- Java 内嵌静态页面、背诵手册，以及 Python 模板和静态资源保持应用内原有相对位置。
- 教师 AI 助手的 `public/` 和 `src/` 一起迁移，保留其 Express 服务与 Web 页面的相对路径。
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
| Vue 管理端 | `apps/web/admin` | `npm ci`、`npm run dev` |
| 教师 AI 助手 | `apps/web/teacher-ai-assistant` | `npm ci`、`npm run dev` |

依赖、环境变量、数据库和端口要求继续以各应用及部署文档为准。迁移未引入跨应用依赖或新的启动器。

## 部署影响

Docker 命令仍从仓库根目录执行，Compose 的构建上下文仍为仓库根目录。三套编排的名称和数据卷名称保持不变。人脸库默认宿主路径改为 `apps/api/python/face_database`；如果已有 `docker/.env`，需要把 `FACE_DATABASE_DIR=../API/PythonAPI/face_database` 更新为 `FACE_DATABASE_DIR=../apps/api/python/face_database`。本地密钥文件不会提交。

独立部署的 systemd `WorkingDirectory`、`ExecStart`、IDE 项目根目录及服务器脚本也需使用新路径。已经创建的虚拟环境如果包含旧的绝对路径，应在新目录重新创建；不要修改业务源码来适配本地环境。
