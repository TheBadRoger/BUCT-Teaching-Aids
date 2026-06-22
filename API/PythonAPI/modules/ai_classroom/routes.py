"""AI Classroom blueprint and SocketIO event handlers.

Adapted from API/PythonAPI/ai课堂/backend/app.py (Flask + Flask-SocketIO).

Extended with:
  - Emotion detection, attention analysis, speech recognition core engines
  - Group discussion, real-time quiz, classroom report generation
  - Computer-vision frame analysis endpoint
"""
import logging
import sqlite3
from datetime import datetime, timedelta
from typing import Dict, List

import numpy as np
import pandas as pd
import time

from flask import Blueprint, request, jsonify, render_template
from flask_socketio import SocketIO, emit

from config import Config
from modules.ai_classroom.core.emotion_detector import detect_emotion_from_frame
from modules.ai_classroom.core.attention_analyzer import analyzer as attention_analyzer
from modules.ai_classroom.core.speech_recognizer import analyze_speech
from modules.ai_classroom.core.report_generator import generate_classroom_report, generate_student_report

logger = logging.getLogger(__name__)

ai_classroom_bp = Blueprint('ai_classroom', __name__)

# ── 内存数据存储 ────────────────────────────────────────────────────────────────
classrooms: Dict = {}
students: Dict = {}

# ── 分组讨论数据 ────────────────────────────────────────────────────────────────
group_discussions: Dict = {}  # discussion_id -> {classroom_id, groups: {group_id: {students, messages}}}

