// 模拟教师数据
const teacherData = [
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    },
    {
        name: "加勒特·温特斯",
        institution: "教师",
        gender: "男",
        education: "博士学位",
        phone: "123456789",
        email: "info@example.com",
        joinDate: "2020-12-01"
    }
];

// 渲染表格数据
function renderTeacherTable(data) {
    const tbody = document.getElementById("teacherTbody");
    tbody.innerHTML = ""; // 清空原有内容

    data.forEach(item => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><input type="checkbox" class="row-checkbox" checked></td>
            <td>${item.name}</td>
            <td>${item.institution}</td>
            <td>${item.gender}</td>
            <td>${item.education}</td>
            <td>${item.phone}</td>
            <td>${item.email}</td>
            <td>${item.joinDate}</td>
            <td>查看 编辑</td>
        `;
        tbody.appendChild(tr);
    });
}

// 全选功能
document.getElementById("selectAll").addEventListener("change", function () {
    const checkboxes = document.querySelectorAll(".row-checkbox");
    checkboxes.forEach(checkbox => {
        checkbox.checked = this.checked;
    });
});

// 页面加载时渲染初始数据
window.onload = function () {
    renderTeacherTable(teacherData);
};

// 按钮事件（可根据需求扩展逻辑）
document.getElementById("queryBtn").addEventListener("click", function () {
    alert("查询功能触发");
    // 实际开发中可在这里写筛选逻辑
});

document.getElementById("resetBtn").addEventListener("click", function () {
    document.getElementById("teacherName").value = "";
    document.getElementById("institution").value = "";
    document.getElementById("startDate").value = "";
    document.getElementById("endDate").value = "";
    document.getElementById("gender").value = "";
    document.getElementById("education").value = "";
});

document.getElementById("addBtn").addEventListener("click", function () {
    alert("新增教师功能触发");
});

document.getElementById("deleteBtn").addEventListener("click", function () {
    alert("删除功能触发");
});

document.getElementById("exportBtn").addEventListener("click", function () {
    alert("导出功能触发");
});