/**
 * 学生智慧前台公共脚本
 * 用途：统一封装与 Java 后端（/api/**）的交互、登录态与角色识别、导航渲染、
 *      提示、分页、加载/空/错误状态，供新增前台页面复用。
 * 约定：后端统一响应结构为 { code, msg, timestamp, data }，code === 2000 表示成功。
 */

/** 接口基础路径 */
export const API_BASE = "/api";

/** 成功业务码 */
export const CODE_SUCCESS = 2000;

/** 各页面在导航树中的位置，用于侧边栏高亮 */
export const PAGE_KEY = {
  REPORT: "report",
  AI_AGENT: "ai-agent",
  AI_CHAT: "ai-chat",
  GROWTH_MAP: "growth-map",
  STUDY_DISTRIBUTION: "study-distribution",
  LEARN_RECORD: "learn-record",
  NOTE: "my-note",
  QUESTION: "myquestion",
  FOLLOW: "myfollow",
  GUESS: "guessyouneed",
  COURSE_LIST: "course-list",
  COURSE_INFO: "course-info",
  COURSE_PLAY: "course-play",
  MY_CLASS: "my-class",
  MY_COURSE: "mycourse",
  MY_COURSE_DETAIL: "my-course-detail",
  PUBLISH_INFO: "publish-info",
  APPLY_TEACHER: "apply-teacher",
  VIDEO_RECORD: "video-record",
  INSTITUTION: "institution",
  GRADE_LIST: "gradelist",
  PERSONAL_CENTER: "personal-center",
  FRIEND_ACTIVITY: "friend-activity",
  TEACHING_MATERIAL: "my-teaching-materials"
};

/** 图标占位：使用内联 SVG，避免依赖图标库 */
const ICON_STAR =
  '<svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 1.5l1.9 3.9 4.3.6-3.1 3 .7 4.3L8 11.3l-3.8 2 .7-4.3-3.1-3 4.3-.6z"/></svg>';

/**
 * 通用请求封装：自动解析统一响应结构，非 2000 抛错。
 * @param {string} path 以 /api 开头的接口路径
 * @param {RequestInit} [options] fetch 选项
 * @returns {Promise<any>} 成功时返回响应体中的 data 字段
 * @throws {Error} 网络异常或业务码非 2000 时抛出，error.code 为业务码
 */
export async function request(path, options = {}) {
  const response = await fetch(path, {
    credentials: "same-origin",
    ...options,
    headers: { ...(options.headers || {}) }
  });

  const contentType = (response.headers.get("content-type") || "").toLowerCase();
  let body = null;
  if (contentType.includes("application/json")) {
    body = await response.json();
  }

  if (!response.ok) {
    const error = new Error(body?.msg || `请求失败（HTTP ${response.status}）`);
    error.code = body?.code ?? response.status;
    throw error;
  }
  if (body && typeof body.code === "number" && body.code !== CODE_SUCCESS) {
    const error = new Error(body.msg || "业务处理失败");
    error.code = body.code;
    throw error;
  }
  return body ? body.data : null;
}

/**
 * 将参数对象拼接为查询字符串，自动忽略空值。
 * @param {Record<string, (string|number|boolean|null|undefined)>} params 参数对象
 * @returns {string} 形如 "?a=1&b=2" 的查询串，无有效参数时返回空串
 */
