# 整体部署

部署入口为仓库根目录 `compose.yaml` 和 `Dockerfile`，需要 Docker Engine 与 Docker Compose v2。

## 配置与启动

在仓库根目录执行：

```bash
cp .env.example .env
chmod 600 .env
# 编辑 .env，填写必填变量
docker compose config --quiet
docker compose up -d --build --remove-orphans --wait --wait-timeout 300
docker compose ps
```

| 配置 | 要求 |
| --- | --- |
| 必填密码 | `MYSQL_ROOT_PASSWORD`、`BUCTTA_JAVA_DB_PASSWORD`、`BUCTTA_PYTHON_DB_PASSWORD`、`REDIS_PASSWORD`、`APP_ADMIN_PASSWORD`、`PYTHON_SECRET_KEY` |
| 密码格式 | 数据库初始化脚本拼接 SQL，数据库密码使用随机字母数字；可用 `openssl rand -hex 32` 生成 |
| `.env` | Compose 自动读取；含 `$`、`#` 的值用单引号包裹；文件不进入 Git 和镜像 |
| 项目名 | 固定为 `buct-teaching-aids`，关联持久化卷名称 |
| 外部访问 | 默认 `APP_BIND_IP=127.0.0.1`；需要其他机器访问时设置 `0.0.0.0` |

## 服务与入口

| 服务 | 职责 | 默认宿主端口 |
| --- | --- | --- |
| `java-app` | 业务 API、教师 AI、统一 Vue 页面 | 80（`JAVA_HOST_PORT`） |
| `python-app` | 课堂互动、视觉和分析 API | 8080（`PYTHON_HOST_PORT`） |
| `mysql` | 共享 MySQL 数据库 | 不发布 |
| `redis` | Java 缓存 | 不发布 |

| 页面 | 默认地址 |
| --- | --- |
| 学习端 | `http://127.0.0.1/js/app/index.html#/learn` |
| 管理登录 | `http://127.0.0.1/js/app/index.html#/login` |
| 站点入口 | `http://127.0.0.1/index.html` |

教师 AI 使用同源 `/api/teacher-ai/**` 和 Java Cookie 会话。Hash 路由无需页面回退配置。

## 构建与模型配置

```text
Vue 源码 → Node 构建 → Java static/js/app/ + static/index.html → Maven JAR → Java 镜像
Python 源码 + 运行依赖 → Python 镜像
```

| 配置 | 用途 |
| --- | --- |
| `DEEPSEEK_API_KEY` | Java 教师 AI 密钥 |
| `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` | 模型地址 / 名称 |
| `DEEPSEEK_REQUEST_TIMEOUT` | 模型连接与读取等待超时，单位毫秒，默认 60000 |
| `POLYMAS_AUTH_KEY` | Java AI 批改和背诵手册密钥 |
| `NODE_IMAGE` / `MAVEN_IMAGE` / `JAVA_IMAGE` / `PYTHON_IMAGE` | 构建与运行基础镜像 |
| `MYSQL_IMAGE` / `REDIS_IMAGE` / `PIP_INDEX_URL` | 数据服务镜像与 Python 包源 |

密钥只注入服务端。未配置模型密钥时页面可以访问，模型生成不可用；健康检查不验证密钥有效性。Java 镜像需支持 apt-get/useradd，Python 使用 Debian bookworm 基础镜像；Python 视觉依赖构建需要较多时间与内存。

## 数据与运行限制

| 持久化位置 | 内容 |
| --- | --- |
| `mysql_data` | MySQL 数据 |
| `redis_data` | Redis AOF |
| `java_data` → `/app/data` | Java 上传媒体 |
| `python_data` → `/app/data` | Python 上传、模型、SQLite 和 Ultralytics 配置 |
| `FACE_DATABASE_DIR` → `/app/face_database` | 人脸库，默认 `./apps/api/python/face_database` |

| 项目 | 约束 |
| --- | --- |
| MySQL 初始化 | `database/init/` 只在空数据库卷首次启动时执行；`database/manual/` 不自动执行 |
| 文件权限 | Java/Python 使用 UID/GID `10001:10001`，人脸库需允许此用户读写 |
| 凭据更新 | 修改 `.env` 不会自动修改已有数据库账号密码；Flyway 关闭 |
| Python 进程 | 单 worker、多线程 Gunicorn；`GUNICORN_THREADS` 和 `GUNICORN_TIMEOUT` 控制线程与超时；进程内课堂状态不支持直接增加 worker |
| 模型与设备 | 模型路径 `/app/data/models/yolov8n-pose.pt`；离线环境预置模型；摄像头按设备配置权限，不启用 privileged |
| 日志与健康 | 每服务最多 3 个 10 MB 日志；unhealthy 不自动重启，进程退出使用 `unless-stopped` 重启 |

## 检查、更新与停止

```bash
curl -f http://127.0.0.1/js/app/index.html
curl -f http://127.0.0.1:8080/health
docker compose logs --tail=100

# 更新
git pull --ff-only
docker compose up -d --build --remove-orphans --wait --wait-timeout 300

# 停止并保留数据卷
docker compose down
```

教师 AI 状态接口 `GET /api/teacher-ai/health` 需要登录会话。健康检查不代表业务表、模型或摄像头可用。定期备份 `.env`、数据库、媒体和人脸库；生产数据不使用 `docker compose down -v`。
