# Docker 部署说明（Java + Python 同容器编排）

本文档说明如何使用 Docker 在同一套编排中同时部署：

- Java 后端（容器内端口 `80`）
- Python 后端（容器内端口 `8080`）
- MySQL（供两个后端共享）
- Redis（供 Java 后端使用）

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
├── .env.example
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

进入 Docker 目录并复制模板：

```bash
cd /path/to/BUCT-Teaching-Aids/docker
cp .env.example .env
```

编辑 `.env`，至少修改以下字段：

```ini
MYSQL_ROOT_PASSWORD=your_mysql_root_password
DOCKER_DB_PASSWORD=your_shared_docker_db_password
REDIS_PASSWORD=your_redis_password
SPRING_PROFILES_ACTIVE=prod

JAVA_HOST_PORT=80
PYTHON_HOST_PORT=8080
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379
```

---

## 4. 启动服务

在 `docker/` 目录执行：

```bash
docker compose up -d --build
```

> 首次部署或 Dockerfile/依赖变更时使用 `--build`。
> 仅修改 `.env`、`docker-compose*.yml` 或运行参数时，可直接：
> `docker compose up -d`（更快，无需重建镜像）。

---

## 4.1 项目更新后是否必须重新构建？

- **代码更新（Java/Python 源码变化）**：需要重建镜像，建议使用：
  ```bash
  docker compose up -d --build
  ```
- **仅配置更新（环境变量、端口映射、profile）**：通常不需要重建，使用：
  ```bash
  docker compose up -d
  ```
- **更方便的选择**：开发阶段可直接在宿主机运行后端（参考 `java_deploy_linux.md`、`python_deploy_linux.md`），仅将 MySQL/Redis 用 Docker 托管，减少反复镜像构建时间。

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f
```

---

## 5. 端口与访问

容器内端口规划（避免冲突）：

- Java 后端：`80`
- Python 后端：`8080`

默认主机映射（可在 `.env` 调整）：

- Java：`http://<服务器IP>:80`
- Python：`http://<服务器IP>:8080`
- Python 健康检查：`http://<服务器IP>:8080/health`

---

## 6. 停止与清理

停止服务：

```bash
docker compose down
```

同时删除数据卷（会清空 MySQL 数据）：

```bash
docker compose down -v
```

---

## 7. 实现说明（与项目现有配置的对应关系）

1. **Java 容器内端口 80**
   - 在 `docker-compose.yml` 中为 Java 服务注入 `SERVER_PORT=80`。
   - 保持 `SPRING_PROFILES_ACTIVE=prod`，并通过环境变量覆盖数据库与 Redis 地址（容器内服务名 `mysql`、`redis`）。

2. **Python 容器内端口 8080**
   - 在 `docker-compose.yml` 中为 Python 服务注入 `PORT=8080`。
   - 与现有 `config.py` 的 `PORT` 环境变量读取机制保持一致。

3. **数据库初始化**
   - `mysql/init/01-init-users.sh` 在 MySQL 首次初始化时读取环境变量并创建项目所需用户与授权：
     - `java_springboot_buctta`
     - `python_flask_buctta`

4. **与现有部署文档一致的依赖思路**
   - Java 镜像基于 JDK 25 + Maven 构建。
   - Python 镜像安装了现有文档提到的关键系统依赖（OpenCV / cmake / build-essential / boost）与 `requirements.txt`。

---

## 8. 常见问题

1. **80 端口被占用**
   - 修改 `.env` 中 `JAVA_HOST_PORT`，例如改成 `8081`，然后重启：
   ```bash
   docker compose up -d
   ```

2. **Python 第一次构建较慢**
   - `face-recognition` 等依赖需要编译，首次构建耗时较长，属正常现象。

3. **YOLO 模型首次运行下载**
   - `ultralytics` 首次可能下载模型，请确保网络可访问相应源。

4. **Docker 构建时可否为后端选择配置文件？**
   - **可以**。
- Java 后端默认读取 `SPRING_PROFILES_ACTIVE`，可在 `.env` 中设置，例如：
   ```ini
   SPRING_PROFILES_ACTIVE=dev
   ```
   - 启动时也可临时覆盖：
   ```bash
    SPRING_PROFILES_ACTIVE=prod docker compose up -d
    ```
   - 建议将真实口令保存在项目根目录的 `.env/secrets.env` 中，再手动同步到 `docker/.env`，避免凭据直接写入文档或源码模板。
   - 约定：**手动部署**使用 `BUCTTA_JAVA_DB_PASSWORD` / `BUCTTA_PYTHON_DB_PASSWORD`；**Docker 部署**使用统一的 `DOCKER_DB_PASSWORD`。

5. **按需求使用 root 身份连接 MySQL（专用覆盖文件）**
   - 本仓库提供两个覆盖文件（与 `docker-compose.yml` 叠加使用）：
    - `docker-compose.java-root.yml`：Java 使用 `root / ${MYSQL_ROOT_PASSWORD}`（必填）
    - `docker-compose.python-root.yml`：Python 使用 `root / ${MYSQL_ROOT_PASSWORD}`（必填）
   - **注意**：同一个 MySQL 实例只能有一个 root 密码，因此两个覆盖文件应分场景分别使用，不要同时叠加。
   - Java 场景（含 root 密码覆盖）：
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.java-root.yml up -d --build
   ```
    - Python 场景（含 root 密码覆盖）：
    ```bash
   docker compose -f docker-compose.yml -f docker-compose.python-root.yml up -d --build
    ```
    - **不要同时叠加两个 root 覆盖文件**，否则 Java/Python 会同时改用 root 账户，偏离“按场景单独覆盖”的设计意图：
    ```bash
    # 不推荐（不要这样做）
    docker compose -f docker-compose.yml -f docker-compose.java-root.yml -f docker-compose.python-root.yml up -d --build
    ```
     - 建议在 `.env` 中维护 `MYSQL_ROOT_PASSWORD`，避免在 Compose 文件中写死凭据。
     - 当前统一口令如下（Java/Python 场景一致，仅 Docker 场景使用）：
     ```ini
    DOCKER_DB_PASSWORD=your_shared_docker_db_password
     ```

6. **如何在生产环境用 GitHub Actions + Secrets 管理环境变量？**
   - 推荐做法：
     1) 在仓库中创建 `production` 环境 (Settings → Environments)
     2) 在该环境中配置 Secrets（至少）：
        - `PROD_HOST`、`PROD_USER`、`PROD_SSH_KEY`
        - `PROD_DEPLOY_PATH`（可选，默认 `/opt/BUCT-Teaching-Aids`）
        - `MYSQL_ROOT_PASSWORD`
        - `DOCKER_DB_PASSWORD`
        - `REDIS_PASSWORD`
     3) 使用仓库内工作流 `.github/workflows/deploy-production.yml` 手动触发部署（`workflow_dispatch`）。
   - 该工作流会在服务器上生成 `docker/.env` 并执行：
   ```bash
   docker compose up -d --build
   ```
   - 安全建议：
     - 不要将真实口令写入仓库文件；
     - 仅在 GitHub Secrets/Environment Secrets 中维护生产口令；
     - 为 `production` 环境开启审批规则（required reviewers）。
