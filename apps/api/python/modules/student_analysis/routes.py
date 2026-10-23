"""Student analysis blueprint.

Adapted from API/PythonAPI/student_analysis/ (FastAPI → Flask Blueprint).
Exposes three groups of endpoints:
  /api/student/  – student observation
  /api/knowledge/ – knowledge-point observation
  /api/report/   – learning-analysis reports
"""
import logging

from flask import Blueprint, request, jsonify

from modules.db import get_db_session
from modules.student_analysis import services

logger = logging.getLogger(__name__)

student_analysis_bp = Blueprint('student_analysis', __name__)


# ── /api/student/ ─────────────────────────────────────────────────────────────────
@student_analysis_bp.route('/api/student/class_list', methods=['GET'])
def get_class_student_list():
    """获取班级学生列表（支撑学生观测界面）。

    Query params:
    - class_name (required): 班级名称，如"高材2304"
    """
    class_name = request.args.get('class_name')
    if not class_name:
        return jsonify({"code": 400, "msg": "缺少参数 class_name"}), 400
    try:
        with get_db_session() as db:
            result = services.get_class_student_list(db, class_name)
        return jsonify({"code": 200, "msg": "获取成功", "data": result})
    except Exception as e:
        logger.exception("获取班级学生列表失败")
        return jsonify({"code": 500, "msg": str(e)}), 500


# ── /api/knowledge/ ───────────────────────────────────────────────────────────────
@student_analysis_bp.route('/api/knowledge/class_list', methods=['GET'])
def get_class_knowledge_list():
    """获取班级知识点列表（支撑知识点观测界面）。

    Query params:
    - class_name (required): 班级名称
    """
    class_name = request.args.get('class_name')
    if not class_name:
        return jsonify({"code": 400, "msg": "缺少参数 class_name"}), 400
    try:
        with get_db_session() as db:
            result = services.get_class_knowledge_list(db, class_name)
        return jsonify({"code": 200, "msg": "获取成功", "data": result})
    except Exception as e:
        logger.exception("获取班级知识点列表失败")
        return jsonify({"code": 500, "msg": str(e)}), 500


# ── /api/report/ ──────────────────────────────────────────────────────────────────
@student_analysis_bp.route('/api/report/student', methods=['GET'])
def get_student_report():
    """获取学生学情报告。

    Query params:
    - student_id (required, int): 学号
    """
    student_id = request.args.get('student_id', type=int)
    if student_id is None:
        return jsonify({"code": 400, "msg": "缺少参数 student_id"}), 400
    try:
        with get_db_session() as db:
            result = services.get_report_data(db, student_id)
        return jsonify({"code": 200, "msg": "获取成功", "data": result})
    except ValueError as e:
        return jsonify({"code": 404, "msg": str(e)}), 404
    except Exception as e:
        logger.exception("获取学情报告失败")
        return jsonify({"code": 500, "msg": str(e)}), 500
