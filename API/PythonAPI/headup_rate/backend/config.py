import os
from dotenv import load_dotenv

# 加载环境变量（复用原有.env文件，新增配置项即可）
load_dotenv("../.env")  # 指向根目录的.env文件

class DBConfig:
    """数据库配置（独立连接，不影响原有项目）"""
    HOST = os.getenv("MYSQL_HOST", "localhost")
    PORT = int(os.getenv("MYSQL_PORT", 3306))
    USER = os.getenv("MYSQL_USER", "root")
    PASSWORD = os.getenv("MYSQL_PASSWORD", "你的MySQL密码")
    DB_NAME = os.getenv("MYSQL_DB", "student_analysis")  # 可复用原有数据库，也可新建
    URL = f"mysql+pymysql://{USER}:{PASSWORD}@{HOST}:{PORT}/{DB_NAME}?charset=utf8mb4"

class DetectionConfig:
    """抬头率检测配置"""
    HEAD_UP_THRESHOLD = 0.8  # 抬头判定阈值
    SUPPORTED_DATA_TYPES = ["image", "video_frame"]  # 支持的数据类型
    RATE_PRECISION = 2  # 结果保留小数位数
    MODEL_PATH = "yolov8n-pose.pt"  # 检测模型路径（YOLO预训练模型）