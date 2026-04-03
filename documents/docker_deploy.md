# Docker 部署说明（Java + Python 同容器编排）

本文档说明如何使用 Docker 在同一套编排中同时部署：

- Java 后端（容器内端口 `80`）
- Python 后端（容器内端口 `8080`）
- MySQL（容器内端口 `3306`）
- Redis（容器内端口 `6371`）

> 相关依赖项已参考现有部署文档：`java_deploy_linux.md`、`python_deploy_linux.md`。

---

## 1. 前置条件

请先安装以下工具：

- Docker Engine
- Docker Compose（`docker compose` 子命令）

---

## 2. 目录说明

本次 Docker 部署文件位于项目根目录的 `docker/` 下：

```text
docker/
├── docker-compose.yml
├── java/
│   └── Dockerfile
├── python/
│   └── Dockerfile
└── mysql/
    └── init/
        └── 01-init-users.sh
```

---

## 3. 配置环境变量

推荐在仓库根目录维护统一变量文件：`.env`。

可通过 Docker 模板快速初始化：

```bash
cd /path/to/BUCT-Teaching-Aids
cp docker/.env.example .env
```

然后编辑 `.env` 填写真实变量：

```bash
nano .env
```

编辑 `.env`，至少修改以下字段：

```ini
MYSQL_ROOT_PASSWORD=your_mysql_root_password
REDIS_PASSWORD=your_redis_password
```

---

## 4. 启动服务

在 `docker/` 目录执行（推荐显式指定 `--env-file`）：

```bash
docker compose --env-file ../.env up -d
```

查看日志：

```bash
docker compose --env-file ../.env logs -f
```

---

## 5. 端口与访问

容器内端口规划（固定，不通过环境变量配置）：

- Java 后端：`80`
- Python 后端：`8080`
- MySQL：`3306`
- Redis：`6371`

默认主机映射（可在 `.env` 调整）：

- Java：`http://<服务器IP>:80`
- Python：`http://<服务器IP>:8080`
- Python 健康检查：`http://<服务器IP>:8080/health`
- MySQL：`<服务器IP>:3306`
- Redis：`<服务器IP>:6379`

`.env` 中的 `*_CONTAINER_PORT` 变量用于定义 Docker 对外发布端口（即主机侧端口），例如：

- `JAVA_CONTAINER_PORT` 映射到 Java 容器 `80`
- `PYTHON_CONTAINER_PORT` 映射到 Python 容器 `8080`
- `MYSQL_CONTAINER_PORT` 映射到 MySQL 容器 `3306`
- `REDIS_CONTAINER_PORT` 映射到 Redis 容器 `6371`

命令行临时覆盖主机端口映射示例：

```bash
JAVA_CONTAINER_PORT=8081 PYTHON_CONTAINER_PORT=8082 MYSQL_CONTAINER_PORT=3307 REDIS_CONTAINER_PORT=6380 docker compose --env-file ../.env up -d --build
```

PowerShell 示例：

```powershell
$env:JAVA_CONTAINER_PORT="8081"; $env:PYTHON_CONTAINER_PORT="8082"; $env:MYSQL_CONTAINER_PORT="3307"; $env:REDIS_CONTAINER_PORT="6380"; docker compose --env-file ../.env up -d --build
```

---

## 6. 停止与清理

停止服务：

```bash
docker compose --env-file ../.env down
```

同时删除数据卷（会清空 MySQL 数据）：

```bash
docker compose --env-file ../.env down -v
```

---

## 7. 实现说明（与项目现有配置的对应关系）

- Java 服务在 Docker 中固定使用 `SPRING_PROFILES_ACTIVE=docker`，读取 `application-docker.properties`。
- Java 的 Docker 配置固定连接 `mysql:3306` 与 `redis:6371`，并使用 `MYSQL_ROOT_PASSWORD`/`REDIS_PASSWORD`。
- Python 服务在 Docker 中通过 `APP_PROFILE=docker` 读取 `config_docker.py`。
- Python 的 Docker 配置固定连接 `mysql:3306`，并使用 `MYSQL_ROOT_PASSWORD`。
- Docker 启动时不会执行 [Java 脚本](../API/JavaAPI/docker/mysql/01-create-user.sh) 或 [Python 脚本](../API/PythonAPI/docker/mysql/01-create-user.sh)；它们仅用于非 Docker 直连部署。
- MySQL 首次初始化会创建 `BUCTTA_DATABASE`，并执行 `docker/mysql/init/02-init-schema.sql`。
- Docker 默认场景下 Java/Python 使用 `root / ${MYSQL_ROOT_PASSWORD}` 连接数据库。

---

## 8. 常见问题

1. **80 端口被占用**
   - 直接修改 `.env` 中 `JAVA_CONTAINER_PORT`，或按第 5 节命令行临时覆盖后重启。

2. **Python 第一次构建较慢**
   - `face-recognition` 等依赖需要编译，首次构建耗时较长，属正常现象。

3. **YOLO 模型首次运行下载**
   - `ultralytics` 首次可能下载模型，请确保网络可访问相应源。

4. **Docker 构建时可否为后端选择配置文件？**
   - Java：Docker Compose 已固定为 `SPRING_PROFILES_ACTIVE=docker`。
   - Python：Docker Compose 已固定为 `APP_PROFILE=docker`。
   - 当前约定：非 Docker 部署使用本地 `.env` 中的密码变量，Docker 部署使用 `MYSQL_ROOT_PASSWORD` 和 `REDIS_PASSWORD`。

5. **MySQL root 连接说明（默认即为 root）**
   - 默认即使用 `root / ${MYSQL_ROOT_PASSWORD}`，通常不需要叠加历史覆盖文件。
   - 仅在兼容历史命令时追加 `-f docker-compose.java-root.yml` 或 `-f docker-compose.python-root.yml`，并避免同时叠加两者。
   - 只需在 `.env` 维护：`MYSQL_ROOT_PASSWORD=your_mysql_root_password`。

6. **如何在生产环境用 GitHub Actions + Secrets 管理环境变量？**
   - 在 GitHub `production` 环境配置最小 Secrets：`PROD_HOST`、`PROD_USER`、`PROD_SSH_KEY`、`MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`（可选 `PROD_DEPLOY_PATH`）。
   - 按场景选择工作流：
     - 重建镜像：`.github/workflows/deploy-production.yml`
     - 快速重启：`.github/workflows/deploy-production-fast.yml`
     - 拉取依赖镜像后重启：`.github/workflows/deploy-production-pull.yml`
   - 工作流会临时生成 env 并调用第 4 节启动流程，不会写入宿主机全局环境变量。
