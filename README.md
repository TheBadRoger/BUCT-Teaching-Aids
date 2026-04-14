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

> 统计更新时间（UTC+8）：`2026-04-14 12:44:42`

### 核心统计

| 指标 | 数值 |
| :-- | --: |
| 代码总行数（非空行） | 22677 |
| Java 接口数 | 55 |
| Python 接口数 | 27 |
| Java 单元测试用例数 | 129 |
| Python 单元测试用例数 | 28 |

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
    "HTML (34.94%)" : 7923
    "Java (28.97%)" : 6569
    "CSS (12.51%)" : 2837
    "Python (9.05%)" : 2053
    "Markdown (8.15%)" : 1849
    "JavaScript (3.43%)" : 777
    "YAML (2.09%)" : 475
    "SQL (0.79%)" : 180
    "Shell (0.06%)" : 14
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
