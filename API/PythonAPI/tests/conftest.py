from flask import Flask
import pytest

from modules.pbl.routes import pbl_bp
from modules.student_analysis.routes import student_analysis_bp


@pytest.fixture
def app():
    app = Flask(__name__)
    app.config["TESTING"] = True
    app.register_blueprint(pbl_bp)
    app.register_blueprint(student_analysis_bp)
    return app


@pytest.fixture
def client(app):
    return app.test_client()


@pytest.fixture
def assert_api_shape():
    def _assert(payload, required_keys):
        assert isinstance(payload, dict)
        for key in required_keys:
            assert key in payload
    return _assert
