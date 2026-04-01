FROM python:3.10-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    libboost-all-dev \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

COPY API/PythonAPI/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir --upgrade pip && pip install --no-cache-dir -r /app/requirements.txt

COPY API/PythonAPI /app

EXPOSE 8080
CMD ["python", "app.py"]
