from sqlalchemy import create_engine, Column, Integer, Float, String, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime
from config import MYSQL_CONFIG


SQLALCHEMY_DATABASE_URL = (
    f"mysql+pymysql://{MYSQL_CONFIG['user']}:{MYSQL_CONFIG['password']}@"
    f"{MYSQL_CONFIG['host']}:{MYSQL_CONFIG['port']}/{MYSQL_CONFIG['db']}?charset={MYSQL_CONFIG['charset']}"
)
engine = create_engine(SQLALCHEMY_DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# 定义举手率统计记录表（核心表）
class HandUpRecord(Base):
    __tablename__ = "hand_up_record"
    id = Column(Integer, primary_key=True, index=True)  # 主键ID
    total_student = Column(Integer, comment="已识别有效学生数")  # 有效学生数
    hand_up_student = Column(Integer, comment="举手学生数")  # 举手人数
    hand_up_rate = Column(Float, comment="举手率（0-1）")  # 举手率
    record_time = Column(DateTime, default=datetime.now, comment="记录时间")  # 记录时间
    class_name = Column(String(50), default="默认班级", comment="班级名称")  # 可选：班级名称

# 定义学生信息表（可选，可直接用人脸库文件名，这里做扩展）
class Student(Base):
    __tablename__ = "student"
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(String(20), unique=True, comment="学生ID")  # 学生唯一ID
    name = Column(String(50), comment="学生姓名")  # 姓名
    face_path = Column(String(200), comment="人脸照片路径")  # 人脸库路径

# 创建所有表（首次运行自动创建数据库文件和表）
Base.metadata.create_all(bind=engine)

# 获取数据库会话（依赖注入，FastAPI中用）
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()