# 连接本地MySQL
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime
from sqlalchemy import text
import os
import pymysql  # MySQL连接依赖

MYSQL_USER = os.environ.get("MYSQL_USER", "root")  # 本地MySQL默认最高权限账号
# 优先从 MYSQL_PWD 或 MYSQL_PASSWORD 环境变量读取密码，未设置则使用占位提示
MYSQL_PWD = os.environ.get("MYSQL_PWD", os.environ.get("MYSQL_PASSWORD", "123456"))
MYSQL_HOST = os.environ.get("MYSQL_HOST", "127.0.0.1")  # 本地电脑地址，等价于localhost
MYSQL_PORT = int(os.environ.get("MYSQL_PORT", "4000"))  # 端口默认为你配置的4000
MYSQL_DB = os.environ.get("MYSQL_DB", "BUCTTA_DATABASE")  # 数据库名，默认BUCTTA_DATABASE

# 如果密码未修改（仍为占位符），在日志中给出提示（不抛出异常）
if MYSQL_PWD == "你的本地MySQL根密码":
    print("警告：MySQL 密码仍为占位符，请通过环境变量 MYSQL_PWD/MYSQL_PASSWORD 或直接修改 database.py 来设置密码。")

# MySQL连接URL格式：mysql+pymysql://账号:密码@地址:端口/数据库名?连接参数
# 加入charset=utf8mb4支持中文，serverTimezone=Asia/Shanghai解决时区报错
DATABASE_URL = f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PWD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}?charset=utf8mb4"

# 创建MySQL引擎，添加连接池配置避免连接失效
engine = create_engine(
    DATABASE_URL,
    connect_args={},  # MySQL无需check_same_thread，删除原SQLite的该参数
    pool_pre_ping=True,  # 连接前检测，避免失效连接
    pool_recycle=3600    # 1小时回收连接，防止MySQL超时断开
)

# 简短连接测试（仅打印提示，不会抛出异常中断程序）
try:
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    print(f"已连接到 MySQL 数据库：{MYSQL_DB} @ {MYSQL_HOST}:{MYSQL_PORT}")
except Exception as e:
    print("警告：无法连接到 MySQL 数据库，请检查配置（用户/密码/主机/端口/数据库）：", str(e))

# 数据库会话工厂
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# -------------------------- PBLRecord表结构 --------------------------
class PBLRecord(Base):
    __tablename__ = "pbl_records"
    
    id = Column(Integer, primary_key=True, index=True)
    grade_subject = Column(String(100), index=True)
    textbook_version = Column(String(100))
    unit = Column(String(200))
    difficulty = Column(String(20), default="综合型")  # 新增
    teachingHours = Column(String(10), default="8")     # 新增
    requirements = Column(Text, nullable=True)
    content = Column(Text, nullable=True)
    generated_content = Column(Text)
    created_at = Column(DateTime, default=datetime.now)
    
# -------------------------- 自动创建数据表 --------------------------
# 连接MySQL后，自动在BUCTTA_DATABASE库中创建pbl_records表（若已存在则不重复创建）
Base.metadata.create_all(bind=engine)

# -------------------------- 数据库会话依赖（main.py可能调用） --------------------------
def get_db():
    """获取数据库会话（原代码不变，兼容FastAPI的依赖注入）"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()