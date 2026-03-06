"""Business-logic services for the student_analysis module.

Merges student_service.py, knowledge_service.py, and report_service.py.
"""
from datetime import datetime
from typing import List

from sqlalchemy.orm import Session

from modules.student_analysis import dao


# ── Student list ─────────────────────────────────────────────────────────────────
def get_class_student_list(db: Session, class_name: str) -> dict:
    students = dao.get_students_by_class(db, class_name)
    student_list = []
    for student in students:
        mastery_list = dao.get_mastery_by_class_student(db, class_name, student.student_id)
        if mastery_list:
            avg_progress = sum(float(m.progress) for m in mastery_list) / len(mastery_list)
            avg_mastery = sum(m.mastery_rate for m in mastery_list) / len(mastery_list)
        else:
            avg_progress = avg_mastery = 0.0
        student_list.append({
            "student_name": student.name,
            "student_id": student.student_id,
            "learning_progress": round(avg_progress, 1),
            "mastery_rate": round(avg_mastery, 1),
            "data_study_duration": student.data_study_duration,
            "practice_duration": student.practice_duration,
            "practice_count": student.practice_count
        })
    return {"class_name": class_name, "student_list": student_list}


# ── Knowledge list ────────────────────────────────────────────────────────────────
def get_class_knowledge_list(db: Session, class_name: str) -> dict:
    subject_names = dao.get_knowledge_by_class(db, class_name)
    total_count = len(dao.get_students_by_class(db, class_name))
    knowledge_list = []
    for name in subject_names:
        stats = dao.get_knowledge_stats(db, class_name, name)
        knowledge_list.append({
            "knowledge_name": name,
            "avg_progress": round(stats["avg_progress"], 1),
            "avg_mastery": round(stats["avg_mastery"], 1),
            "learned_count": stats["learned_students"],
            "total_count": total_count
        })
    return {"class_name": class_name, "knowledge_list": knowledge_list}


# ── Student report ────────────────────────────────────────────────────────────────
def get_report_data(db: Session, student_id: int) -> dict:
    student = dao.get_student_by_student_id(db, student_id)
    if not student:
        raise ValueError(f"学号 {student_id} 不存在")

    mastery_list = dao.get_mastery_by_student(db, student_id)
    knowledge_mastery = []
    for mastery in mastery_list:
        mastery_rate_value = float(mastery.rate)
        if mastery_rate_value >= 90:
            level, weak_points = "优", []
        elif mastery_rate_value >= 80:
            level, weak_points = "良", ["个别细节需巩固"]
        elif mastery_rate_value >= 60:
            level, weak_points = "中", ["核心知识点需加强"]
        else:
            level, weak_points = "差", ["基础薄弱，需全面复习"]
        knowledge_mastery.append({
            "knowledge_name": mastery.subject_name,
            "mastery_rate": mastery_rate_value,
            "level": level,
            "weak_points": weak_points
        })

    return {
        "student_name": student.name,
        "student_id": student.student_id,
        "class_name": student.class_name,
        "report_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "knowledge_mastery": knowledge_mastery
    }
