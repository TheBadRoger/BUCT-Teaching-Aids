# Docker 部署方法

本文档说明如何使用项目根目录的 Docker 文件进行一键部署。所有 Docker 文件均集中在 `docker/` 目录，不修改各子项目目录。

## 1. 目录说明

- `docker/docker-compose.yml`：完整部署（Java + Python + MySQL + Redis）
- `docker/docker-compose.java-root.yml`：仅 Java 相关服务
- `docker/docker-compose.python-root.yml`：仅 Python 相关服务
- `docker/java/Dockerfile`：Java 镜像构建
- `docker/python/Dockerfile`：Python 镜像构建
- `docker/mysql/init/`：MySQL 初始化脚本

## 2. 国内加速镜像源

已默认启用以下国内加速：

- Maven：`https://maven.aliyun.com/repository/public`
- pip：`https://mirrors.aliyun.com/pypi/simple/`
- apt：`mirrors.aliyun.com`
- 基础镜像拉取：`docker.m.daocloud.io`

## 3. 环境变量配置

```bash
cd docker
cp .env.example .env
```

编辑 `.env`，至少配置：

- `MYSQL_ROOT_PASSWORD`
- `BUCTTA_JAVA_DB_PASSWORD`
- `BUCTTA_PYTHON_DB_PASSWORD`
- `REDIS_PASSWORD`

端口变量只控制宿主机映射端口，容器内部端口固定为默认值：

- Java 容器内部端口：`80`
- Python 容器内部端口：`8080`
- MySQL 容器内部端口：`3306`
- Redis 容器内部端口：`6379`

可通过以下变量修改宿主机映射端口：

- `JAVA_HOST_PORT`
- `PYTHON_HOST_PORT`
- `MYSQL_HOST_PORT`
- `REDIS_HOST_PORT`

## 4. 启动部署

在项目根目录执行：

```bash
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build
```

## 5. 可选：按后端拆分启动

仅 Java：

```bash
docker compose --env-file docker/.env -f docker/docker-compose.java-root.yml up -d --build
```

仅 Python：

```bash
docker compose --env-file docker/.env -f docker/docker-compose.python-root.yml up -d --build
```

## 6. 数据初始化与持久化

- MySQL 初始化：`docker/mysql/init/01-init-users.sh` 与 `docker/mysql/init/02-init-schema.sql`
- 初始化内容：创建 `BUCTTA_DATABASE`，创建并授权
  - `java_springboot_buctta`
  - `python_flask_buctta`

持久化卷：

- `mysql_data`：MySQL 数据
- `redis_data`：Redis 数据
- `python_data`：Python 运行数据（上传目录、模型、SQLite 文件）

Python 人脸库挂载目录：

- 宿主机：`API/PythonAPI/face_database`
- 容器内：`/app/face_database`
