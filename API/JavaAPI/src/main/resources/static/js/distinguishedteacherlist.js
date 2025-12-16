document.addEventListener('DOMContentLoaded', function () {

    // ========== 数据部分 ==========
    const teacherInfo = {
        name: "张三",
        avatar: "./image/default.png",
        stats: { classes: 10, messages: 20, fans: 20 },
        brief: "擅长前端、交互设计与教学方法论。",
        field: "HTML/CSS/JS / 小程序",
        score: 8.5,
    };

    const teacherFollows = Array(9).fill({ avatar: "./image/default.png" });
    const recentVisitors = Array(6).fill({ avatar: "./image/default.png" });

    const courses = [
        { name: "HTML 与语义化", hours: 12, target: "零基础", desc: "掌握基础语义化标签与页面结构。" },
        { name: "CSS 布局实战", hours: 14, target: "有基础", desc: "深入掌握 Flex、Grid 与响应式布局。" },
        { name: "JavaScript 入门与实践", hours: 20, target: "入门/进阶", desc: "从基础语法到 DOM 操作与异步编程。" }
    ];

    const hotCourses = ["课程1","课程2","课程3","课程4","课程5","课程6","课程7","课程8","课程9"];

    const recommendTeachers = [
        { name:"李四", avatar:"./image/default.png" },
        { name:"王五", avatar:"./image/default.png" },
        { name:"赵六", avatar:"./image/default.png" }
    ];

    // ========== 渲染部分 ==========

    // 左侧名师信息卡
    const teacherCardContainer = document.getElementById("teacher-card-container");
    teacherCardContainer.innerHTML = `
        <div class="teacher-card">
            <img class="teacher-large-avatar" src="${teacherInfo.avatar}" alt="名师头像">
            <div class="teacher-name">${teacherInfo.name}</div>
            <div class="teacher-stats">
                <div>TA的课堂 <span>${teacherInfo.stats.classes}</span></div>
                <div>TA的留言 <span>${teacherInfo.stats.messages}</span></div>
                <div>TA的粉丝 <span>${teacherInfo.stats.fans}</span></div>
            </div>
        </div>
    `;

    // 左侧关注
    const teacherFollowContainer = document.getElementById("teacher-follow-container");
    teacherFollowContainer.innerHTML = `
        <div class="box">
            <h4>TA的关注</h4>
            <div class="avatar-grid">
                ${teacherFollows.map(f => `<img src="${f.avatar}" alt="头像">`).join('')}
            </div>
        </div>
    `;

    // 左侧最近来访
    const teacherVisitContainer = document.getElementById("teacher-visit-container");
    teacherVisitContainer.innerHTML = `
        <div class="box">
            <h4>最近来访的人</h4>
            <div class="avatar-grid">
                ${recentVisitors.map(v => `<img src="${v.avatar}" alt="头像">`).join('')}
            </div>
        </div>
    `;

    // 中间名师头部
    const teacherHeaderContainer = document.getElementById("teacher-header-container");
    teacherHeaderContainer.innerHTML = `
        <div class="teacher-header">
            <div class="header-left">
                <h1 class="t-name">${teacherInfo.name}</h1>
                <div class="rating">
                    <div class="stars" data-score="${teacherInfo.score}"></div>
                    <div class="score">${teacherInfo.score} <span class="count">(1311人评价)</span></div>
                </div>
                <p class="brief"><strong>个人简介：</strong>${teacherInfo.brief}</p>
                <p class="field"><strong>擅长领域：</strong>${teacherInfo.field}</p>
            </div>
            <div class="header-right">
                <button class="follow-btn">关注</button>
                <button class="msg-btn">留言</button>
            </div>
        </div>
    `;

    // 课程列表
    const coursesContainer = document.getElementById("courses-container");
    coursesContainer.innerHTML = `
        <div class="courses-title">共有${courses.length}个课程</div>
        ${courses.map(c => `
            <article class="course-item">
                <h4 class="course-name">课程名：${c.name}</h4>
                <div class="course-meta">课时：${c.hours} | 适合人群：${c.target}</div>
                <p class="course-desc">课程简介：${c.desc}</p>
            </article>
        `).join('')}
    `;

    // 右侧热门课程
    const hotCoursesContainer = document.getElementById("hot-courses-container");
    hotCoursesContainer.innerHTML = `
        <div class="box">
            <h4>热门课程</h4>
            <div class="hot-grid">
                ${hotCourses.map(h => `<div class="hot-item">${h}</div>`).join('')}
            </div>
        </div>
    `;

    // 右侧推荐老师
    const recommendTeachersContainer = document.getElementById("recommend-teachers-container");
    recommendTeachersContainer.innerHTML = `
        <div class="box">
            <h4>您可能感兴趣的老师</h4>
            <div class="recommend-list">
                ${recommendTeachers.map(r => `
                    <div class="rec-item">
                        <img src="${r.avatar}" alt="头像">
                        <div class="rec-name">${r.name}</div>
                        <button class="follow-small">关注</button>
                    </div>
                `).join('')}
            </div>
        </div>
    `;

    // ========== 交互 ==========
    const followBtn = document.querySelector('.follow-btn');
    if (followBtn) {
        followBtn.addEventListener('click', function () {
            this.classList.toggle('active');
            this.textContent = this.classList.contains('active') ? '已关注' : '关注';
        });
    }

    document.querySelectorAll('.follow-small').forEach(btn => {
        btn.addEventListener('click', function () {
            this.textContent = this.textContent.trim() === '关注' ? '已关注' : '关注';
        });
    });

    // 评分显示
const starsEl = document.querySelector('.stars');
if (starsEl) {
    const score = parseFloat(starsEl.getAttribute('data-score') || '0'); 
    const pct = Math.max(0, Math.min(100, (score / 10) * 100));
    starsEl.style.setProperty('--star-fill', `${pct}%`);
    starsEl.style.width = "100px";

    starsEl.style.position = "relative";
    starsEl.style.display = "inline-block";
    starsEl.style.color = "#e5e5e5";
    starsEl.style.fontFamily = "Arial, sans-serif";
    starsEl.style.fontSize = "20px";

    const fillLayer = document.createElement('span');
    fillLayer.style.position = "absolute";
    fillLayer.style.left = 0;
    fillLayer.style.top = 0;
    fillLayer.style.width = pct + "%";
    fillLayer.style.overflow = "hidden";
    fillLayer.style.whiteSpace = "nowrap";
    fillLayer.style.color = "#ff8a00";
    fillLayer.style.pointerEvents = "none";
    fillLayer.textContent = "★★★★★";
    starsEl.textContent = "★★★★★";
    starsEl.appendChild(fillLayer);
}

});