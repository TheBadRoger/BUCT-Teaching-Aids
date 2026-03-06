"""Head-up rate blueprint.

Provides REST endpoints for detecting and recording student head-up rates.
Adapted from API/PythonAPI/headup_rate/backend/ (no previous HTTP layer existed).
Database: MySQL via SQLAlchemy (modules.db).
"""
import logging
from datetime import datetime

from flask import Blueprint, request, jsonify
from sqlalchemy import Column, Integer, Float, String, DateTime

from modules.db import Base, get_db_session
from modules.headup_rate.detection_core import detect_head_up_rate

logger = logging.getLogger(__name__)

headup_rate_bp = Blueprint('headup_rate', __name__, url_prefix='/api/headup_rate')


# ── SQLAlchemy 数据模型 ────────────────────────────────────────────────────────────
class HeadUpRateRecord(Base):
    __tablename__ = "head_up_rates"
    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    student_id = Column(Integer, index=True, nullable=False)
    course_id = Column(String(50), index=True, nullable=False)
    course_name = Column(String(100), nullable=False)
    detection_time = Column(DateTime, default=datetime.now, nullable=False)
    head_up_rate = Column(Float, nullable=False)
    detection_device = Column(String(100))
    remarks = Column(String(255))


# ── API 路由 ──────────────────────────────────────────────────────────────────────
@headup_rate_bp.route('/detect', methods=['POST'])
def detect():
    """检测并记录抬头率。

    JSON body:
    {
        "student_id": 1001,
        "course_id": "math_101",
        "course_name": "高等数学（上）",
        "data_type": "image",           # "image" | "video_frame"
        "raw_data": "<base64>",         # base64图片 (后端计算时必须)
        "calculated_rate": 85.5,        # 前端已计算的抬头率 (可选)
        "detection_device": "教室摄像头-1",
        "remarks": "上课10分钟检测"
    }
    """
    data = request.get_json(silent=True) or {}

    required = ('student_id', 'course_id', 'course_name', 'data_type')
    missing = [f for f in required if f not in data]
    if missing:
        return jsonify({"code": 400, "msg": f"缺少必填字段：{', '.join(missing)}"}), 400

    try:
        head_up_rate = detect_head_up_rate(
            data_type=data['data_type'],
            raw_data=data.get('raw_data'),
            calculated_rate=data.get('calculated_rate')
        )
    except (ValueError, RuntimeError) as e:
        return jsonify({"code": 400, "msg": str(e)}), 400
    except Exception as e:
        logger.exception("抬头率检测异常")
        return jsonify({"code": 500, "msg": f"检测失败：{e}"}), 500

    try:
        with get_db_session() as db:
            record = HeadUpRateRecord(
                student_id=data['student_id'],
                course_id=data['course_id'],
                course_name=data['course_name'],
                head_up_rate=head_up_rate,
                detection_device=data.get('detection_device'),
                remarks=data.get('remarks')
            )
            db.add(record)
    except Exception as e:
        logger.warning("抬头率记录写入数据库失败: %s", e)

    return jsonify({
        "code": 200,
        "msg": "检测成功",
        "data": {
            "student_id": data['student_id'],
            "course_id": data['course_id'],
            "head_up_rate": head_up_rate,
            "detection_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
    })


@headup_rate_bp.route('/history', methods=['GET'])
def get_history():
    """获取抬头率历史记录（可按课程过滤）。

    Query params:
    - course_id (optional)
    - limit (default 20, max 100)
    """
    course_id = request.args.get('course_id')
    limit = min(int(request.args.get('limit', 20)), 100)

    try:
        with get_db_session() as db:
            query = db.query(HeadUpRateRecord).order_by(HeadUpRateRecord.detection_time.desc())
            if course_id:
                query = query.filter(HeadUpRateRecord.course_id == course_id)
            records = query.limit(limit).all()
            data = [
                {
                    "id": r.id,
                    "student_id": r.student_id,
                    "course_id": r.course_id,
                    "course_name": r.course_name,
                    "detection_time": r.detection_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "head_up_rate": r.head_up_rate,
                    "detection_device": r.detection_device
                }
                for r in records
            ]
        return jsonify({"code": 200, "msg": "获取成功", "data": data})
    except Exception as e:
        return jsonify({"code": 500, "msg": str(e)}), 500


@headup_rate_bp.route('/student/<int:student_id>', methods=['GET'])
def get_student_history(student_id: int):
    """获取指定学生的抬头率历史记录。

    Query params:
    - course_id (optional)
    - limit (default 20, max 100)
    """
    course_id = request.args.get('course_id')
    limit = min(int(request.args.get('limit', 20)), 100)

    try:
        with get_db_session() as db:
            query = (
                db.query(HeadUpRateRecord)
                .filter(HeadUpRateRecord.student_id == student_id)
                .order_by(HeadUpRateRecord.detection_time.desc())
            )
            if course_id:
                query = query.filter(HeadUpRateRecord.course_id == course_id)
            records = query.limit(limit).all()
            data = [
                {
                    "id": r.id,
                    "course_id": r.course_id,
                    "course_name": r.course_name,
                    "detection_time": r.detection_time.strftime("%Y-%m-%d %H:%M:%S"),
                    "head_up_rate": r.head_up_rate
                }
                for r in records
            ]
        return jsonify({
            "code": 200,
            "msg": "获取成功",
            "student_id": student_id,
            "data": data
        })
    except Exception as e:
        return jsonify({"code": 500, "msg": str(e)}), 500
