ARG PYTHON_IMAGE=python:3.11-slim-bookworm
FROM ${PYTHON_IMAGE}
ARG PIP_INDEX_URL=https://pypi.org/simple
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 PIP_NO_CACHE_DIR=1
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
      build-essential cmake libboost-all-dev libopencv-dev \
      libgl1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*
COPY API/PythonAPI/requirements*.txt ./
COPY docker/python/requirements-server.txt ./
RUN pip install --index-url "$PIP_INDEX_URL" -r requirements.txt -r requirements-server.txt
COPY API/PythonAPI/ ./
COPY docker/python/entrypoint.py /usr/local/bin/buctta-entrypoint.py
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app && \
    mkdir -p /app/data/uploads /app/data/models /app/data/ultralytics /app/face_database/face_db && \
    chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8080/health', timeout=4)" || exit 1
ENTRYPOINT ["python", "/usr/local/bin/buctta-entrypoint.py"]
