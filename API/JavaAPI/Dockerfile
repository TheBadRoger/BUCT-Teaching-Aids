FROM eclipse-temurin:25-jdk

ARG MAVEN_VERSION=3.9.11
ARG MAVEN_HOME=/opt/apache-maven-${MAVEN_VERSION}

# Keep package installation non-interactive.
ENV DEBIAN_FRONTEND=noninteractive

RUN sed -i 's|http://.*.ubuntu.com|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://.*.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://security.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://deb.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list

# Install Maven 3.9.x and MySQL 8.0 server from official sources.
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget gnupg lsb-release ca-certificates tar \
    && wget -qO /tmp/apache-maven.tar.gz https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && tar -xzf /tmp/apache-maven.tar.gz -C /opt \
    && ln -s ${MAVEN_HOME}/bin/mvn /usr/local/bin/mvn \
    && wget -qO /tmp/mysql-apt-config.deb https://dev.mysql.com/get/mysql-apt-config_0.8.33-1_all.deb \
    && echo "mysql-apt-config mysql-apt-config/select-server select mysql-8.0" | debconf-set-selections \
    && echo "mysql-apt-config mysql-apt-config/select-product select Ok" | debconf-set-selections \
    && dpkg -i /tmp/mysql-apt-config.deb \
    && apt-get update \
    && apt-get install -y --no-install-recommends mysql-community-server \
    && rm -rf /var/lib/apt/lists/* /tmp/mysql-apt-config.deb /tmp/apache-maven.tar.gz

WORKDIR /workspace

# Runtime defaults. Override in `docker run -e ...` when needed.
ENV MYSQL_ROOT_PASSWORD=root \
    MYSQL_DATABASE=BUCTTA_DATABASE \
    MYSQL_USER=java_springboot_buctta \
    MYSQL_PASSWORD=change_me \
    SPRING_PROFILES_ACTIVE=docker \
    SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/BUCTTA_DATABASE?serverTimezone=UTC \
    MAVEN_OPTS="-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'"

# HTTP + JDWP + MySQL
EXPOSE 80 5005 3306

# MySQL bootstrap + Spring Boot dev startup.
RUN <<'EOF' bash
cat >/usr/local/bin/start-dev.sh <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail

if [ ! -d "/var/lib/mysql/mysql" ]; then
  mysqld --initialize-insecure --user=mysql
fi

# Start MySQL in background.
mysqld --user=mysql --daemonize

# Wait for MySQL to become ready.
until mysqladmin ping --silent; do
  sleep 1
done

mysql -uroot <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE};
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON ${MYSQL_DATABASE}.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL

# Keep project dependencies cached inside the container between restarts.
mvn -q -DskipTests dependency:go-offline

# Use spring-boot:run for IDE debugging and hot reload in mounted source volume.
exec mvn -Pdocker spring-boot:run
SCRIPT

chmod +x /usr/local/bin/start-dev.sh
EOF

CMD ["/usr/local/bin/start-dev.sh"]



