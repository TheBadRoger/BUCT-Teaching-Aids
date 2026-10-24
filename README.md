# 北化教学辅助系统

<p align="center">
  <strong>项目信息</strong><br />
  <a href="./LICENSE"><img alt="License" src="https://img.shields.io/github/license/TheBadRoger/BUCT-Teaching-Aids?style=flat-square&labelColor=0f172a&color=64748b" /></a>
  <a href="./README.md"><img alt="Project Type" src="https://img.shields.io/badge/Project%20Type-Faculty--level%20Project-6366f1?style=flat-square&labelColor=0f172a" /></a>
  <a href="https://openjdk.org/projects/jdk/25/"><img alt="Java" src="https://img.shields.io/badge/Java-25-0ea5e9?logo=openjdk&logoColor=white&style=flat-square&labelColor=0f172a" /></a>
  <a href="https://www.python.org/downloads/release/python-3110/"><img alt="Python" src="https://img.shields.io/badge/Python-3.11-3b82f6?logo=python&logoColor=white&style=flat-square&labelColor=0f172a" /></a>
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids/commits/main"><img alt="Last Commit" src="https://img.shields.io/github/last-commit/TheBadRoger/BUCT-Teaching-Aids?style=flat-square&labelColor=0f172a&color=06b6d4" /></a>
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids"><img alt="Repo Size" src="https://img.shields.io/github/repo-size/TheBadRoger/BUCT-Teaching-Aids?style=flat-square&labelColor=0f172a&color=14b8a6" /></a>
  <br /><br />
  <strong>代码统计</strong><br />
  <a href="#项目统计"><img alt="Total Lines" src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fstats.json&query=%24.total_lines&label=Total%20Lines&style=flat-square&labelColor=0f172a&color=22c55e" /></a>
  <a href="#项目统计"><img alt="Language Count" src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fstats.json&query=%24.language_count&label=Language%20Count&style=flat-square&labelColor=0f172a&color=0ea5e9" /></a>
  <a href="#项目统计"><img alt="Top Language" src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fstats.json&query=%24.top_language&label=Top%20Language&style=flat-square&labelColor=0f172a&color=a855f7" /></a>
  <br /><br />
  <strong>CI状态</strong><br />
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids/actions/workflows/java-build.yml"><img alt="Java Build" src="https://img.shields.io/github/actions/workflow/status/TheBadRoger/BUCT-Teaching-Aids/java-build.yml?branch=main&label=Java%20Build&style=flat-square&labelColor=0f172a&color=22c55e" /></a>
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids/actions/workflows/java-tests.yml"><img alt="Java Unit Test" src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fjava-unit-test-badge.json&style=flat-square" /></a>
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids/actions/workflows/python-build.yml"><img alt="Python Build" src="https://img.shields.io/github/actions/workflow/status/TheBadRoger/BUCT-Teaching-Aids/python-build.yml?branch=main&label=Python%20Build&style=flat-square&labelColor=0f172a&color=22c55e" /></a>
</p>

统一 Vue 前端的 AI 教学平台，提供课程学习、教学管理、教师 AI 工具和课堂分析。页面由 Vue 应用和后端静态页面组成。

## 架构与目录

| 目录 | 职责与技术栈 |
| --- | --- |
| `apps/web/` | 唯一前端：Vue 3、TypeScript、Vite、Pinia、Element Plus |
| `apps/api/java/` | 主业务与教师 AI：Java 25、Spring Boot 4；同时托管前端编译产物 |
| `apps/api/python/` | Flask / Socket.IO：课堂互动、PBL、视觉识别和学情分析 |
| `database/` | `init/` 为容器初始化脚本，`manual/` 为按需执行的 SQL |
| `docs/` | 接口、架构与整体部署说明 |
| `scripts/` | 仓库工具及生成的统计数据 |

Java 与 Python 使用 MySQL，Java 使用 Redis；Python 课堂模块另有 SQLite。教师 AI 由 Java 提供接口。

