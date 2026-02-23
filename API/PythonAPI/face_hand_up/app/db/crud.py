from sqlalchemy.orm import Session
from datetime import datetime
from app.db.model import HandUpRecord, Student

# 新增举手率记录
def create_hand_up_record(db: Session, total_student: int, hand_up_student: int, hand_up_rate: float, class_name: str = "默认班级"):
    db_record = HandUpRecord(
        total_student=total_student,
        hand_up_student=hand_up_student,
        hand_up_rate=hand_up_rate,
        class_name=class_name
    )
    db.add(db_record)
    db.commit()
    db.refresh(db_record)
    return db_record

# 获取最新的N条举手率记录
def get_latest_hand_up_records(db: Session, skip: int = 0, limit: int = 10, class_name: str = "默认班级"):
    return db.query(HandUpRecord).filter(HandUpRecord.class_name == class_name).order_by(HandUpRecord.record_time.desc()).offset(skip).limit(limit).all()

# 获取指定时间段的举手率统计（可选，扩展用）
def get_hand_up_records_by_time(db: Session, start_time: datetime, end_time: datetime, class_name: str = "默认班级"):
    return db.query(HandUpRecord).filter(HandUpRecord.class_name == class_name, HandUpRecord.record_time >= start_time, HandUpRecord.record_time <= end_time).all()

# 新增学生信息（可选，扩展用）
def create_student(db: Session, student_id: str, name: str, face_path: str):
    db_student = Student(student_id=student_id, name=name, face_path=face_path)
    db.add(db_student)
    db.commit()
    db.refresh(db_student)
    return db_student