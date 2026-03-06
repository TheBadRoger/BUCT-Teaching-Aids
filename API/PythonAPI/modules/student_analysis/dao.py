"""Data Access Objects for the student_analysis module.

Merges student_dao.py, mastery_dao.py, and knowledge_dao.py from the original project.
"""
from typing import List

from sqlalchemy import distinct, func, Integer as SAInteger, cast
from sqlalchemy.orm import Session

from modules.student_analysis.models import Student, KnowledgeMastery


# ── Student ───────────────────────────────────────────────────────────────────────
def get_students_by_class(db: Session, class_name: str) -> List[Student]:
    return db.query(Student).filter(Student.class_name == class_name).all()


def get_student_by_student_id(db: Session, student_id: int):
    return db.query(Student).filter(Student.student_id == student_id).first()


# ── KnowledgeMastery ──────────────────────────────────────────────────────────────
def get_mastery_by_student(db: Session, student_id: int) -> List[KnowledgeMastery]:
    return (
        db.query(KnowledgeMastery)
        .filter(KnowledgeMastery.student_id == student_id)
        .all()
    )


def get_mastery_by_class_student(
    db: Session, class_name: str, student_id: int
) -> List[KnowledgeMastery]:
    return (
        db.query(KnowledgeMastery)
        .filter(
            KnowledgeMastery.class_name == class_name,
            KnowledgeMastery.student_id == student_id
        )
        .all()
    )


# ── Knowledge ─────────────────────────────────────────────────────────────────────
def get_knowledge_by_class(db: Session, class_name: str) -> List[str]:
    """Return a deduplicated list of subject names for the given class."""
    rows = (
        db.query(distinct(KnowledgeMastery.subject_name))
        .filter(KnowledgeMastery.class_name == class_name)
        .all()
    )
    return [row[0] for row in rows]


def get_knowledge_stats(db: Session, class_name: str, subject_name: str) -> dict:
    """Return aggregate statistics for one subject in one class."""
    stats = db.query(
        func.avg(KnowledgeMastery.progress).label("avg_progress"),
        func.avg(KnowledgeMastery.rate).label("avg_mastery"),
        func.count(KnowledgeMastery.student_id).label("total_students"),
        func.sum(cast(KnowledgeMastery.rate > 0, SAInteger)).label("learned_students")
    ).filter(
        KnowledgeMastery.class_name == class_name,
        KnowledgeMastery.subject_name == subject_name
    ).first()

    if not stats:
        return {"avg_progress": 0.0, "avg_mastery": 0.0,
                "total_students": 0, "learned_students": 0}
    return {
        "avg_progress": float(stats[0] or 0),
        "avg_mastery": float(stats[1] or 0),
        "total_students": int(stats[2] or 0),
        "learned_students": int(stats[3] or 0)
    }
