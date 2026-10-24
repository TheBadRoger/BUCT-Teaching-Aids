# Docker 整体部署

只保留项目根目录一套整体部署配置：`compose.yaml`、多目标 `Dockerfile`、`.env.example` 和 `.dockerignore`。旧 `docker/` 目录、Java/Python 独立部署入口以及 Java 应用内的 Compose 均已删除。

## 配置并启动

需要 Docker Engine 和 Docker Compose v2，所有命令在仓库根目录执行：

```bash
cp .env.example .env
chmod 600 .env
# 编辑 .env，填写全部必填变量
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
```

必须填写 `MYSQL_ROOT_PASSWORD`、`BUCTTA_JAVA_DB_PASSWORD`、`BUCTTA_PYTHON_DB_PASSWORD`、`REDIS_PASSWORD`、`APP_ADMIN_PASSWORD`、`PYTHON_SECRET_KEY`，为空时 Compose 拒绝启动。可用 `openssl rand -hex 32` 生成随机值。现有数据库初始化脚本直接拼接 SQL，数据库密码使用随机字母数字；含 `$`、`#` 的其他配置用单引号包裹。本次不修改数据库业务结构。

`.env` 由 Compose 自动读取，已被 Git 和 Docker 构建上下文忽略。项目名固定为 `buct-teaching-aids`，不要用同一份生产数据随意切换 `-p` 或项目名。

## 一套编排，五个服务

| 服务 | 内容 | 默认宿主端口 |
| --- | --- | --- |
| java-app | Java API、现场编译的统一 Vue 前端 | 80 |
| python-app | Python API、视觉和课堂能力 | 8080 |
| teacher-ai | 教师 AI API，不托管网页 | 3000 |
| mysql | 共享数据库 | 不发布 |
| redis | 缓存 | 不发布 |

默认仅绑定 `127.0.0.1`。需要其他机器访问时设置 `APP_BIND_IP=0.0.0.0`，按需修改 `JAVA_HOST_PORT`、`PYTHON_HOST_PORT`、`TEACHER_AI_HOST_PORT`。各应用仍运行在各自容器中，由一个命令统一构建、启动和停止，不再维护分开部署文件。

学习入口：`http://127.0.0.1/js/app/index.html#/learn`；管理员入口：`http://127.0.0.1/js/app/index.html#/login`。Docker 同时生成 `/index.html`。Hash 路由无需服务器页面回退配置，API 继续沿用 Cookie 认证。未迁移的旧页面见[迁移清单](../frontend/legacy-vue-migration.md)。

## 构建与环境变量

根目录 `Dockerfile` 定义 `java`、`python`、`teacher-ai` 三个镜像目标，由 Compose 自动选择。Java 目标先用 Node 编译 Vue，再复制到 Maven 阶段的 `static/js/app/` 和 `static/index.html`，最后打入 JAR。源码中的 `dist/`、本地 static 编译目录、依赖、密钥、模型及上传文件不进入构建上下文，编译产物不提交。无需 Node 前端运行服务、Nginx 或 Caddy。

`TEACHER_AI_PUBLIC_URL` 必须是浏览器可访问的地址，默认 `http://localhost:3000/api` 仅适用于浏览器与 Docker 宿主机在同一台机器。远程部署改成实际 IP/域名，端口修改后也需同步调整，并重建 Java 镜像。HTTPS 页面需配套浏览器可访问的 HTTPS AI 地址；TLS 由外部环境提供。

`DEEPSEEK_API_KEY` 仅注入教师 AI 服务端，不进入 Vue 产物；可配置 `DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL`、`DEEPSEEK_REQUEST_TIMEOUT`。`POLYMAS_AUTH_KEY` 供 Java AI 能力使用。未配置模型密钥仍能启动并访问页面，但不能生成内容，健康检查也不验证密钥有效性。

镜像和包源可通过 `NODE_IMAGE`、`MAVEN_IMAGE`、`JAVA_IMAGE`、`PYTHON_IMAGE`、`MYSQL_IMAGE`、`REDIS_IMAGE`、`PIP_INDEX_URL` 配置。Java 镜像需支持 apt-get/useradd，Python 使用 Debian bookworm 基础镜像。Python 初次构建涉及 dlib 和视觉依赖，耗时与内存开销较大。

## 辅助文件与数据

数据库初始化脚本位于 `database/init/`，直接挂载至 MySQL 初始化目录；只在空数据库卷首次启动时执行。Python 容器启动器及额外运行依赖位于 `apps/api/python/deployment/`，不改变应用业务逻辑。

| 持久化位置 | 内容 |
| --- | --- |
| mysql_data | MySQL 数据 |
| redis_data | Redis AOF |
| java_data → /app/data | Java 上传媒体 |
| python_data → /app/data | Python 上传、模型、SQLite、Ultralytics 配置 |
| FACE_DATABASE_DIR → /app/face_database | 人脸库，默认根目录 `./apps/api/python/face_database` |

Java/Python 使用 UID/GID `10001:10001`，宿主人脸库应允许该用户读写；已有数据卷可能需要部署者先备份并修复属主。`FACE_DATABASE_DIR` 可用绝对路径。更改 `.env` 不会自动更改现有数据库账号密码；Flyway 继续保持现有关闭配置。

Python 使用单 worker、多线程 Gunicorn，保持进程内课堂状态一致，不能直接增加 worker。线程数和超时由 `GUNICORN_THREADS`、`GUNICORN_TIMEOUT` 控制。模型路径为 `/app/data/models/yolov8n-pose.pt`，离线环境应预先放入模型。摄像头不默认透传，按实际设备单独配置权限，不启用 privileged。

## 检查、更新与停止

```bash
curl -f http://127.0.0.1/js/app/index.html
curl -f http://127.0.0.1:8080/health
curl -f http://127.0.0.1:3000/api/health
docker compose logs --tail=100

# 更新整套服务
git pull --ff-only
docker compose up -d --build --wait --wait-timeout 300

# 停止容器，保留数据卷
docker compose down
```

健康检查用于进程及依赖就绪判断，不代表所有业务表、模型和摄像头可用；变为 unhealthy 不会自动重启，进程退出时使用 `unless-stopped` 重启。日志轮转为每服务 3 个、每个 10 MB。

定期备份 `.env`、数据库、媒体卷和人脸库。`down` 保留命名卷，正式数据不要使用 `down -v`。CI 解析唯一 Compose、检查缺失密码、测试 Python 启动器与 AI 接口并构建镜像；不会连接生产服务或调用付费模型。
