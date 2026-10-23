/**
 * 已经看过的课 - 获取学生已查看的课程列表
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 获取当前登录用户
    function getCurrentUser() {
        return fetch(API_BASE + '/user/auth/current')
            .then(res => res.json())
            .then(data => {
                if (data && data.code === 2000 && data.data) return data.data;
                throw new Error('未登录');
            });
    }

    // 渲染课程列表
    function renderCourses(courses) {
        const grid = document.querySelector('.course-grid');
        if (!grid) return;
        if (!courses || courses.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #999;">暂无已查看的课程</div>';
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

    // 加载已查看的课程
    function loadViewedCourses(userId) {
        fetch(API_BASE + `/student-courses/viewed-courses?studentId=${userId}&page=0&size=20&sort=id&direction=desc`)
            .then(res => res.json())
            .then(data => {
                if (data && data.code === 2000 && data.data) {
                    renderCourses(data.data.content || data.data);
                } else {
                    renderCourses([]);
                }
            })
            .catch(err => {
                console.error('加载已查看课程失败', err);
                renderCourses([]);
            });
    }

    // 排序切换
    const sortSelect = document.querySelector('.sort-area select');
    if (sortSelect) {
        sortSelect.addEventListener('change', function () {
            const val = this.value;
            const items = document.querySelectorAll('.course-item');
            const arr = Array.from(items);
            if (val === '评分排序' || val === '热门排序') {
                arr.reverse();
                const grid = document.querySelector('.course-grid');
                if (grid) {
                    grid.innerHTML = '';
                    arr.forEach(item => grid.appendChild(item));
                }
            }
        });
    }

    getCurrentUser()
        .then(user => loadViewedCourses(user.id))
        .catch(() => {
            // 模拟数据
            renderCourses([
                { course: { courseName: '数据结构与算法', courseImage: 'pictures.jpg', duration: 16 } },
                { course: { courseName: '操作系统原理', courseImage: 'pictures.jpg', duration: 24 } }
            ]);
        });

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