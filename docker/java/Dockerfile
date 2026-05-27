FROM docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-25 AS builder

WORKDIR /build

# Prefer domestic Maven mirror to speed up dependency downloads.
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>aliyun maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

COPY API/JavaAPI /build
RUN mvn -s /root/.m2/settings.xml -f /build/pom.xml clean package -DskipTests && \
    JAR_FILE=$(find /build/target -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' | head -n 1) && \
    test -n "$JAR_FILE" && cp "$JAR_FILE" /build/app.jar

FROM docker.m.daocloud.io/library/eclipse-temurin:25-jre

WORKDIR /app
COPY --from=builder /build/app.jar /app/app.jar

EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
