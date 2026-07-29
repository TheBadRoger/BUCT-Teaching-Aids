/**
 * 还未看的课 - 获取学生未查看的课程列表
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 获取当前登录用户
    function getCurrentUser() {
        return fetch(API_BASE + '/user/auth/current')
            .then(res => res.json())
            .then(data => {
                if (data && data.code === 2000 && data.data) {
                    return data.data;
                }
                throw new Error('未登录');
            });
    }

    // 渲染课程列表
    function renderCourses(courses) {
        const grid = document.querySelector('.course-grid');
        if (!grid) return;
        if (!courses || courses.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #999;">暂无未查看的课程</div>';
            return;
        }
        grid.innerHTML = courses.map(sc => {
            const course = sc.course || sc;
            return `
                <div class="course-item">
                    <img src="${course.courseImage || 'pictures.jpg'}" alt="${course.courseName || '课程'}">
                    <div class="course-info">
                        <div class="course-title">${course.courseName || '课程名称'}</div>
                        <div class="course-meta">共${course.duration || '?'}课时</div>
                        <div class="course-stats">教参${course.teachingMaterials || 0}/笔记${course.notesCount || 0}/问答${course.qaCount || 0}</div>
                    </div>
                </div>
            `;
        }).join('');
    }

    // 加载未查看的课程
    function loadNotViewedCourses(userId) {
        fetch(API_BASE + `/student-courses/not-viewed-courses?studentId=${userId}&page=0&size=20&sort=id&direction=desc`)
            .then(res => res.json())
            .then(data => {
                if (data && data.code === 2000 && data.data) {
                    renderCourses(data.data.content || data.data);
                } else {
                    renderCourses([]);
                }
            })
            .catch(err => {
                console.error('加载未查看课程失败', err);
                renderCourses([]);
            });
    }

    // 加载用户信息到侧边栏
    function loadUserProfile() {
        const userNameEl = document.querySelector('.user-name');
        const userAvatar = document.querySelector('.user-profile img');
        getCurrentUser()
            .then(user => {
                if (userNameEl) userNameEl.textContent = user.username || '用户';
                if (userAvatar) userAvatar.src = 'images/default.png';
            })
            .catch(() => {
                if (userNameEl) userNameEl.textContent = '未登录';
            });
    }

    getCurrentUser()
        .then(user => {
            loadNotViewedCourses(user.id);
        })
        .catch(() => {
            // 模拟数据
            renderCourses([
                { course: { courseName: 'HTML 与语义化', courseImage: 'pictures.jpg', duration: 12 } },
                { course: { courseName: 'CSS 布局实战', courseImage: 'pictures.jpg', duration: 14 } },
                { course: { courseName: 'JavaScript 入门与实践', courseImage: 'pictures.jpg', duration: 20 } }
            ]);
        });

    loadUserProfile();

    // 搜索功能
    const searchInput = document.querySelector('.search-area input');
    const searchBtn = document.querySelector('.search-area button');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', function () {
            const keyword = searchInput.value.trim();
            if (keyword) {
                window.location.href = 'search.html?keyword=' + encodeURIComponent(keyword);
            }
        });
    }
});