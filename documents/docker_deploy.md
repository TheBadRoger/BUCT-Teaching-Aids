# Linux Docker 部署

本文只覆盖容器构建与编排。沿用现有后端和数据库初始化脚本，不配置 Nginx、TLS、Vue 前端托管或数据库业务结构升级。

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

默认应用只监听宿主机 `127.0.0.1`，Java 为 `80`，Python 为 `8080`。需要局域网直接访问时，配置 `APP_BIND_IP=0.0.0.0`，并按需设置 `JAVA_HOST_PORT`、`PYTHON_HOST_PORT`。MySQL、Redis 仅在 Compose 网络内访问，不发布宿主机端口。Java 容器内部端口已从 80 调整为 8080，以支持非 root 用户运行。

## 检查并启动

```bash
docker compose --env-file docker/.env -f docker/docker-compose.yml config --quiet
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build --wait --wait-timeout 300
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
```

两套独立部署入口复用完整编排的服务定义，选择其中一套运行，无需叠加多个 `-f`：

```bash
# Java + MySQL + Redis
docker compose --env-file docker/.env -f docker/docker-compose.java-root.yml up -d --build --wait --wait-timeout 300

# Python + MySQL
docker compose --env-file docker/.env -f docker/docker-compose.python-root.yml up -d --build --wait --wait-timeout 300
```

三套入口使用同一份 `.env`；即使只启动某一后端，也请填写模板中的全部必填配置。默认沿用目录名 `docker` 作为 Compose 项目名，保持已有命名卷的名称。已有部署不要随意更改 `-p` 或 `COMPOSE_PROJECT_NAME`，否则会使用另一组数据卷。切换部署入口时，先使用原入口执行 `down`，保留数据卷后再启动新入口。

初次构建 Python 会编译 dlib 并安装视觉模型依赖，耗时和内存占用较高。需要更换镜像源时，在 `.env` 配置 `MYSQL_IMAGE`、`REDIS_IMAGE`、`MAVEN_IMAGE`、`JAVA_IMAGE`、`PYTHON_IMAGE`、`PIP_INDEX_URL`。默认使用官方镜像和官方 Python 包索引，正式部署可填入自行验证过的固定镜像 digest。Java 运行镜像需保留 Debian/Ubuntu 系的 `apt-get`、`useradd` 工具；Python 镜像需基于 Debian bookworm。Node、Nginx 不在本次镜像中。

## 检查服务

```bash
curl -f http://127.0.0.1/enter.html
curl -f http://127.0.0.1:8080/health
docker compose --env-file docker/.env -f docker/docker-compose.yml logs --tail=100 java-app python-app
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
| `FACE_DATABASE_DIR` → `/app/face_database` | 现有宿主人脸库，默认 `API/PythonAPI/face_database` |

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

`.dockerignore` 排除本地密钥、构建产物、上传文件、模型和人脸库，避免把服务器数据或凭据烘焙进镜像。所有配置文件使用 LF。CI 对三套 Compose 配置进行解析、缺失密码校验和容器启动器单元测试，不会连接生产服务。

参考：[Compose 依赖健康检查](https://docs.docker.com/compose/how-tos/startup-order/)、[Compose extends](https://docs.docker.com/compose/how-tos/multiple-compose-files/extends/)、[Flask-SocketIO 生产部署](https://flask-socketio.readthedocs.io/en/stable/deployment.html)。
