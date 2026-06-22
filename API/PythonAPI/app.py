import os
import logging

from flask import Flask
from flask_cors import CORS
from flask_socketio import SocketIO

from modules.pbl.routes import pbl_bp
from modules.ai_classroom.routes import ai_classroom_bp, register_socketio_events
from modules.face_hand_up.routes import face_hand_up_bp
from modules.headup_rate.routes import headup_rate_bp
from modules.student_analysis.routes import student_analysis_bp
from config import Config

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(name)s: %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['SECRET_KEY'] = Config.SECRET_KEY
CORS(app)
# NOTE: In production, restrict CORS to your actual frontend origin(s), e.g.:
# CORS(app, origins=["https://your-frontend.example.com"])
socketio = SocketIO(app, cors_allowed_origins="*", async_mode='threading')

# Register Flask Blueprints
app.register_blueprint(pbl_bp)
app.register_blueprint(ai_classroom_bp)
app.register_blueprint(face_hand_up_bp)
app.register_blueprint(headup_rate_bp)
app.register_blueprint(student_analysis_bp)

# Register SocketIO event handlers for the AI classroom module
register_socketio_events(socketio)


@app.route('/health')
def health():
    return {'status': 'ok', 'message': 'BUCT Teaching Aids – Unified Python Backend'}


if __name__ == '__main__':
    # Ensure upload directory exists for PBL module
    os.makedirs(Config.UPLOAD_DIR, exist_ok=True)
    logger.info("启动BUCT教学辅助系统统一后端，端口 %d ...", Config.PORT)
    socketio.run(app, host='0.0.0.0', port=Config.PORT, debug=Config.DEBUG)
