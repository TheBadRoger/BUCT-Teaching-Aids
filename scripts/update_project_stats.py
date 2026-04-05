#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen

ROOT_DIR = Path(__file__).resolve().parents[1]
README_PATH = ROOT_DIR / "README.md"
GENERATED_DIR = ROOT_DIR / "scripts" / "generated"
STATS_JSON_PATH = GENERATED_DIR / "stats.json"
STATS_MD_PATH = GENERATED_DIR / "README_STATS.md"
UTC_PLUS_8 = timezone(timedelta(hours=8))

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
JAVA_TEST_CASE_PATTERN = re.compile(
    r"^\s*@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b",
    re.MULTILINE,
)
PYTHON_TEST_CASE_PATTERN = re.compile(r"^\s*def\s+test_\w*\s*\(", re.MULTILINE)

JAVA_TEST_DIR = ROOT_DIR / "API" / "JavaAPI" / "src" / "test" / "java"
PYTHON_TEST_DIR = ROOT_DIR / "API" / "PythonAPI" / "tests"
GITHUB_REPO = "TheBadRoger/BUCT-Teaching-Aids"
MAIN_BRANCH = "main"

SECTION_START = "<!-- STATS_SECTION_START -->"
SECTION_END = "<!-- STATS_SECTION_END -->"


def _is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)


def _count_non_empty_lines(file_path: Path) -> int:
    with file_path.open("r", encoding="utf-8", errors="ignore") as f:
        return sum(1 for line in f if line.strip())


def _count_pattern_in_files(base_dir: Path, suffix: str, pattern: re.Pattern[str]) -> int:
    if not base_dir.exists():
        return 0

    total = 0
    for file_path in base_dir.rglob(f"*{suffix}"):
        if not file_path.is_file() or _is_excluded(file_path):
            continue
        content = file_path.read_text(encoding="utf-8", errors="ignore")
        total += len(pattern.findall(content))
    return total


def _fetch_workflow_status(workflow_file: str) -> str:
    api = (
        f"https://api.github.com/repos/{GITHUB_REPO}/actions/workflows/"
        f"{workflow_file}/runs?branch={MAIN_BRANCH}&per_page=1"
    )

    request = Request(
        api,
        headers={
            "Accept": "application/vnd.github+json",
            "User-Agent": "buct-teaching-aids-stats-bot",
        },
    )

    try:
        with urlopen(request, timeout=10) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except (URLError, OSError, json.JSONDecodeError):
        return "no result"

    runs = payload.get("workflow_runs", [])
    if not runs:
        return "no result"

    latest = runs[0]
    if latest.get("status") != "completed":
        return "no result"

    conclusion = latest.get("conclusion")
    if conclusion == "success":
        return "passing"
    if conclusion in {
        "failure",
        "timed_out",
        "cancelled",
        "action_required",
        "startup_failure",
        "stale",
    }:
        return "failing"
    return "no result"


def _unit_test_badge_text(case_count: int, workflow_file: str) -> str:
    status = _fetch_workflow_status(workflow_file)
    return f"{case_count} case(s) {status}"


def collect_stats() -> dict:
    language_lines: dict[str, int] = defaultdict(int)
    java_endpoints = 0
    python_endpoints = 0
    java_test_case_count = _count_pattern_in_files(
        JAVA_TEST_DIR, ".java", JAVA_TEST_CASE_PATTERN
    )
    python_test_case_count = _count_pattern_in_files(
        PYTHON_TEST_DIR, ".py", PYTHON_TEST_CASE_PATTERN
    )
    java_unit_test_badge_text = _unit_test_badge_text(
        java_test_case_count, "java-tests.yml"
    )
    python_unit_test_badge_text = _unit_test_badge_text(
        python_test_case_count, "python-tests.yml"
    )

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
        "updated_at_utc8": datetime.now(UTC_PLUS_8).strftime("%Y-%m-%d %H:%M:%S"),
        "total_lines": total_lines,
        "language_count": len(sorted_language_lines),
        "top_language": top_language,
        "java_endpoint_count": java_endpoints,
        "python_endpoint_count": python_endpoints,
        "java_test_case_count": java_test_case_count,
        "python_test_case_count": python_test_case_count,
        "java_unit_test_badge_text": java_unit_test_badge_text,
        "python_unit_test_badge_text": python_unit_test_badge_text,
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
            "## 项目统计",
            "",
            f"> 统计更新时间（UTC+8）：`{stats['updated_at_utc8']}`",
            "",
            "### 核心统计",
            "",
            "| 指标 | 数值 |",
            "| :-- | --: |",
            f"| 代码总行数（非空行） | {stats['total_lines']} |",
            f"| Java 接口数 | {stats['java_endpoint_count']} |",
            f"| Python 接口数 | {stats['python_endpoint_count']} |",
            f"| Java 单元测试用例数 | {stats['java_test_case_count']} |",
            f"| Python 单元测试用例数 | {stats['python_test_case_count']} |",
            "",
            "### 语言占比图",
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


def update_readme(stats_md: str, stats: dict) -> None:
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
    update_readme(stats_md, stats)


if __name__ == "__main__":
    main()
