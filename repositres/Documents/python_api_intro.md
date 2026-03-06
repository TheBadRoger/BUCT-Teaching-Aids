# Python 后端设计思路

## 项目背景

本 Python 后端是 BUCT 教学辅助系统（BUCT Teaching Aids）的一部分，与 Java SpringBoot 后端并行运行，负责承载计算密集型与实时交互类功能模块。之所以选用 Python，是因为其生态在计算机视觉（OpenCV、dlib、face-recognition、YOLO）、数据分析（NumPy、pandas）及实时通信（Flask-SocketIO）等领域拥有大量成熟开源库，这些库的 Java 替代品要么性能更差，要么接入成本更高。

---

## 整体架构

```
API/PythonAPI/
├── app.py                  # Flask 应用入口，注册所有 Blueprint 并启动 SocketIO
├── config.py               # 统一配置类，从 .env 读取全部环境变量
├── requirements.txt        # 依赖声明
├── .env.example            # 环境变量模板
├── modules/                # 各功能模块（Blueprint 形式）
│   ├── db.py               # SQLAlchemy 数据库连接池与会话管理
│   ├── pbl/                # PBL 项目式学习生成模块
│   ├── ai_classroom/       # AI 课堂实时监控模块（含 SocketIO 事件）
│   ├── face_hand_up/       # 人脸识别与举手率检测模块
│   ├── headup_rate/        # 抬头率检测模块（YOLO 姿态估计）
│   └── student_analysis/   # 学情分析模块
├── templates/              # 前端 HTML 模板（Flask render_template 渲染）
│   └── ai_classroom/       # AI 课堂前端页面
└── static/                 # 前端静态资源（CSS / JS / 图片）
    └── ai_classroom/css/   # AI 课堂样式表
```

### 设计原则

1. **单一入口**：所有模块共用同一个 Flask 应用实例（`app.py`），通过 Blueprint 机制隔离路由命名空间，既保持了代码的模块化，又避免了多进程部署的运维复杂度。

2. **配置集中**：`config.py` 中的 `Config` 类统一管理所有环境变量，各模块直接 `from config import Config` 即可取到配置，不需要各自解析 `.env`，减少重复代码。

3. **数据库统一**：`modules/db.py` 提供全局 SQLAlchemy 引擎和 `get_db_session()` 上下文管理器，各模块复用同一数据库连接池，避免连接数膨胀。主业务数据存储在 MySQL（`BUCTTA_DATABASE`），AI 课堂的课堂实时数据使用本地 SQLite，降低 MySQL 写入压力。

4. **前后端合并部署**：AI 课堂模块的前端页面（HTML/CSS）直接随后端一同发布。HTML 模板存放在 `templates/ai_classroom/`，通过 Flask 的 `render_template()` 渲染；CSS 静态文件存放在 `static/ai_classroom/css/`，由 Flask 内置静态文件服务（`/static/...`）提供。这样前端和后端共用同一端口，无需配置跨域或独立 Web 服务器即可访问完整功能界面。

5. **实时通信**：AI 课堂模块使用 Flask-SocketIO，通过 WebSocket 将课堂实时指标（学生注意力、情感状态、互动数据等）从后端推送到教师仪表盘，延迟远低于轮询方案。SocketIO 事件处理函数通过 `register_socketio_events(socketio)` 注入，与 Blueprint 路由解耦。

6. **延迟初始化**：人脸识别（`face-recognition` / dlib）和摄像头（OpenCV VideoCapture）在首次被调用时才初始化，而非应用启动时。这样即便服务器没有摄像头或不安装 dlib，其他模块也可以正常运行。

---

## 模块设计说明

### PBL 模块（`/api/pbl/`）

PBL（Project-Based Learning，项目式学习）模块提供课程设计方案的自动生成能力。当前实现基于内置的学科模板（Jinja2 字符串格式化），根据教师填写的年级学科、教材版本、单元信息等参数生成标准化的 Markdown 格式方案。模板库设计为字典结构，后续可按学科扩充。支持上传 `.txt` 课文文件作为素材输入。

