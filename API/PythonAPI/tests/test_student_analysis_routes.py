from contextlib import contextmanager

from modules.student_analysis import routes


def _fake_session_ctx():
    @contextmanager
    def fake_session():
        yield object()

    return fake_session


def test_student_class_list_missing_class_name_returns_400(client, assert_api_shape):
    response = client.get("/api/student/class_list")

    assert response.status_code == 400
    body = response.get_json()
    assert_api_shape(body, ["code", "msg"])
    assert body["msg"] == "缺少参数 class_name"


def test_student_class_list_success(client, monkeypatch, assert_api_shape):
    expected_data = {"class_name": "高材2304", "student_list": []}

    monkeypatch.setattr(routes, "get_db_session", _fake_session_ctx())
    monkeypatch.setattr(routes.services, "get_class_student_list", lambda db, class_name: expected_data)

    response = client.get("/api/student/class_list?class_name=高材2304")

    assert response.status_code == 200
    body = response.get_json()
    assert_api_shape(body, ["code", "msg", "data"])
    assert body["code"] == 200
    assert body["data"] == expected_data


def test_student_class_list_internal_error_returns_500(client, monkeypatch, assert_api_shape):
    monkeypatch.setattr(routes, "get_db_session", _fake_session_ctx())

    def raise_error(db, class_name):
        raise RuntimeError("db down")

    monkeypatch.setattr(routes.services, "get_class_student_list", raise_error)

    response = client.get("/api/student/class_list?class_name=高材2304")

    assert response.status_code == 500
    body = response.get_json()
    assert_api_shape(body, ["code", "msg"])
    assert body["code"] == 500


def test_knowledge_class_list_missing_class_name_returns_400(client):
    response = client.get("/api/knowledge/class_list")

    assert response.status_code == 400
    assert response.get_json()["msg"] == "缺少参数 class_name"


def test_knowledge_class_list_success(client, monkeypatch, assert_api_shape):
    expected_data = {"class_name": "高材2304", "knowledge_list": []}
    monkeypatch.setattr(routes, "get_db_session", _fake_session_ctx())
    monkeypatch.setattr(routes.services, "get_class_knowledge_list", lambda db, class_name: expected_data)

    response = client.get("/api/knowledge/class_list?class_name=高材2304")

    assert response.status_code == 200
    body = response.get_json()
    assert_api_shape(body, ["code", "msg", "data"])
    assert body["data"] == expected_data


def test_report_missing_student_id_returns_400(client):
    response = client.get("/api/report/student")

    assert response.status_code == 400
    assert response.get_json()["msg"] == "缺少参数 student_id"


def test_report_student_not_found_returns_404(client, monkeypatch, assert_api_shape):
    monkeypatch.setattr(routes, "get_db_session", _fake_session_ctx())

    def raise_not_found(db, student_id):
        raise ValueError(f"学号 {student_id} 不存在")

    monkeypatch.setattr(routes.services, "get_report_data", raise_not_found)

    response = client.get("/api/report/student?student_id=1001")

    assert response.status_code == 404
    body = response.get_json()
    assert_api_shape(body, ["code", "msg"])
    assert "不存在" in body["msg"]


def test_report_internal_error_returns_500(client, monkeypatch, assert_api_shape):
    monkeypatch.setattr(routes, "get_db_session", _fake_session_ctx())

    def raise_error(db, student_id):
        raise RuntimeError("unexpected")

    monkeypatch.setattr(routes.services, "get_report_data", raise_error)

    response = client.get("/api/report/student?student_id=1001")

    assert response.status_code == 500
    body = response.get_json()
    assert_api_shape(body, ["code", "msg"])
    assert body["code"] == 500
