# Python 后端

| 项目 | 实现 |
| --- | --- |
| 运行环境 | Python 3.11 |
| HTTP 与实时通信 | Flask、Blueprint、Flask-SocketIO |
| 数据访问 | SQLAlchemy / PyMySQL 访问 MySQL；课堂模块使用 SQLite |
| 视觉处理 | OpenCV、dlib / face-recognition、YOLO 姿态估计 |
| 容器运行 | Gunicorn 单 worker、多线程；课堂状态保存在进程内 |

## 模块

| 模块目录 | 职责 |
| --- | --- |
| `modules/pbl/` | 项目式学习方案生成与上传处理 |
| `modules/ai_classroom/` | 实时课堂指标、师生互动、分组讨论、测评与 Socket.IO 事件 |
| `modules/face_hand_up/` | 人脸识别、举手检测与记录 |
| `modules/headup_rate/` | YOLO 姿态分析、抬头率统计与历史查询 |
| `modules/student_analysis/` | 学生、知识点掌握度和学情报告；包含 routes、services、dao、models 分层 |
| `modules/db.py` | 数据库连接与会话 |

```text
app.py
  ├─ 注册功能 Blueprint
  ├─ 初始化 Socket.IO 事件
  └─ 提供 /health

config.py → 环境变量、数据库、模型和数据路径
```

## 数据与设备

| 位置 | 内容 |
| --- | --- |
| MySQL `BUCTTA_DATABASE` | 举手与抬头率记录、学生及知识点掌握度等数据 |
| SQLite `CLASSROOM_DB_PATH` | 实时课堂指标与活动记录 |
| `FACE_DB_PATH` | 人脸库，图片命名为 `学号_姓名.jpg` |
| `YOLO_MODEL_PATH` | 姿态模型，容器默认 `/app/data/models/yolov8n-pose.pt` |
| `UPLOAD_DIR` | 上传文件 |

摄像头和视觉模型按功能调用初始化。离线部署需要预置模型，设备访问需要宿主权限。教师知识点手册和教案生成由 Java `/api/teacher-ai/**` 提供。

接口与事件见[Python 接口手册](../api/python_apidoc.md)，容器与持久化配置见[整体部署](../deploy.md)。
