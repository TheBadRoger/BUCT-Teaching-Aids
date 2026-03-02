from flask import Flask, render_template, request, jsonify, send_from_directory
from flask_socketio import SocketIO, emit
from flask_cors import CORS
import json
import random
from datetime import datetime, timedelta
import time
import numpy as np
import os
import cv2
import base64
from typing import Dict, List, Optional
import logging
import sqlite3
import pandas as pd
from collections import defaultdict

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key'
CORS(app)
socketio = SocketIO(app, cors_allowed_origins="*", async_mode='threading')

# 数据存储
classrooms = {}
students = {}
teacher_dashboards = {}

# 情感状态分类
EMOTION_CATEGORIES = {
    'positive': ['focused', 'happy', 'engaged', 'excited'],
    'neutral': ['neutral', 'calm', 'thinking'],
    'negative': ['confused', 'bored', 'frustrated', 'distracted']
}

# 注意力指标维度
ATTENTION_METRICS = ['gaze_focus', 'posture_engagement', 'interaction_frequency', 'task_persistence']

# ==================== 数据持久化存储 ====================
class StudentDataManager:
    def __init__(self):
        self.init_database()
    
    def init_database(self):
        """初始化SQLite数据库"""
        conn = sqlite3.connect('classroom_data.db')
        cursor = conn.cursor()
        
        # 创建学生指标表
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
        
        # 创建课堂活动表
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
        
        # 创建学习分析表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS learning_analytics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT NOT NULL,
                date DATE,
                avg_attention REAL,
                avg_engagement REAL,
                focus_duration INTEGER,
                learning_state TEXT
            )
        ''')
        
        conn.commit()
        conn.close()
        logger.info("数据库初始化完成")
    
    def save_student_metrics(self, student_id, classroom_id, metrics):
        """保存学生指标数据"""
        try:
            conn = sqlite3.connect('classroom_data.db')
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
            return True
        except Exception as e:
            logger.error(f"保存学生指标失败: {e}")
            return False
    
    def get_student_history(self, student_id, hours=24):
        """获取学生历史数据"""
        try:
            conn = sqlite3.connect('classroom_data.db')
            time_threshold = datetime.now() - timedelta(hours=hours)
            
            df = pd.read_sql_query('''
                SELECT * FROM student_metrics 
                WHERE student_id = ? AND timestamp > ?
                ORDER BY timestamp
            ''', conn, params=[student_id, time_threshold])
            
            conn.close()
            return df
        except Exception as e:
            logger.error(f"获取学生历史数据失败: {e}")
            return pd.DataFrame()
    
    def get_classroom_summary(self, classroom_id):
        """获取课堂总结数据"""
        try:
            conn = sqlite3.connect('classroom_data.db')
            
            # 获取今日数据统计
            today = datetime.now().date()
            df = pd.read_sql_query('''
                SELECT 
                    COUNT(*) as total_records,
                    AVG(attention_level) as avg_attention,
                    AVG(engagement_level) as avg_engagement,
                    emotion,
                    COUNT(*) as emotion_count
                FROM student_metrics 
                WHERE classroom_id = ? AND DATE(timestamp) = ?
                GROUP BY emotion
            ''', conn, params=[classroom_id, today])
            
            conn.close()
            return df
        except Exception as e:
            logger.error(f"获取课堂总结失败: {e}")
            return pd.DataFrame()

# ==================== 真实学生分析器 ====================
class RealStudentAnalyzer:
    def __init__(self):
        self.analysis_history = {}
        logger.info("真实学生分析器初始化完成")
    
    def analyze_student_metrics(self, student_id: str, metrics: Dict) -> Dict:
        """分析学生指标并生成学习洞察"""
        try:
            # 存储历史数据
            if student_id not in self.analysis_history:
                self.analysis_history[student_id] = []
            
            self.analysis_history[student_id].append({
                'timestamp': datetime.now().isoformat(),
                'metrics': metrics
            })
            
            # 限制历史数据长度
            if len(self.analysis_history[student_id]) > 50:
                self.analysis_history[student_id].pop(0)
            
            # 生成学习洞察
            insights = self.generate_learning_insights(student_id, metrics)
            
            # 计算趋势
            trends = self.calculate_learning_trends(student_id)
            
            return {
                'insights': insights,
                'trends': trends,
                'learning_state': self.assess_learning_state(metrics),
                'recommendations': self.generate_recommendations(metrics, insights)
            }
            
        except Exception as e:
            logger.error(f"分析学生指标时出错: {e}")
            return {
                'insights': [],
                'trends': {},
                'learning_state': 'unknown',
                'recommendations': []
            }
    
    def generate_learning_insights(self, student_id: str, current_metrics: Dict) -> List[str]:
        """生成学习洞察"""
        insights = []
        
        attention = current_metrics.get('attention_level', 0)
        engagement = current_metrics.get('engagement_level', 0)
        
        # 基于当前指标生成洞察
        if attention >= 80 and engagement >= 80:
            insights.append("学生处于高度专注状态")
        elif attention >= 60:
            insights.append("学生注意力水平良好")
        elif attention <= 40:
            insights.append("学生可能需要关注，注意力较低")
        
        # 基于详细指标生成洞察
        detailed_metrics = current_metrics.get('detailed_metrics', {})
        if detailed_metrics.get('gaze_focus', 0) < 50:
            insights.append("视线专注度有待提高")
        if detailed_metrics.get('posture_engagement', 0) < 50:
            insights.append("坐姿参与度需要改善")
        
        return insights
    
    def calculate_learning_trends(self, student_id: str) -> Dict:
        """计算学习趋势"""
        if student_id not in self.analysis_history or len(self.analysis_history[student_id]) < 2:
            return {'attention_trend': 'stable', 'engagement_trend': 'stable'}
        
        history = self.analysis_history[student_id]
        recent_attention = [entry['metrics'].get('attention_level', 0) for entry in history[-5:]]
        recent_engagement = [entry['metrics'].get('engagement_level', 0) for entry in history[-5:]]
        
        # 计算简单趋势
        attention_trend = 'improving' if len(recent_attention) > 1 and recent_attention[-1] > recent_attention[0] else 'declining'
        engagement_trend = 'improving' if len(recent_engagement) > 1 and recent_engagement[-1] > recent_engagement[0] else 'declining'
        
        return {
            'attention_trend': attention_trend,
            'engagement_trend': engagement_trend,
            'stability_score': self.calculate_stability_score(recent_attention)
        }
    
    def calculate_stability_score(self, values: List[float]) -> float:
        """计算稳定性分数"""
        if len(values) < 2:
            return 100.0
        
        variance = np.var(values)
        stability = max(0, 100 - variance * 2)
        return round(stability, 1)
    
    def assess_learning_state(self, metrics: Dict) -> str:
        """评估学习状态"""
        attention = metrics.get('attention_level', 0)
        engagement = metrics.get('engagement_level', 0)
        
        if attention >= 80 and engagement >= 80:
            return "excellent"
        elif attention >= 60 and engagement >= 60:
            return "good"
        elif attention >= 40:
            return "moderate"
        else:
            return "needs_help"
    
    def generate_recommendations(self, metrics: Dict, insights: List[str]) -> List[str]:
        """生成教学建议"""
        recommendations = []
        attention = metrics.get('attention_level', 0)
        
        if attention < 50:
            recommendations.append("建议与学生互动，提高参与度")
        if '视线专注度有待提高' in insights:
            recommendations.append("可以提醒学生保持视线专注")
        if '坐姿参与度需要改善' in insights:
            recommendations.append("建议关注学生坐姿，保持良好学习姿态")
        
        return recommendations

# ==================== 高级分析引擎 ====================
class AnomalyDetector:
    """异常检测器"""
    def detect_attention_anomalies(self, attention_series):
        """检测注意力异常"""
        if len(attention_series) < 10:
            return []
        
        anomalies = []
        rolling_mean = attention_series.rolling(window=5).mean()
        rolling_std = attention_series.rolling(window=5).std()
        
        for i in range(5, len(attention_series)):
            if abs(attention_series.iloc[i] - rolling_mean.iloc[i]) > 2 * rolling_std.iloc[i]:
                anomalies.append({
                    'timestamp': attention_series.index[i],
                    'value': attention_series.iloc[i],
                    'type': 'statistical_outlier'
                })
        
        return anomalies

class TrendAnalyzer:
    """趋势分析器"""
    def analyze_trend(self, data_series):
        """分析数据趋势"""
        if len(data_series) < 2:
            return "insufficient_data"
        
        # 简单线性趋势判断
        x = np.arange(len(data_series))
        y = data_series.values
        slope = np.polyfit(x, y, 1)[0]
        
        if slope > 0.5:
            return "strong_improving"
        elif slope > 0.1:
            return "improving"
        elif slope < -0.5:
            return "strong_declining"
        elif slope < -0.1:
            return "declining"
        else:
            return "stable"

class EngagementPredictor:
    """参与度预测器"""
    def predict_future_engagement(self, historical_data):
        """预测未来参与度"""
        # 简化版的预测逻辑
        if len(historical_data) < 5:
            return "无法预测（数据不足）"
        
        recent_trend = historical_data.tail(5)
        if recent_trend.mean() > 70:
            return "预计保持高参与度"
        elif recent_trend.mean() < 40:
            return "预计参与度可能继续偏低"
        else:
            return "预计参与度保持稳定"

class AdvancedLearningAnalytics:
    def __init__(self):
        self.anomaly_detector = AnomalyDetector()
        self.trend_analyzer = TrendAnalyzer()
        self.engagement_predictor = EngagementPredictor()
    
    def analyze_learning_patterns(self, student_id):
        """分析学生学习模式"""
        history_data = data_manager.get_student_history(student_id)
        
        if history_data.empty:
            return {"error": "无足够历史数据"}
        
        analysis = {
            'attention_patterns': self.analyze_attention_patterns(history_data),
            'engagement_trends': self.analyze_engagement_trends(history_data),
            'anomalies': self.detect_anomalies(history_data),
            'recommendations': self.generate_personalized_recommendations(history_data),
            'learning_insights': self.extract_learning_insights(history_data)
        }
        
        return analysis
    
    def analyze_attention_patterns(self, data):
        """分析注意力模式"""
        attention_data = data['attention_level']
        
        patterns = {
            'average_attention': round(attention_data.mean(), 1),
            'attention_stability': round(100 - attention_data.std(), 1),
            'high_attention_percentage': round((attention_data > 80).sum() / len(attention_data) * 100, 1),
            'low_attention_percentage': round((attention_data < 50).sum() / len(attention_data) * 100, 1),
            'attention_trend': 'improving' if attention_data.iloc[-1] > attention_data.iloc[0] else 'declining'
        }
        
        return patterns
    
    def analyze_engagement_trends(self, data):
        """分析参与度趋势"""
        engagement_data = data['engagement_level']
        
        # 计算时间段内的参与度变化
        hourly_engagement = data.groupby(pd.to_datetime(data['timestamp']).dt.hour)['engagement_level'].mean()
        
        trends = {
            'average_engagement': round(engagement_data.mean(), 1),
            'peak_engagement_hour': hourly_engagement.idxmax() if not hourly_engagement.empty else None,
            'engagement_consistency': round(100 - engagement_data.std(), 1),
            'trend_direction': 'positive' if engagement_data.iloc[-1] > engagement_data.mean() else 'negative'
        }
        
        return trends
    
    def detect_anomalies(self, data):
        """检测异常学习行为"""
        anomalies = []
        
        # 检测注意力骤降
        attention_data = data['attention_level']
        if len(attention_data) > 5:
            recent_avg = attention_data.tail(5).mean()
            overall_avg = attention_data.mean()
            if recent_avg < overall_avg - 20:
                anomalies.append({
                    'type': 'attention_drop',
                    'severity': 'high',
                    'message': '近期注意力水平明显下降',
                    'suggestion': '建议关注学生学习状态，适当调整教学节奏'
                })
        
        # 检测持续低参与度
        low_engagement_count = (data['engagement_level'] < 40).sum()
        if low_engagement_count > len(data) * 0.3:
            anomalies.append({
                'type': 'sustained_low_engagement',
                'severity': 'medium',
                'message': '学生参与度持续偏低',
                'suggestion': '建议增加互动环节，提高学生参与感'
            })
        
        return anomalies
    
    def generate_personalized_recommendations(self, data):
        """生成个性化学习建议"""
        recommendations = []
        
        avg_attention = data['attention_level'].mean()
        avg_engagement = data['engagement_level'].mean()
        
        if avg_attention < 60:
            recommendations.append({
                'type': 'attention_improvement',
                'priority': 'high',
                'action': '缩短单次学习时长，增加休息间隔',
                'reason': f'平均注意力水平较低 ({avg_attention:.1f}%)'
            })
        
        if avg_engagement < 60:
            recommendations.append({
                'type': 'engagement_boost',
                'priority': 'medium',
                'action': '采用更多互动式教学方法',
                'reason': f'参与度有待提升 ({avg_engagement:.1f}%)'
            })
        
        # 基于时间模式推荐
        hourly_attention = data.groupby(pd.to_datetime(data['timestamp']).dt.hour)['attention_level'].mean()
        if not hourly_attention.empty:
            best_hour = hourly_attention.idxmax()
            recommendations.append({
                'type': 'optimal_learning_time',
                'priority': 'low',
                'action': f'建议在{best_hour}:00-{best_hour+1}:00安排重要学习内容',
                'reason': '该时段注意力表现最佳'
            })
        
        return recommendations
    
    def extract_learning_insights(self, data):
        """提取学习洞察"""
        insights = []
        
        # 注意力稳定性分析
        attention_std = data['attention_level'].std()
        if attention_std < 15:
            insights.append("学习注意力表现稳定")
        else:
            insights.append("注意力波动较大，需要关注学习环境")
        
        # 学习效率分析
        high_attention_time = (data['attention_level'] > 70).sum()
        efficiency_ratio = high_attention_time / len(data)
        if efficiency_ratio > 0.7:
            insights.append("学习效率较高，大部分时间保持专注")
        elif efficiency_ratio < 0.4:
            insights.append("学习效率有待提升，专注时间不足")
        
        return insights

# 初始化组件
data_manager = StudentDataManager()
real_analyzer = RealStudentAnalyzer()
advanced_analytics = AdvancedLearningAnalytics()

# ==================== 辅助函数 ====================
def find_student_classroom(student_id):
    """查找学生所在的课堂"""
    for classroom_id, classroom in classrooms.items():
        for student in classroom['students']:
            if student['id'] == student_id:
                return classroom_id
    return None

def get_student_analytics(student):
    """生成学生个体的分析数据（模拟版本）"""
    attention_level = student.get('attention_level', 0)
    engagement_level = student.get('engagement_level', 0)
    emotion = student.get('emotion', 'neutral')
    
    # 计算综合学习状态
    learning_state = "excellent"
    if attention_level < 50 or engagement_level < 50:
        learning_state = "needs_help"
    elif attention_level < 70 or engagement_level < 70:
        learning_state = "moderate"
    
    return {
        'learning_state': learning_state,
        'attention_trend': 'improving' if random.random() > 0.5 else 'stable',
        'engagement_score': engagement_level,
        'focus_duration': random.randint(5, 25)  # 模拟持续专注时间（分钟）
    }

# ==================== Flask路由 ====================
# 静态文件路由 - 修正路径指向frontend目录
@app.route('/')
def index():
    return send_from_directory('../frontend', 'index.html')

@app.route('/<path:filename>')
def serve_static(filename):
    return send_from_directory('../frontend', filename)

@app.route('/css/<path:filename>')
def serve_css(filename):
    return send_from_directory('../frontend/css', filename)

@app.route('/api/classroom/create', methods=['POST'])
def create_classroom():
    data = request.json
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
    
    logger.info(f"创建课堂: {classroom_id}")
    
    return jsonify({
        "success": True,
        "classroom_id": classroom_id,
        "message": "Classroom created successfully"
    })

@app.route('/api/classroom/<classroom_id>/status')
def get_classroom_status(classroom_id):
    if classroom_id not in classrooms:
        return jsonify({"error": "Classroom not found"}), 404
    
    return jsonify(classrooms[classroom_id])

@app.route('/api/classroom/<classroom_id>/analytics')
def get_classroom_analytics(classroom_id):
    if classroom_id not in classrooms:
        return jsonify({"error": "Classroom not found"}), 404
    
    classroom = classrooms[classroom_id]
    students = classroom['students']
    
    if not students:
        return jsonify({"error": "No students in classroom"}), 400
    
    # 计算情感分布
    emotion_counts = {'positive': 0, 'neutral': 0, 'negative': 0}
    attention_levels = []
    engagement_levels = []
    
    for student in students:
        # 情感分类统计
        for category, emotions in EMOTION_CATEGORIES.items():
            if student.get('emotion') in emotions:
                emotion_counts[category] += 1
                break
        
        attention_levels.append(student.get('attention_level', 0))
        engagement_levels.append(student.get('engagement_level', 0))
    
    # 计算注意力趋势
    current_time = datetime.now().isoformat()
    avg_attention = np.mean(attention_levels) if attention_levels else 0
    avg_engagement = np.mean(engagement_levels) if engagement_levels else 0
    
    if len(classroom['attention_trend']) > 10:
        classroom['attention_trend'].pop(0)
    
    classroom['attention_trend'].append({
        'timestamp': current_time,
        'avg_attention': round(avg_attention, 1),
        'avg_engagement': round(avg_engagement, 1)
    })
    
    analytics = {
        'emotion_distribution': emotion_counts,
        'attention_metrics': {
            'average_attention': round(avg_attention, 1),
            'average_engagement': round(avg_engagement, 1),
            'attention_variance': round(np.var(attention_levels), 2) if attention_levels else 0,
            'high_attention_count': len([a for a in attention_levels if a > 80]),
            'low_attention_count': len([a for a in attention_levels if a < 50])
        },
        'attention_trend': classroom['attention_trend'][-5:],  # 最近5个数据点
        'total_students': len(students),
        'real_time_analysis': len([s for s in students if s.get('data_source') == 'real_ai_analysis'])
    }
    
    return jsonify(analytics)

@app.route('/api/student/<student_id>/analytics')
def get_student_analytics_route(student_id):
    """获取学生个体分析数据"""
    if student_id not in students:
        return jsonify({"error": "Student not found"}), 404
    
    student = students[student_id]
    analytics = real_analyzer.analyze_student_metrics(student_id, student)
    
    return jsonify({
        'student_info': {
            'name': student.get('name'),
            'id': student_id
        },
        'current_metrics': {
            'attention_level': student.get('attention_level'),
            'engagement_level': student.get('engagement_level'),
            'emotion': student.get('emotion')
        },
        'analysis': analytics
    })

# ==================== 新增API接口 ====================
@app.route('/api/analytics/student/<student_id>/detailed')
def get_student_detailed_analytics(student_id):
    """获取学生详细分析报告"""
    if student_id not in students:
        return jsonify({"error": "学生不存在"}), 404
    
    # 基础分析
    basic_analysis = real_analyzer.analyze_student_metrics(student_id, students[student_id])
    
    # 高级分析
    advanced_analysis = advanced_analytics.analyze_learning_patterns(student_id)
    
    # 保存数据
    classroom_id = find_student_classroom(student_id)
    if classroom_id:
        data_manager.save_student_metrics(
            student_id, 
            classroom_id, 
            students[student_id]
        )
    
    return jsonify({
        'student_info': {
            'name': students[student_id].get('name'),
            'id': student_id
        },
        'basic_analysis': basic_analysis,
        'advanced_analysis': advanced_analysis,
        'timestamp': datetime.now().isoformat()
    })

@app.route('/api/analytics/classroom/<classroom_id>/summary')
def get_classroom_detailed_summary(classroom_id):
    """获取课堂详细总结"""
    if classroom_id not in classrooms:
        return jsonify({"error": "课堂不存在"}), 404
    
    classroom = classrooms[classroom_id]
    summary_data = data_manager.get_classroom_summary(classroom_id)
    
    # 计算课堂整体指标
    attention_levels = [s.get('attention_level', 0) for s in classroom['students']]
    engagement_levels = [s.get('engagement_level', 0) for s in classroom['students']]
    
    analysis = {
        'classroom_overview': {
            'total_students': len(classroom['students']),
            'active_students': len([s for s in classroom['students'] if s.get('attention_level', 0) > 50]),
            'avg_attention': round(np.mean(attention_levels), 1) if attention_levels else 0,
            'avg_engagement': round(np.mean(engagement_levels), 1) if engagement_levels else 0,
        },
        'performance_distribution': {
            'excellent_students': len([s for s in classroom['students'] if s.get('attention_level', 0) >= 80]),
            'good_students': len([s for s in classroom['students'] if 60 <= s.get('attention_level', 0) < 80]),
            'needs_help_students': len([s for s in classroom['students'] if s.get('attention_level', 0) < 60])
        },
        'historical_data': summary_data.to_dict('records') if not summary_data.empty else []
    }
    
    return jsonify(analysis)

@app.route('/api/analytics/student/<student_id>/history')
def get_student_historical_data(student_id):
    """获取学生历史数据"""
    hours = request.args.get('hours', 24, type=int)
    history_data = data_manager.get_student_history(student_id, hours)
    
    if history_data.empty:
        return jsonify({"error": "无历史数据"}), 404
    
    return jsonify({
        'student_id': student_id,
        'time_range_hours': hours,
        'data_points': len(history_data),
        'historical_data': history_data.to_dict('records')
    })

# ==================== Socket事件处理 ====================
@socketio.on('connect')
def handle_connect():
    logger.info(f"客户端连接: {request.sid}")

@socketio.on('disconnect')
def handle_disconnect():
    logger.info(f"客户端断开: {request.sid}")

@socketio.on('join_classroom')
def handle_join_classroom(data):
    classroom_id = data['classroom_id']
    student_id = data['student_id']
    student_name = data['student_name']
    
    # 添加学生到教室
    if classroom_id in classrooms:
        # 为真实学生生成基础数据
        student_info = {
            'id': student_id,
            'name': student_name,
            'socket_id': request.sid,
            'attention_level': 50,  # 初始值
            'engagement_level': 50, # 初始值
            'emotion': 'neutral',
            'detailed_metrics': {
                'gaze_focus': 50,
                'posture_engagement': 50,
                'interaction_frequency': 50,
                'facial_engagement': 50
            },
            'data_source': 'real_ai_analysis',  # 标记为真实数据
            'joined_at': datetime.now().isoformat(),
            'last_updated': datetime.now().isoformat()
        }
        
        classrooms[classroom_id]['students'].append(student_info)
        students[student_id] = student_info
        
        # 通知所有用户更新
        emit('classroom_update', classrooms[classroom_id], room=classroom_id)
        emit('student_joined', student_info, room=classroom_id)
        
        logger.info(f"真实学生 {student_name} 加入课堂 {classroom_id}")

@socketio.on('teacher_join')
def handle_teacher_join(data):
    classroom_id = data['classroom_id']
    teacher_id = data['teacher_id']
    
    # 将教师加入到教室的socket房间
    socketio.server.enter_room(request.sid, classroom_id)
    
    logger.info(f"教师 {teacher_id} 加入课堂 {classroom_id}")

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
    
    # 向教室内的所有学生发送问题
    emit('new_question', {
        'question': question,
        'options': options,
        'activity_id': len(classrooms[classroom_id]['activities']) - 1
    }, room=classroom_id)
    
    logger.info(f"课堂 {classroom_id} 发布新问题: {question}")

@socketio.on('student_response')
def handle_student_response(data):
    classroom_id = data['classroom_id']
    student_id = data['student_id']
    activity_id = data['activity_id']
    response = data['response']
    
    if classroom_id in classrooms and activity_id < len(classrooms[classroom_id]['activities']):
        response_record = {
            'student_id': student_id,
            'student_name': students[student_id]['name'],
            'response': response,
            'timestamp': datetime.now().isoformat()
        }
        
        classrooms[classroom_id]['activities'][activity_id]['responses'].append(response_record)
        
        # 更新教师仪表盘
        emit('response_received', {
            'activity_id': activity_id,
            'response': response_record,
            'total_responses': len(classrooms[classroom_id]['activities'][activity_id]['responses'])
        }, room=classroom_id)
        
        logger.info(f"学生 {students[student_id]['name']} 回答问题: {response}")

@socketio.on('student_feedback')
def handle_student_feedback(data):
    """处理学生快速反馈"""
    classroom_id = data['classroom_id']
    student_id = data['student_id']
    feedback_type = data['feedback_type']
    
    feedback_message = {
        'understand': '理解了内容',
        'confused': '有疑问需要解答',
        'faster': '希望讲快一些',
        'slower': '希望讲慢一些'
    }.get(feedback_type, feedback_type)
    
    # 通知教师
    emit('student_feedback_received', {
        'student_id': student_id,
        'student_name': students[student_id]['name'],
        'feedback_type': feedback_type,
        'feedback_message': feedback_message,
        'timestamp': datetime.now().isoformat()
    }, room=classroom_id)
    
    logger.info(f"学生 {students[student_id]['name']} 发送反馈: {feedback_message}")

@socketio.on('update_student_metrics')
def handle_update_metrics(data):
    """更新学生指标 - 支持真实AI数据"""
    student_id = data['student_id']
    metrics = data['metrics']
    data_source = data.get('source', 'simulated')  # 默认为模拟数据
    
    if student_id in students:
        # 更新学生数据
        students[student_id].update(metrics)
        students[student_id]['last_updated'] = datetime.now().isoformat()
        students[student_id]['data_source'] = data_source
        
        # 保存到数据库
        classroom_id = find_student_classroom(student_id)
        if classroom_id:
            data_manager.save_student_metrics(student_id, classroom_id, metrics)
        
        # 生成AI分析
        if data_source == 'real_ai_analysis':
            analysis = real_analyzer.analyze_student_metrics(student_id, metrics)
        else:
            analysis = get_student_analytics(students[student_id])
        
        # 找到学生所在的教室并广播更新
        for classroom_id, classroom in classrooms.items():
            for student in classroom['students']:
                if student['id'] == student_id:
                    student.update(metrics)
                    student['last_updated'] = datetime.now().isoformat()
                    student['data_source'] = data_source
                    
                    # 发送分析数据更新
                    emit('student_metrics_updated', {
                        'student_id': student_id,
                        'metrics': metrics,
                        'analytics': analysis,
                        'data_source': data_source,
                        'timestamp': datetime.now().isoformat()
                    }, room=classroom_id)
                    
                    # 更新课堂实时指标
                    if classroom_id not in classroom['real_time_metrics']:
                        classroom['real_time_metrics'][student_id] = []
                    
                    classroom['real_time_metrics'][student_id].append({
                        'timestamp': datetime.now().isoformat(),
                        'metrics': metrics
                    })
                    
                    # 限制数据长度
                    if len(classroom['real_time_metrics'][student_id]) > 20:
                        classroom['real_time_metrics'][student_id].pop(0)
                    
                    break
        
        logger.debug(f"更新学生 {student_id} 指标: 注意力={metrics.get('attention_level')}%")

if __name__ == '__main__':
    logger.info("启动AI智慧课堂服务器...")
    socketio.run(app, host='0.0.0.0', port=5000, debug=True)