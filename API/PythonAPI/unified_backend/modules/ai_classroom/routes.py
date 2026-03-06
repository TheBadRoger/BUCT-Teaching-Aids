"""AI Classroom blueprint and SocketIO event handlers.

Adapted from API/PythonAPI/ai课堂/backend/app.py (Flask + Flask-SocketIO).
"""
import logging
import sqlite3
from collections import defaultdict
from datetime import datetime, timedelta
from typing import Dict, List

import numpy as np
import pandas as pd
import time

from flask import Blueprint, request, jsonify
from flask_socketio import SocketIO, emit

from config import Config

logger = logging.getLogger(__name__)

ai_classroom_bp = Blueprint('ai_classroom', __name__)

# ── 内存数据存储 ────────────────────────────────────────────────────────────────
classrooms: Dict = {}
students: Dict = {}

EMOTION_CATEGORIES = {
    'positive': ['focused', 'happy', 'engaged', 'excited'],
    'neutral':  ['neutral', 'calm', 'thinking'],
    'negative': ['confused', 'bored', 'frustrated', 'distracted']
}


# ── 数据持久化（SQLite） ─────────────────────────────────────────────────────────
class StudentDataManager:
    def __init__(self):
        self._init_database()

    def _init_database(self):
        conn = sqlite3.connect(Config.CLASSROOM_DB_PATH)
        cursor = conn.cursor()
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS student_metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT NOT NULL,
                classroom_id TEXT NOT NULL,
                attention_level REAL,
                engagement_level REAL,
                emotion TEXT,
                gaze_focus REAL,
                posture_engagement REAL,
                interaction_frequency REAL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS classroom_activities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                classroom_id TEXT NOT NULL,
                activity_type TEXT,
                question TEXT,
                responses_count INTEGER,
                start_time DATETIME,
                end_time DATETIME
            )
        ''')
        conn.commit()
        conn.close()
        logger.info("AI课堂数据库初始化完成")

    def save_student_metrics(self, student_id, classroom_id, metrics):
        try:
            conn = sqlite3.connect(Config.CLASSROOM_DB_PATH)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO student_metrics
                (student_id, classroom_id, attention_level, engagement_level, emotion,
                 gaze_focus, posture_engagement, interaction_frequency)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                student_id, classroom_id,
                metrics.get('attention_level'),
                metrics.get('engagement_level'),
                metrics.get('emotion'),
                metrics.get('detailed_metrics', {}).get('gaze_focus'),
                metrics.get('detailed_metrics', {}).get('posture_engagement'),
                metrics.get('detailed_metrics', {}).get('interaction_frequency')
            ))
            conn.commit()
            conn.close()
        except Exception as e:
            logger.error("保存学生指标失败: %s", e)

    def get_student_history(self, student_id, hours=24):
        try:
            conn = sqlite3.connect(Config.CLASSROOM_DB_PATH)
            threshold = datetime.now() - timedelta(hours=hours)
            df = pd.read_sql_query(
                'SELECT * FROM student_metrics WHERE student_id=? AND timestamp>? ORDER BY timestamp',
                conn, params=[student_id, threshold]
            )
            conn.close()
            return df
        except Exception as e:
            logger.error("获取学生历史数据失败: %s", e)
            return pd.DataFrame()

    def get_classroom_summary(self, classroom_id):
        try:
            conn = sqlite3.connect(Config.CLASSROOM_DB_PATH)
            today = datetime.now().date()
            df = pd.read_sql_query('''
                SELECT COUNT(*) as total_records,
                       AVG(attention_level) as avg_attention,
                       AVG(engagement_level) as avg_engagement,
                       emotion, COUNT(*) as emotion_count
                FROM student_metrics
                WHERE classroom_id=? AND DATE(timestamp)=?
                GROUP BY emotion
            ''', conn, params=[classroom_id, today])
            conn.close()
            return df
        except Exception as e:
            logger.error("获取课堂总结失败: %s", e)
            return pd.DataFrame()


# ── 学习分析引擎 ─────────────────────────────────────────────────────────────────
class RealStudentAnalyzer:
    def __init__(self):
        self.analysis_history: Dict = {}

    def analyze_student_metrics(self, student_id: str, metrics: Dict) -> Dict:
        if student_id not in self.analysis_history:
            self.analysis_history[student_id] = []
        self.analysis_history[student_id].append({
            'timestamp': datetime.now().isoformat(),
            'metrics': metrics
        })
        if len(self.analysis_history[student_id]) > 50:
            self.analysis_history[student_id].pop(0)
        return {
            'insights': self._generate_insights(student_id, metrics),
            'trends': self._calculate_trends(student_id),
            'learning_state': self._assess_state(metrics),
            'recommendations': self._generate_recommendations(metrics)
        }

    def _generate_insights(self, student_id, metrics) -> List[str]:
        insights = []
        attention = metrics.get('attention_level', 0)
        engagement = metrics.get('engagement_level', 0)
        if attention >= 80 and engagement >= 80:
            insights.append("学生处于高度专注状态")
        elif attention >= 60:
            insights.append("学生注意力水平良好")
        elif attention <= 40:
            insights.append("学生可能需要关注，注意力较低")
        dm = metrics.get('detailed_metrics', {})
        if dm.get('gaze_focus', 100) < 50:
            insights.append("视线专注度有待提高")
        if dm.get('posture_engagement', 100) < 50:
            insights.append("坐姿参与度需要改善")
        return insights

    def _calculate_trends(self, student_id) -> Dict:
        history = self.analysis_history.get(student_id, [])
        if len(history) < 2:
            return {'attention_trend': 'stable', 'engagement_trend': 'stable'}
        recent_att = [e['metrics'].get('attention_level', 0) for e in history[-5:]]
        recent_eng = [e['metrics'].get('engagement_level', 0) for e in history[-5:]]
        att_trend = 'improving' if recent_att[-1] > recent_att[0] else 'declining'
        eng_trend = 'improving' if recent_eng[-1] > recent_eng[0] else 'declining'
        return {
            'attention_trend': att_trend,
            'engagement_trend': eng_trend,
            'stability_score': round(max(0, 100 - np.var(recent_att) * 2), 1)
        }

    def _assess_state(self, metrics) -> str:
        att = metrics.get('attention_level', 0)
        eng = metrics.get('engagement_level', 0)
        if att >= 80 and eng >= 80:
            return "excellent"
        elif att >= 60 and eng >= 60:
            return "good"
        elif att >= 40:
            return "moderate"
        return "needs_help"

    def _generate_recommendations(self, metrics) -> List[str]:
        recs = []
        if metrics.get('attention_level', 100) < 50:
            recs.append("建议与学生互动，提高参与度")
        dm = metrics.get('detailed_metrics', {})
        if dm.get('gaze_focus', 100) < 50:
            recs.append("可以提醒学生保持视线专注")
        if dm.get('posture_engagement', 100) < 50:
            recs.append("建议关注学生坐姿，保持良好学习姿态")
        return recs


# ── 初始化组件 ───────────────────────────────────────────────────────────────────
data_manager = StudentDataManager()
real_analyzer = RealStudentAnalyzer()


# ── 辅助函数 ─────────────────────────────────────────────────────────────────────
def _find_student_classroom(student_id):
    for cid, classroom in classrooms.items():
        for s in classroom['students']:
            if s['id'] == student_id:
                return cid
    return None


def _get_student_analytics_simple(student):
    att = student.get('attention_level', 0)
    eng = student.get('engagement_level', 0)
    if att < 50 or eng < 50:
        state = "needs_help"
    elif att < 70 or eng < 70:
        state = "moderate"
    else:
        state = "excellent"
    return {
        'learning_state': state,
        'attention_trend': 'stable',
        'engagement_score': eng,
        'focus_duration': 0
    }


# ── HTTP 路由 ─────────────────────────────────────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/create', methods=['POST'])
def create_classroom():
    data = request.json or {}
    classroom_id = data.get('classroom_id', f"class_{int(time.time())}")
    classrooms[classroom_id] = {
        'id': classroom_id,
        'teacher': data.get('teacher_name', 'Unknown Teacher'),
        'subject': data.get('subject', 'General'),
        'students': [],
        'activities': [],
        'emotion_stats': {'positive': 0, 'neutral': 0, 'negative': 0},
        'attention_trend': [],
        'real_time_metrics': {},
        'created_at': datetime.now().isoformat()
    }
    logger.info("创建课堂: %s", classroom_id)
    return jsonify({"success": True, "classroom_id": classroom_id, "message": "Classroom created successfully"})


@ai_classroom_bp.route('/api/classroom/<classroom_id>/status')
def get_classroom_status(classroom_id):
    if classroom_id not in classrooms:
        return jsonify({"error": "Classroom not found"}), 404
    return jsonify(classrooms[classroom_id])


@ai_classroom_bp.route('/api/classroom/<classroom_id>/analytics')
def get_classroom_analytics(classroom_id):
    if classroom_id not in classrooms:
        return jsonify({"error": "Classroom not found"}), 404
    classroom = classrooms[classroom_id]
    student_list = classroom['students']
    if not student_list:
        return jsonify({"error": "No students in classroom"}), 400

    emotion_counts = {'positive': 0, 'neutral': 0, 'negative': 0}
    attention_levels, engagement_levels = [], []
    for student in student_list:
        for category, emotions in EMOTION_CATEGORIES.items():
            if student.get('emotion') in emotions:
                emotion_counts[category] += 1
                break
        attention_levels.append(student.get('attention_level', 0))
        engagement_levels.append(student.get('engagement_level', 0))

    avg_attention = float(np.mean(attention_levels)) if attention_levels else 0
    avg_engagement = float(np.mean(engagement_levels)) if engagement_levels else 0
    classroom['attention_trend'].append({
        'timestamp': datetime.now().isoformat(),
        'avg_attention': round(avg_attention, 1),
        'avg_engagement': round(avg_engagement, 1)
    })
    if len(classroom['attention_trend']) > 10:
        classroom['attention_trend'].pop(0)

    return jsonify({
        'emotion_distribution': emotion_counts,
        'attention_metrics': {
            'average_attention': round(avg_attention, 1),
            'average_engagement': round(avg_engagement, 1),
            'attention_variance': round(float(np.var(attention_levels)), 2) if attention_levels else 0,
            'high_attention_count': len([a for a in attention_levels if a > 80]),
            'low_attention_count': len([a for a in attention_levels if a < 50])
        },
        'attention_trend': classroom['attention_trend'][-5:],
        'total_students': len(student_list)
    })


@ai_classroom_bp.route('/api/student/<student_id>/analytics')
def get_student_analytics_route(student_id):
    if student_id not in students:
        return jsonify({"error": "Student not found"}), 404
    student = students[student_id]
    analytics = real_analyzer.analyze_student_metrics(student_id, student)
    return jsonify({
        'student_info': {'name': student.get('name'), 'id': student_id},
        'current_metrics': {
            'attention_level': student.get('attention_level'),
            'engagement_level': student.get('engagement_level'),
            'emotion': student.get('emotion')
        },
        'analysis': analytics
    })


@ai_classroom_bp.route('/api/analytics/student/<student_id>/detailed')
def get_student_detailed_analytics(student_id):
    if student_id not in students:
        return jsonify({"error": "学生不存在"}), 404
    basic = real_analyzer.analyze_student_metrics(student_id, students[student_id])
    classroom_id = _find_student_classroom(student_id)
    if classroom_id:
        data_manager.save_student_metrics(student_id, classroom_id, students[student_id])
    return jsonify({
        'student_info': {'name': students[student_id].get('name'), 'id': student_id},
        'basic_analysis': basic,
        'timestamp': datetime.now().isoformat()
    })


@ai_classroom_bp.route('/api/analytics/classroom/<classroom_id>/summary')
def get_classroom_detailed_summary(classroom_id):
    if classroom_id not in classrooms:
        return jsonify({"error": "课堂不存在"}), 404
    classroom = classrooms[classroom_id]
    summary_data = data_manager.get_classroom_summary(classroom_id)
    att = [s.get('attention_level', 0) for s in classroom['students']]
    eng = [s.get('engagement_level', 0) for s in classroom['students']]
    return jsonify({
        'classroom_overview': {
            'total_students': len(classroom['students']),
            'active_students': len([s for s in classroom['students'] if s.get('attention_level', 0) > 50]),
            'avg_attention': round(float(np.mean(att)), 1) if att else 0,
            'avg_engagement': round(float(np.mean(eng)), 1) if eng else 0,
        },
        'performance_distribution': {
            'excellent_students': len([s for s in classroom['students'] if s.get('attention_level', 0) >= 80]),
            'good_students': len([s for s in classroom['students'] if 60 <= s.get('attention_level', 0) < 80]),
            'needs_help_students': len([s for s in classroom['students'] if s.get('attention_level', 0) < 60])
        },
        'historical_data': summary_data.to_dict('records') if not summary_data.empty else []
    })


@ai_classroom_bp.route('/api/analytics/student/<student_id>/history')
def get_student_historical_data(student_id):
    hours = request.args.get('hours', 24, type=int)
    history = data_manager.get_student_history(student_id, hours)
    if history.empty:
        return jsonify({"error": "无历史数据"}), 404
    return jsonify({
        'student_id': student_id,
        'time_range_hours': hours,
        'data_points': len(history),
        'historical_data': history.to_dict('records')
    })


# ── SocketIO 事件注册 ─────────────────────────────────────────────────────────────
def register_socketio_events(socketio: SocketIO):
    """Bind SocketIO event handlers to the provided SocketIO instance."""

    @socketio.on('connect')
    def handle_connect():
        logger.info("客户端连接: %s", request.sid)

    @socketio.on('disconnect')
    def handle_disconnect():
        logger.info("客户端断开: %s", request.sid)

    @socketio.on('join_classroom')
    def handle_join_classroom(data):
        classroom_id = data['classroom_id']
        student_id = data['student_id']
        student_name = data['student_name']
        if classroom_id in classrooms:
            student_info = {
                'id': student_id,
                'name': student_name,
                'socket_id': request.sid,
                'attention_level': 50,
                'engagement_level': 50,
                'emotion': 'neutral',
                'detailed_metrics': {
                    'gaze_focus': 50,
                    'posture_engagement': 50,
                    'interaction_frequency': 50,
                    'facial_engagement': 50
                },
                'data_source': 'real_ai_analysis',
                'joined_at': datetime.now().isoformat(),
                'last_updated': datetime.now().isoformat()
            }
            classrooms[classroom_id]['students'].append(student_info)
            students[student_id] = student_info
            emit('classroom_update', classrooms[classroom_id], room=classroom_id)
            emit('student_joined', student_info, room=classroom_id)
            logger.info("学生 %s 加入课堂 %s", student_name, classroom_id)

    @socketio.on('teacher_join')
    def handle_teacher_join(data):
        classroom_id = data['classroom_id']
        teacher_id = data['teacher_id']
        socketio.server.enter_room(request.sid, classroom_id)
        logger.info("教师 %s 加入课堂 %s", teacher_id, classroom_id)

    @socketio.on('start_quick_response')
    def handle_quick_response(data):
        classroom_id = data['classroom_id']
        question = data['question']
        options = data.get('options', [])
        activity = {
            'type': 'quick_response',
            'question': question,
            'options': options,
            'started_at': datetime.now().isoformat(),
            'responses': []
        }
        classrooms[classroom_id]['activities'].append(activity)
        emit('new_question', {
            'question': question,
            'options': options,
            'activity_id': len(classrooms[classroom_id]['activities']) - 1
        }, room=classroom_id)

    @socketio.on('student_response')
    def handle_student_response(data):
        classroom_id = data['classroom_id']
        student_id = data['student_id']
        activity_id = data['activity_id']
        response = data['response']
        if classroom_id in classrooms and activity_id < len(classrooms[classroom_id]['activities']):
            rec = {
                'student_id': student_id,
                'student_name': students[student_id]['name'],
                'response': response,
                'timestamp': datetime.now().isoformat()
            }
            classrooms[classroom_id]['activities'][activity_id]['responses'].append(rec)
            emit('response_received', {
                'activity_id': activity_id,
                'response': rec,
                'total_responses': len(classrooms[classroom_id]['activities'][activity_id]['responses'])
            }, room=classroom_id)

    @socketio.on('student_feedback')
    def handle_student_feedback(data):
        classroom_id = data['classroom_id']
        student_id = data['student_id']
        feedback_type = data['feedback_type']
        msg_map = {
            'understand': '理解了内容',
            'confused': '有疑问需要解答',
            'faster': '希望讲快一些',
            'slower': '希望讲慢一些'
        }
        emit('student_feedback_received', {
            'student_id': student_id,
            'student_name': students.get(student_id, {}).get('name', student_id),
            'feedback_type': feedback_type,
            'feedback_message': msg_map.get(feedback_type, feedback_type),
            'timestamp': datetime.now().isoformat()
        }, room=classroom_id)

    @socketio.on('update_student_metrics')
    def handle_update_metrics(data):
        student_id = data['student_id']
        metrics = data['metrics']
        data_source = data.get('source', 'simulated')
        if student_id not in students:
            return
        students[student_id].update(metrics)
        students[student_id]['last_updated'] = datetime.now().isoformat()
        students[student_id]['data_source'] = data_source

        classroom_id = _find_student_classroom(student_id)
        if classroom_id:
            data_manager.save_student_metrics(student_id, classroom_id, metrics)

        if data_source == 'real_ai_analysis':
            analysis = real_analyzer.analyze_student_metrics(student_id, metrics)
        else:
            analysis = _get_student_analytics_simple(students[student_id])

        for cid, classroom in classrooms.items():
            for s in classroom['students']:
                if s['id'] == student_id:
                    s.update(metrics)
                    s['last_updated'] = datetime.now().isoformat()
                    s['data_source'] = data_source
                    emit('student_metrics_updated', {
                        'student_id': student_id,
                        'metrics': metrics,
                        'analytics': analysis,
                        'data_source': data_source,
                        'timestamp': datetime.now().isoformat()
                    }, room=cid)
                    rt = classroom['real_time_metrics'].setdefault(student_id, [])
                    rt.append({'timestamp': datetime.now().isoformat(), 'metrics': metrics})
                    if len(rt) > 20:
                        rt.pop(0)
                    break