### AI 课堂模块（`/api/classroom/`、WebSocket）

AI 课堂模块是本后端中实时性要求最高的模块。它通过 SocketIO 房间机制将每个课堂（`classroom_id`）的实时指标广播给对应的订阅者（教师仪表盘）。后端维护内存数据结构（`classrooms`、`students`字典）存储当前在线课堂与学生的状态快照，同时将历史指标异步写入 SQLite 数据库以供事后分析。情感分析和注意力评估当前使用统计模型实现，后续可替换为真实 AI 推理模型。前端页面（教师仪表盘、学生视图、首页）集成在后端内，通过 `/classroom/` 路由直接访问。

### 人脸识别与举手率模块（`/api/face_hand_up/`）

该模块使用 `face-recognition` 库（基于 dlib）进行人脸编码和比对，识别当前帧中的已知学生。举手检测使用 OpenCV 的轮廓检测算法，判断图像上半区域内是否存在满足尺寸条件的轮廓来推断举手动作。人脸库存放在 `face_database/人脸库/` 目录，文件命名为 `学号_姓名.jpg`；编码结果在首次加载后缓存到 `Config.FACE_ENCODINGS_CACHE`，避免重复计算。摄像头采用延迟初始化策略，仅在接口被调用时打开。

### 抬头率检测模块（`/api/headup_rate/`）

该模块基于 YOLOv8-pose 姿态估计模型（`ultralytics`）检测学生是否抬头。输入可以是前端传入的 base64 图片（后端计算），也可以是前端摄像头已计算好的抬头率数值（直接入库），通过 `data_type` 字段区分。检测结果写入 MySQL 的 `head_up_rates` 表，支持按课程和学生查询历史记录。YOLO 模型在 `detection_core.py` 中延迟加载，首次运行时由 `ultralytics` 自动下载。

### 学情分析模块（`/api/student/`、`/api/knowledge/`、`/api/report/`）

该模块承接学生学习行为数据的存储与查询，数据模型包括学生信息（`Student`）、知识点（`Knowledge`）、知识点掌握度（`KnowledgeMastery`）等。服务层（`services.py`）封装了数据聚合与报告生成逻辑，数据访问层（`dao.py`）负责具体的数据库查询。该模块原始实现基于 FastAPI，迁移到 Flask Blueprint 时接口路由和数据格式保持一致。

---

## 数据库设计

### MySQL（主数据库 `BUCTTA_DATABASE`）

| 表名 | 所属模块 | 说明 |
|------|----------|------|
| `hand_up_record` | face_hand_up | 举手率检测记录 |
| `head_up_rates` | headup_rate | 抬头率检测记录 |
| `students` | student_analysis | 学生基础信息 |
| `knowledge` | student_analysis | 知识点信息 |
| `knowledge_mastery` | student_analysis | 知识点掌握度记录 |
| `admin_users` | student_analysis | 后台管理员账户 |

### SQLite（`classroom_data.db`）

| 表名 | 所属模块 | 说明 |
|------|----------|------|
| `student_metrics` | ai_classroom | 学生实时课堂指标历史 |
| `classroom_activities` | ai_classroom | 课堂互动活动记录 |
| `learning_analytics` | ai_classroom | 每日学习分析汇总 |

---

## 技术选型说明

| 技术 | 选型理由 |
|------|----------|
| Flask + Flask-SocketIO | 轻量灵活，SocketIO 支持 WebSocket 实时通信，与 Python AI 生态无缝集成 |
| SQLAlchemy | ORM 层屏蔽 MySQL 方言差异，支持连接池，事务管理简洁 |
| face-recognition (dlib) | 开源人脸识别精度高，接口简单，无需训练即可识别已知人脸 |
| ultralytics (YOLOv8) | 最新 YOLO 系列姿态估计模型，精度与速度均衡，接口友好 |
| OpenCV | 计算机视觉标准库，摄像头采集、图像处理、轮廓检测 |
| pandas / NumPy | 数据分析与统计计算 |
| python-dotenv | 环境变量管理，生产/开发环境配置隔离 |
