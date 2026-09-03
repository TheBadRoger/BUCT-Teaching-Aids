/**
 * 首页 - 热门课程 & 推荐课程
 * 从后端接口获取数据并渲染
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 渲染课程卡片列表
    function renderCourses(courses, container, emptyMsg) {
        if (!container) return;
        if (!courses || courses.length === 0) {
            container.innerHTML = '<div class="empty-state">' + (emptyMsg || '暂无课程') + '</div>';
            return;
        }
        container.innerHTML = courses.map(function(course) {
            return '<div class="course-card">' +
                '<div class="course-img">' +
                    '<img src="' + (course.courseImage || 'images/test-img.jpg') + '" alt="' + (course.courseName || '课程') + '">' +
                    '<span class="course-tag">' + (course.courseTags || '精品') + '</span>' +
                '</div>' +
                '<div class="course-info">' +
                    '<h3 class="course-title">' + (course.courseName || '课程名称') + '</h3>' +
                    '<div class="course-meta">' +
                        '<span>' + (course.teachingTeachers || '讲师') + '</span>' +
                        '<span>' + (course.viewCount || 0) + '人学习</span>' +
                    '</div>' +
                '</div>' +
            '</div>';
        }).join('');
    }

    // 获取热门课程
    function loadPopularCourses() {
        fetch(API_BASE + '/course/view/popular?limit=8')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    var container = document.querySelector('.course-section:first-child .course-grid');
                    renderCourses(data.data, container, '暂无热门课程');
                }
            })
            .catch(function(err) { console.error('加载热门课程失败', err); });
    }

    // 获取推荐课程
    function loadRecommendedCourses() {
        fetch(API_BASE + '/course/view/top10')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    var sections = document.querySelectorAll('.course-section');
                    var container = sections.length > 1 ? sections[1].querySelector('.course-grid') : null;
                    renderCourses(data.data, container, '暂无推荐课程');
                }
            })
            .catch(function(err) { console.error('加载推荐课程失败', err); });
    }

    // 搜索功能
    function setupSearch() {
        var searchBtn = document.querySelector('.search-btn');
        var searchInput = document.querySelector('.search-box input');
        if (searchBtn && searchInput) {
            searchBtn.addEventListener('click', function () {
                var keyword = searchInput.value.trim();
                if (keyword) {
                    window.location.href = 'search.html?keyword=' + encodeURIComponent(keyword);
                }
            });
            searchInput.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    searchBtn.click();
                }
            });
        }
    }

    // 加载当前用户信息
    function loadUserInfo() {
        fetch(API_BASE + '/user/auth/current')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    var avatar = document.querySelector('.user-avatar');
                    if (avatar && data.data.username) {
                        avatar.title = data.data.username;
                    }
                }
            })
            .catch(function() { /* 未登录状态 */ });
    }

    loadPopularCourses();
    loadRecommendedCourses();
    setupSearch();
    loadUserInfo();
});
