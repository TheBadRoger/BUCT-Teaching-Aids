/**
 * 猜你需要 - 推荐课程列表
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    function renderCourses(courses) {
        var grid = document.querySelector('.course-grid');
        if (!grid) return;
        if (!courses || courses.length === 0) {
            grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:#999;">暂无推荐课程</div>';
            return;
        }
        grid.innerHTML = courses.map(function(c) {
            return '<div class="course-card">' +
                '<div class="cover">' + (c.courseImage ? '<img src="' + c.courseImage + '" alt="' + (c.courseName || '') + '" style="width:100%;height:100%;object-fit:cover;">' : '课程封面图') + '</div>' +
                '<div class="info">' +
                    '<div class="title">' + (c.courseName || '课程名称') + '</div>' +
                    '<div class="meta">共' + (c.duration || '?') + '课时 | 教参' + (c.teachingMaterials || 0) + '/笔记' + (c.notesCount || 0) + '/问答' + (c.qaCount || 0) + '</div>' +
                '</div>' +
            '</div>';
        }).join('');
    }

    // 从热门课程接口获取推荐
    fetch(API_BASE + '/course/view/popular?limit=6')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data && data.code === 2000 && data.data) {
                renderCourses(data.data);
            } else {
                renderCourses([]);
            }
        })
        .catch(function() {
            renderCourses([
                { courseName: 'Python入门', duration: 15 },
                { courseName: '数据结构', duration: 20 },
                { courseName: 'Web开发实战', duration: 18 }
            ]);
        });

    // 搜索
    var searchBtn = document.querySelector('.top-bar .search button, .search-area button');
    var searchInput = document.querySelector('.top-bar .search input, .search-area input');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', function () {
            var keyword = searchInput.value.trim();
            if (keyword) {
                window.location.href = 'search.html?keyword=' + encodeURIComponent(keyword);
            }
        });
    }
});