export function buildQuery(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.append(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

/**
 * GET 请求
 * @param {string} path 接口路径（不含 /api 前缀）
 * @param {object} [params] 查询参数
 * @returns {Promise<any>} data 字段
 */
export function apiGet(path, params) {
  return request(`${API_BASE}${path}${buildQuery(params)}`, { method: "GET" });
}

/**
 * JSON 请求体 POST
 * @param {string} path 接口路径（不含 /api 前缀）
 * @param {object} [body] 请求体
 * @param {object} [params] 查询参数
 * @returns {Promise<any>} data 字段
 */
export function apiPost(path, body, params) {
  return request(`${API_BASE}${path}${buildQuery(params)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body ?? {})
  });
}

/**
 * PUT 请求
 * @param {string} path 接口路径（不含 /api 前缀）
 * @param {object} [body] 请求体
 * @param {object} [params] 查询参数
 * @returns {Promise<any>} data 字段
 */
export function apiPut(path, body, params) {
  return request(`${API_BASE}${path}${buildQuery(params)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body ?? {})
  });
}

/**
 * DELETE 请求，请求体为 ID 数组或空
 * @param {string} path 接口路径（不含 /api 前缀）
 * @param {object} [params] 查询参数
 * @param {Array<number|string>} [ids] 批量删除的 ID 列表
 * @returns {Promise<any>} data 字段
 */
export function apiDelete(path, params, ids) {
  const options = { method: "DELETE" };
  if (Array.isArray(ids)) {
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(ids);
  }
  return request(`${API_BASE}${path}${buildQuery(params)}`, options);
}

/**
 * 读取当前页面 URL 查询参数
 * @param {string} name 参数名
 * @returns {string} 参数值，不存在时返回空串
 */
export function getParam(name) {
  return new URLSearchParams(window.location.search).get(name) ?? "";
}

/**
 * HTML 转义，防止用户输入直接插入 DOM 造成 XSS
 * @param {unknown} value 任意待转义内容
 * @returns {string} 转义后的安全字符串
 */
export function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * 轻提示
 * @param {string} message 提示文案
 * @param {"success"|"error"|"warn"} [type] 提示类型
 * @returns {void}
 */
export function toast(message, type = "success") {
  let container = document.querySelector(".toast-container");
  if (!container) {
    container = document.createElement("div");
    container.className = "toast-container";
    container.setAttribute("role", "status");
    container.setAttribute("aria-live", "polite");
    document.body.append(container);
  }
  const item = document.createElement("div");
  item.className = `toast${type === "success" ? "" : ` toast--${type}`}`;
  item.textContent = message;
  container.append(item);
  window.setTimeout(() => item.remove(), 3000);
}

/**
 * 格式化日期时间为 YYYY-MM-DD
 * @param {string|number|Date} value 原始时间
 * @returns {string} 格式化结果，无效时返回 "—"
 */
export function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10) || "—";
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/**
 * 格式化日期时间为 YYYY-MM-DD HH:mm
 * @param {string|number|Date} value 原始时间
 * @returns {string} 格式化结果，无效时返回 "—"
 */
export function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${formatDate(date)} ${hours}:${minutes}`;
}

/**
 * 相对时间描述（如“3 天前”）
 * @param {string|number|Date} value 原始时间
 * @returns {string} 相对时间文案
 */
export function fromNow(value) {
  if (!value) return "—";
  const time = new Date(value).getTime();
  if (Number.isNaN(time)) return String(value);
  const diff = Date.now() - time;
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < minute) return "刚刚";
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`;
  if (diff < 30 * day) return `${Math.floor(diff / day)} 天前`;
  return formatDate(value);
}

/**
 * 读取统一分页对象中的内容数组
 * @param {any} page 后端返回的分页对象或数组
 * @returns {Array<any>} 列表数据
 */
export function pageContent(page) {
  if (Array.isArray(page)) return page;
  return Array.isArray(page?.content) ? page.content : [];
}

/**
 * 渲染“加载中”骨架屏
 * @param {HTMLElement|null} container 目标容器
 * @param {number} [rows] 骨架行数
 * @returns {void}
 */
export function renderLoading(container, rows = 3) {
  if (!container) return;
  const blocks = Array.from(
    { length: rows },
    () => '<div class="skeleton skeleton--block mb-md"></div>'
  ).join("");
  container.innerHTML = `<div aria-busy="true" aria-label="内容加载中">${blocks}</div>`;
}

/**
 * 渲染空状态
 * @param {HTMLElement|null} container 目标容器
 * @param {string} [title] 标题
 * @param {string} [desc] 说明文案
 * @returns {void}
 */
export function renderEmpty(container, title = "暂无数据", desc = "换个条件再试试，或稍后再来看看。") {
  if (!container) return;
  container.innerHTML = `
    <div class="state" role="status">
      <p class="state__title">${escapeHtml(title)}</p>
      <p class="state__desc">${escapeHtml(desc)}</p>
    </div>`;
}

