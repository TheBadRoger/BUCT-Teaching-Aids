import json
from pathlib import Path
import tempfile
import unittest
from init_credentials import initialize


class CredentialsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.cleanup)
        self.base = Path(self.temp.name) / 'secrets'
        self.database = Path(self.temp.name) / 'database'

    def cleanup(self):
        for path in Path(self.temp.name).rglob('*'):
            if path.is_file():
                path.chmod(0o600)
        self.temp.cleanup()

    def test_generates_matching_separate_application_credentials(self):
        initialize(self.base, self.database)
        read = lambda name: (self.base / name).read_text(encoding='utf-8')
        self.assertEqual(read('mysql/java_user'), read('java/spring.datasource.username'))
        self.assertEqual(read('mysql/java_password'), read('java/spring.datasource.password'))
        self.assertEqual(read('mysql/python_password'), read('python/python_password'))
        self.assertEqual(read('redis/password'), read('java/spring.data.redis.password'))
        self.assertIn('requirepass ' + read('redis/password'), read('redis/redis.conf'))
        self.assertNotEqual(read('mysql/java_password'), read('mysql/python_password'))
        self.assertEqual(len(read('mysql/root_password')), 64)
        self.assertFalse((self.base / 'java/root_password').exists())

    def test_restart_reuses_accounts_and_passwords(self):
        initialize(self.base, self.database)
        before = {p.relative_to(self.base): p.read_bytes() for p in self.base.rglob('*') if p.is_file()}
        (self.database / 'mysql').mkdir(parents=True)
        initialize(self.base, self.database)
        after = {p.relative_to(self.base): p.read_bytes() for p in self.base.rglob('*') if p.is_file()}
        self.assertEqual(before, after)

    def test_existing_database_without_credentials_fails_closed(self):
        (self.database / 'mysql').mkdir(parents=True)
        with self.assertRaisesRegex(RuntimeError, 'original credential volume'):
            initialize(self.base, self.database)
        self.assertFalse((self.base / 'mysql/credentials.json').exists())

    def test_separate_deployments_get_different_credentials(self):
        initialize(self.base, self.database)
        other = self.base.parent / 'other'
        initialize(other, self.database)
        self.assertNotEqual((self.base / 'mysql/root_password').read_bytes(), (other / 'mysql/root_password').read_bytes())

    def test_damaged_master_is_not_silently_rotated(self):
        initialize(self.base, self.database)
        path = self.base / 'mysql/credentials.json'
        path.chmod(0o600)
        value = json.loads(path.read_text())
        value['java_user'] = "bad'user"
        path.write_text(json.dumps(value))
        with self.assertRaisesRegex(ValueError, 'Invalid persisted account'):
            initialize(self.base, self.database)
