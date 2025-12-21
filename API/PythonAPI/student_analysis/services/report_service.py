from sqlalchemy.orm import Session
from datetime import datetime
from student_analysis.dao import student_dao, mastery_dao
from student_analysis.models.data_models import ReportResponse, ReportMasteryItem


def get_report_data(db: Session, student_id: int):
    student = student_dao.get_student_by_student_id(db, student_id)
    if not student:
        raise ValueError(f"学号{student_id}不存在")

    mastery_list = mastery_dao.get_mastery_by_student(db, student_id)
    knowledge_mastery = []

    for mastery in mastery_list:
        rate = float(mastery.rate)

        # 判断掌握等级
        if rate >= 90:
            level = "优"
            weak_points = []
        elif rate >= 80:
            level = "良"
            weak_points = ["个别细节需巩固"]
        elif rate >= 60:
            level = "中"
            weak_points = ["核心知识点需加强"]
        else:
            level = "差"
            weak_points = ["基础薄弱，需全面复习"]

        knowledge_mastery.append(ReportMasteryItem(
            knowledge_name=mastery.subject_name,  # 直接用知识点名称
            mastery_rate=rate,
            level=level,
            weak_points=weak_points
        ))

    return ReportResponse(
        student_name=student.name,
        student_id=int(student.id),
        class_name=student.class_name,
        report_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        knowledge_mastery=knowledge_mastery
    )