"""Tests for the AI classroom core engines and new routes."""
import numpy as np
import pytest

from flask import Flask
from flask_socketio import SocketIO

from modules.ai_classroom.routes import (
    ai_classroom_bp,
    register_socketio_events,
    classrooms,
    students,
    group_discussions,
    quizzes,
)
from modules.ai_classroom.core.emotion_detector import (
    detect_emotion_from_landmarks,
    classify_emotion_category,
    EMOTION_LABELS,
)
from modules.ai_classroom.core.attention_analyzer import AttentionAnalyzer
from modules.ai_classroom.core.report_generator import (
    generate_classroom_report,
    generate_student_report,
)


# ── Fixtures ─────────────────────────────────────────────────────────────────────
@pytest.fixture
def app():
    app = Flask(__name__, template_folder='../../templates')
    app.config["TESTING"] = True
    app.register_blueprint(ai_classroom_bp)
    socketio = SocketIO(app, cors_allowed_origins="*", async_mode='threading')
    register_socketio_events(socketio)
    return app


@pytest.fixture
def client(app):
    return app.test_client()


@pytest.fixture(autouse=True)
def reset_state():
    """Clear in-memory stores before each test."""
    classrooms.clear()
    students.clear()
    group_discussions.clear()
    quizzes.clear()
    yield
    classrooms.clear()
    students.clear()
    group_discussions.clear()
    quizzes.clear()


# ── Emotion Detector Tests ───────────────────────────────────────────────────────
class TestEmotionDetector:
    def test_classify_emotion_category(self):
        assert classify_emotion_category('happy') == 'positive'
        assert classify_emotion_category('focused') == 'positive'
        assert classify_emotion_category('neutral') == 'neutral'
        assert classify_emotion_category('thinking') == 'neutral'
        assert classify_emotion_category('bored') == 'negative'
        assert classify_emotion_category('confused') == 'negative'
        assert classify_emotion_category('unknown_emotion') == 'neutral'

    def test_detect_emotion_from_landmarks_returns_valid_structure(self):
        # Create a dummy 68-point landmark array
        landmarks = np.array(np.random.rand(68, 2) * 100, dtype=np.float64)
        result = detect_emotion_from_landmarks(landmarks)
        assert 'emotion' in result
        assert 'scores' in result
        assert 'features' in result
        assert result['emotion'] in EMOTION_LABELS
        assert 'mar' in result['features']
        assert 'ear' in result['features']
        assert 'brow' in result['features']


# ── Attention Analyzer Tests ─────────────────────────────────────────────────────
class TestAttentionAnalyzer:
    def test_analyze_returns_valid_metrics(self):
        analyzer = AttentionAnalyzer(window_size=5)
        result = analyzer.analyze(
            student_id='test_001',
            head_up_rate=80.0,
            face_detected=True,
            emotion='happy',
            hand_up=False,
        )
        assert 'attention_level' in result
        assert 'engagement_level' in result
        assert 'detailed_metrics' in result
        assert 'emotion' in result
        assert 0 <= result['attention_level'] <= 100
        assert 0 <= result['engagement_level'] <= 100
        assert result['emotion'] == 'happy'

    def test_record_interaction_increases_score(self):
        analyzer = AttentionAnalyzer(window_size=5)
        # No interactions
        result1 = analyzer.analyze('test_002', emotion='neutral')
        # Record several interactions
        for _ in range(5):
            analyzer.record_interaction('test_002')
        result2 = analyzer.analyze('test_002', emotion='neutral')
        # Interaction frequency should increase
        assert result2['detailed_metrics']['interaction_frequency'] >= result1['detailed_metrics']['interaction_frequency']

    def test_high_attention_student(self):
        analyzer = AttentionAnalyzer(window_size=5)
        result = analyzer.analyze(
            student_id='test_003',
            head_up_rate=95.0,
            face_detected=True,
            emotion='focused',
            hand_up=True,
        )
        assert result['attention_level'] > 70

    def test_low_attention_student(self):
        analyzer = AttentionAnalyzer(window_size=5)
        result = analyzer.analyze(
            student_id='test_004',
            head_up_rate=10.0,
            face_detected=False,
            emotion='distracted',
            hand_up=False,
        )
        assert result['attention_level'] < 50


