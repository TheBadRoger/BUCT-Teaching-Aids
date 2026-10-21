"""验证统计结果稳定且生成内容不会参与下一轮统计。"""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import update_project_stats as stats


class ProjectStatsTest(unittest.TestCase):
    def test_repeated_generation_is_identical(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            generated = root / 'scripts' / 'generated'
            readme = root / 'README.md'
            readme.write_text('# Example\n', encoding='utf-8')
            (root / 'page.vue').write_text('<template>Hello</template>\n', encoding='utf-8')
            with patch.multiple(stats, ROOT_DIR=root, README_PATH=readme,
                                GENERATED_DIR=generated,
                                STATS_JSON_PATH=generated / 'stats.json',
                                STATS_MD_PATH=generated / 'README_STATS.md'), \
                    patch.object(stats.subprocess, 'check_output', return_value='1700000000\n'):
                stats.main()
                paths = [readme, stats.STATS_JSON_PATH, stats.STATS_MD_PATH]
                before = [path.read_bytes() for path in paths]
                stats.main()
                self.assertEqual(before, [path.read_bytes() for path in paths])
                self.assertEqual(stats.collect_stats()['language_lines'], {'Vue': 1})


if __name__ == '__main__':
    unittest.main()
