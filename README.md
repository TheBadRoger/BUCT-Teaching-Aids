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

<!-- STATS_SECTION_START -->
## 项目统计

> 统计更新时间（UTC+8）：`2026-10-24 13:46:17`

### 核心统计

| 指标 | 数值 |
| :-- | --: |
| 代码总行数（非空行） | 63507 |
| Java 接口数 | 142 |
| Python 接口数 | 36 |
| Java 单元测试用例数 | 278 |
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
    "Java (25.23%)" : 16020
    "Markdown (22.05%)" : 14001
    "JavaScript (14.33%)" : 9099
    "HTML (14.11%)" : 8963
    "CSS (10.63%)" : 6753
    "Python (5.43%)" : 3447
    "Vue (4.29%)" : 2723
    "TypeScript (2.54%)" : 1614
    "YAML (0.97%)" : 618
    "SQL (0.33%)" : 209
    "Shell (0.09%)" : 60
```
<!-- STATS_SECTION_END -->

## 开发框架简介

- 管理前端：Vue 3 + TypeScript + Vite + Pinia + Element Plus；保留随后端部署的 HTML/CSS/JavaScript 页面

- 后端：Java SpringBoot + Python Flask

- 数据库：MySQL - 持久化，Redis - 高并发情况下的中间缓存

## 工作进度

**后台管理系统：完成前后端分离**

- 已实现课程相关的大多数功能
- 实现AI智能批改实验报告，前后端均已实现。
- 打通教师/学生/课程后台管理页（列表、查看、新增、编辑、删除、导出）的前后端链路，详见 [admin_frontend_api_map.md](docs/api/admin_frontend_api_map.md)
- 新增 AI 背诵手册：上传 `.txt/.docx/.pdf` 学习资料，自动生成知识归纳、记忆口诀、必背要点、自测题（复用 polymas AI，前端见 `static/recitation/`）。

> 其余项目组的进度，以后就在这里加就行

## 项目文件结构

```text
apps/
  api/
    java/                  # Java Spring Boot 后端
    python/                # Python Flask 后端
    teacher-ai/            # 教师 AI Express 接口服务
  web/                     # 唯一 Vue 前端：学习端、管理端、AI 教学工具
Dockerfile                 # 全项目镜像构建（多阶段/多目标）
compose.yaml               # 唯一 Docker 整体部署入口
.env.example               # 部署变量模板（复制为 .env）
database/
  init/                    # 容器数据库初始化脚本
  manual/                  # 独立 SQL，按需人工执行
docs/
  api/                     # 接口文档及页面接口映射
  architecture/            # 后端架构说明
  deployment/              # Linux、Windows、Docker 部署说明
scripts/                   # 仓库工具及生成的统计数据
```

Java 的 `src/main/resources/static/` 和 Python 的 `static/`、`templates/` 保持在各自应用内，保留现有资源加载方式。各应用使用原有构建和启动命令，不引入新的工作区工具。旧路径对照和运行入口见 [目录说明](docs/project-layout.md)，全部文档见 [文档导航](docs/README.md)。

## AI 配置说明（polymas）

AI 批改、背诵手册等功能共用一个第三方 AI 平台 **polymas**。相关配置在
`apps/api/java/src/main/resources/application.properties` 的 `app.ai.polymas.*` 段，可通过环境变量 `POLYMAS_AUTH_KEY` 配置。

## 文档

|                   后端接口文档                   |                              后端部署方法                              |                     后端项目介绍                     |
| :----------------------------------------------: | :--------------------------------------------------------------------: | :--------------------------------------------------: |
|   [Java](docs/api/java_apidoc.md)   |      [Windows - Java](docs/deployment/java_deploy_win.md)       | [Java](docs/architecture/java_api_intro.md) |
| [Python](docs/api/python_apidoc.md) |   [Ubuntu Linux - Java](docs/deployment/java_deploy_linux.md)   | [Python](docs/architecture/python_api_intro.md) |
|                        -                         | [Ubuntu Linux - Python](docs/deployment/python_deploy_linux.md) | - |
|                        -                         |           [Docker](docs/deployment/docker_deploy.md)            | - |
