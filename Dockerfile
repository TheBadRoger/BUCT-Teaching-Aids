# 由根目录 compose.yaml 选择构建目标，统一部署整套服务。
ARG NODE_IMAGE=node:22-alpine
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-25
ARG JAVA_IMAGE=eclipse-temurin:25-jre
ARG PYTHON_IMAGE=python:3.11-slim-bookworm

FROM ${PYTHON_IMAGE} AS python
ARG PIP_INDEX_URL=https://pypi.org/simple
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 PIP_NO_CACHE_DIR=1
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
      build-essential cmake libboost-all-dev libopencv-dev \
      libgl1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*
COPY apps/api/python/requirements*.txt ./
COPY apps/api/python/deployment/requirements-server.txt ./
RUN pip install --index-url "$PIP_INDEX_URL" -r requirements.txt -r requirements-server.txt
COPY apps/api/python/ ./
COPY apps/api/python/deployment/entrypoint.py /usr/local/bin/buctta-entrypoint.py
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app && \
    mkdir -p /app/data/uploads /app/data/models /app/data/ultralytics /app/face_database/face_db && \
    chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8080/health', timeout=4)" || exit 1
ENTRYPOINT ["python", "/usr/local/bin/buctta-entrypoint.py"]

FROM ${NODE_IMAGE} AS frontend
WORKDIR /web
COPY apps/web/package.json apps/web/package-lock.json ./
RUN npm ci
COPY apps/web/ ./
COPY apps/api/java/src/main/resources/static/images/ /api/java/src/main/resources/static/images/
RUN npm run build -- --base=/js/app/

FROM ${MAVEN_IMAGE} AS builder
WORKDIR /build
COPY apps/api/java/pom.xml ./
RUN mvn -B dependency:go-offline
COPY apps/api/java/src ./src
COPY --from=frontend /web/dist/ ./src/main/resources/static/js/app/
COPY --from=frontend /web/dist/index.html ./src/main/resources/static/index.html
RUN mvn -B package -DskipTests && \
    cp target/japi-*.jar /build/app.jar && \
    jar tf /build/app.jar | grep -q '^BOOT-INF/classes/static/js/app/index.html$'

FROM ${JAVA_IMAGE} AS java
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app && \
    mkdir -p /app/data/uploads && chown -R app:app /app
COPY --from=builder --chown=app:app /build/app.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent --output /dev/null http://127.0.0.1:8080/js/app/index.html || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
