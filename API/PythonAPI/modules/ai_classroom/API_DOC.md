# AI 课堂实时互动与反馈系统 — API 文档

## 系统架构

```
modules/ai_classroom/
├── routes.py                      # Flask 蓝图 + SocketIO 事件
├── core/
│   ├── emotion_detector.py        # 情感计算（face_recognition landmarks + 启发式规则）
│   ├── attention_analyzer.py      # 注意力/参与度分析（多信号加权融合）
│   ├── speech_recognizer.py       # 语音识别（SpeechRecognition + 关键词检测）
│   └── report_generator.py        # 课堂/学生报告生成（pandas 聚合 + 规则建议）
```

## 轻量化技术路线

| 功能 | 当前方案 | 后期升级 |
|------|----------|----------|
| 情感检测 | face_recognition 68点 landmarks → 几何特征 → 启发式分类 | DeepFace / FER 深度模型 |
| 注意力监测 | 抬头率 + 人脸检测 + 举手 + 情感效价 + 互动频率 加权融合 | + Gaze estimation + 姿态估计 |
| 语音识别 | SpeechRecognition + Google 免费 API + 关键词检测 | Whisper 本地模型 |
| 互动功能 | SocketIO 实时通信 | + WebRTC 音视频流 |
| 报告生成 | pandas 聚合分析 + 规则建议 | + LLM 智能叙事生成 |

---

## HTTP API

### 课堂管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/classroom/create` | 创建课堂 |
| GET | `/api/classroom/<classroom_id>/status` | 获取课堂状态 |
| GET | `/api/classroom/<classroom_id>/analytics` | 获取课堂实时分析 |
| GET | `/api/analytics/classroom/<classroom_id>/summary` | 获取课堂详细总结 |

### 计算机视觉分析

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/classroom/<classroom_id>/analyze_frame` | 分析单帧图片：情感 + 注意力 |
| POST | `/api/classroom/<classroom_id>/snapshot` | 批量分析多学生帧数据 |

**analyze_frame 请求体：**
```json
{
    "student_id": "s001",
    "frame": "<base64编码图片>",
    "head_up_rate": 85.0,
    "hand_up": false
}
```

**snapshot 请求体：**
```json
{
    "students": [
        {"student_id": "s001", "frame": "<base64>", "head_up_rate": 85.0, "hand_up": false},
        {"student_id": "s002", "frame": "<base64>", "head_up_rate": 70.0, "hand_up": true}
    ]
}
```

### 语音识别

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/classroom/<classroom_id>/speech_recognize` | 语音转文字 + 关键词检测 |

**请求体：**
```json
{
    "student_id": "s001",
    "audio": "<base64编码音频(wav/aiff/flac)>",
    "language": "zh-CN"
}
```

### 学生分析

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/student/<student_id>/analytics` | 获取学生实时分析 |
| GET | `/api/analytics/student/<student_id>/detailed` | 获取学生详细分析 |
| GET | `/api/analytics/student/<student_id>/history` | 获取学生历史数据 |
| GET | `/api/student/<student_id>/report` | 生成学生个人报告 |

### 课堂报告

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/classroom/<classroom_id>/report` | 生成课堂互动报告 |
| GET | `/api/classroom/<classroom_id>/report?save=true` | 生成并保存报告 |

### 分组讨论管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/classroom/<classroom_id>/discussions` | 列出所有讨论 |
| GET | `/api/classroom/<classroom_id>/discussions/<discussion_id>` | 获取讨论详情 |

### 实时测评管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/classroom/<classroom_id>/quizzes` | 列出所有测评 |
| GET | `/api/classroom/<classroom_id>/quizzes/<quiz_id>` | 获取测评详情 |

---

## SocketIO 事件

### 基础事件

| 事件 | 方向 | 说明 |
|------|------|------|
| `connect` | C→S | 客户端连接 |
| `disconnect` | C→S | 客户端断开 |
| `join_classroom` | C→S | 学生加入课堂 |
| `teacher_join` | C→S | 教师加入课堂 |
| `classroom_update` | S→C | 课堂状态更新 |
| `student_joined` | S→C | 学生加入通知 |

### 在线抢答

| 事件 | 方向 | 说明 |
|------|------|------|
| `start_quick_response` | C→S | 教师发起抢答 |
| `student_response` | C→S | 学生回答 |
| `new_question` | S→C | 推送新问题 |
| `response_received` | S→C | 推送回答结果 |

### 学生反馈

| 事件 | 方向 | 说明 |
|------|------|------|
| `student_feedback` | C→S | 学生发送反馈 |
| `student_feedback_received` | S→C | 推送学生反馈 |

### 指标更新

| 事件 | 方向 | 说明 |
|------|------|------|
| `update_student_metrics` | C→S | 更新学生指标 |
| `student_metrics_updated` | S→C | 推送指标更新 |

### 分组讨论

| 事件 | 方向 | 说明 |
|------|------|------|
| `start_group_discussion` | C→S | 启动分组讨论 |
| `group_discussion_message` | C→S | 发送小组消息 |
| `end_group_discussion` | C→S | 结束讨论 |
| `group_discussion_started` | S→C | 讨论已启动 |
| `group_message_received` | S→C | 推送小组消息 |
| `group_discussion_ended` | S→C | 讨论已结束 |

**start_group_discussion 数据：**
```json
{
    "classroom_id": "class_001",
    "topic": "讨论人工智能在教育中的应用",
    "num_groups": 4
}
```

### 实时测评

| 事件 | 方向 | 说明 |
|------|------|------|
| `start_quiz` | C→S | 启动测评 |
| `submit_quiz_answer` | C→S | 提交答案 |
| `end_quiz` | C→S | 结束测评 |
| `quiz_started` | S→C | 测评已启动 |
| `quiz_answer_received` | S→C | 推送答题情况 |
| `quiz_ended` | S→C | 测评已结束 |

**start_quiz 数据：**
```json
{
    "classroom_id": "class_001",
    "questions": [
        {
            "id": "q1",
            "question": "1+1等于几？",
            "options": ["1", "2", "3", "4"],
            "correct_answer": "2"
        }
    ]
}
```

---

## 情感类别

| 分类 | 情感标签 |
|------|----------|
| positive | focused, happy, engaged, excited |
| neutral | neutral, calm, thinking |
| negative | confused, bored, frustrated, distracted |

## 注意力分析信号权重

| 信号 | 注意力权重 | 参与度权重 |
|------|-----------|-----------|
| head_up (抬头率) | 0.30 | 0.20 |
| face_detected (人脸检测) | 0.15 | 0.10 |
| emotion_valence (情感效价) | 0.25 | 0.30 |
| hand_up (举手) | 0.10 | 0.20 |
| interaction (互动频率) | 0.20 | 0.20 |