/**
 * 渲染错误状态，并提供重试按钮
 * @param {HTMLElement|null} container 目标容器
 * @param {Error|string} error 错误对象或提示文案
 * @param {Function} [onRetry] 重试回调
 * @returns {void}
 */
export function renderError(container, error, onRetry) {
  if (!container) return;
  const message = typeof error === "string" ? error : error?.message || "加载失败";
  container.innerHTML = `
    <div class="state" role="alert">
      <p class="state__title">加载失败</p>
      <p class="state__desc">${escapeHtml(message)}</p>
      <button class="btn btn--primary" type="button" data-action="retry">重新加载</button>
    </div>`;
  container.querySelector('[data-action="retry"]')?.addEventListener("click", () => {
    if (typeof onRetry === "function") onRetry();
  });
}

/**
 * 渲染分页控件，使用事件委托绑定翻页
 * @param {HTMLElement|null} container 目标容器
 * @param {{page:number,totalPages:number,totalElements:number}} meta 分页元数据（page 从 0 开始）
 * @param {(page:number)=>void} onChange 页码变化回调
 * @returns {void}
 */
export function renderPagination(container, meta, onChange) {
  if (!container) return;
  const page = Number(meta?.page) || 0;
  const totalPages = Number(meta?.totalPages) || 0;
  const totalElements = Number(meta?.totalElements) || 0;

  if (totalPages <= 1) {
    container.innerHTML = totalElements
      ? `<span class="pagination__info">共 ${totalElements} 条</span>`
      : "";
    return;
  }

  const items = [];
  items.push(
    `<button class="pagination__item${page <= 0 ? " is-disabled" : ""}" type="button" data-page="${page - 1}" ${
      page <= 0 ? "disabled" : ""
    }>上一页</button>`
  );

  const windowSize = 2;
  const start = Math.max(0, page - windowSize);
  const end = Math.min(totalPages - 1, page + windowSize);
  for (let index = start; index <= end; index += 1) {
    items.push(
      `<button class="pagination__item${index === page ? " is-active" : ""}" type="button" data-page="${index}">${
        index + 1
      }</button>`
    );
  }

  items.push(
    `<button class="pagination__item${page >= totalPages - 1 ? " is-disabled" : ""}" type="button" data-page="${
      page + 1
    }" ${page >= totalPages - 1 ? "disabled" : ""}>下一页</button>`
  );

  container.innerHTML = `<span class="pagination__info">共 ${totalElements} 条 / ${totalPages} 页</span>${items.join("")}`;
  container.querySelectorAll("button[data-page]").forEach((button) => {
    button.addEventListener("click", () => {
      const target = Number(button.dataset.page);
      if (!Number.isNaN(target) && target >= 0 && target < totalPages && target !== page) {
        onChange(target);
      }
    });
  });
}

/**
 * 获取当前登录用户
 * @returns {Promise<object|null>} 用户对象，未登录返回 null
 */
export async function getCurrentUser() {
  try {
    return await apiGet("/user/auth/current");
  } catch {
    return null;
  }
}

/**
 * 获取当前用户的身份绑定信息
 * @returns {Promise<object|null>} 绑定信息，未登录返回 null
 */
export async function getBindingInfo() {
  try {
    return await apiGet("/user/binding/info");
  } catch {
    return null;
  }
}

/**
 * 解析当前用户角色
 * @param {object|null} user 用户对象
 * @param {object|null} binding 绑定信息
 * @returns {"student"|"teacher"|"guest"} 角色标识
 */
export function resolveRole(user, binding) {
  const type = binding?.userType ?? user?.userType;
  if (type === "STUDENT") return "student";
  if (type === "TEACHER") return "teacher";
  if (user?.student) return "student";
  if (user?.teacher) return "teacher";
  return "guest";
}

/**
 * 取当前用户关联的学生对象（供 /api/student-courses、/api/notes 等接口使用）
 * @param {object|null} user 用户对象
 * @param {object|null} binding 绑定信息
 * @returns {object|null} 学生对象
 */
export function resolveStudent(user, binding) {
  return binding?.studentInfo ?? user?.student ?? null;
}

