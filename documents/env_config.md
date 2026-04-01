# `.env` 配置方法

本文档说明本项目在本地开发或服务器部署时，如何准备 `.env` 环境变量文件。

## 1. 为什么需要 `.env`

项目的 Docker 部署会读取环境变量（例如数据库密码、端口、Spring 运行环境等）。
使用 `.env` 可以把配置与代码分离，避免将敏感信息直接写入仓库。

## 2. 推荐配置项

可在项目根目录创建 `.env`，并按需填写以下变量：

```env
# 基础密码（请替换为强密码）
MYSQL_ROOT_PASSWORD=your_mysql_password
REDIS_PASSWORD=your_redis_password

# 运行环境
SPRING_PROFILES_ACTIVE=docker

# 端口映射（宿主机端口）
JAVA_HOST_PORT=80
PYTHON_HOST_PORT=8080
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379

# Java 后端 Redis 连接（统一在 application.properties 中读取）
REDIS_HOST=localhost
REDIS_PORT=6379
```

## 3. Java Redis 变量说明（统一配置）

以下变量对应 `API/JavaAPI/src/main/resources/application.properties` 中的统一 Redis 配置：

- `REDIS_HOST`：Redis 主机地址，默认 `localhost`
- `REDIS_PORT`：Redis 端口，默认 `6379`
- `REDIS_PASSWORD`：Redis 密码，默认 `change_me`（建议务必覆盖）
> 说明：Redis 连接池参数已固定在 `application.properties` 默认值，不再通过 `.env` 提供连接池覆盖项。

## 4. 使用方式

- Docker 部署时，`docker compose` 会自动读取当前目录下 `.env`
- 也可以显式指定：`docker compose --env-file .env up -d --build`
- 在 GitHub Actions 远程部署中，通常由工作流将 Secrets 写入临时 env 文件

## 5. 安全建议

- 不要将包含真实密码的 `.env` 提交到仓库
- 建议提交 `.env.example` 作为模板，便于团队成员复制配置
- 生产环境请使用随机强密码，并定期轮换

