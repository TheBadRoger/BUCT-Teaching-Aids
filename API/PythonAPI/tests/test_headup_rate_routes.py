from contextlib import contextmanager
from datetime import datetime
import importlib
import sys
import types

from flask import Flask
import pytest


@pytest.fixture
def headup_routes(monkeypatch):
    # Allow importing headup routes even when CV libs are absent in lightweight envs.
    if "cv2" not in sys.modules:
        fake_cv2 = types.ModuleType("cv2")
        fake_cv2.IMREAD_COLOR = 1
        fake_cv2.imdecode = lambda *_args, **_kwargs: None
        monkeypatch.setitem(sys.modules, "cv2", fake_cv2)

    if "numpy" not in sys.modules:
        fake_np = types.ModuleType("numpy")
        fake_np.ndarray = object
        fake_np.uint8 = object
        fake_np.frombuffer = lambda *_args, **_kwargs: b""
        monkeypatch.setitem(sys.modules, "numpy", fake_np)

    module = importlib.import_module("modules.headup_rate.routes")
    return importlib.reload(module)


@pytest.fixture
def headup_client(headup_routes):
    app = Flask(__name__)
    app.config["TESTING"] = True
    app.register_blueprint(headup_routes.headup_rate_bp)
    return app.test_client()


def test_detect_missing_required_fields_returns_400(headup_client):
    response = headup_client.post("/api/headup_rate/detect", json={"student_id": 1001})

    assert response.status_code == 400
    body = response.get_json()
    assert body["code"] == 400
    assert "缺少必填字段" in body["msg"]


def test_detect_runtime_error_from_core_returns_400(headup_client, headup_routes, monkeypatch):
    monkeypatch.setattr(
        headup_routes,
        "detect_head_up_rate",
        lambda **_kwargs: (_ for _ in ()).throw(RuntimeError("core exploded")),
    )

    response = headup_client.post(
        "/api/headup_rate/detect",
        json={
            "student_id": 1001,
            "course_id": "math_101",
            "course_name": "高等数学",
            "data_type": "image",
            "raw_data": "abc",
        },
    )

    # RuntimeError is treated as user error in route and mapped to 400.
    assert response.status_code == 400
    assert response.get_json()["msg"] == "core exploded"


def test_detect_value_error_from_core_returns_400(headup_client, headup_routes, monkeypatch):
    monkeypatch.setattr(
        headup_routes,
        "detect_head_up_rate",
        lambda **_kwargs: (_ for _ in ()).throw(ValueError("bad input")),
    )

    response = headup_client.post(
        "/api/headup_rate/detect",
        json={
            "student_id": 1001,
            "course_id": "math_101",
            "course_name": "高等数学",
            "data_type": "image",
            "raw_data": "abc",
        },
    )

    assert response.status_code == 400
    assert response.get_json()["msg"] == "bad input"


def test_detect_success_even_if_db_write_fails(headup_client, headup_routes, monkeypatch):
    monkeypatch.setattr(headup_routes, "detect_head_up_rate", lambda **_kwargs: 88.5)

    @contextmanager
    def failing_session():
        raise RuntimeError("db unavailable")
        yield

    monkeypatch.setattr(headup_routes, "get_db_session", failing_session)

    response = headup_client.post(
        "/api/headup_rate/detect",
        json={
            "student_id": 1001,
            "course_id": "math_101",
            "course_name": "高等数学",
            "data_type": "video_frame",
            "calculated_rate": 88.5,
        },
    )

    assert response.status_code == 200
    body = response.get_json()
    assert body["code"] == 200
    assert body["data"]["head_up_rate"] == 88.5
    datetime.strptime(body["data"]["detection_time"], "%Y-%m-%d %H:%M:%S")


def test_history_success_caps_limit_and_formats_records(headup_client, headup_routes, monkeypatch):
    class FakeRecord:
        def __init__(self):
            self.id = 1
            self.student_id = 1001
            self.course_id = "math_101"
            self.course_name = "高等数学"
            self.detection_time = datetime(2026, 1, 1, 8, 0, 0)
            self.head_up_rate = 77.5
            self.detection_device = "cam-1"

    class FakeQuery:
        def __init__(self):
            self.last_limit = None

        def order_by(self, *_args, **_kwargs):
            return self

        def filter(self, *_args, **_kwargs):
            return self

        def limit(self, value):
            self.last_limit = value
            return self

        def all(self):
            return [FakeRecord()]

    fake_query = FakeQuery()

    class FakeDB:
        def query(self, *_args, **_kwargs):
            return fake_query

        def add(self, *_args, **_kwargs):
            return None

    @contextmanager
    def fake_session():
        yield FakeDB()

    monkeypatch.setattr(headup_routes, "get_db_session", fake_session)

    response = headup_client.get("/api/headup_rate/history?course_id=math_101&limit=300")

    assert response.status_code == 200
    body = response.get_json()
    assert body["code"] == 200
    assert len(body["data"]) == 1
    assert body["data"][0]["course_id"] == "math_101"
    assert fake_query.last_limit == 100


def test_student_history_success(headup_client, headup_routes, monkeypatch):
    class FakeRecord:
        def __init__(self):
            self.id = 2
            self.course_id = "math_101"
            self.course_name = "高等数学"
            self.detection_time = datetime(2026, 1, 1, 9, 0, 0)
            self.head_up_rate = 90.0

    class FakeQuery:
        def filter(self, *_args, **_kwargs):
            return self

        def order_by(self, *_args, **_kwargs):
            return self

        def limit(self, *_args, **_kwargs):
            return self

        def all(self):
            return [FakeRecord()]

    class FakeDB:
        def query(self, *_args, **_kwargs):
            return FakeQuery()

    @contextmanager
    def fake_session():
        yield FakeDB()

    monkeypatch.setattr(headup_routes, "get_db_session", fake_session)

    response = headup_client.get("/api/headup_rate/student/1001?course_id=math_101&limit=20")

    assert response.status_code == 200
    body = response.get_json()
    assert body["code"] == 200
    assert body["student_id"] == 1001
    assert body["data"][0]["head_up_rate"] == 90.0


def test_history_db_error_returns_500(headup_client, headup_routes, monkeypatch):
    @contextmanager
    def broken_session():
        raise RuntimeError("db broken")
        yield

    monkeypatch.setattr(headup_routes, "get_db_session", broken_session)

    response = headup_client.get("/api/headup_rate/history")

    assert response.status_code == 500
    body = response.get_json()
    assert body["code"] == 500
    assert "db broken" in body["msg"]


def test_student_history_db_error_returns_500(headup_client, headup_routes, monkeypatch):
    @contextmanager
    def broken_session():
        raise RuntimeError("db broken")
        yield

    monkeypatch.setattr(headup_routes, "get_db_session", broken_session)

    response = headup_client.get("/api/headup_rate/student/1001")

    assert response.status_code == 500
    body = response.get_json()
    assert body["code"] == 500
    assert "db broken" in body["msg"]
