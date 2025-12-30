# 数据库配置（未来可以存储用户生成的PBL）
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime

# 创建数据库连接（这里使用SQLite，简单易用）
DATABASE_URL = "sqlite:///./pbl_database.db"

engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

class PBLRecord(Base):
    """PBL记录表"""
    __tablename__ = "pbl_records"
    
    id = Column(Integer, primary_key=True, index=True)
    grade_subject = Column(String(100))
    textbook_version = Column(String(100))
    unit = Column(String(200))
    requirements = Column(Text, nullable=True)
    content = Column(Text, nullable=True)
    generated_content = Column(Text)
    created_at = Column(DateTime, default=datetime.now)

# 创建表
Base.metadata.create_all(bind=engine)

def get_db():
    """获取数据库会话"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()