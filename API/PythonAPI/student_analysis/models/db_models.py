from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime ,DECIMAL
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

# 学生表（新增学习行为字段）
class Student(Base):
    __tablename__ = "student"
    id = Column(Integer, primary_key=True, index=True)  # 自增ID
    student_id = Column(Integer, unique=True, nullable=False)  # 学号（对外展示）
    name = Column(String(50), nullable=False)  # 姓名
    class_name = Column(String(50), nullable=False)  # 班级
    data_study_duration = Column(Integer, default=0)  # 资料学习时长（分钟）
    practice_duration = Column(Integer, default=0)    # 练习时长（分钟）
    practice_count = Column(Integer, default=0)       # 练习次数

# 知识点表（存储所有知识点名称）
class Knowledge(Base):
    __tablename__ = "knowledge"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), unique=True, nullable=False)  # 知识点名称（如"MSE"）

# 学生-知识点掌握度表（记录每个学生对每个知识点的掌握情况）
class KnowledgeMastery(Base):
    __tablename__ = "knowledge_mastery"
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, nullable=False)
    subject_name = Column(String(100), nullable=False)
    rate = Column(DECIMAL(5, 2), nullable=False)  # 掌握度百分比（如85.50）
    progress = Column(DECIMAL(5, 2), nullable=False, default=0.0)  # 学习进度（如90.00）
    class_name = Column(String(50), nullable=False)  # 班级名称方便查询

    @property
    def mastery_rate(self):
        return self.rate
# 管理员表（预留，暂不用）
class AdminUser(Base):
    __tablename__ = "admin_users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, nullable=False)
    password = Column(String(100), nullable=False)