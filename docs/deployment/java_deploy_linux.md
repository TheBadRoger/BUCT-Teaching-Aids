# 部署方法（Ubuntu Linux）

## **1. 安装依赖项**

```shell
sudo apt update

# Java25 OpenJDK JDK
sudo apt install openjdk-25-jdk

# Maven
sudo apt install maven

# MySQL
sudo apt install mysql-server

# Redis
sudo apt install redis-server
```

## **2. 配置项目环境变量**

Java 后端单独部署时，请使用 `apps/api/java/.env`，不要使用根目录 `/.env`。

```shell
cd /path/to/BUCT-Teaching-Aids/apps/api/java
cp .env.example .env
```

编辑 `apps/api/java/.env`，至少确认以下变量：

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=80
BUCTTA_JAVA_DB_PASSWORD=replace_with_java_db_password
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=replace_with_redis_password
```

## **3. 配置MySQL**
MySQL安装完成后输入```sudo systemctl status mysql.service```验证服务状态，如果服务未启动，需要输入```sudo systemctl start mysql.service```来启动服务

输入```sudo systemctl enable mysql.service```来让MySQL开机自启

随后输入```sudo mysql_secure_installation```启动安全安装脚本，跟随指引进行安装

完成后输入sudo mysql即可以根用户身份登录MySQL并打开MySQL Shell。之后输入如下代码
```sql
CREATE DATABASE IF NOT EXISTS BUCTTA_DATABASE;
CREATE USER 'java_springboot_buctta'@'localhost' IDENTIFIED BY 'replace_with_java_db_password';
GRANT ALL PRIVILEGES ON BUCTTA_DATABASE.* TO 'java_springboot_buctta'@'localhost';
```
* 注意这里的密码要替换成你环境变量里设置的密码

输入```quit```退出MySQL Shell，返回默认Shell



## **4. 启动并测试项目**
* 启动后端：打开命令行，进入 `apps/api/java` 目录。输入命令```mvn spring-boot:run```,等到命令行能够一直保持并且不再弹出文本，说明启动成功。启动成功后不要关闭命令行窗口。（如下图所示）

出现 Spring 启动日志且没有 ERROR 错误后，可继续检查应用接口是否可访问。

* 测试项目：编写单元测试，会在运行和调试时得到测试结果；或者构建并启动项目后使用IDE、ApiPost或Postman，向后端发送消息，查看响应

## **5. 构建项目并部署**
进入 `apps/api/java` 目录，
* 不跳过测试：```mvn clean package```
* 跳过测试：```mvn clean package -Dmaven.skip.test=true```

完成后会在当前目录的target下生成一个 *japi-[版本号].jar* 文件。

进入这个目录，输入```nohup java -jar *japi-[版本号].jar```前端和后端即可同时启动，并且无需保留控制台。之后通过4444端口访问前端即可，后端的任何日志均会输出到当前目录下的一个 *nohup.out* 文件中

---
* **注意：** 开发环境中，应该将application.properties文件中的```spring.profiles.active```字段设置为```dev```；生产环境中则为```prod```

## **6. Docker 整体部署**

Docker 只支持项目整体部署，不再提供 Java 单独编排或依赖服务单独编排。在仓库根目录复制 `.env.example` 为 `.env`、填写必填变量后执行：

```bash
docker compose up -d --build --wait --wait-timeout 300
```

Java（含现场编译的 Vue）、Python、教师 AI、MySQL、Redis 由同一份根目录 `compose.yaml` 管理。详细配置见 [Docker 部署说明](docker_deploy.md)。直接运行 Maven/Java 的开发方式仍可使用自行准备的数据库与 Redis，Java 不再自动启动 Compose。

## **7. Testcontainers 集成测试（说明）**

项目使用 Testcontainers 在集成测试中动态启动临时容器（如 MySQL、Redis）。要正确运行基于 Testcontainers 的测试，请注意：

- **Docker 必须在宿主机上运行且可访问**：在 Linux 上确保 Docker daemon 正常运行；在 Windows 建议使用 Docker Desktop + WSL2 后端。
- **运行单元/集成测试**：在 `apps/api/java` 目录执行：

```bash
cd /path/to/BUCT-Teaching-Aids/apps/api/java
mvn test
```

- **常见 CI/权限问题**：若 CI 环境中无法启动容器，检查构建 runner 是否有 Docker 访问权限（挂载 Docker socket 或使用 DinD）。可以通过设置环境变量来绕过 Ryuk（不推荐，生产 CI 请保证隔离）：

```bash
# 禁用 Ryuk（仅在受控 CI 中使用）
export TESTCONTAINERS_RYUK_DISABLED=true
# 或者配置 ~/.testcontainers.properties: reusable=true
```

- **测试失败排查**：若测试无法连接到数据库，确认 `TEST` 环境下的 JDBC URL 允许通过容器暴露的端口访问（Testcontainers 通常自动注入）并查看 Docker 日志。

完整容器部署统一使用仓库根目录 `compose.yaml`。
