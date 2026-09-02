# 📚 教师AI全能助手

> 基于 **DeepSeek API** 的智能教学工具，提供「知识点手册」和「智能教案」两大核心功能，助力教师高效备课。

---

## ✨ 功能概览

### 📖 知识点手册

输入学科、年级、章节，AI 自动生成 **结构化的知识点体系**：

- **结构化梳理** — 自动拆分章节知识体系，3-8 个核心知识点
- **难度分级** — 每个知识点标注 简单/中等/困难
- **重要性分类** — 区分 必学/拓展/选学
- **详细讲解** — 每个知识点配有 200-400 字精讲
- **核心概念** — 提取关键概念清单
- **常见误区** — 列出学生常见理解偏差，帮助教师提前预防

### 📝 智能教案

输入学科、年级、课题，AI 自动生成 **完整课堂教学方案**：

- **三维教学目标** — 知识与技能、过程与方法、情感态度价值观
- **教学重难点** — 明确标注重点 + 难点突破策略
- **完整教学过程** — 各环节教师活动、学生活动、设计意图
- **板书设计** — 结构化板书模板
- **分层作业** — 基础巩固 / 能力提升 / 挑战拓展
- **教学反思模板** — 课后反思框架
- **多种课型支持** — 新授课、复习课、习题课、实验课等
- **教学风格可选** — 探究式、启发式、合作学习、翻转课堂、PBL 等

---

## 🚀 快速开始

### 1. 环境要求

- **Node.js** >= 18.0.0
- **DeepSeek API Key** — [获取地址](https://platform.deepseek.com/api_keys)

### 2. 克隆项目

```bash
git clone <your-repo-url>
cd teacher-ai-assistant
```

### 3. 安装依赖

```bash
npm install
```

### 4. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入你的 DeepSeek API Key：

```env
DEEPSEEK_API_KEY=sk-your-api-key-here
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
DEEPSEEK_MODEL=deepseek-chat
PORT=3000
```

### 5. 启动服务

```bash
npm start
```

或者开发模式（文件变更自动重启）：

```bash
npm dev
```

### 6. 打开浏览器

访问 **http://localhost:3000**

---

## 📁 项目结构

```
teacher-ai-assistant/
├── public/                          # 前端静态文件
│   ├── index.html                   # 主页面
│   ├── css/
│   │   └── style.css                # 样式表
│   └── js/
│       ├── app.js                   # 主应用逻辑 + API 封装 + Toast
│       ├── knowledge-handbook.js    # 知识点手册模块
│       └── smart-lesson-plan.js     # 智能教案模块
├── src/                             # 后端服务
│   ├── server.js                    # Express 服务入口
│   ├── routes/
│   │   ├── knowledge.js             # 知识点手册 API 路由
│   │   └── lesson-plan.js           # 智能教案 API 路由
│   └── services/
│       └── deepseek.js              # DeepSeek API 封装层
├── package.json
├── .env.example                     # 环境变量模板
├── .gitignore
└── README.md
```

---

## 🔌 DeepSeek API 封装说明

项目中对 DeepSeek API 进行了 **完整的服务端封装**（`src/services/deepseek.js`），具备以下特性：

### 底层能力

| 特性 | 说明 |
|------|------|
| **自动重试** | 网络错误/429/5xx 自动重试，最多 3 次 |
| **指数退避** | 重试间隔指数增长 + 随机抖动，避免雪崩 |
| **超时控制** | 可配置请求超时（默认 60s） |
| **JSON 模式** | 开启 `response_format: json_object` 获取结构化输出 |
| **流式支持** | SSE 流式响应，适合长文本实时展示 |
| **错误分类** | 区分认证错误、限流、服务端错误、网络错误、超时 |
| **用量统计** | 每次调用返回 prompt/completion/total tokens |

### 业务封装

基于底层 API 封装了面向教学的业务方法：

```js
// 生成知识点手册
const data = await generateKnowledgePoints({
  subject: '数学',
  grade: '八年级',
  topic: '一次函数',
  focus: '函数图像的性质'  // 可选
});
// 返回: { chapterTitle, overview, knowledgePoints: [...] }

// 生成智能教案
const data = await generateLessonPlan({
  subject: '物理',
  grade: '初三',
  topic: '浮力',
  lessonType: '新授课',         // 可选
  duration: 45,                 // 可选
  teachingStyle: '探究式教学',   // 可选
  extraRequirements: '要有实验环节' // 可选
});
// 返回: { title, teachingObjectives, keyPoints, difficultPoints,
//         teachingProcess, boardDesign, homework, teachingReflection }
```

### 自定义调用

如果业务方法不满足需求，可直接使用底层 `chatCompletion`：

```js
const { chatCompletion } = require('./src/services/deepseek');

const result = await chatCompletion(
  [{ role: 'user', content: '你的提示词' }],
  { temperature: 0.7, maxTokens: 4096, jsonMode: false }
);

console.log(result.content);   // 模型回复文本
console.log(result.usage);     // Token 用量
```

---

## 🛠️ API 接口

### `GET /api/health`
健康检查，返回服务状态和 API Key 配置情况。

### `GET /api/knowledge/subjects`
获取学科、年级、课型等预设选项。

### `POST /api/knowledge/generate`
生成知识点手册。

```json
{
  "subject": "数学",
  "grade": "八年级",
  "topic": "一次函数",
  "focus": "函数图像的平移"
}
```

### `POST /api/lesson-plan/generate`
生成智能教案。

```json
{
  "subject": "物理",
  "grade": "初三",
  "topic": "浮力",
  "lessonType": "新授课",
  "duration": 45,
  "teachingStyle": "探究式教学",
  "extraRequirements": "加入分组实验环节"
}
```

### `POST /api/lesson-plan/generate-stream`
流式（SSE）生成教案，参数同上。

---

## 🎨 界面特点

- 🎯 双标签页设计，一目了然
- 📱 响应式布局，适配桌面/平板
- ✨ 清晰的知识点卡片展示
- 📋 教案结构化分区，便于阅读
- 📋 一键复制 / 导出文本文件
- 🎨 现代简洁的视觉风格
- ⚡ 实时状态指示器（API 连接状态）

---

## 📄 License

MIT

---

## 🙏 致谢

- 灵感来源：[老师帮/九章爱学](https://www.laoshibang.com)
- AI 驱动：[DeepSeek API](https://platform.deepseek.com)
