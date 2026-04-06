FROM python:3.10-slim

WORKDIR /app

RUN sed -i 's|http://.*.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://security.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://deb.debian.org|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    libboost-all-dev \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

COPY API/PythonAPI/requirements.txt /app/requirements.txt

RUN pip install --no-cache-dir --upgrade pip -i https://mirrors.aliyun.com/pypi/simple/ && \
    pip install --no-cache-dir -r /app/requirements.txt -i https://mirrors.aliyun.com/pypi/simple/

COPY API/PythonAPI /app

EXPOSE 8080
CMD ["python", "app.py"]