/**
 * 取当前用户关联的教师对象
 * @param {object|null} user 用户对象
 * @param {object|null} binding 绑定信息
 * @returns {object|null} 教师对象
 */
export function resolveTeacher(user, binding) {
  return binding?.teacherInfo ?? user?.teacher ?? null;
}

/** 侧边栏菜单树：按角色显示不同分组 */
const SIDE_NAV_GROUPS = [
  {
    title: "我的学习",
    roles: ["student", "teacher"],
    items: [
      { key: PAGE_KEY.MY_CLASS, label: "我的易课堂", href: "my-class.html" },
      { key: PAGE_KEY.LEARN_RECORD, label: "我的学习记录", href: "learn-record.html" },
      { key: PAGE_KEY.NOTE, label: "我的笔记", href: "my-note.html" },
      { key: PAGE_KEY.QUESTION, label: "我的提问", href: "myquestion.html" },
      { key: PAGE_KEY.TEACHING_MATERIAL, label: "我的教参", href: "my-teaching-materials.html" },
      { key: PAGE_KEY.FOLLOW, label: "我要关注", href: "myfollow.html" }
    ]
  },
  {
    title: "学习与成长",
    roles: ["student", "teacher"],
    items: [
      { key: PAGE_KEY.REPORT, label: "个性化学情报告", href: "report.html" },
      { key: PAGE_KEY.GROWTH_MAP, label: "成长地图", href: "growth-map.html" },
      { key: PAGE_KEY.STUDY_DISTRIBUTION, label: "学习分布", href: "study-distribution.html" },
      { key: PAGE_KEY.GUESS, label: "猜你需要", href: "guessyouneed.html" }
    ]
  },
  {
    title: "课程",
    roles: ["student", "teacher", "guest"],
    items: [
      { key: PAGE_KEY.COURSE_LIST, label: "课程大全", href: "course-list.html" }
    ]
  },
  {
    title: "教师与机构",
    roles: ["teacher"],
    items: [
      { key: PAGE_KEY.MY_COURSE, label: "我开的课", href: "my-course.html" },
      { key: PAGE_KEY.VIDEO_RECORD, label: "在线录制视频", href: "video-record.html" },
      { key: PAGE_KEY.PUBLISH_INFO, label: "发布信息", href: "publish-info.html" },
      { key: PAGE_KEY.GRADE_LIST, label: "名单管理", href: "gradelist.html" }
    ]
  },
  {
    title: "互动",
    roles: ["student", "teacher"],
    items: [
      { key: PAGE_KEY.FRIEND_ACTIVITY, label: "好友动态", href: "friend-activity.html" },
      { key: PAGE_KEY.AI_AGENT, label: "AI 智能体", href: "ai-agent.html" },
      { key: PAGE_KEY.INSTITUTION, label: "机构专区", href: "institution.html" }
    ]
  },
  {
    title: "账号",
    roles: ["student", "teacher", "guest"],
    items: [
      { key: PAGE_KEY.PERSONAL_CENTER, label: "个人中心", href: "personal-center.html" },
      { key: PAGE_KEY.APPLY_TEACHER, label: "申请成为老师", href: "apply-teacher.html" }
    ]
  }
];

/**
 * 渲染页面头部（Logo + 搜索 + 用户区）
 * @param {object|null} user 当前用户
 * @returns {void}
 */
export function renderSiteHeader(user) {
  const host = document.querySelector("[data-site-header]");
  if (!host) return;
  const displayName = escapeHtml(user?.username ?? "未登录");
  const avatar = escapeHtml(user?.avatar ?? "images/default.png");
  host.innerHTML = `
    <header class="site-header">
      <div class="site-header__inner">
        <a href="index.html" aria-label="返回首页">
          <img class="site-header__logo" src="images/BUCT-Logo-blue.png" alt="北京化工大学教学辅助系统">
        </a>
        <form class="site-header__search" role="search" data-search-form>
          <label class="sr-only" for="site-search-input">搜索课程、教师或机构</label>
          <input id="site-search-input" name="keyword" type="search" placeholder="搜索课程、教师或机构...">
          <button type="submit">搜索</button>
        </form>
        <div class="site-header__actions">
          <a class="btn btn--sm" href="personal-center.html">${displayName}</a>
          <a href="personal-center.html" aria-label="进入个人中心">
            <img class="site-header__avatar" src="${avatar}" alt="">
          </a>
        </div>
      </div>
    </header>`;

  host.querySelector("[data-search-form]")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const keyword = new FormData(event.currentTarget).get("keyword")?.toString().trim() ?? "";
    if (!keyword) {
      toast("请先输入搜索关键词", "warn");
      return;
    }
    window.location.href = `search.html?keyword=${encodeURIComponent(keyword)}`;
  });
}

