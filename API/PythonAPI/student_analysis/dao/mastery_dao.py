from sqlalchemy.orm import Session
from typing import List  # 导入typing模块的List
from student_analysis.models.db_models import KnowledgeMastery

# 根据学生ID查所有知识点掌握度
def get_mastery_by_student(db: Session, student_id: int) -> List[KnowledgeMastery]:  # 改用List
    return db.query(KnowledgeMastery).filter(KnowledgeMastery.student_id == student_id).all()


def get_mastery_by_class_student(db: Session, class_name: str, student_id: int) -> List[KnowledgeMastery]:  # 改用List
    """
    根据班级名和学生ID查询该学生的所有知识点掌握情况

    Args:
        db: 数据库会话
        class_name: 班级名称（如"高材2304"）
        student_id: 学生ID（学号，对应student.student_id）

    Returns:
        该学生的知识点掌握记录列表
    """
    return db.query(KnowledgeMastery).filter(
        KnowledgeMastery.class_name == class_name,
        KnowledgeMastery.student_id == student_id
    ).all()