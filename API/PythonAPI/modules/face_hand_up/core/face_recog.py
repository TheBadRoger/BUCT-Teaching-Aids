"""Face recognition core logic.

Adapted from API/PythonAPI/face_hand_up/app/core/face_recog.py.
Uses Config.FACE_DB_PATH and Config.TOLERANCE from the unified config.
"""
import logging
import os

import cv2

from config import Config

logger = logging.getLogger(__name__)

# In-process cache; shared via Config.FACE_ENCODINGS_CACHE
FACE_ENCODINGS_CACHE: dict = Config.FACE_ENCODINGS_CACHE


def load_face_database() -> dict:
    """Load face images from Config.FACE_DB_PATH and cache their encodings."""
    if FACE_ENCODINGS_CACHE:
        return FACE_ENCODINGS_CACHE
    try:
        import face_recognition
    except ImportError:
        raise RuntimeError("face_recognition 未安装，请先运行 pip install face-recognition")

    face_dir = Config.FACE_DB_PATH
    if not os.path.isdir(face_dir):
        raise FileNotFoundError(f"人脸库目录不存在: {face_dir}")

    for filename in os.listdir(face_dir):
        if not filename.lower().endswith((".jpg", ".jpeg", ".png")):
            continue
        name_id = os.path.splitext(filename)[0]
        img_path = os.path.join(face_dir, filename)
        image = face_recognition.load_image_file(img_path)
        encodings = face_recognition.face_encodings(image)
        if encodings:
            FACE_ENCODINGS_CACHE[name_id] = encodings[0]
        else:
            logger.warning("警告：%s 中未检测到人脸，已跳过", filename)

    if not FACE_ENCODINGS_CACHE:
        raise RuntimeError("人脸库为空，请在 face_database/人脸库 中放入人脸照片")
    logger.info("人脸库加载完成，共加载 %d 个人脸", len(FACE_ENCODINGS_CACHE))
    return FACE_ENCODINGS_CACHE


def recognize_face(frame) -> list:
    """Recognise faces in an OpenCV BGR frame.

    Returns a list of tuples: (top, right, bottom, left, name_id).
    """
    import face_recognition

    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    face_locations = face_recognition.face_locations(rgb_frame)
    face_encodings_in_frame = face_recognition.face_encodings(rgb_frame, face_locations)
    db_encodings = load_face_database()

    recognized = []
    for (top, right, bottom, left), enc in zip(face_locations, face_encodings_in_frame):
        matches = face_recognition.compare_faces(
            list(db_encodings.values()), enc, tolerance=Config.TOLERANCE
        )
        name_id = "未知人员"
        if True in matches:
            name_id = list(db_encodings.keys())[matches.index(True)]
        recognized.append((top, right, bottom, left, name_id))
    return recognized


def draw_face_box(frame, recognized_faces: list):
    """Draw bounding boxes and labels on the frame (for debugging)."""
    for (top, right, bottom, left, name_id) in recognized_faces:
        cv2.rectangle(frame, (left, top), (right, bottom), (0, 255, 0), 2)
        cv2.rectangle(frame, (left, bottom - 35), (right, bottom), (0, 255, 0), cv2.FILLED)
        cv2.putText(frame, name_id, (left + 6, bottom - 6),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
    return frame
