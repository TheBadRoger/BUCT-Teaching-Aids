# 北化教学辅助系统

<p align="center">
  <a href="./LICENSE"><img alt="License" src="https://img.shields.io/badge/License-MIT-yellow.svg" /></a>
  <a href="./README.md"><img alt="Project Type" src="https://img.shields.io/badge/Project%20Type-Faculty--level%20Project-7A3E65" /></a>
  <img alt="Java" src="https://img.shields.io/badge/Java-25-437291?logo=openjdk&logoColor=white" />
  <img alt="Python" src="https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white" />
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids/actions/workflows/ci.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/TheBadRoger/BUCT-Teaching-Aids/ci.yml?branch=main&label=Build" /></a>
  <a href="https://github.com/TheBadRoger/BUCT-Teaching-Aids/actions/workflows/ci.yml"><img alt="Unit Tests" src="https://img.shields.io/github/actions/workflow/status/TheBadRoger/BUCT-Teaching-Aids/ci.yml?branch=main&label=Unit%20Tests" /></a>
  <br /><br />
  <img alt="Last Commit" src="https://img.shields.io/github/last-commit/TheBadRoger/BUCT-Teaching-Aids?label=Last%20Commit" />
  <img alt="Repo Size" src="https://img.shields.io/github/repo-size/TheBadRoger/BUCT-Teaching-Aids?label=Repo%20Size" />
  <img alt="Total Lines" src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fstats.json&query=%24.total_lines&label=Total%20Lines&color=3B82F6" />
  <img alt="Language Count" src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fstats.json&query=%24.language_count&label=Language%20Count&color=10B981" />
  <img alt="Top Language" src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fraw.githubusercontent.com%2FTheBadRoger%2FBUCT-Teaching-Aids%2Fmain%2Fscripts%2Fgenerated%2Fstats.json&query=%24.top_language&label=Top%20Language&color=8B5CF6" />
</p>

<!-- STATS_SECTION_START -->

## 项目统计

> 统计更新时间（UTC）：`2026-04-01T13:30:57Z`

### 核心统计

| 指标                 |  数值 |
| :------------------- | ----: |
| 代码总行数（非空行） | 21123 |
| Java 接口数          |    53 |
| Python 接口数        |    27 |

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
    "HTML (37.09%)" : 7835
    "Java (26.68%)" : 5635
    "CSS (13.43%)" : 2837
    "Markdown (9.61%)" : 2030
    "Python (7.48%)" : 1581
    "JavaScript (3.17%)" : 670
    "YAML (1.69%)" : 356
    "SQL (0.82%)" : 174
    "Shell (0.02%)" : 5
```

<!-- STATS_SECTION_END -->

## 开发框架简介

- 前端：经典HTML+js+css

- 后端：Java SpringBoot + Python Flask

- 数据库：MySQL - 持久化，Redis - 高并发情况下的中间缓存

## 工作进度

**后台管理系统：完成前后端分离**

- 已实现课程相关的大多数功能
- 实现AI智能批改实验报告，前后端均已实现。
  > 其余项目组的进度，以后就在这里加就行

## 项目文件结构

由于我们有Java和Python两个后端，这两个后端都支持 **前后端合并部署**，所以两个部分的前端要分开

- **_API/JavaAPI/src/_** - Java后端文件夹
- **_API/JavaAPI/src/resources/static/_** - 与Java后端合并部署的前端文件夹
- **_API/PythonAPI/_** - Python后端文件夹
- **_API/PythonAPI/static_** - 与Python后端合并部署的前端css，js文件夹
- **_API/PythonAPI/templates_** - 与Python后端合并部署的前端html网页文件夹
- **_documents_** - 仓库文档相关资源文件夹

## 文档

|                   后端接口文档                   |                              后端部署方法                              |                     后端项目介绍                     |
| :----------------------------------------------: | :--------------------------------------------------------------------: | :--------------------------------------------------: |
|   [Java](documents/java_apidoc.md)   |      [Windows - Java](documents/java_deploy_win.md)       | [Java](documents/java_api_intro.md) |
| [Python](documents/python_apidoc.md) |   [Ubuntu Linux - Java](documents/java_deploy_linux.md)   | [Python](documents/python_api_intro.md) |
|                        -                         | [Ubuntu Linux - Python](documents/python_deploy_linux.md) | - |
|                        -                         |           [Docker](documents/docker_deploy.md)            | - |
|                        -                         |           [.env 配置方法](documents/env_config.md)            | - |
