# Linux Docker 部署

本文覆盖容器构建与编排，包含 Vue 管理前端、教师 AI、Java、Python 和依赖服务。前端产物打入 Java JAR 的 static 目录，由现有服务提供访问，不使用 Caddy 或 Nginx；沿用现有后端与数据库初始化脚本，不修改其业务逻辑，也不配置 TLS。

## 准备配置

需要 Linux Docker Engine 和 Docker Compose v2。下列命令均在仓库根目录执行。

```bash
cp docker/.env.example docker/.env
chmod 600 docker/.env
```

编辑 `docker/.env`，填写所有留空的必填项：

| 变量 | 用途 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL 初始化及健康检查 |
| `BUCTTA_JAVA_DB_PASSWORD` | Java 数据库账号 |
| `BUCTTA_PYTHON_DB_PASSWORD` | Python 数据库账号 |
| `REDIS_PASSWORD` | Redis 认证 |
| `APP_ADMIN_PASSWORD` | 后端管理员初始化配置 |
| `PYTHON_SECRET_KEY` | Flask 会话密钥 |

Compose 会拒绝空的必填值，不提供默认密码。可用 `openssl rand -hex 32` 生成随机值。现有 MySQL 初始化脚本直接拼接 SQL，数据库密码使用随机字母数字，避免单引号、反斜杠等字符；本次未修改该脚本。其他包含 `$`、`#` 的配置在 `.env` 中用单引号包裹。

`POLYMAS_AUTH_KEY` 是可选 AI 平台配置，未填写时容器会传递空值，不使用源码中的默认密钥。

默认应用只监听宿主机 `127.0.0.1`：Java（含新版前端）为 `80`，Python 为 `8080`，教师 AI 为 `3000`。前端入口为 `http://127.0.0.1/js/admin/index.html#/login`，生产环境不运行 Vite 开发服务器。需要局域网直接访问时，配置 `APP_BIND_IP=0.0.0.0`，并按需设置 `JAVA_HOST_PORT`、`PYTHON_HOST_PORT`、`TEACHER_AI_HOST_PORT`。MySQL、Redis 仅在 Compose 网络内访问，不发布宿主机端口。Java 容器内部端口已从 80 调整为 8080，以支持非 root 用户运行。

## 检查并启动

```bash
docker compose --env-file docker/.env -f docker/docker-compose.yml config --quiet
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build --wait --wait-timeout 300
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
```

两套独立部署入口复用完整编排的服务定义，选择其中一套运行，无需叠加多个 `-f`：

```bash
# Vue + 教师 AI + Java + MySQL + Redis
docker compose --env-file docker/.env -f docker/docker-compose.java-root.yml up -d --build --wait --wait-timeout 300

# Python + MySQL
docker compose --env-file docker/.env -f docker/docker-compose.python-root.yml up -d --build --wait --wait-timeout 300
```

三套入口使用同一份 `.env`；即使只启动某一后端，也请填写模板中的全部必填配置。默认沿用目录名 `docker` 作为 Compose 项目名，保持已有命名卷的名称。已有部署不要随意更改 `-p` 或 `COMPOSE_PROJECT_NAME`，否则会使用另一组数据卷。切换部署入口时，先使用原入口执行 `down`，保留数据卷后再启动新入口。

## 前端与教师 AI

`docker/java/Dockerfile` 先使用 Node 22 编译 Vue，再把产物复制到 Maven 构建阶段的 `src/main/resources/static/js/admin/`，最终打入 Java JAR。运行镜像不需要 Node 或单独的 Web 服务。构建阶段检查 JAR 内确实包含前端入口。

访问 `/js/admin/index.html#/login`。Hash 路由中的页面路径不会发送给服务器，直接刷新教师列表、AI 工具等页面不需要后端路由回退。使用专用目录保留现有学生首页和旧静态页面；该目录沿用后端已公开的 `/js/**` 资源规则，不修改安全配置。Java API 请求继续使用同源 `/api/*` 和现有 Cookie 会话。

本地也可以在 `apps/web/admin/` 执行 `npm ci`、`npm run build:static`，直接输出到 `apps/api/java/src/main/resources/static/js/admin/` 后再打包 Java。编译产物不纳入 Git，也不作为 Docker 构建输入，容器始终从源码重新构建。

教师 AI 仍为独立容器，浏览器直接访问其公开地址，使用原服务已有的跨域支持。部署前设置 `TEACHER_AI_PUBLIC_URL`，例如 `http://192.168.1.10:3000/api`；默认 `http://localhost:3000/api` 仅适用于浏览器和 Docker 宿主机在同一台机器。更换端口或域名时同步修改该地址，并重新构建 Java 镜像。HTTPS 页面须使用浏览器可访问的 HTTPS AI 地址，TLS 由现有部署环境提供。

`TEACHER_AI_PUBLIC_URL` 通过 `VITE_TEACHER_AI_BASE_URL` 在编译时写入浏览器产物，只能包含公开地址。`DEEPSEEK_API_KEY` 只通过 `docker/.env` 注入教师 AI 服务端，不参与前端构建。可配置 `DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL` 和 `DEEPSEEK_REQUEST_TIMEOUT`（毫秒）。不填密钥仍能启动并访问 `/api/health`，但不能生成内容；健康检查不调用付费模型或验证密钥有效性。

