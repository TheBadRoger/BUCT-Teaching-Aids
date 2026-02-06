// 模拟课程数据
const courseData = {
    "大学": [
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
        {title: "大学计算机", teacher: "张教授", students: "1243人学习", tag: "必修", img: "./image/test-img.jpg"},
    ]
};

function renderCourses(keyword) {
    const courseGrid = document.querySelector('.course-grid');
    courseGrid.innerHTML = ''; // 清空容器

    // 关键修复：如果关键词不存在，默认显示“大学”课程
    const courses = courseData[keyword] || courseData["大学"];
    courses.forEach(course => {
        const courseCard = document.createElement('div');
        courseCard.className = 'course-card';
        courseCard.innerHTML = `
            <div class="course-img">
                <img src="${course.img}" alt="${course.title}">
                <span class="course-tag">${course.tag}</span>
            </div>
            <div class="course-info">
                <h3 class="course-title">${course.title}</h3>
                <div class="course-meta">
                    <span>${course.teacher}</span>
                    <span>${course.students}</span>
                </div>
            </div>
        `;
        courseGrid.appendChild(courseCard);
    });
}

// 渲染结果说明
function renderResultInfo(keyword) {
    const keywordEl = document.getElementById('searchKeyword');
    keywordEl.textContent = `"${keyword}" 课程搜索结果（"高中" 标签搜索结果）`;

    const countEl = document.getElementById('resultCount');
    const total = (courseData[keyword] || courseData["大学"]).length;
    countEl.textContent = `约有 ${total} 门课程，以下是第1-16篇`;
}

// 渲染分页（简化，实际可根据课程总数动态生成）
function renderPagination() {
    const pageNumbersContainer = document.querySelector('.page-numbers');
    pageNumbersContainer.innerHTML = '';

    const pageBtn = document.createElement('button');
    pageBtn.className = 'page-number active';
    pageBtn.textContent = '1';
    pageNumbersContainer.appendChild(pageBtn);
}

// 搜索按钮点击事件
document.getElementById('searchBtn').addEventListener('click', () => {
    const input = document.getElementById('searchInput');
    const keyword = input.value.trim() || '大学'; // 输入为空时默认“大学”
    renderResultInfo(keyword);
    renderCourses(keyword);
});

// 页面加载初始化
window.addEventListener('load', () => {
    renderResultInfo('大学');
    renderCourses('大学');
    renderPagination();
});