# ── Report Generator Tests ─��─────────────────────────────────────────────────────
class TestReportGenerator:
    def test_generate_classroom_report_empty(self):
        classroom = {
            'id': 'test_class',
            'teacher': 'Test Teacher',
            'subject': 'Math',
            'students': [],
            'activities': [],
        }
        report = generate_classroom_report(classroom)
        assert 'message' in report
        assert report['report_meta']['total_students'] == 0

    def test_generate_classroom_report_with_students(self):
        classroom = {
            'id': 'test_class',
            'teacher': 'Test Teacher',
            'subject': 'Math',
            'students': [
                {'id': 's1', 'name': 'Alice', 'attention_level': 85, 'engagement_level': 90, 'emotion': 'happy'},
                {'id': 's2', 'name': 'Bob', 'attention_level': 40, 'engagement_level': 30, 'emotion': 'bored'},
                {'id': 's3', 'name': 'Charlie', 'attention_level': 60, 'engagement_level': 65, 'emotion': 'neutral'},
            ],
            'activities': [
                {'type': 'quick_response', 'question': 'Q1', 'responses': [{'student_id': 's1'}, {'student_id': 's3'}]},
            ],
        }
        report = generate_classroom_report(classroom)
        assert report['report_meta']['total_students'] == 3
        assert 'overall_summary' in report
        assert 'emotion_distribution' in report
        assert 'student_rankings' in report
        assert 'teaching_suggestions' in report
        assert len(report['student_rankings']['top_students']) <= 5
        assert len(report['student_rankings']['needs_help']) <= 5
        # Alice should be in top, Bob in needs_help
        top_ids = [s['student_id'] for s in report['student_rankings']['top_students']]
        help_ids = [s['student_id'] for s in report['student_rankings']['needs_help']]
        assert 's1' in top_ids
        assert 's2' in help_ids

    def test_generate_student_report(self):
        student = {
            'id': 's1',
            'name': 'Alice',
            'attention_level': 85,
            'engagement_level': 90,
            'emotion': 'happy',
            'detailed_metrics': {
                'gaze_focus': 80,
                'posture_engagement': 85,
                'interaction_frequency': 70,
                'facial_engagement': 90,
            },
        }
        report = generate_student_report(student)
        assert report['student_info']['name'] == 'Alice'
        assert report['current_state']['learning_state'] == 'excellent'
        assert report['current_state']['emotion_category'] == 'positive'
        assert len(report['suggestions']) > 0


# ── HTTP Route Tests ─────────────────────────────────────────────────────────────
class TestClassroomRoutes:
    def test_create_classroom(self, client):
        resp = client.post('/api/classroom/create', json={
            'classroom_id': 'test_room',
            'teacher_name': 'Prof. Test',
            'subject': 'Physics',
        })
        assert resp.status_code == 200
        data = resp.get_json()
        assert data['success'] is True
        assert data['classroom_id'] == 'test_room'

    def test_get_classroom_status(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room1'})
        resp = client.get('/api/classroom/room1/status')
        assert resp.status_code == 200
        data = resp.get_json()
        assert data['id'] == 'room1'

    def test_get_classroom_status_not_found(self, client):
        resp = client.get('/api/classroom/nonexistent/status')
        assert resp.status_code == 404

    def test_classroom_report(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room2'})
        resp = client.get('/api/classroom/room2/report')
        assert resp.status_code == 200
        data = resp.get_json()
        assert 'report_meta' in data

    def test_list_discussions_empty(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room3'})
        resp = client.get('/api/classroom/room3/discussions')
        assert resp.status_code == 200
        data = resp.get_json()
        assert data['discussions'] == []

    def test_list_quizzes_empty(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room4'})
        resp = client.get('/api/classroom/room4/quizzes')
        assert resp.status_code == 200
        data = resp.get_json()
        assert data['quizzes'] == []

    def test_analyze_frame_no_classroom(self, client):
        resp = client.post('/api/classroom/nonexistent/analyze_frame', json={
            'frame': 'dummy_base64'
        })
        assert resp.status_code == 404

    def test_analyze_frame_missing_frame(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room5'})
        resp = client.post('/api/classroom/room5/analyze_frame', json={})
        assert resp.status_code == 400

    def test_speech_recognize_missing_audio(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room6'})
        resp = client.post('/api/classroom/room6/speech_recognize', json={})
        assert resp.status_code == 400

    def test_snapshot_missing_students(self, client):
        client.post('/api/classroom/create', json={'classroom_id': 'room7'})
        resp = client.post('/api/classroom/room7/snapshot', json={})
        assert resp.status_code == 400

    def test_student_report_not_found(self, client):
        resp = client.get('/api/student/nonexistent/report')
        assert resp.status_code == 404

