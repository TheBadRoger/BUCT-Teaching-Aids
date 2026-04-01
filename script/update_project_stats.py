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
    pie_rows = []
    for language, lines in stats["language_lines"].items():
        percent = lines / total * 100
        pie_rows.append(f'    "{language} ({percent:.2f}%)" : {lines}')

    return "\n".join(
        [
            "## Project Statistics",
            "",
            f"> Updated (UTC): `{stats['updated_at_utc']}`",
            "",
            "### Core Metrics",
            "",
            "| Metric | Value |",
            "| :-- | --: |",
            f"| Total Lines (Non-empty) | {stats['total_lines']} |",
            f"| Java API Endpoints | {stats['java_endpoint_count']} |",
            f"| Python API Endpoints | {stats['python_endpoint_count']} |",
            "",
            "### Language Distribution",
            "",
            "```mermaid",
            "%%{init: {'theme':'base','themeVariables': {",
            "  'fontFamily': 'Fira Code, JetBrains Mono, Source Code Pro, Cascadia Code, Menlo, Consolas, monospace',",
            "  'pieStrokeColor': 'transparent',",
            "  'pieStrokeWidth': '0px',",
            "  'pieOuterStrokeWidth': '0px',",
            "  'pie1': '#FF4D6D',",
            "  'pie2': '#FF8E3C',",
            "  'pie3': '#FFD60A',",
            "  'pie4': '#22C55E',",
            "  'pie5': '#00D1FF',",
            "  'pie6': '#4F46E5',",
            "  'pie7': '#D946EF',",
            "  'pie8': '#14B8A6',",
            "  'pie9': '#F97316',",
            "  'pie10': '#A855F7'",
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
