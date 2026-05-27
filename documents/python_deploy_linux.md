# Python 后端部署方法（Ubuntu Linux）

本文档说明如何在 Ubuntu Linux 服务器上部署 **BUCT 教学辅助系统统一 Python 后端**。
该后端由 Flask + Flask-SocketIO 构建，集成了以下功能模块：

| 模块 | URL 前缀 | 功能 |
|------|----------|------|
| PBL 生成器 | `/api/pbl/` | 项目式学习方案生成 |
| AI 课堂 | `/api/classroom/`、WebSocket | 课堂实时监控与分析 |
| 人脸识别 & 举手率 | `/api/face_hand_up/` | 人脸识别与举手率统计 |
| 抬头率检测 | `/api/headup_rate/` | YOLO 姿态检测抬头率 |
| 学情分析 | `/api/student/`、`/api/knowledge/`、`/api/report/` | 学情数据查询与报告 |

---

## 1. 安装系统依赖

```bash
sudo apt update && sudo apt upgrade -y

# Python 3.10+
sudo apt install -y python3 python3-pip python3-venv python3-dev

# OpenCV 系统库
sudo apt install -y libopencv-dev python3-opencv

# dlib 编译依赖（face_hand_up 模块）
sudo apt install -y cmake build-essential libboost-all-dev

# 摄像头支持（可选，仅使用摄像头功能时需要）
sudo apt install -y v4l-utils
```

## 2. 配置项目环境变量

复制模板文件并填写真实配置：

```bash
cp .env.example .env
nano .env   # 或使用 vim .env
```

> 说明：这里的 `.env` 是 `API/PythonAPI/.env`，仅用于 Python 后端单独部署；不要复用根目录 `/.env`。

需要修改的关键字段：

```ini
SECRET_KEY=<随机安全密钥，可用 python3 -c "import secrets; print(secrets.token_hex(32)" 生成>
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=python_flask_buctta
MYSQL_PASSWORD=replace_with_python_db_password
MYSQL_DB=BUCTTA_DATABASE
```

## 3. 安装并配置 MySQL

```bash
sudo apt install -y mysql-server
```

安装完成后验证并启动服务：

```bash
sudo systemctl status mysql.service
sudo systemctl start mysql.service   # 若服务未运行
sudo systemctl enable mysql.service  # 开机自启
```

运行安全初始化脚本：

```bash
sudo mysql_secure_installation
```

以 root 身份登录 MySQL，创建数据库和专用用户：

```sql
sudo mysql

CREATE DATABASE IF NOT EXISTS BUCTTA_DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'python_flask_buctta'@'localhost' IDENTIFIED BY '${BUCTTA_PYTHON_DB_PASSWORD}';

GRANT ALL PRIVILEGES ON BUCTTA_DATABASE.* TO 'python_flask_buctta'@'localhost';

FLUSH PRIVILEGES;
QUIT;
```

* 注意这里的密码要替换成你环境变量里设置的密码

## 4. 获取代码并配置环境

进入项目中 Python 后端所在目录：

```bash
cd /path/to/BUCT-Teaching-Aids/API/PythonAPI
```

创建并激活 Python 虚拟环境：

```bash
python3 -m venv venv
source venv/bin/activate
```

## 5. 安装 Python 依赖

> **注意**：`face-recognition` 依赖 `dlib`，编译耗时较长（约 5–10 分钟）。

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

若仅使用部分模块、不需要人脸识别，可跳过 face-recognition 相关包：

```bash
pip install Flask Flask-Cors Flask-SocketIO eventlet SQLAlchemy PyMySQL \
            python-dotenv numpy opencv-python pandas Jinja2 Pillow imutils ultralytics
```


## 6. 初始化数据库表

数据库表会在后端**首次启动时自动创建**。若需手动提前创建，可运行：

```bash
source venv/bin/activate
python3 - <<'EOF'
from app import app
from modules.db import Base, engine
from modules.face_hand_up.routes import HandUpRecord
from modules.headup_rate.routes import HeadUpRateRecord
from modules.student_analysis.models import Student, Knowledge, KnowledgeMastery, AdminUser
Base.metadata.create_all(bind=engine)
print("数据库表创建完成")
EOF
```

## 7. 配置人脸库（face_hand_up 模块）

将学生人脸照片放入以下目录：

```
face_database/face_db/
```

文件命名格式：`学号_姓名.jpg`，例如 `2024001_张三.jpg`。
每张照片中只允许出现**一张人脸**，支持 `.jpg`、`.jpeg`、`.png` 格式。


## 8. 启动并测试项目

**开发模式**（前台运行，便于查看日志）：

```bash
source venv/bin/activate
python3 app.py
```

启动成功后，访问 `http://服务器IP:5000/health`，应看到：

```json
{"message": "BUCT Teaching Aids – Unified Python Backend", "status": "ok"}
```

**快速接口测试**（可选）：

```bash
# PBL 模块
curl http://localhost:5000/api/pbl/templates

# 学情分析模块
curl "http://localhost:5000/api/student/class_list?class_name=高材2304"

# 抬头率检测（POST，需传 JSON）
curl -X POST http://localhost:5000/api/headup_rate/detect \
     -H "Content-Type: application/json" \
     -d '{"student_id":1001,"course_id":"math_101","course_name":"数学","data_type":"image","calculated_rate":85.5}'
```

## 9. 生产部署（后台持久运行）

### 方式一：nohup（简单快速）

```bash
source venv/bin/activate
nohup python3 app.py > app.log 2>&1 &
echo "后端已在后台启动，日志输出至 app.log"
```

查看运行日志：

```bash
tail -f app.log
```

### 方式二：systemd 服务（推荐，支持开机自启与自动重启）

创建服务文件（将路径替换为实际安装路径）：

```bash
sudo nano /etc/systemd/system/buctta-python.service
```

填入以下内容：

```ini
[Unit]
Description=BUCT Teaching Aids – Python Backend
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/path/to/BUCT-Teaching-Aids/API/PythonAPI
Environment="PATH=/path/to/BUCT-Teaching-Aids/API/PythonAPI/venv/bin"
ExecStart=/path/to/BUCT-Teaching-Aids/API/PythonAPI/venv/bin/python3 app.py
Restart=on-failure
RestartSec=5s
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启用并启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable buctta-python.service
sudo systemctl start buctta-python.service
sudo systemctl status buctta-python.service
```

## 10. 注意事项

- **开发环境**：在 `.env` 中设置 `DEBUG=true`，Flask 会开启热重载和详细错误信息。
- **生产环境**：务必设置 `DEBUG=false`，并使用强随机 `SECRET_KEY`。
- **YOLO 模型**：`headup_rate` 模块首次运行时，`ultralytics` 会自动下载 `yolov8n-pose.pt`（约 6 MB），请确保服务器能访问 GitHub/CDN。若无法访问，请手动下载后通过 `.env` 中的 `YOLO_MODEL_PATH` 指定本地路径。
- **摄像头权限**：运行 `face_hand_up` 或 `headup_rate` 视频流接口时，需确保运行用户有摄像头设备访问权限（`sudo usermod -aG video <username>`）。
- **端口**：默认端口为 `5000`，可在 `.env` 中通过 `PORT=<端口号>` 修改。
