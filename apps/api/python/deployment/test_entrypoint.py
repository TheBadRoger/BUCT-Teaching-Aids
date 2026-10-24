"""容器连接串配置的回归测试，不加载后端或访问数据库。"""
import unittest
from urllib.parse import unquote, urlsplit

from entrypoint import database_url


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


if __name__ == '__main__':
    unittest.main()
