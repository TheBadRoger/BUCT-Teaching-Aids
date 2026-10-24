"""容器连接串配置的回归测试，不加载后端或访问数据库。"""
import unittest
from urllib.parse import unquote, urlsplit

from entrypoint import database_url, load_credentials
from pathlib import Path
import tempfile


class DatabaseUrlTest(unittest.TestCase):
    def test_special_characters_round_trip(self):
        password = "a@b:/#?$%'\\"
        url = urlsplit(database_url({'MYSQL_PASSWORD': password, 'MYSQL_USER': 'a@b'}))
        self.assertEqual(unquote(url.password), password)
        self.assertEqual(unquote(url.username), 'a@b')
        self.assertEqual(url.hostname, 'mysql')
        self.assertEqual(url.port, 3306)

    def test_explicit_url_is_preserved(self):
        self.assertEqual(database_url({'DATABASE_URL': 'mysql+pymysql://custom/db'}),
                         'mysql+pymysql://custom/db')

    def test_missing_password_fails(self):
        with self.assertRaises(KeyError):
            database_url({})

    def test_container_credentials_override_development_values(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, 'python_user').write_text('generated_user')
            Path(directory, 'python_password').write_text('generated_password')
            env = {'CREDENTIALS_DIR': directory, 'MYSQL_USER': 'development', 'MYSQL_PASSWORD': 'development', 'DATABASE_URL': 'old'}
            load_credentials(env)
            self.assertEqual(env['MYSQL_USER'], 'generated_user')
            self.assertEqual(env['MYSQL_PASSWORD'], 'generated_password')
            self.assertNotIn('DATABASE_URL', env)

    def test_development_environment_is_unchanged(self):
        env = {'MYSQL_USER': 'dev', 'MYSQL_PASSWORD': 'manual'}
        load_credentials(env)
        self.assertEqual(env, {'MYSQL_USER': 'dev', 'MYSQL_PASSWORD': 'manual'})

    def test_missing_container_credentials_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError):
                load_credentials({'CREDENTIALS_DIR': directory})


if __name__ == '__main__':
    unittest.main()