/**
 * 渲染二级导航
 * @param {string} activeKey 当前高亮项文案（与链接文案一致）
 * @returns {void}
 */
export function renderSubNav(activeKey) {
  const host = document.querySelector("[data-sub-nav]");
  if (!host) return;
  const links = [
    { label: "首页", href: "index.html" },
    { label: "课程大全", href: "course-list.html" },
    { label: "AI 智能体", href: "ai-agent.html" },
    { label: "机构专区", href: "institution.html" },
    { label: "我的易课堂", href: "my-class.html" }
  ];
  host.innerHTML = `
    <nav class="sub-nav" aria-label="二级导航">
      <div class="sub-nav__inner">
        ${links
          .map(
            (link) =>
              `<a class="sub-nav__link${link.label === activeKey ? " sub-nav__link--active" : ""}" href="${
                link.href
              }">${link.label}</a>`
          )
          .join("")}
      </div>
    </nav>`;
}

/**
 * 渲染左侧导航栏
 * @param {{active?:string, user?:object|null, role?:string, extraGroups?:Array<object>}} options 配置
 * @returns {void}
 */
export function renderSideNav({ active = "", user = null, role = "student", extraGroups = [] } = {}) {
  const host = document.querySelector("[data-side-nav]");
  if (!host) return;

  const name = escapeHtml(
    user?.student?.name ?? user?.teacher?.name ?? user?.username ?? "未登录用户"
  );
  const meta = escapeHtml(
    user?.student?.className ?? user?.teacher?.organization ?? "北化教学辅助系统"
  );
  const avatar = escapeHtml(user?.avatar ?? "images/default.png");

  const groups = [...SIDE_NAV_GROUPS, ...extraGroups].filter(
    (group) => !Array.isArray(group.roles) || group.roles.includes(role)
  );

  host.innerHTML = `
    <aside class="layout-aside" aria-label="功能导航">
      <div class="side-nav__user">
        <img class="side-nav__avatar" src="${avatar}" alt="">
        <p class="side-nav__name">${name}</p>
        <p class="side-nav__meta">${meta}</p>
      </div>
      ${groups
        .map(
          (group) => `
        <div class="side-nav__group">
          <p class="side-nav__group-title">${escapeHtml(group.title)}</p>
          <ul>
            ${group.items
              .map(
                (item) => `
              <li>
                <a class="side-nav__link${item.key === active ? " side-nav__link--active" : ""}" href="${
                  item.href
                }"${item.key === active ? ' aria-current="page"' : ""}>${escapeHtml(item.label)}</a>
              </li>`
              )
              .join("")}
          </ul>
        </div>`
        )
        .join("")}
    </aside>`;
}

/**
 * 页面启动时的通用初始化：并行拉取用户与绑定信息，再渲染头部与侧边栏
 * @param {{active?:string, requireLogin?:boolean}} options 配置
 * @returns {Promise<{user:object|null, binding:object|null, role:string, student:object|null, teacher:object|null}>}
 */
export async function initPageShell({ active = "", requireLogin = true } = {}) {
  const [user, binding] = await Promise.all([getCurrentUser(), getBindingInfo()]);
  if (!user && requireLogin) {
    window.location.href = "enter.html";
    return { user: null, binding: null, role: "guest", student: null, teacher: null };
  }
  const role = resolveRole(user, binding);
  renderSiteHeader(user);
  renderSideNav({ active, user, role });
  return {
    user,
    binding,
    role,
    student: resolveStudent(user, binding),
    teacher: resolveTeacher(user, binding)
  };
}

/**
 * 渲染课程卡片（用于课程大全、猜你需要、我开的课等列表）
 * 说明：课程详情页依据课程编号（courseNumber）定位课程，因此详情链接统一携带 courseNumber。
 * @param {object} course 课程对象
 * @param {{href?:string, showStatus?:boolean}} [options] 渲染选项
 * @returns {string} 卡片 HTML
 */
export function courseCardHtml(course, { href = "course-info.html", showStatus = false } = {}) {
  const key = encodeURIComponent(course?.courseNumber ?? course?.id ?? "");
  const detailUrl = `${href}?courseNumber=${key}`;
  const title = escapeHtml(course?.courseName ?? "未命名课程");
  const teacher = escapeHtml(course?.teachingTeachers ?? "待定");
  const cover = escapeHtml(course?.courseImage || "images/test-img.jpg");
  const views = Number(course?.viewCount) || 0;
  const status = escapeHtml(course?.courseStatus ?? "");
  const tags = String(course?.courseTags ?? "")
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean)
    .slice(0, 3);

  return `
    <article class="course-card">
      <a class="course-card__cover" href="${detailUrl}">
        <img src="${cover}" alt="${title} 的课程封面" loading="lazy">
        ${showStatus && status ? `<span class="tag tag--accent course-card__badge">${status}</span>` : ""}
      </a>
      <div class="course-card__body">
        <h3 class="course-card__title"><a href="${detailUrl}">${title}</a></h3>
        <p class="course-card__meta">
          <span>${teacher}</span>
          <span>${views} 人学习</span>
        </p>
        ${
          tags.length
            ? `<p class="course-card__tags">${tags.map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}</p>`
            : ""
        }
      </div>
    </article>`;
}

/**
 * 拼接课程详情页链接
 * @param {object} course 课程对象
 * @returns {string} 详情页地址（携带课程编号）
 */
export function courseDetailUrl(course) {
  return `course-info.html?courseNumber=${encodeURIComponent(course?.courseNumber ?? course?.id ?? "")}`;
}

/**
 * 拼接课时播放页链接
 * @param {object} course 课程对象
 * @param {number} [lesson] 课时序号
 * @returns {string} 播放页地址
 */
export function coursePlayUrl(course, lesson = 1) {
  return `course-play.html?courseNumber=${encodeURIComponent(
    course?.courseNumber ?? course?.id ?? ""
  )}&lesson=${Number(lesson) || 1}`;
}

/**
 * 生成星级评分 DOM 字符串
 * @param {number} score 0-5 的评分
 * @returns {string} 星级 HTML
 */
export function ratingHtml(score) {
  const value = Math.max(0, Math.min(5, Math.round(Number(score) || 0)));
  return `<span class="rating" title="${value} 分" aria-label="评分 ${value} 分">${Array.from(
    { length: 5 },
    (_, index) => `<span class="rating__star${index < value ? " is-on" : ""}">${ICON_STAR}</span>`
  ).join("")}</span>`;
}

/**
 * 收集表单数据为普通对象
 * @param {HTMLFormElement|null} form 表单元素
 * @returns {Record<string, string>} 表单键值对
 */
export function readForm(form) {
  if (!form) return {};
  const result = {};
  new FormData(form).forEach((value, key) => {
    result[key] = typeof value === "string" ? value.trim() : value;
  });
  return result;
}

/**
 * 防抖
 * @param {Function} fn 目标函数
 * @param {number} [delay] 延迟毫秒
 * @returns {Function} 包装后的函数
 */
export function debounce(fn, delay = 300) {
  let timer = null;
  return (...args) => {
    if (timer) window.clearTimeout(timer);
    timer = window.setTimeout(() => fn(...args), delay);
  };
}

/**
 * 渲染缺省封面（图片加载失败时使用）
 * @param {string} [fallback] 兜底图片地址
 * @returns {void}
 */
export function installImageFallback(fallback = "images/test-img.jpg") {
  document.addEventListener(
    "error",
    (event) => {
      const target = event.target;
      if (target instanceof HTMLImageElement && !target.dataset.fallbackApplied) {
        target.dataset.fallbackApplied = "true";
        target.src = fallback;
      }
    },
    true
  );
}