# ── 实时测评数据 ────────────────────────────────────────────────────────────────
quizzes: Dict = {}  # quiz_id -> {classroom_id, questions, submissions, started_at, ended_at}

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
                conn, params=[str(student_id), str(threshold)]
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
            ''', conn, params=[str(classroom_id), str(today)])
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
            'stability_score': round(max(0.0, 100.0 - float(np.var(recent_att)) * 2), 1)
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


# ── 前端页面路由 ──────────────────────────────────────────────────────────────────
@ai_classroom_bp.route('/classroom/')
@ai_classroom_bp.route('/classroom/index.html')
def classroom_index():
    """AI智慧课堂首页"""
    return render_template('ai_classroom/index.html')


@ai_classroom_bp.route('/classroom/teacher-dashboard.html')
def classroom_teacher_dashboard():
    """教师仪表盘页面"""
    return render_template('ai_classroom/teacher-dashboard.html')


@ai_classroom_bp.route('/classroom/student-view.html')
def classroom_student_view():
    """学生视图页面"""
    return render_template('ai_classroom/student-view.html')


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


@ai_classroom_bp.route('/api/student/<student_id>/history')
def get_student_historical_data(student_id):
    hours = request.args.get('hours', 24, type=int)
    history = data_manager.get_student_history(student_id, hours)
    if history.empty:
        return jsonify({"error": "无历史数据"}), 404
    return jsonify({
        'student_id': student_id,
        'time_range_hours': hours,
        'data_points': len(history.index),
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

    # ── 分组讨论 SocketIO 事件 ───────────────────────────────────────
    @socketio.on('start_group_discussion')
    def handle_start_group_discussion(data):
        classroom_id = data['classroom_id']
        topic = data.get('topic', '')
        num_groups = data.get('num_groups', 4)
        discussion_id = f"discussion_{int(time.time())}"

        if classroom_id not in classrooms:
            emit('error', {'message': '课堂不存在'})
            return

        classroom_students = classrooms[classroom_id]['students']
        # Shuffle and split into groups
        shuffled = list(classroom_students)
        import random
        random.shuffle(shuffled)
        groups = {}
        group_size = max(1, len(shuffled) // num_groups)
        for i in range(num_groups):
            start = i * group_size
            end = start + group_size if i < num_groups - 1 else len(shuffled)
            group_members = shuffled[start:end]
            groups[f'group_{i+1}'] = {
                'group_id': f'group_{i+1}',
                'students': [s['id'] for s in group_members],
                'student_names': [s['name'] for s in group_members],
                'messages': []
            }

        group_discussions[discussion_id] = {
            'discussion_id': discussion_id,
            'classroom_id': classroom_id,
            'topic': topic,
            'groups': groups,
            'started_at': datetime.now().isoformat(),
            'ended_at': None
        }
        # Record as classroom activity
        classrooms[classroom_id]['activities'].append({
            'type': 'group_discussion',
            'topic': topic,
            'discussion_id': discussion_id,
            'started_at': datetime.now().isoformat(),
            'responses': []
        })
        emit('group_discussion_started', {
            'discussion_id': discussion_id,
            'topic': topic,
            'groups': groups
        }, room=classroom_id)
        logger.info("课堂 %s 启动分组讨论: %s", classroom_id, discussion_id)

    @socketio.on('group_discussion_message')
    def handle_group_message(data):
        discussion_id = data['discussion_id']
        group_id = data['group_id']
        student_id = data['student_id']
        message = data['message']

        if discussion_id not in group_discussions:
            emit('error', {'message': '讨论不存在'})
            return
        discussion = group_discussions[discussion_id]
        if group_id not in discussion['groups']:
            emit('error', {'message': '小组不存在'})
            return

        msg = {
            'student_id': student_id,
            'student_name': students.get(student_id, {}).get('name', student_id),
            'message': message,
            'timestamp': datetime.now().isoformat()
        }
        discussion['groups'][group_id]['messages'].append(msg)
        # Record interaction for attention analysis
        attention_analyzer.record_interaction(student_id)

        classroom_id = discussion['classroom_id']
        emit('group_message_received', {
            'discussion_id': discussion_id,
            'group_id': group_id,
            'message': msg
        }, room=classroom_id)

    @socketio.on('end_group_discussion')
    def handle_end_group_discussion(data):
        discussion_id = data['discussion_id']
        if discussion_id in group_discussions:
            group_discussions[discussion_id]['ended_at'] = datetime.now().isoformat()
            classroom_id = group_discussions[discussion_id]['classroom_id']
            emit('group_discussion_ended', {
                'discussion_id': discussion_id,
                'summary': _summarize_discussion(group_discussions[discussion_id])
            }, room=classroom_id)
            logger.info("分组讨论结束: %s", discussion_id)

    # ── 实时测评 SocketIO 事件 ───────────────────────────────────────
    @socketio.on('start_quiz')
    def handle_start_quiz(data):
        classroom_id = data['classroom_id']
        questions = data.get('questions', [])
        quiz_id = f"quiz_{int(time.time())}"

        if classroom_id not in classrooms:
            emit('error', {'message': '课堂不存在'})
            return

        quizzes[quiz_id] = {
            'quiz_id': quiz_id,
            'classroom_id': classroom_id,
            'questions': questions,
            'submissions': {},  # student_id -> {question_id: answer}
            'started_at': datetime.now().isoformat(),
            'ended_at': None
        }
        classrooms[classroom_id]['activities'].append({
            'type': 'quiz',
            'quiz_id': quiz_id,
            'question_count': len(questions),
            'started_at': datetime.now().isoformat(),
            'responses': []
        })
        emit('quiz_started', {
            'quiz_id': quiz_id,
            'questions': questions
        }, room=classroom_id)
        logger.info("课堂 %s 启动实时测评: %s (%d题)", classroom_id, quiz_id, len(questions))

    @socketio.on('submit_quiz_answer')
    def handle_submit_quiz_answer(data):
        quiz_id = data['quiz_id']
        student_id = data['student_id']
        question_id = data['question_id']
        answer = data['answer']

        if quiz_id not in quizzes:
            emit('error', {'message': '测评不存在'})
            return
        quiz = quizzes[quiz_id]
        if student_id not in quiz['submissions']:
            quiz['submissions'][student_id] = {}
        quiz['submissions'][student_id][question_id] = {
            'answer': answer,
            'timestamp': datetime.now().isoformat()
        }
        attention_analyzer.record_interaction(student_id)

        classroom_id = quiz['classroom_id']
        emit('quiz_answer_received', {
            'quiz_id': quiz_id,
            'student_id': student_id,
            'student_name': students.get(student_id, {}).get('name', student_id),
            'question_id': question_id,
            'total_submissions': len(quiz['submissions'])
        }, room=classroom_id)

    @socketio.on('end_quiz')
    def handle_end_quiz(data):
        quiz_id = data['quiz_id']
        if quiz_id in quizzes:
            quiz = quizzes[quiz_id]
            quiz['ended_at'] = datetime.now().isoformat()
            classroom_id = quiz['classroom_id']
            result = _summarize_quiz(quiz)
            emit('quiz_ended', {
                'quiz_id': quiz_id,
                'result': result
            }, room=classroom_id)
            logger.info("实时测评结束: %s", quiz_id)


# ── 辅助函数：讨论/测评总结 ─────────────────────────────────────────────────────
def _summarize_discussion(discussion: Dict) -> Dict:
    groups = discussion['groups']
    return {
        'discussion_id': discussion['discussion_id'],
        'topic': discussion['topic'],
        'group_count': len(groups),
        'total_messages': sum(len(g['messages']) for g in groups.values()),
        'group_summaries': [
            {
                'group_id': gid,
                'member_count': len(g['students']),
                'message_count': len(g['messages']),
                'active_members': len(set(m['student_id'] for m in g['messages']))
            }
            for gid, g in groups.items()
        ]
    }


def _summarize_quiz(quiz: Dict) -> Dict:
    questions = quiz['questions']
    submissions = quiz['submissions']
    total_students = len(students) if students else 0
    submission_count = len(submissions)

    question_stats = []
    for q in questions:
        qid = q.get('id') or q.get('question_id')
        correct_answer = q.get('correct_answer')
        answers = [s.get(qid, {}).get('answer') for s in submissions.values() if qid in s]
        correct_count = sum(1 for a in answers if correct_answer is not None and a == correct_answer)
        question_stats.append({
            'question_id': qid,
            'question': q.get('question', ''),
            'response_count': len(answers),
            'correct_count': correct_count,
            'correct_rate': round(correct_count / len(answers) * 100, 1) if answers else 0
        })

    return {
        'quiz_id': quiz['quiz_id'],
        'question_count': len(questions),
        'submission_count': submission_count,
        'participation_rate': round(submission_count / total_students * 100, 1) if total_students else 0,
        'question_stats': question_stats
    }


# ── 新增 HTTP 路由：CV帧分析 ─────────────────────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/<classroom_id>/analyze_frame', methods=['POST'])
def analyze_frame(classroom_id):
    """分析课堂摄像头帧：情感检测 + 注意力评估。

    JSON body:
    {
        "student_id": "s001",          # 可选，关联到具体学生
        "frame": "<base64>",           # base64编码的图片
        "head_up_rate": 85.0,          # 可选，前端已计算的抬头率
        "hand_up": false               # 可选，是否举手
    }
    """
    if classroom_id not in classrooms:
        return jsonify({"error": "课堂不存在"}), 404

    data = request.json or {}
    frame_b64 = data.get('frame')
    student_id = data.get('student_id')
    head_up_rate = data.get('head_up_rate', 50.0)
    hand_up = data.get('hand_up', False)

    if not frame_b64:
        return jsonify({"error": "缺少 frame 参数"}), 400

    # Decode base64 image
    try:
        import base64
        import cv2
        img_bytes = base64.b64decode(str(frame_b64))
        img_np = np.frombuffer(img_bytes, dtype=np.uint8)
        frame = cv2.imdecode(img_np, cv2.IMREAD_COLOR)
        if frame is None:
            return jsonify({"error": "图片解码失败"}), 400
    except Exception as e:
        return jsonify({"error": f"图片解码异常: {e}"}), 400

    # Emotion detection
    emotion_result = detect_emotion_from_frame(frame)
    emotion = emotion_result['emotion']

    # Attention analysis
    face_detected = emotion_result.get('features', {}) != {}
    analysis = attention_analyzer.analyze(
        student_id=student_id or 'anonymous',
        head_up_rate=head_up_rate,
        face_detected=face_detected,
        emotion=emotion,
        hand_up=hand_up
    )

    # If student_id is provided and exists, update their metrics
    if student_id and student_id in students:
        students[student_id].update(analysis)
        students[student_id]['last_updated'] = datetime.now().isoformat()
        students[student_id]['data_source'] = 'real_ai_analysis'
        classroom_id_found = _find_student_classroom(student_id)
        if classroom_id_found:
            data_manager.save_student_metrics(student_id, classroom_id_found, analysis)

    return jsonify({
        "success": True,
        "emotion": emotion_result,
        "attention_analysis": analysis,
        "timestamp": datetime.now().isoformat()
    })


# ── 新增 HTTP 路由：语音识别 ─────────────────────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/<classroom_id>/speech_recognize', methods=['POST'])
def speech_recognize(classroom_id):
    """语音识别：转录音频并检测课堂互动关键词。

    JSON body:
    {
        "student_id": "s001",       # 可选
        "audio": "<base64>",        # base64编码的音频(wav/aiff/flac)
        "language": "zh-CN"         # 可选，默认中文
    }
    """
    if classroom_id not in classrooms:
        return jsonify({"error": "课堂不存在"}), 404

    data = request.json or {}
    audio_b64 = data.get('audio')
    student_id = data.get('student_id')
    language = data.get('language', 'zh-CN')

    if not audio_b64:
        return jsonify({"error": "缺少 audio 参数"}), 400

    result = analyze_speech(str(audio_b64), language=language)

    # Record interaction if student is identified and speech was successful
    if result['success'] and student_id:
        attention_analyzer.record_interaction(student_id)
        if student_id in students:
            # Update facial_engagement metric with speech engagement score
            dm = students[student_id].get('detailed_metrics', {})
            dm['interaction_frequency'] = result['engagement_score']
            students[student_id]['detailed_metrics'] = dm
            students[student_id]['last_updated'] = datetime.now().isoformat()

    return jsonify({
        "success": result['success'],
        "text": result['text'],
        "keywords": result.get('keywords', {}),
        "engagement_score": result.get('engagement_score', 0),
        "error": result.get('error'),
        "timestamp": datetime.now().isoformat()
    })


# ── 新增 HTTP 路由：课堂互动报告 ─────────────────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/<classroom_id>/report')
def get_classroom_report(classroom_id):
    """生成课堂互动报告。

    Query params:
    - save: 是否保存到数据库 (true/false, 默认 false)
    """
    if classroom_id not in classrooms:
        return jsonify({"error": "课堂不存在"}), 404

    classroom = classrooms[classroom_id]
    report = generate_classroom_report(classroom)

    # Optionally save report as a classroom activity
    if request.args.get('save', 'false').lower() == 'true':
        try:
            conn = sqlite3.connect(Config.CLASSROOM_DB_PATH)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO classroom_activities
                (classroom_id, activity_type, question, responses_count, start_time, end_time)
                VALUES (?, ?, ?, ?, ?, ?)
            ''', (
                classroom_id,
                'report',
                report.get('report_meta', {}).get('generated_at', ''),
                report.get('overall_summary', {}).get('total_students', 0) if 'total_students' in report.get('overall_summary', {}) else 0,
                classroom.get('created_at'),
                datetime.now().isoformat()
            ))
            conn.commit()
            conn.close()
        except Exception as e:
            logger.warning("报告保存到数据库失败: %s", e)

    return jsonify(report)


