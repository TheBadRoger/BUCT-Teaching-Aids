import os


class DockerConfig:
    # Flask
    SECRET_KEY = os.environ.get('SECRET_KEY', 'buctta-secret-key-change-in-production')
    DEBUG = False
    PORT = 8080

    # Database - fixed for docker internal network
    MYSQL_HOST = 'mysql'
    MYSQL_PORT = 3306
    MYSQL_USER = 'root'
    MYSQL_PASSWORD = os.environ.get('MYSQL_ROOT_PASSWORD', '')
    MYSQL_DB = 'BUCTTA_DATABASE'
    DATABASE_URL = os.environ.get(
        'DATABASE_URL',
        f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}?charset=utf8mb4"
    )

    # Camera (face_hand_up module)
    CAMERA_ID = int(os.environ.get('CAMERA_ID', 0))
    CAMERA_WIDTH = 640
    CAMERA_HEIGHT = 480
    FRAME_FPS = 10

    # Face Recognition (face_hand_up module)
    FACE_DB_PATH = os.environ.get(
        'FACE_DB_PATH',
        os.path.join(os.path.dirname(__file__), 'face_database', 'face_db')
    )
    TOLERANCE = 0.6
    FACE_ENCODINGS_CACHE: dict = {}

    # Hand Detection (face_hand_up module)
    CONTOUR_THRESHOLD = 1500
    HAND_UP_RATIO = 0.2
    DETECT_AREA_TOP = 0.05
    DETECT_AREA_BOTTOM = 0.5

    # Head-up Rate (headup_rate module)
    HEAD_UP_THRESHOLD = 0.8
    RATE_PRECISION = 2
    MODEL_PATH = os.environ.get('YOLO_MODEL_PATH', 'yolov8n-pose.pt')
    SUPPORTED_DATA_TYPES = ["image", "video_frame"]

    # PBL module
    UPLOAD_DIR = os.environ.get(
        'UPLOAD_DIR',
        os.path.join(os.path.dirname(__file__), 'uploads')
    )

    # AI Classroom module – SQLite fallback for classroom live data
    CLASSROOM_DB_PATH = os.environ.get(
        'CLASSROOM_DB_PATH',
        os.path.join(os.path.dirname(__file__), 'classroom_data.db')
    )
