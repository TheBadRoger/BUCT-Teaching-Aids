from datetime import datetime
from types import SimpleNamespace

import pytest

from modules.student_analysis import services


class DummyMastery:
    def __init__(self, progress, mastery_rate):
        self.progress = progress
        self.mastery_rate = mastery_rate


class DummyReportMastery:
    def __init__(self, subject_name, rate):
        self.subject_name = subject_name
        self.rate = rate


def test_get_class_student_list_calculates_averages(monkeypatch):
    students = [
        SimpleNamespace(
            name="Alice",
            student_id=1001,
            data_study_duration=10,
            practice_duration=5,
            practice_count=2,
        ),
        SimpleNamespace(
            name="Bob",
            student_id=1002,
            data_study_duration=8,
            practice_duration=4,
            practice_count=1,
        ),
    ]

    monkeypatch.setattr(services.dao, "get_students_by_class", lambda db, class_name: students)

    def fake_mastery(db, class_name, student_id):
        if student_id == 1001:
            return [DummyMastery("80", 90.0), DummyMastery("60", 70.0)]
        return []

    monkeypatch.setattr(services.dao, "get_mastery_by_class_student", fake_mastery)

    result = services.get_class_student_list(db=object(), class_name="高材2304")

    assert result["class_name"] == "高材2304"
    assert result["student_list"][0]["learning_progress"] == 70.0
    assert result["student_list"][0]["mastery_rate"] == 80.0
    assert result["student_list"][1]["learning_progress"] == 0.0
    assert result["student_list"][1]["mastery_rate"] == 0.0


def test_get_class_knowledge_list_aggregates_stats(monkeypatch):
    monkeypatch.setattr(services.dao, "get_knowledge_by_class", lambda db, class_name: ["函数", "极限"])
    monkeypatch.setattr(
        services.dao,
        "get_students_by_class",
        lambda db, class_name: [SimpleNamespace(student_id=1), SimpleNamespace(student_id=2)],
    )

    def fake_stats(db, class_name, subject_name):
        if subject_name == "函数":
            return {"avg_progress": 88.88, "avg_mastery": 91.11, "learned_students": 2}
        return {"avg_progress": 55.55, "avg_mastery": 60.01, "learned_students": 1}

    monkeypatch.setattr(services.dao, "get_knowledge_stats", fake_stats)

    result = services.get_class_knowledge_list(db=object(), class_name="高材2304")

    assert result["class_name"] == "高材2304"
    assert len(result["knowledge_list"]) == 2
    assert result["knowledge_list"][0] == {
        "knowledge_name": "函数",
        "avg_progress": 88.9,
        "avg_mastery": 91.1,
        "learned_count": 2,
        "total_count": 2,
    }
    assert result["knowledge_list"][1]["total_count"] == 2


def test_get_report_data_raises_when_student_missing(monkeypatch):
    monkeypatch.setattr(services.dao, "get_student_by_student_id", lambda db, student_id: None)

    with pytest.raises(ValueError, match="不存在"):
        services.get_report_data(db=object(), student_id=9999)


def test_get_report_data_maps_mastery_levels(monkeypatch):
    student = SimpleNamespace(name="Alice", student_id=1001, class_name="高材2304")
    mastery_list = [
        DummyReportMastery("知识点A", 95),
        DummyReportMastery("知识点B", 85),
        DummyReportMastery("知识点C", 65),
        DummyReportMastery("知识点D", 45),
    ]

    monkeypatch.setattr(services.dao, "get_student_by_student_id", lambda db, student_id: student)
    monkeypatch.setattr(services.dao, "get_mastery_by_student", lambda db, student_id: mastery_list)

    result = services.get_report_data(db=object(), student_id=1001)

    levels = [entry["level"] for entry in result["knowledge_mastery"]]
    assert levels == ["优", "良", "中", "差"]
    assert result["student_name"] == "Alice"
    # Ensure report time is a valid timestamp string.
    datetime.strptime(result["report_time"], "%Y-%m-%d %H:%M:%S")


def test_get_report_data_boundary_levels(monkeypatch):
    student = SimpleNamespace(name="Boundary", student_id=1002, class_name="高材2305")
    mastery_list = [
        DummyReportMastery("A", 90),
        DummyReportMastery("B", 80),
        DummyReportMastery("C", 60),
        DummyReportMastery("D", 59.99),
    ]
    monkeypatch.setattr(services.dao, "get_student_by_student_id", lambda db, student_id: student)
    monkeypatch.setattr(services.dao, "get_mastery_by_student", lambda db, student_id: mastery_list)

    result = services.get_report_data(db=object(), student_id=1002)

    levels = [entry["level"] for entry in result["knowledge_mastery"]]
    assert levels == ["优", "良", "中", "差"]
    assert result["student_id"] == 1002