完整编排及 Java 入口包含教师 AI 服务；Python 独立入口仍只启动 Python 和 MySQL。

初次构建 Python 会编译 dlib 并安装视觉模型依赖，耗时和内存占用较高。需要更换镜像源时，在 `.env` 配置 `MYSQL_IMAGE`、`REDIS_IMAGE`、`MAVEN_IMAGE`、`JAVA_IMAGE`、`PYTHON_IMAGE`、`NODE_IMAGE`、`PIP_INDEX_URL`。默认使用官方镜像和官方 Python 包索引，正式部署可填入自行验证过的固定镜像 digest。Java 运行镜像需保留 Debian/Ubuntu 系的 `apt-get`、`useradd` 工具；Python 镜像需基于 Debian bookworm。Node 22 用于前端构建和教师 AI 运行；Java 运行镜像不包含 Node、Caddy 或 Nginx。

## 检查服务

```bash
curl -f http://127.0.0.1/js/admin/index.html
curl -f http://127.0.0.1:3000/api/health
curl -f http://127.0.0.1/enter.html
curl -f http://127.0.0.1:8080/health
docker compose --env-file docker/.env -f docker/docker-compose.yml logs --tail=100 teacher-ai java-app python-app
```

Java 健康检查请求公开登录页面，Python 请求 `/health`；它们证明 HTTP 进程可响应，不代表全部业务表、AI 服务或摄像头可用。MySQL 使用认证查询检查就绪，Redis 检查带认证的 PING。`depends_on` 等待依赖健康后才创建应用容器。容器运行后变为 unhealthy 不会自动重启，需通过 `ps` 和日志排查；退出的容器使用 `unless-stopped` 重启。

Python 用 Gunicorn 单 worker、多线程模式启动，配合 simple-websocket 支持当前 Socket.IO threading 模式。当前课堂状态存储在进程内，不能直接增加 worker 数；可通过 `GUNICORN_THREADS` 调整线程数。`GUNICORN_TIMEOUT` 默认为 120 秒。应用端口固定为 8080。

## 数据持久化

| 位置 | 内容 |
| --- | --- |
| `mysql_data` | MySQL 数据库 |
| `redis_data` | Redis AOF |
| `java_data` → `/app/data` | Java 上传视频、图片等媒体文件 |
| `python_data` → `/app/data` | Python 上传、模型、SQLite、Ultralytics 配置 |
| `FACE_DATABASE_DIR` → `/app/face_database` | 现有宿主人脸库，默认 `apps/api/python/face_database` |

Java 和 Python 均使用 UID/GID `10001:10001`。新建命名卷会继承镜像目录权限。已有 `python_data` 等卷如果由旧版 root 容器创建，可能需要先备份，再一次性修复权限：

```bash
docker compose --env-file docker/.env -f docker/docker-compose.yml stop java-app python-app
docker compose --env-file docker/.env -f docker/docker-compose.yml run --rm --no-deps --user 0 --entrypoint sh java-app -c 'chown -R 10001:10001 /app/data'
docker compose --env-file docker/.env -f docker/docker-compose.yml run --rm --no-deps --user 0 --entrypoint sh python-app -c 'chown -R 10001:10001 /app/data /app/face_database'
```

第二条命令也会修改宿主人脸库目录的属主，请确认 `FACE_DATABASE_DIR` 正确。自定义宿主路径建议使用绝对路径；SELinux 主机需由管理员配置相应的挂载标签和访问权限。

已有 Java 容器中未挂载的数据不会自动迁入新卷。更换旧容器前，使用 `docker cp` 备份旧容器实际媒体目录，再导入 `/app/data/uploads` 并修复属主。数据库已有卷不会重新运行 `docker/mysql/init/`；修改 `.env` 不会自动改变数据库内现存账号密码。继续沿用现有 `SPRING_FLYWAY_ENABLED=false`，本次不处理数据库结构变更。

模型默认读取 `/app/data/models/yolov8n-pose.pt`。离线服务器应事先把模型放入此路径。摄像头设备默认不透传；确有需要时由部署者为 Python 服务配置指定的 `devices` 映射和设备组权限，不要直接启用 privileged。

## 更新、停止与备份

```bash
git pull --ff-only
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build --wait --wait-timeout 300
docker compose --env-file docker/.env -f docker/docker-compose.yml down
```

`down` 保留命名卷；不要对正式数据执行 `down -v`。定期备份数据库、媒体卷、人脸库及 `.env`。日志写入标准输出，每个服务使用 3 个、每个上限 10 MB 的日志文件。

`.dockerignore` 排除本地密钥、构建产物、上传文件、模型和人脸库，避免把服务器数据或凭据烘焙进镜像。所有配置文件使用 LF。CI 对三套 Compose 配置进行解析、缺失密码校验和容器启动器单元测试，并构建包含 Vue 产物的 Java 镜像、检查 JAR 内的前端入口、启动教师 AI 容器并检查健康接口；不连接生产服务或调用付费模型。

参考：[Compose 依赖健康检查](https://docs.docker.com/compose/how-tos/startup-order/)、[Compose extends](https://docs.docker.com/compose/how-tos/multiple-compose-files/extends/)、[Flask-SocketIO 生产部署](https://flask-socketio.readthedocs.io/en/stable/deployment.html)。