@ai_classroom_bp.route('/api/student/<student_id>/report')
def get_student_report_route(student_id):
    """生成学生个人课堂报告。

    Query params:
    - hours: 历史数据时间范围（默认24小时）
    """
    if student_id not in students:
        return jsonify({"error": "学生不存在"}), 404

    hours = request.args.get('hours', 24, type=int)
    history_df = data_manager.get_student_history(student_id, hours)
    if not isinstance(history_df, pd.DataFrame):
        history_df = pd.DataFrame()
    report = generate_student_report(students[student_id], history_df)
    return jsonify(report)


# ── 新增 HTTP 路由：分组讨论管理 ─────────────────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/<classroom_id>/discussions')
def list_discussions(classroom_id):
    """获取课堂中的所有分组讨论记录。"""
    result = []
    for did, disc in group_discussions.items():
        if disc['classroom_id'] == classroom_id:
            result.append(_summarize_discussion(disc))
    return jsonify({"discussions": result})


@ai_classroom_bp.route('/api/classroom/<classroom_id>/discussions/<discussion_id>')
def get_discussion_detail(classroom_id, discussion_id):
    """获取分组讨论详情（含所有小组消息）。"""
    if discussion_id not in group_discussions:
        return jsonify({"error": "讨论不存在"}), 404
    disc = group_discussions[discussion_id]
    if disc['classroom_id'] != classroom_id:
        return jsonify({"error": "讨论不属于该课堂"}), 404
    return jsonify(disc)


