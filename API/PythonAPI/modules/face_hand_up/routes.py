"""Face & hand-up detection blueprint.

Exposes REST endpoints for the face_hand_up module.
Adapted from API/PythonAPI/face_hand_up/app/ (FastAPI → Flask Blueprint).
Database: MySQL via SQLAlchemy (modules.db).
"""
import logging
from datetime import datetime

import cv2
from flask import Blueprint, request, jsonify, Response, stream_with_context
from sqlalchemy import Column, Integer, Float, String, DateTime
from sqlalchemy.orm import Session

from modules.db import Base, get_db_session
from config import Config

logger = logging.getLogger(__name__)

face_hand_up_bp = Blueprint('face_hand_up', __name__, url_prefix='/api/face_hand_up')

# ── SQLAlchemy 数据模型 ────────────────────────────────────────────────────────────
class HandUpRecord(Base):
    __tablename__ = "hand_up_record"
    id = Column(Integer, primary_key=True, index=True)
    total_student = Column(Integer)
    hand_up_student = Column(Integer)
    hand_up_rate = Column(Float)
    record_time = Column(DateTime, default=datetime.now)
    class_name = Column(String(50), default="默认班级")


# ── 摄像头（延迟初始化） ────────────────────────────────────────────────────────────
_cap = None


def _get_camera():
    global _cap
    if _cap is None:
        _cap = cv2.VideoCapture(Config.CAMERA_ID)
        _cap.set(cv2.CAP_PROP_FRAME_WIDTH, Config.CAMERA_WIDTH)
        _cap.set(cv2.CAP_PROP_FRAME_HEIGHT, Config.CAMERA_HEIGHT)
        _cap.set(cv2.CAP_PROP_FPS, Config.FRAME_FPS)
        if not _cap.isOpened():
            _cap = None
            raise RuntimeError("无法打开摄像头，请检查摄像头ID或连接")
    return _cap


# ── API 路由 ──────────────────────────────────────────────────────────────────────
@face_hand_up_bp.route('/reload_face_db', methods=['GET'])
def reload_face_database():
    """重新加载人脸库（新增/删除人脸照片后调用）"""
    from modules.face_hand_up.core.face_recog import FACE_ENCODINGS_CACHE, load_face_database
    FACE_ENCODINGS_CACHE.clear()
    Config.FACE_ENCODINGS_CACHE.clear()
    try:
        load_face_database()
        return jsonify({"code": 200, "msg": "人脸库重新加载成功"})
    except Exception as e:
        return jsonify({"code": 500, "msg": str(e)}), 500


@face_hand_up_bp.route('/real_time_hand_up', methods=['GET'])
def real_time_hand_up():
    """获取实时举手率（识别当前帧并自动记录到数据库）"""
    from modules.face_hand_up.core.face_recog import recognize_face
    from modules.face_hand_up.core.hand_detect import detect_hand_up, calculate_hand_up_rate

    class_name = request.args.get('class_name', '默认班级')

    try:
        cap = _get_camera()
    except RuntimeError as e:
        return jsonify({"code": 500, "msg": str(e), "data": None}), 500

    ret, frame = cap.read()
    if not ret:
        return jsonify({"code": 500, "msg": "无法读取摄像头画面", "data": None}), 500

    recognized = recognize_face(frame)
    hand_up_students, hand_up_locations = detect_hand_up(frame, recognized)
    rate, hand_up_num, valid_num = calculate_hand_up_rate(recognized, hand_up_students)

    if valid_num > 0:
        try:
            with get_db_session() as db:
                record = HandUpRecord(
                    total_student=valid_num,
                    hand_up_student=hand_up_num,
                    hand_up_rate=rate,
                    class_name=class_name
                )
                db.add(record)
        except Exception as e:
            logger.warning("举手率记录写入数据库失败: %s", e)

    return jsonify({
        "code": 200,
        "msg": "获取实时举手率成功",
        "data": {
            "record_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "class_name": class_name,
            "total_student": valid_num,
            "hand_up_student": hand_up_num,
            "hand_up_rate": rate,
            "hand_up_students": hand_up_students,
            "recognized_students": [f[4] for f in recognized if f[4] != "未知人员"]
        }
    })


@face_hand_up_bp.route('/history_hand_up', methods=['GET'])
def history_hand_up():
    """获取历史举手率记录"""
    class_name = request.args.get('class_name', '默认班级')
    limit = min(int(request.args.get('limit', 10)), 100)

    try:
        with get_db_session() as db:
            records = (
                db.query(HandUpRecord)
                .filter(HandUpRecord.class_name == class_name)
                .order_by(HandUpRecord.record_time.desc())
                .limit(limit)
                .all()
            )
            data = [
                {
                    "id": r.id,
                    "record_time": r.record_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "class_name": r.class_name,
                    "total_student": r.total_student,
                    "hand_up_student": r.hand_up_student,
                    "hand_up_rate": r.hand_up_rate
                }
                for r in records
            ]
        return jsonify({"code": 200, "msg": "获取历史记录成功", "data": data})
    except Exception as e:
        return jsonify({"code": 500, "msg": str(e)}), 500


@face_hand_up_bp.route('/video_feed', methods=['GET'])
def video_feed():
    """摄像头实时画面流（带人脸及举手标记，MJPEG格式）"""
    from modules.face_hand_up.core.face_recog import recognize_face, draw_face_box
    from modules.face_hand_up.core.hand_detect import detect_hand_up, draw_hand_up_box

    def generate_frames():
        try:
            cap = _get_camera()
        except RuntimeError:
            return
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            recognized = recognize_face(frame)
            frame = draw_face_box(frame, recognized)
            hand_up_students, hand_up_locations = detect_hand_up(frame, recognized)
            frame = draw_hand_up_box(frame, hand_up_locations)
            success, buffer = cv2.imencode('.jpg', frame)
            if not success:
                continue
            yield (
                b'--frame\r\n'
                b'Content-Type: image/jpeg\r\n\r\n' + buffer.tobytes() + b'\r\n'
            )

    return Response(
        stream_with_context(generate_frames()),
        mimetype='multipart/x-mixed-replace; boundary=frame'
    )
