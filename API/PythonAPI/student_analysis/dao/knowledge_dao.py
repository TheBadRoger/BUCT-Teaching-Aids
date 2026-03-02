from sqlalchemy.orm import Session
from sqlalchemy import distinct, func, Integer
from student_analysis.models.db_models import KnowledgeMastery


# 根据班级查所有知识点（去重）
def get_knowledge_by_class(db: Session, class_name: str):
    # 从掌握度表中提取班级下的知识点名称（去重）
    subject_names = db.query(distinct(KnowledgeMastery.subject_name)).filter(
        KnowledgeMastery.class_name == class_name
    ).all()
    # 转换为列表格式
    return [name[0] for name in subject_names]


# 统计知识点的班级平均数据
def get_knowledge_stats(db: Session, class_name: str, subject_name: str):
    stats = db.query(
        func.avg(KnowledgeMastery.progress).label("avg_progress"),
        func.avg(KnowledgeMastery.rate).label("avg_mastery"),
        func.count(KnowledgeMastery.student_id).label("total_students"),
        func.sum((KnowledgeMastery.rate > 0).cast(Integer)).label("learned_students")
    ).filter(
        KnowledgeMastery.class_name == class_name,
        KnowledgeMastery.subject_name == subject_name
    ).first()

    return {
        "avg_progress": stats[0] or 0.0 if stats else 0.0,  # 第一个字段：avg_progress
        "avg_mastery": stats[1] or 0.0 if stats else 0.0,  # 第二个字段：avg_mastery
        "total_students": stats[2] or 0 if stats else 0,  # 第三个字段：total_students
        "learned_students": stats[3] or 0 if stats else 0  # 第四个字段：learned_students
    }