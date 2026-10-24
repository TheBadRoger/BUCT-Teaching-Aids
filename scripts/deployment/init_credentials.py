"""Generate persistent container credentials without printing secrets."""
import json
import os
from pathlib import Path
import secrets


def write_secret(path, value, app=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.parent.chmod(0o755)
    if path.exists() and path.read_text(encoding='utf-8') == value:
        return
    temporary = path.with_name(path.name + '.tmp')
    temporary.write_text(value, encoding='utf-8')
    temporary.chmod(0o400 if not app else 0o440)
    if app and os.name == 'posix' and os.geteuid() == 0:
        os.chown(temporary, 10001, 10001)
    temporary.replace(path)


def initialize(base, database):
    base, database = Path(base), Path(database)
    master = base / 'mysql' / 'credentials.json'
    if master.exists():
        credentials = json.loads(master.read_text(encoding='utf-8'))
    else:
        if (database / 'mysql').exists():
            raise RuntimeError('Existing MySQL data requires its original credential volume; restore credentials before starting')
        credentials = {key: secrets.token_hex(32) for key in ['root_password', 'java_password', 'python_password', 'redis_password']}
        credentials['java_user'] = 'java_' + secrets.token_hex(6)
        credentials['python_user'] = 'python_' + secrets.token_hex(6)
        write_secret(master, json.dumps(credentials))
    # Strict validation also prevents SQL injection through a damaged credential volume.
    import re
    for key in ['root_password', 'java_password', 'python_password', 'redis_password']:
        if not re.fullmatch(r'[0-9a-f]{64}', credentials[key]):
            raise ValueError('Invalid persisted credential format')
    for key in ['java_user', 'python_user']:
        if not re.fullmatch(r'(java|python)_[0-9a-f]{12}', credentials[key]):
            raise ValueError('Invalid persisted account format')
    for key in ['root_password', 'java_user', 'java_password', 'python_user', 'python_password']:
        path = base / 'mysql' / key
        write_secret(path, credentials[key])
        path.chmod(0o444)  # MySQL initializes under its image user, not root.
    for key, value in {
        'spring.datasource.username': credentials['java_user'],
        'spring.datasource.password': credentials['java_password'],
        'spring.data.redis.password': credentials['redis_password'],
    }.items():
        write_secret(base / 'java' / key, value, app=True)
    for key in ['python_user', 'python_password']:
        write_secret(base / 'python' / key, credentials[key], app=True)
    # Redis drops to its image user; this dedicated read-only volume is not mounted in other apps.
    for name, value in {'password': credentials['redis_password'], 'redis.conf': 'appendonly yes\nrequirepass ' + credentials['redis_password'] + '\n'}.items():
        path = base / 'redis' / name
        write_secret(path, value)
        path.chmod(0o444)


if __name__ == '__main__':
    os.umask(0o077)
    initialize('/credentials', '/existing-mysql')
