from sqlalchemy.orm import Session
from student_analysis.dao import student_dao, mastery_dao
from student_analysis.models.data_models import ClassStudentListResponse, StudentListItem


def get_class_student_list(db: Session, class_name: str) -> ClassStudentListResponse:
    """获取班级学生列表（支撑学生观测界面）"""
    # 1. 查班级所有学生
    students = student_dao.get_students_by_class(db, class_name)
    student_list = []

    for student in students:
        # 2. 查该学生的所有知识点掌握情况，计算平均进度和掌握度
        mastery_list = mastery_dao.get_mastery_by_class_student(
            db, class_name, student.student_id
        )

        if mastery_list:
            avg_progress = sum(m.progress for m in mastery_list) / len(mastery_list)
            avg_mastery = sum(m.mastery_rate for m in mastery_list) / len(mastery_list)
        else:
            avg_progress = 0.0
            avg_mastery = 0.0

        # 3. 组装学生列表项
        student_item = StudentListItem(
            student_name=student.name,
            student_id=student.student_id,
            learning_progress=round(avg_progress, 1),
            mastery_rate=round(avg_mastery, 1),
            data_study_duration=student.data_study_duration,
            practice_duration=student.practice_duration,
            practice_count=student.practice_count
        )
        student_list.append(student_item)

    return ClassStudentListResponse(
        class_name=class_name,
        student_list=student_list
    )