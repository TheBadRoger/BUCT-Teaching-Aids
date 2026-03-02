from sqlalchemy.orm import Session
from student_analysis.dao import knowledge_dao, student_dao
from student_analysis.models.data_models import ClassKnowledgeListResponse, KnowledgeListItem


def get_class_knowledge_list(db: Session, class_name: str) -> ClassKnowledgeListResponse:
    """获取班级知识点列表（支撑知识点观测界面）"""
    # 1. 查班级下的所有知识点（去重）
    subject_names = knowledge_dao.get_knowledge_by_class(db, class_name)
    # 获取班级总人数
    total_student_count = len(student_dao.get_students_by_class(db, class_name))
    knowledge_list = []

    for subject_name in subject_names:
        # 2. 查该知识点的班级统计数据
        stats = knowledge_dao.get_knowledge_stats(db, class_name, subject_name)

        # 3. 组装知识点列表项
        knowledge_item = KnowledgeListItem(
            knowledge_name=subject_name,
            avg_progress=round(stats["avg_progress"], 1),
            avg_mastery=round(stats["avg_mastery"], 1),
            learned_count=stats["learned_students"],
            total_count=total_student_count
        )
        knowledge_list.append(knowledge_item)

    return ClassKnowledgeListResponse(
        class_name=class_name,
        knowledge_list=knowledge_list
    )