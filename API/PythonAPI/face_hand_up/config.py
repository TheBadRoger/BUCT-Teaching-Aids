# 全局配置文件
import os
import cv2

# 项目根目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# 人脸库路径
FACE_DB_PATH = os.path.join(BASE_DIR, "face_database")
# 数据库文件路径（SQLite）
#DB_FILE = os.path.join(BASE_DIR, "app", "db", "face_hand_up.db")
MYSQL_CONFIG = {
    "host": "127.0.0.1",    # MySQL主机
    "port": 4000,           # MySQL默认端口3306，修改过的话填实际端口
    "user": "root",         # 你的MySQL用户名
    "password": "168748",   # 你的MySQL密码
    "db": "face_hand_up",   # 第一步创建的数据库名
    "charset": "utf8mb4"
}
# 摄像头配置
CAMERA_ID = 0  # 0为电脑内置摄像头，外接摄像头填1/2，视频文件填文件路径
CAMERA_WIDTH = 640  # 摄像头画面宽度
CAMERA_HEIGHT = 480 # 摄像头画面高度
FRAME_FPS = 10      # 处理帧率（降低帧率提升性能）

# 人脸识别配置
TOLERANCE = 0.6  # 人脸识别阈值（越小越严格，0.6是官方推荐）
FACE_ENCODINGS_CACHE = {}  # 人脸编码缓存（避免重复加载）

# 举手检测配置
# 人体轮廓检测的阈值（OpenCV）
CONTOUR_THRESHOLD = 1500
# 举手判定：手部区域占上半身比例（可根据实际场景调整）
HAND_UP_RATIO = 0.2
# 画面分区域：上半部分为举手检测区（y轴比例，0.0-1.0）
DETECT_AREA_TOP = 0.05
DETECT_AREA_BOTTOM = 0.5

# 日志配置（可选）
LOG_LEVEL = "INFO"

# 初始化摄像头
def init_camera():
    cap = cv2.VideoCapture(CAMERA_ID)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT)
    cap.set(cv2.CAP_PROP_FPS, FRAME_FPS)
    if not cap.isOpened():
        raise Exception("无法打开摄像头，请检查摄像头ID或连接")
    return cap