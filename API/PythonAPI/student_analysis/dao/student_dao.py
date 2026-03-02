from sqlalchemy.orm import Session
from student_analysis.models.db_models import Student

# 根据班级查学生列表
def get_students_by_class(db: Session, class_name: str):
    return db.query(Student).filter(Student.class_name == class_name).all()

# 根据学号查学生
def get_student_by_student_id(db: Session, student_id: int):
    return db.query(Student).filter(Student.student_id == student_id).first()