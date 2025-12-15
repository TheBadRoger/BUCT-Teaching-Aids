from sqlalchemy import create_engine, Column, Integer, Float, String, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from datetime import datetime
import config

# 独立的数据库基础配置
Base = declarative_base()
engine = create_engine(config.DBConfig.URL, echo=False)  # echo=False不打印SQL日志，避免干扰原有项目
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


# 独立的抬头率数据表模型（与原有表无冲突）
class HeadUpRate(Base):
    __tablename__ = "head_up_rates"  # 表名如果和原有冲突，可改为head_up_rates_v2

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    student_id = Column(Integer, index=True, nullable=False)
    course_id = Column(String(50), index=True, nullable=False)
    course_name = Column(String(100), nullable=False)
    detection_time = Column(DateTime, default=datetime.now, nullable=False)
    head_up_rate = Column(Float, nullable=False)
    detection_device = Column(String(100))
    remarks = Column(String(255))


# 创建数据表（首次运行时执行，仅创建一次）
def create_tables():
    Base.metadata.create_all(bind=engine)


# 获取数据库会话（独立连接，不影响原有项目的DB连接）
def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# 数据存储工具函数
def save_detection_result(db: Session, detection_data: dict) -> HeadUpRate:
    """保存检测结果到数据库"""
    db_record = HeadUpRate(
        student_id=detection_data["student_id"],
        course_id=detection_data["course_id"],
        course_name=detection_data["course_name"],
        head_up_rate=detection_data["head_up_rate"],
        detection_device=detection_data.get("detection_device"),
        remarks=detection_data.get("remarks")
    )
    db.add(db_record)
    db.commit()
    db.refresh(db_record)
    return db_record


# 数据查询工具函数（可选）
def query_student_results(db: Session, student_id: int, course_id: str = None):
    """查询单个学生的抬头率记录"""
    query = db.query(HeadUpRate).filter(HeadUpRate.student_id == student_id)
    if course_id:
        query = query.filter(HeadUpRate.course_id == course_id)
    return query.order_by(HeadUpRate.detection_time.desc()).all()