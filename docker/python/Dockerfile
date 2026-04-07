FROM docker.m.daocloud.io/library/python:3.11-slim-bookworm

ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /app

# Switch to Aliyun mirror for apt and pip to speed up builds in China.
RUN sed -i 's|deb.debian.org|mirrors.aliyun.com|g; s|security.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list.d/debian.sources && \
    apt-get update && apt-get install -y --no-install-recommends \
      build-essential cmake libboost-all-dev libopencv-dev python3-opencv \
      libgl1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*

COPY API/PythonAPI/requirements*.txt /app/
RUN pip config set global.index-url https://mirrors.aliyun.com/pypi/simple/ && \
    pip config set global.trusted-host mirrors.aliyun.com && \
    pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

COPY API/PythonAPI /app
RUN mkdir -p /app/data/uploads /app/data/models

EXPOSE 8080
CMD ["python", "app.py"]
