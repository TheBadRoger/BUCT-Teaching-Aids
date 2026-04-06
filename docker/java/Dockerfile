FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

RUN sed -i 's|http://.*.ubuntu.com|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://.*.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://security.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://deb.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/share/maven/conf && \
    cat > /usr/share/maven/conf/settings.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
      <id>aliyunmaven-spring</id>
      <name>阿里云Spring仓库</name>
      <url>https://maven.aliyun.com/repository/spring</url>
      <mirrorOf>spring*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF

COPY API/JavaAPI/pom.xml /build/pom.xml
COPY API/JavaAPI/src /build/src
RUN mvn -B -DskipTests -Pdocker clean package

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /build/target/japi-*.jar /app/japi.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app/japi.jar"]
