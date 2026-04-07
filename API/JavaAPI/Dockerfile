FROM maven:3.9.11-eclipse-temurin-25

ARG APT_MIRROR=mirrors.aliyun.com
ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public

# Use Aliyun Ubuntu mirrors and install MySQL 8.0 client tools.
RUN sed -i "s|archive.ubuntu.com|${APT_MIRROR}|g; s|security.ubuntu.com|${APT_MIRROR}|g" /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends mysql-client-core-8.0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Configure Maven to use Aliyun mirror.
RUN mkdir -p /root/.m2 \
    && printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">\n  <mirrors>\n    <mirror>\n      <id>aliyunmaven</id>\n      <mirrorOf>central</mirrorOf>\n      <name>Aliyun Maven</name>\n      <url>'"${MAVEN_MIRROR_URL}"'</url>\n    </mirror>\n  </mirrors>\n</settings>\n' > /root/.m2/settings.xml

# Cache Maven dependencies first to speed up rebuilds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# Project sources for IDEA bind-mount or regular docker build usage.
COPY src/ src/

EXPOSE 80 5005

# Fixed dev debug configuration for IDEA container debugging.
CMD ["./mvnw", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-Dspring-boot.run.arguments=--spring.profiles.active=dev --spring.datasource.url=jdbc:mysql://mysql:3306/BUCTTA_DATABASE?serverTimezone=UTC"]

