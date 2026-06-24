import os
from dotenv import load_dotenv

_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(dotenv_path=os.path.join(_BASE_DIR, '.env'))

# Read database connection parameters at module level so they can be
# referenced when building DATABASE_URL without duplicating os.environ calls.
_MYSQL_USER = os.environ.get('MYSQL_USER', 'python_flask_buctta')
_MYSQL_PASSWORD = os.environ.get('MYSQL_PASSWORD', os.environ.get('BUCTTA_PYTHON_DB_PASSWORD', ''))
_MYSQL_DB = os.environ.get('MYSQL_DB', 'BUCTTA_DATABASE')
_DEFAULT_DB_URL = (
    f"mysql+pymysql://{_MYSQL_USER}:{_MYSQL_PASSWORD}@"
    f"localhost:3306/{_MYSQL_DB}?charset=utf8mb4"
)


class BaseConfig:
    # Flask
    SECRET_KEY = os.environ.get('SECRET_KEY', 'buctta-secret-key-change-in-production')
    DEBUG = os.environ.get('DEBUG', 'false').lower() == 'true'
    PORT = 8080

    # Database - MySQL
    MYSQL_HOST = 'localhost'
    MYSQL_PORT = 3306
    MYSQL_USER = _MYSQL_USER
    MYSQL_PASSWORD = _MYSQL_PASSWORD
    MYSQL_DB = _MYSQL_DB
    DATABASE_URL = os.environ.get('DATABASE_URL', _DEFAULT_DB_URL)

    # Camera (face_hand_up module)
    CAMERA_ID = int(os.environ.get('CAMERA_ID', 0))
    CAMERA_WIDTH = 640
    CAMERA_HEIGHT = 480
    FRAME_FPS = 10

    # Face Recognition (face_hand_up module)
    FACE_DB_PATH = os.environ.get(
        'FACE_DB_PATH',
        os.path.join(_BASE_DIR, 'face_database', 'face_db')
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
        os.path.join(_BASE_DIR, 'uploads')
    )

    # AI Classroom module – SQLite fallback for classroom live data
    CLASSROOM_DB_PATH = os.environ.get(
        'CLASSROOM_DB_PATH',
        os.path.join(_BASE_DIR, 'classroom_data.db')
    )

    # AI Classroom – Speech Recognition
    SPEECH_LANGUAGE = os.environ.get('SPEECH_LANGUAGE', 'zh-CN')

    # AI Classroom – Attention Analyzer
    ATTENTION_WINDOW_SIZE = int(os.environ.get('ATTENTION_WINDOW_SIZE', 30))
    INTERACTION_WINDOW_SECONDS = int(os.environ.get('INTERACTION_WINDOW_SECONDS', 300))


Config = BaseConfig
