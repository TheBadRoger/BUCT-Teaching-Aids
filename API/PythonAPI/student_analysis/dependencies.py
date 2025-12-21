from sqlalchemy.orm import Session
from student_analysis.config.database import SessionLocal

def get_db():
    """数据库Session依赖，自动创建和关闭连接"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()