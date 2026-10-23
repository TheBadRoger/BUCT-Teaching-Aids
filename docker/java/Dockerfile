ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-25
ARG JAVA_IMAGE=eclipse-temurin:25-jre
FROM ${MAVEN_IMAGE} AS builder
WORKDIR /build
COPY apps/api/java/pom.xml ./
RUN mvn -B dependency:go-offline
COPY apps/api/java/src ./src
RUN mvn -B package -DskipTests && \
    cp target/japi-*.jar /build/app.jar

FROM ${JAVA_IMAGE}
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app && \
    mkdir -p /app/data/uploads && chown -R app:app /app
COPY --from=builder --chown=app:app /build/app.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent --output /dev/null http://127.0.0.1:8080/enter.html || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
