#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[1]
README_PATH = ROOT_DIR / "README.md"
GENERATED_DIR = ROOT_DIR / "script" / "generated"
STATS_JSON_PATH = GENERATED_DIR / "stats.json"
STATS_MD_PATH = GENERATED_DIR / "README_STATS.md"

EXCLUDED_DIRS = {
    ".git",
    ".idea",
    ".vscode",
    "__pycache__",
    "node_modules",
    "target",
    "build",
    "dist",
    ".venv",
    "venv",
    ".mypy_cache",
    ".pytest_cache",
}

LANGUAGE_BY_SUFFIX = {
    ".java": "Java",
    ".py": "Python",
    ".js": "JavaScript",
    ".ts": "TypeScript",
    ".css": "CSS",
    ".html": "HTML",
    ".sql": "SQL",
    ".yml": "YAML",
    ".yaml": "YAML",
    ".sh": "Shell",
    ".md": "Markdown",
}

JAVA_ENDPOINT_PATTERN = re.compile(
    r"@\s*(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\b"
)
PYTHON_ENDPOINT_PATTERN = re.compile(r"@\s*[\w.]*route\s*\(")

SECTION_START = "<!-- STATS_SECTION_START -->"
SECTION_END = "<!-- STATS_SECTION_END -->"


def _is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)


def _count_non_empty_lines(file_path: Path) -> int:
    with file_path.open("r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for line in f if line.strip())


def collect_stats() -> dict:
    language_lines: dict[str, int] = defaultdict(int)
    java_endpoints = 0
    python_endpoints = 0

    for file_path in ROOT_DIR.rglob("*"):
        if not file_path.is_file() or _is_excluded(file_path):
            continue

        language = LANGUAGE_BY_SUFFIX.get(file_path.suffix.lower())
        if not language:
            continue

        lines = _count_non_empty_lines(file_path)
        language_lines[language] += lines

        if file_path.suffix.lower() == ".java":
            content = file_path.read_text(encoding="utf-8", errors="ignore")
            java_endpoints += len(JAVA_ENDPOINT_PATTERN.findall(content))
        elif file_path.suffix.lower() == ".py":
            content = file_path.read_text(encoding="utf-8", errors="ignore")
            python_endpoints += len(PYTHON_ENDPOINT_PATTERN.findall(content))

    sorted_language_lines = dict(
        sorted(language_lines.items(), key=lambda item: item[1], reverse=True)
    )
    total_lines = sum(sorted_language_lines.values())
    top_language = next(iter(sorted_language_lines.keys()), "N/A")

    return {
        "updated_at_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "total_lines": total_lines,
        "language_count": len(sorted_language_lines),
        "top_language": top_language,
        "java_endpoint_count": java_endpoints,
        "python_endpoint_count": python_endpoints,
        "language_lines": sorted_language_lines,
    }


def build_stats_markdown(stats: dict) -> str:
    total = stats["total_lines"] or 1
    rows = []
    for language, lines in stats["language_lines"].items():
        percent = lines / total * 100
        rows.append(f"| {language} | {lines} | {percent:.2f}% |")

    pie_rows = []
    palette = ["#5B8FF9", "#5AD8A6", "#5D7092", "#F6BD16", "#E8684A", "#6DC8EC", "#9270CA"]
    for idx, (language, lines) in enumerate(stats["language_lines"].items()):
        percent = lines / total * 100
        color = palette[idx % len(palette)]
        pie_rows.append(f'    "{language} ({percent:.2f}%)" : {lines}')

    return "\n".join(
        [
            "## 项目统计",
            "",
            f"> 统计更新时间（UTC）：`{stats['updated_at_utc']}`",
            "",
            "### 核心统计",
            "",
            "| 指标 | 数值 |",
            "| :-- | --: |",
            f"| 代码总行数（非空行） | {stats['total_lines']} |",
            f"| 语言数量 | {stats['language_count']} |",
            f"| 使用最多的语言 | {stats['top_language']} |",
            f"| Java 接口数 | {stats['java_endpoint_count']} |",
            f"| Python 接口数 | {stats['python_endpoint_count']} |",
            "",
            "### 各语言代码行数",
            "",
            "| 语言 | 行数 | 占比 |",
            "| :-- | --: | --: |",
            *rows,
            "",
            "### 语言占比图",
            "",
            "```mermaid",
            "%%{init: {'theme':'base','themeVariables': {",
            "  'fontFamily': 'JetBrains Mono, Fira Code, Consolas, monospace',",
            "  'pieStrokeColor': 'transparent',",
            "  'pieStrokeWidth': '0px',",
            "  'pieOuterStrokeWidth': '0px'",
            "}}}%%",
            "pie showData",
            *pie_rows,
            "```",
            "",
        ]
    )


def update_readme(stats_md: str) -> None:
    readme_content = README_PATH.read_text(encoding="utf-8")
    replacement = f"{SECTION_START}\n{stats_md}{SECTION_END}"

    if SECTION_START in readme_content and SECTION_END in readme_content:
        pattern = re.compile(
            rf"{re.escape(SECTION_START)}.*?{re.escape(SECTION_END)}",
            re.DOTALL,
        )
        readme_content = pattern.sub(replacement, readme_content)
    else:
        readme_content = readme_content.rstrip() + "\n\n" + replacement + "\n"

    README_PATH.write_text(readme_content, encoding="utf-8")


def main() -> None:
    stats = collect_stats()
    stats_md = build_stats_markdown(stats)

    GENERATED_DIR.mkdir(parents=True, exist_ok=True)
    STATS_JSON_PATH.write_text(
        json.dumps(stats, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    STATS_MD_PATH.write_text(stats_md, encoding="utf-8")
    update_readme(stats_md)


if __name__ == "__main__":
    main()
