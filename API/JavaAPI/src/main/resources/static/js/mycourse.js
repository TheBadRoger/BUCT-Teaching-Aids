/**
 * 我的课 - 获取学生所有课程列表
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    function getCurrentUser() {
        return fetch(API_BASE + '/user/auth/current')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) return data.data;
                throw new Error('未登录');
            });
    }

    function renderCourses(courses) {
        var grid = document.querySelector('.course-grid');
        if (!grid) return;
        if (!courses || courses.length === 0) {
            grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:#999;">暂无课程</div>';
            return;
        }
        grid.innerHTML = courses.map(function(sc) {
            var course = sc.course || sc;
            return '<div class="course-card">' +
                '<div class="cover">' + (course.courseImage ? '<img src="' + course.courseImage + '" alt="" style="width:100%;height:100%;object-fit:cover;">' : '课程封面图') + '</div>' +
                '<div class="info">' +
                    '<div class="title">' + (course.courseName || '课程名称') + '</div>' +
                    '<div class="meta">共' + (course.duration || '?') + '课时 | 教参' + (course.teachingMaterials || 0) + '/笔记' + (course.notesCount || 0) + '/问答' + (course.qaCount || 0) + '</div>' +
                '</div>' +
            '</div>';
        }).join('');
    }

    getCurrentUser()
        .then(function(user) {
            fetch(API_BASE + '/student-courses/all-courses?studentId=' + user.id + '&page=0&size=20&sort=id&direction=desc')
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (data && data.code === 2000 && data.data) {
                        renderCourses(data.data.content || data.data);
                    } else {
                        renderCourses([]);
                    }
                })
                .catch(function(err) { console.error('加载课程失败', err); renderCourses([]); });
        })
        .catch(function() {
            renderCourses([
                { course: { courseName: 'HTML 与语义化', duration: 12 } },
                { course: { courseName: 'CSS 布局实战', duration: 14 } },
                { course: { courseName: 'JavaScript 入门与实践', duration: 20 } }
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