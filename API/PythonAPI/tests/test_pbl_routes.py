from modules.pbl import routes


def test_pbl_index_endpoint(client):
    response = client.get("/api/pbl/")

    assert response.status_code == 200
    body = response.get_json()
    assert body["module"] == "PBL生成器"
    assert body["docs"] == "/api/pbl/templates"


def test_pbl_health_endpoint(client):
    response = client.get("/api/pbl/health")

    assert response.status_code == 200
    assert response.get_json()["status"] == "PBL模块正常运行"


def test_pbl_templates_endpoint(client):
    response = client.get("/api/pbl/templates")

    assert response.status_code == 200
    body = response.get_json()
    assert body["success"] is True
    assert "数学" in body["templates"]


def test_generate_pbl_success(client):
    response = client.post(
        "/api/pbl/generate",
        data={
            "grade_subject": "五年级数学",
            "textbook_version": "人教版",
            "unit": "第一单元",
            "requirements": "需要包含分组展示",
            "content": "这是课文示例内容",
        },
    )

    assert response.status_code == 200
    body = response.get_json()
    assert body["success"] is True
    assert "PBL生成成功" in body["message"]
    assert "五年级数学" in body["preview_content"]
    assert body["download_link"] == "/api/pbl/download/五年级数学"


def test_generate_pbl_truncates_long_content(client):
    long_content = "A" * 800

    response = client.post(
        "/api/pbl/generate",
        data={
            "grade_subject": "六年级数学",
            "textbook_version": "人教版",
            "unit": "第二单元",
            "content": long_content,
        },
    )

    assert response.status_code == 200
    body = response.get_json()
    assert body["success"] is True
    # Route appends content[:500] + "..."
    assert ("A" * 500) in body["pbl_content"]
    assert ("A" * 501) not in body["pbl_content"]


def test_generate_pbl_handles_template_error(client, monkeypatch):
    monkeypatch.setitem(routes.PBL_TEMPLATES, "数学", "{不存在的变量}")

    response = client.post(
        "/api/pbl/generate",
        data={
            "grade_subject": "五年级数学",
            "textbook_version": "人教版",
            "unit": "第一单元",
        },
    )

    assert response.status_code == 500
    body = response.get_json()
    assert body["success"] is False
    assert "生成失败" in body["message"]


def test_download_endpoint_replaces_underscore(client):
    response = client.get("/api/pbl/download/grade_1_math")

    assert response.status_code == 200
    body = response.get_json()
    assert body["filename"] == "grade 1 math"

