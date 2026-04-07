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

Java 后端单独部署时，请使用 `API/JavaAPI/.env`，不要使用根目录 `/.env`。

```shell
cd /path/to/BUCT-Teaching-Aids/API/JavaAPI
cp .env.example .env
```

编辑 `API/JavaAPI/.env`，至少确认以下变量：

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
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, EXECUTE, SHOW VIEW
    ON BUCTTA_DATABASE.*
    TO 'java_springboot_buctta'@'localhost';
```
* 注意这里的密码要替换成你环境变量里设置的密码

输入```quit```退出MySQL Shell，返回默认Shell



## **4. 启动并测试项目**
* 启动后端：打开命令行，进入 `API/JavaAPI` 目录。输入命令```mvn spring-boot:run```,等到命令行能够一直保持并且不再弹出文本，说明启动成功。启动成功后不要关闭命令行窗口。（如下图所示）

![如图，出现“Spring”的LOGO，并且不显示任何ERROR错误，即表示启动成功](../ReadmeRes/backsucceed.png "后端启动成功的参考图")

* 测试项目：编写单元测试，会在运行和调试时得到测试结果；或者构建并启动项目后使用IDE、ApiPost或Postman，向后端发送消息，查看响应

## **5. 构建项目并部署**
进入 `API/JavaAPI` 目录，
* 不跳过测试：```mvn clean package```
* 跳过测试：```mvn clean package -Dmaven.skip.test=true```

完成后会在当前目录的target下生成一个 *japi-[版本号].jar* 文件。

进入这个目录，输入```nohup java -jar *japi-[版本号].jar```前端和后端即可同时启动，并且无需保留控制台。之后通过4444端口访问前端即可，后端的任何日志均会输出到当前目录下的一个 *nohup.out* 文件中

---
* **注意：** 开发环境中，应该将application.properties文件中的```spring.profiles.active```字段设置为```dev```；生产环境中则为```prod```