## 部署与开发

整套项目通过根目录 `compose.yaml` 和 `Dockerfile` 部署，包含 Java、Python、MySQL、Redis 四个服务：

```bash
cp .env.example .env
# 填写必填密码；教师 AI 按需填写 DEEPSEEK_API_KEY
# 启动前可运行 docker compose config --quiet 检查配置
docker compose up -d --build --remove-orphans --wait --wait-timeout 300
```

默认学习入口：`http://127.0.0.1/js/app/index.html#/learn`；管理登录：`http://127.0.0.1/js/app/index.html#/login`。端口、数据卷和更新方法见[Docker 部署](docs/deploy.md)。Vue 在 Docker 构建时编译并打入 Java JAR，编译产物不提交。

前端开发入口见[前端说明](apps/web/README.md)。教师 AI 使用 `DEEPSEEK_API_KEY`；AI 批改和背诵手册使用 `POLYMAS_AUTH_KEY`，密钥只放在服务端。

## 文档导航

| 主题 | 文档 |
| --- | --- |
| 后端架构 | [Java 后端](docs/architecture/java_api_intro.md) · [Python 后端](docs/architecture/python_api_intro.md) |
| 整体部署 | [Docker 配置、更新与数据维护](docs/deploy.md) |
| 前端 | [开发与构建](apps/web/README.md) · [管理页面及数据来源](apps/web/ADMIN-PAGES.md) |
| 教师 AI | [接口与流式协议](docs/api/java_apidoc.md#教师-ai-apiteacher-ai) · [模型配置](docs/deploy.md#构建与模型配置) |
| 接口手册 | [Java](docs/api/java_apidoc.md) · [Python](docs/api/python_apidoc.md) |
| 页面接口映射 | [静态管理后台](docs/api/admin_frontend_api_map.md) · [学生前台](docs/api/frontend_student_api_map.md) |

资讯仅支持本机草稿，课程评价使用标注的演示数据；邮件、日历和直播等页面的接入范围见管理页面说明。页面映射描述当前 Vue 页面使用的接口。

Agent 协作、实现与文档规范见 [AGENTS.md](AGENTS.md)，与正式使用文档分开维护。

<!-- STATS_SECTION_START -->
## 项目统计

> 统计更新时间（UTC+8）：`2026-10-24 14:31:19`

### 核心统计

| 指标 | 数值 |
| :-- | --: |
| 代码总行数（非空行） | 62759 |
| Java 接口数 | 148 |
| Python 接口数 | 36 |
| Java 单元测试用例数 | 290 |
| Python 单元测试用例数 | 48 |

### 语言占比图

```mermaid
%%{init: {'theme':'base','themeVariables': {
  'fontFamily': 'Fira Code, JetBrains Mono, Source Code Pro, Cascadia Code, Menlo, Consolas, monospace',
  'pieStrokeColor': 'transparent',
  'pieStrokeWidth': '0px',
  'pieOuterStrokeWidth': '0px',
  'pie1': '#FF4D6D',
  'pie2': '#FF8E3C',
  'pie3': '#FFD60A',
  'pie4': '#22C55E',
  'pie5': '#00D1FF',
  'pie6': '#4F46E5',
  'pie7': '#D946EF',
  'pie8': '#14B8A6',
  'pie9': '#F97316',
  'pie10': '#A855F7'
}}}%%
pie showData
    "Java (26.11%)" : 16387
    "Markdown (21.36%)" : 13405
    "HTML (14.28%)" : 8963
    "JavaScript (13.22%)" : 8295
    "CSS (10.76%)" : 6753
    "Python (5.49%)" : 3447
    "Vue (4.64%)" : 2912
    "TypeScript (2.78%)" : 1745
    "YAML (0.93%)" : 583
    "SQL (0.33%)" : 209
    "Shell (0.10%)" : 60
```
<!-- STATS_SECTION_END -->
