# Python 后端接口参考（供前端参考）

说明：本文档列出 BUCT 教学辅助系统 Python 后端（Flask + Flask-SocketIO）已实现的全部接口。包含调用方法、路由、请求参数（类型/是否必需）、返回值、返回值字段类型，以及接口作用与注意事项。

---

## 通用说明

- **基础地址**：`http://<服务器IP>:5000`（默认端口，可通过 `.env` 中 `PORT` 修改）
- **成功响应结构**（JSON）：
  - `code` (int)：`200` 表示成功，`400` 参数错误，`404` 资源不存在，`500` 服务器内部错误
  - `msg` (string)：操作结果描述
  - `data` (object | array)：实际返回数据（成功时）
- **请求体格式**：含 JSON body 的接口需设置 `Content-Type: application/json`
- **WebSocket**：AI 课堂模块使用 Socket.IO，前端需引入 `socket.io.js` 客户端库
- 前端页面通过浏览器直接访问，无需额外 HTTP 请求

---

## 目录（按模块）

- [健康检查](#健康检查)
- [PBL 生成器模块](#pbl-生成器模块-apipbl)
- [AI 课堂模块（REST）](#ai-课堂模块-rest---apiclassroom)
- [AI 课堂模块（WebSocket）](#ai-课堂模块-websocket)
- [AI 课堂前端页面](#ai-课堂前端页面)
- [人脸识别与举手率模块](#人脸识别与举手率模块-apiface_hand_up)
- [抬头率检测模块](#抬头率检测模块-apiheadup_rate)
- [学情分析模块](#学情分析模块)

---

## 健康检查

### GET /health
- **描述**：检查后端服务是否正常运行。
- **返回**：
  ```json
  { "status": "ok", "message": "BUCT Teaching Aids – Unified Python Backend" }
  ```

---

## PBL 生成器模块 `/api/pbl`

### GET /api/pbl/
- **描述**：PBL 模块欢迎页，返回模块基本信息。
- **返回**：
  ```json
  { "module": "PBL生成器", "description": "...", "docs": "/api/pbl/templates" }
  ```

### GET /api/pbl/health
- **描述**：PBL 模块健康检查。
- **返回**：`{ "status": "PBL模块正常运行" }`

### GET /api/pbl/templates
- **描述**：获取当前支持的 PBL 模板学科列表。
- **返回**：
  ```json
  { "success": true, "templates": ["数学"], "description": "当前支持的教学科目模板" }
  ```

### POST /api/pbl/generate
- **描述**：根据提交的教学信息生成 PBL 设计方案（Markdown 格式）。
- **请求方式**：`POST`，`multipart/form-data`
- **表单参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `grade_subject` | string | 是 | 年级学科，如"五年级数学" |
  | `textbook_version` | string | 是 | 教材版本，如"人教版" |
  | `unit` | string | 是 | 单元信息，如"五年级上册第一单元" |
  | `requirements` | string | 否 | 其他特殊要求 |
  | `content` | string | 否 | 课文文字内容 |
  | `file` | file (.txt) | 否 | 上传课文文件，`.txt` 格式 |

- **返回**（成功）：
  ```json
  {
    "success": true,
    "message": "PBL生成成功！",
    "pbl_content": "# 五年级数学 PBL项目设计方案\n...",
    "preview_content": "# 五年级数学 PBL预览\n...",
    "download_link": "/api/pbl/download/五年级数学"
  }
  ```
- **返回**（失败）：`{ "success": false, "message": "生成失败：<错误信息>" }` HTTP 500

### GET /api/pbl/download/`<filename>`
- **描述**：下载生成的 PBL 文件（占位接口，当前返回提示信息）。
- **路径参数**：`filename` — PBL 方案文件名（下划线连接）
- **返回**：`{ "filename": "...", "message": "请在界面中查看完整内容。" }`

---

## AI 课堂模块 REST — `/api/classroom`

### POST /api/classroom/create
- **描述**：创建新课堂。
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `classroom_id` | string | 否 | 课堂ID，不填则自动生成 |
  | `teacher_name` | string | 否 | 教师姓名，默认 "Unknown Teacher" |
  | `subject` | string | 否 | 学科名称，默认 "General" |

- **返回**：
  ```json
  {
    "success": true,
    "classroom_id": "class_001",
    "message": "课堂创建成功"
  }
  ```

### GET /api/classroom/`<classroom_id>`/status
- **描述**：获取课堂当前状态（学生数量、活动状态等）。
- **路径参数**：`classroom_id` (string)
- **返回**：课堂状态对象，含 `id`、`teacher`、`subject`、`student_count`、`created_at` 等字段

### GET /api/classroom/`<classroom_id>`/analytics
- **描述**：获取课堂整体学习分析数据（注意力均值、情感分布、互动统计等）。
- **路径参数**：`classroom_id` (string)
- **返回**：
  ```json
  {
    "classroom_id": "class_001",
    "total_students": 30,
    "avg_attention": 0.75,
    "avg_engagement": 0.68,
    "emotion_distribution": { "positive": 18, "neutral": 9, "negative": 3 },
    "activity_count": 5,
    "timestamp": "2024-01-01T10:00:00"
  }
  ```

### GET /api/student/`<student_id>`/analytics
- **描述**：获取单个学生的课堂实时分析数据。
- **路径参数**：`student_id` (string)
- **返回**：含 `attention_score`、`engagement_score`、`focus_duration` 等字段的分析对象

### GET /api/analytics/student/`<student_id>`/detailed
- **描述**：获取学生详细学习分析（含趋势数据）。
- **路径参数**：`student_id` (string)
- **返回**：详细分析对象，含历史趋势、情感变化、注意力曲线等

### GET /api/analytics/classroom/`<classroom_id>`/summary
- **描述**：获取课堂综合摘要（学生分组统计、活跃度排行等）。
- **路径参数**：`classroom_id` (string)
- **返回**：含学生列表详情、课堂统计摘要的综合对象

### GET /api/analytics/student/`<student_id>`/history
- **描述**：从 SQLite 数据库中获取学生历史指标记录。
- **路径参数**：`student_id` (string)
- **查询参数**：`limit` (int, 可选, 默认 50) — 返回最近记录条数
- **返回**：`{ "student_id": "...", "records": [...], "count": 50 }`

---

## AI 课堂模块 WebSocket

连接地址：`ws://<服务器IP>:5000`，使用 Socket.IO 协议。

### 客户端 → 服务端事件

#### `join_classroom`
- **描述**：学生或教师加入课堂房间，开始接收实时推送。
- **数据**：
  ```json
  {
    "classroom_id": "class_001",
    "user_id": "student_001",
    "name": "张三",
    "role": "student"
  }
  ```

#### `leave_classroom`
- **描述**：离开课堂房间。
- **数据**：`{ "classroom_id": "class_001", "user_id": "student_001" }`

#### `start_activity`
- **描述**：教师发起课堂互动活动（提问等）。
- **数据**：`{ "classroom_id": "class_001", "activity_type": "question", "question": "..." }`

#### `submit_answer`
- **描述**：学生提交互动活动答案。
- **数据**：`{ "classroom_id": "class_001", "student_id": "S001", "activity_id": "act_001", "answer": "..." }`

#### `student_feedback`
- **描述**：学生发送课堂反馈（理解/疑问/快/慢）。
- **数据**：
  ```json
  {
    "classroom_id": "class_001",
    "student_id": "S001",
    "feedback_type": "understand"
  }
  ```
  - `feedback_type` 可选值：`understand`（理解了）、`confused`（有疑问）、`faster`（讲快点）、`slower`（讲慢点）

#### `update_student_metrics`
- **描述**：上报学生实时学习指标（由学生端 AI 分析后发送）。
- **数据**：
  ```json
  {
    "student_id": "S001",
    "metrics": {
      "attention_level": 0.85,
      "engagement_level": 0.72,
      "emotion": "focused",
      "gaze_focus": 0.9,
      "posture_engagement": 0.8
    },
    "source": "real_ai_analysis"
  }
  ```

### 服务端 → 客户端事件

#### `student_joined`
- **描述**：有新学生加入课堂时广播。
- **数据**：`{ "student_id": "S001", "name": "张三", "student_count": 15, "timestamp": "..." }`

#### `student_left`
- **描述**：学生离开课堂时广播。
- **数据**：`{ "student_id": "S001", "student_count": 14, "timestamp": "..." }`

#### `activity_started`
- **描述**：课堂活动开始时广播给所有学生。
- **数据**：`{ "activity_id": "...", "activity_type": "question", "question": "...", "timestamp": "..." }`

#### `activity_completed`
- **描述**：课堂活动结束时广播。
- **数据**：含活动摘要和响应统计

#### `student_metrics_updated`
- **描述**：某学生指标更新时向课堂教师广播。
- **数据**：`{ "student_id": "S001", "metrics": {...}, "analytics": {...}, "timestamp": "..." }`

#### `student_feedback_received`
- **描述**：学生反馈到达时向教师广播。
- **数据**：`{ "student_id": "S001", "student_name": "张三", "feedback_type": "confused", "feedback_message": "有疑问需要解答", "timestamp": "..." }`

---

## AI 课堂前端页面

以下页面由后端直接渲染，用浏览器访问即可。

| 路径 | 说明 |
|------|------|
| `GET /classroom/` | AI 课堂系统首页（教师/学生入口选择） |
| `GET /classroom/teacher-dashboard.html` | 教师仪表盘（实时课堂数据监控） |
| `GET /classroom/student-view.html` | 学生视图（摄像头监测、互动参与） |

> **注意**：前端页面中的 Socket.IO 连接地址使用 `window.location.origin`，会自动连接到当前页面所在的服务器，无需手动配置。

---

## 人脸识别与举手率模块 `/api/face_hand_up`

> **前置要求**：服务器需连接摄像头，并在 `face_database/人脸库/` 目录中存放人脸图片（命名格式：`学号_姓名.jpg`）。

### GET /api/face_hand_up/reload_face_db
- **描述**：重新加载人脸库（新增或删除人脸照片后调用以刷新缓存）。
- **返回**（成功）：`{ "code": 200, "msg": "人脸库重新加载成功" }`
- **返回**（失败）：`{ "code": 500, "msg": "<错误信息>" }` HTTP 500

### GET /api/face_hand_up/real_time_hand_up
- **描述**：调用摄像头采集当前帧，执行人脸识别和举手检测，返回实时举手率并写入数据库。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `class_name` | string | 否 | 班级名称，默认"默认班级" |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取实时举手率成功",
    "data": {
      "record_time": "2024-01-01 10:00:00",
      "class_name": "高材2304",
      "total_student": 28,
      "hand_up_student": 7,
      "hand_up_rate": 25.0,
      "hand_up_students": ["2024001_张三", "2024002_李四"],
      "recognized_students": ["2024001_张三", "2024002_李四", "..."]
    }
  }
  ```
- **返回**（摄像头不可用）：`{ "code": 500, "msg": "无法打开摄像头，请检查摄像头ID或连接" }` HTTP 500

### GET /api/face_hand_up/history_hand_up
- **描述**：查询历史举手率记录。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `class_name` | string | 否 | 班级名称，默认"默认班级" |
  | `limit` | int | 否 | 返回条数，默认 10，最大 100 |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取历史记录成功",
    "data": [
      {
        "id": 1,
        "record_time": "2024-01-01 10:00:00",
        "class_name": "高材2304",
        "total_student": 28,
        "hand_up_student": 7,
        "hand_up_rate": 25.0
      }
    ]
  }
  ```

### GET /api/face_hand_up/video_feed
- **描述**：获取摄像头实时视频流（MJPEG 格式），含人脸标注框和举手标注框。
- **响应类型**：`multipart/x-mixed-replace; boundary=frame`
- **用法**：在前端 `<img>` 标签的 `src` 属性中直接填写该 URL 即可播放实时视频流。
  ```html
  <img src="http://localhost:5000/api/face_hand_up/video_feed">
  ```

---

## 抬头率检测模块 `/api/headup_rate`

### POST /api/headup_rate/detect
- **描述**：检测并记录学生抬头率。支持两种输入方式：后端计算（传入图片）或直接入库（传入已计算结果）。
- **请求体**（JSON）：

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | `student_id` | int | 是 | 学号 |
  | `course_id` | string | 是 | 课程ID，如 "math_101" |
  | `course_name` | string | 是 | 课程名称，如 "高等数学（上）" |
  | `data_type` | string | 是 | 数据类型：`"image"` 或 `"video_frame"` |
  | `raw_data` | string | 否 | base64 编码的图片数据（后端计算时必填） |
  | `calculated_rate` | float | 否 | 前端已计算好的抬头率（0-100），直接存入数据库 |
  | `detection_device` | string | 否 | 检测设备名称，如 "教室摄像头-1" |
  | `remarks` | string | 否 | 备注，如 "上课10分钟检测" |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "检测成功",
    "data": {
      "student_id": 1001,
      "course_id": "math_101",
      "head_up_rate": 85.5,
      "detection_time": "2024-01-01 10:00:00"
    }
  }
  ```
- **返回**（参数错误）：`{ "code": 400, "msg": "缺少必填字段：student_id, course_id" }` HTTP 400

### GET /api/headup_rate/history
- **描述**：获取抬头率历史记录（可按课程过滤）。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `course_id` | string | 否 | 按课程ID过滤 |
  | `limit` | int | 否 | 返回条数，默认 20，最大 100 |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取成功",
    "data": [
      {
        "id": 1,
        "student_id": 1001,
        "course_id": "math_101",
        "course_name": "高等数学（上）",
        "detection_time": "2024-01-01 10:00:00",
        "head_up_rate": 85.5,
        "detection_device": "教室摄像头-1"
      }
    ]
  }
  ```

### GET /api/headup_rate/student/`<student_id>`
- **描述**：获取指定学生的抬头率历史记录。
- **路径参数**：`student_id` (int) — 学号
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `course_id` | string | 否 | 按课程ID过滤 |
  | `limit` | int | 否 | 返回条数，默认 20，最大 100 |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取成功",
    "student_id": 1001,
    "data": [
      { "id": 1, "course_id": "math_101", "course_name": "高等数学（上）", "detection_time": "...", "head_up_rate": 85.5 }
    ]
  }
  ```

---

## 学情分析模块

### GET /api/student/class_list
- **描述**：获取指定班级的学生列表（含学习状态摘要）。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `class_name` | string | 是 | 班级名称，如 "高材2304" |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取成功",
    "data": [
      {
        "student_id": 2024001,
        "name": "张三",
        "class_name": "高材2304",
        "avg_mastery": 0.75,
        "knowledge_count": 12
      }
    ]
  }
  ```
- **返回**（缺少参数）：`{ "code": 400, "msg": "缺少参数 class_name" }` HTTP 400

### GET /api/knowledge/class_list
- **描述**：获取指定班级的知识点列表（含各知识点掌握度统计）。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `class_name` | string | 是 | 班级名称 |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取成功",
    "data": [
      {
        "knowledge_id": 101,
        "knowledge_name": "导数与微分",
        "avg_mastery": 0.68,
        "student_count": 28
      }
    ]
  }
  ```

### GET /api/report/student
- **描述**：获取指定学生的完整学情报告（知识点掌握详情、学习趋势等）。
- **查询参数**：

  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | `student_id` | int | 是 | 学号 |

- **返回**（成功）：
  ```json
  {
    "code": 200,
    "msg": "获取成功",
    "data": {
      "student": { "student_id": 2024001, "name": "张三", "class_name": "高材2304" },
      "knowledge_mastery": [
        { "knowledge_name": "导数与微分", "mastery_level": 0.72, "last_updated": "2024-01-01" }
      ],
      "summary": { "avg_mastery": 0.75, "strong_areas": [...], "weak_areas": [...] }
    }
  }
  ```
- **返回**（学生不存在）：`{ "code": 404, "msg": "学生不存在" }` HTTP 404
- **返回**（缺少参数）：`{ "code": 400, "msg": "缺少参数 student_id" }` HTTP 400
