/**
 * 后台管理系统前端公共工具
 * 统一封装与 Java 后端的请求交互、导航、提示、分页、URL 参数解析等。
 * 约定：所有接口均返回 { code, msg, timestamp, data }，code === 2000 表示成功。
 */
"use strict";

const API_BASE = "/api";

/**
 * 通用请求封装
 * @param {string} url 以 /api 开头的路径
 * @param {object} options fetch 选项
 * @returns {Promise<object>} 解析后的 JSON
 */
async function request(url, options = {}) {
    options.headers = options.headers || {};
    const resp = await fetch(url, options);

    let json;
    const contentType = (resp.headers.get("content-type") || "").toLowerCase();
    if (contentType.includes("application/json")) {
        json = await resp.json();
    } else {
        // 非 JSON 响应（如文件流），按状态码构造
        json = { code: resp.ok ? 2000 : resp.status, msg: resp.statusText, data: null };
    }

    // 业务码非 2000 视为失败
    if (!resp.ok || (typeof json.code === "number" && json.code !== 2000)) {
        const err = new Error(json.msg || "请求失败：" + resp.status);
        err.code = json.code;
        err.json = json;
        throw err;
    }
    return json;
}

/** 将参数对象转为查询字符串（忽略空值），无参返回空串 */
function buildQuery(params) {
    const usp = new URLSearchParams();
    Object.keys(params || {}).forEach((key) => {
        const v = params[key];
        if (v !== undefined && v !== null && v !== "") usp.append(key, v);
    });
    const s = usp.toString();
    return s ? "?" + s : "";
}

function apiGet(path, params) {
    return request(API_BASE + path + buildQuery(params || {}), { method: "GET" });
}

/** JSON 请求体 POST */
function apiPost(path, body, params) {
    return request(API_BASE + path + buildQuery(params || {}), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
}

/** application/x-www-form-urlencoded POST（适配后端 @RequestParam 的 POST 接口） */
function apiPostForm(path, params) {
    return request(API_BASE + path, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams(params || {}).toString()
    });
}

function apiPut(path, body, params) {
    return request(API_BASE + path + buildQuery(params || {}), {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
}

/** DELETE，请求体为 ID 数组 */
function apiDelete(path, bodyArray) {
    return request(API_BASE + path, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bodyArray || [])
    });
}

/** 读取当前页面 URL 中的查询参数 */
function getParam(name) {
    return new URLSearchParams(location.search).get(name) || "";
}

/** 轻提示 */
function toast(msg, type) {
    let container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.style.cssText =
            "position:fixed;top:16px;right:16px;z-index:9999;display:flex;flex-direction:column;gap:8px;";
        document.body.appendChild(container);
    }
    const el = document.createElement("div");
    const bg = type === "error" ? "#e74c3c" : type === "warn" ? "#f39c12" : "#27ae60";
    el.style.cssText =
        `background:${bg};color:#fff;padding:10px 16px;border-radius:6px;` +
        "box-shadow:0 2px 8px rgba(0,0,0,.2);font-size:14px;";
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 3000);
}

/** 转义 HTML，防止 XSS */
function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

/** 简易登录态守卫：未登录则跳转登录页（客户端轻量校验） */
function requireAdminLogin() {
    if (localStorage.getItem("admin_logged_in") !== "true") {
        location.href = "login.html";
        return false;
    }
    return true;
}

/** 退出登录（同时尝试使服务端 Session 失效） */
function adminLogout() {
    // fire-and-forget：使服务端 session 失效，随后清理客户端登录态
    try {
        fetch(API_BASE + "/user/auth/logout", { method: "POST" });
    } catch (e) { /* 忽略网络异常 */ }
    localStorage.removeItem("admin_logged_in");
    localStorage.removeItem("admin_username");
    location.href = "login.html";
}

/**
 * 渲染后台顶部导航（在页面 body 顶部插入）
 * @param {string} active 当前高亮项：teacher | student | course
 */
function renderAdminNav(active) {
    const nav = document.createElement("div");
    nav.className = "admin-nav";
    const items = [
        { key: "teacher", label: "教师列表", href: "teacherlist.html" },
        { key: "add-teacher", label: "新增教师", href: "add-teacher.html" },
        { key: "student", label: "学生列表", href: "studentlist.html" },
        { key: "add-student", label: "新增学生", href: "add-student.html" },
        { key: "course", label: "课程列表", href: "courselist.html" },
        { key: "add-course", label: "新增课程", href: "addcourse.html" },
    ];
    let html = '<a class="brand" href="courselist.html">北化教学辅助系统 · 后台管理</a>';
    html += '<div class="nav-links">';
    items.forEach((it) => {
        html +=
            `<a href="${it.href}" class="${it.key === active ? "active" : ""}">${it.label}</a>`;
    });
    html += `</div><button class="nav-logout" onclick="adminLogout()">退出</button>`;
    nav.innerHTML = html;
    document.body.insertBefore(nav, document.body.firstChild);
}

/** 分页控件渲染与事件绑定（简易） */
function renderPagination(el, page, totalPages, onPage) {
    if (!el) return;
    let html = "";
    const prev = page > 0 ? `<a href="javascript:;" class="page" data-p="${page - 1}">上一页</a>` : "<span>上一页</span>";
    const next = page < totalPages - 1 ? `<a href="javascript:;" class="page" data-p="${page + 1}">下一页</a>` : "<span>下一页</span>";
    html += prev;
    for (let i = 0; i < totalPages; i++) {
        html += `<a href="javascript:;" class="page ${i === page ? "active" : ""}" data-p="${i}">${i + 1}</a>`;
    }
    html += next;
    el.innerHTML = html;
    el.querySelectorAll(".page").forEach((a) => {
        a.addEventListener("click", () => onPage && onPage(parseInt(a.dataset.p, 10)));
    });
}
