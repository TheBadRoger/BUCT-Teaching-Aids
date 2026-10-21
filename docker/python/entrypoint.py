"""仅供容器使用的启动器，不改变后端应用代码。"""
import os
from pathlib import Path
from urllib.parse import quote


def database_url(environ):
    """按独立环境变量生成连接串，转义密码中的特殊字符。"""
    if environ.get('DATABASE_URL'):
        return environ['DATABASE_URL']
    user = quote(environ.get('MYSQL_USER', 'python_flask_buctta'), safe='')
    password = quote(environ['MYSQL_PASSWORD'], safe='')
    host = environ.get('MYSQL_HOST', 'mysql')
    port = int(environ.get('MYSQL_PORT', '3306'))
    database = quote(environ.get('MYSQL_DB', 'BUCTTA_DATABASE'), safe='')
    return f'mysql+pymysql://{user}:{password}@{host}:{port}/{database}?charset=utf8mb4'


def main():
    """准备数据路径并以单进程、多线程 Gunicorn 启动 Socket.IO 应用。"""
    os.environ['DATABASE_URL'] = database_url(os.environ)
    for key, default in [('UPLOAD_DIR', '/app/data/uploads'),
                         ('YOLO_CONFIG_DIR', '/app/data/ultralytics'),
                         ('FACE_DB_PATH', '/app/face_database/face_db')]:
        Path(os.environ.get(key, default)).mkdir(parents=True, exist_ok=True)
    Path(os.environ.get('CLASSROOM_DB_PATH', '/app/data/classroom_data.db')).parent.mkdir(parents=True, exist_ok=True)
    os.execvp('gunicorn', [
        'gunicorn', '--bind', '0.0.0.0:8080', '--workers', '1', '--worker-class', 'gthread',
        '--threads', os.environ.get('GUNICORN_THREADS', '8'),
        '--timeout', os.environ.get('GUNICORN_TIMEOUT', '120'),
        '--graceful-timeout', '30', '--access-logfile', '-', '--error-logfile', '-', 'app:app',
    ])


if __name__ == '__main__':
    main()