# ── 新增 HTTP 路由：实时测评管理 ─────────────────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/<classroom_id>/quizzes')
def list_quizzes(classroom_id):
    """获取课堂中的所有实时测评记录。"""
    result = []
    for qid, quiz in quizzes.items():
        if quiz['classroom_id'] == classroom_id:
            result.append(_summarize_quiz(quiz))
    return jsonify({"quizzes": result})


@ai_classroom_bp.route('/api/classroom/<classroom_id>/quizzes/<quiz_id>')
def get_quiz_detail(classroom_id, quiz_id):
    """获取实时测评详情（含所有提交）。"""
    if quiz_id not in quizzes:
        return jsonify({"error": "测评不存在"}), 404
    quiz = quizzes[quiz_id]
    if quiz['classroom_id'] != classroom_id:
        return jsonify({"error": "测评不属于该课堂"}), 404
    return jsonify({
        'quiz_id': quiz['quiz_id'],
        'questions': quiz['questions'],
        'submissions': quiz['submissions'],
        'started_at': quiz['started_at'],
        'ended_at': quiz['ended_at'],
        'summary': _summarize_quiz(quiz)
    })


# ── 新增 HTTP 路由：批量CV分析（整堂课快照） ─────────────────────────────────────
@ai_classroom_bp.route('/api/classroom/<classroom_id>/snapshot', methods=['POST'])
def classroom_snapshot(classroom_id):
    """批量分析课堂快照：接收多个学生的帧数据，一次性分析。

    JSON body:
    {
        "students": [
            {
                "student_id": "s001",
                "frame": "<base64>",
                "head_up_rate": 85.0,
                "hand_up": false
            },
            ...
        ]
    }
    """
    if classroom_id not in classrooms:
        return jsonify({"error": "课堂不存在"}), 404

    data = request.json or {}
    student_frames = data.get('students', [])
    if not student_frames:
        return jsonify({"error": "缺少 students 数据"}), 400

    import base64
    import cv2

    results = []
    for sf in student_frames:
        sid = sf.get('student_id')
        frame_b64 = sf.get('frame')
        head_up_rate = sf.get('head_up_rate', 50.0)
        hand_up = sf.get('hand_up', False)

        if not frame_b64:
            results.append({
                'student_id': sid,
                'error': '缺少 frame 参数'
            })
            continue

        try:
            img_bytes = base64.b64decode(frame_b64)
            img_np = np.frombuffer(img_bytes, dtype=np.uint8)
            frame = cv2.imdecode(img_np, cv2.IMREAD_COLOR)
            if frame is None:
                results.append({'student_id': sid, 'error': '图片解码失败'})
                continue
        except Exception as e:
            results.append({'student_id': sid, 'error': f'解码异常: {e}'})
            continue

        emotion_result = detect_emotion_from_frame(frame)
        emotion = emotion_result['emotion']
        face_detected = emotion_result.get('features', {}) != {}
        analysis = attention_analyzer.analyze(
            student_id=sid or 'anonymous',
            head_up_rate=head_up_rate,
            face_detected=face_detected,
            emotion=emotion,
            hand_up=hand_up
        )

        if sid and sid in students:
            students[sid].update(analysis)
            students[sid]['last_updated'] = datetime.now().isoformat()
            students[sid]['data_source'] = 'real_ai_analysis'
            cid = _find_student_classroom(sid)
            if cid:
                data_manager.save_student_metrics(sid, cid, analysis)

        results.append({
            'student_id': sid,
            'emotion': emotion_result,
            'attention_analysis': analysis
        })

    return jsonify({
        "success": True,
        "analyzed_count": len(results),
        "results": results,
        "timestamp": datetime.now().isoformat()
    })

