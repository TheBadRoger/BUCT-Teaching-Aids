/**
 * AI 智能体首页
 * 数据来源：
 *   GET /api/user/auth/current              当前登录用户（用于识别身份）
 *   GET /api/user/binding/info              身份绑定信息
 *   GET /api/course/search?teachingTeachers= 教师身份下统计本人开设的课程
 *   GET /api/student-courses/all-courses    学生身份下统计已选课程
 *   GET /api/notes/student/{studentId}      统计本人笔记数量
 * 说明：智能体对话入口复用已实现的“智能体对话页面”(AIchat.html)，
 *      批改类智能体复用“AI 评判页面”(AIJudge.html)。
 */
import {
  initPageShell,
  apiGet,
  escapeHtml,
  formatDateTime,
  toast,
  installImageFallback
} from "./frontend-common.js";

/** 本机最近使用智能体的存储键 */
const USAGE_STORAGE_KEY = "frontend_agent_usage";

/** 智能体清单：按角色过滤展示 */
const AGENTS = [
  {
    key: "assistant",
    name: "AI 学习助手",
    tag: "问答",
    desc: "围绕课程内容随时提问，支持多轮追问，帮助你理清概念与解题思路。",
    caps: ["多轮对话", "概念讲解"],
    href: "AIchat.html?agent=assistant",
    roles: ["student", "teacher"]
  },
  {
    key: "lesson-plan",
    name: "AI 教案生成",
    tag: "教师工具",
    desc: "输入课程主题与教学目标，快速生成结构化教案与课堂活动建议。",
    caps: ["教案生成", "课堂设计"],
    href: "my-teaching-materials.html",
    roles: ["teacher"]
  },
  {
    key: "judge",
    name: "AI 作业评判",
    tag: "教师工具",
    desc: "上传作业文件，AI 自动给出分数与评判依据，并支持导出 Excel 报告。",
    caps: ["自动批改", "报告导出"],
    href: "AIJudge.html",
    roles: ["teacher"]
  },
  {
    key: "report",
    name: "AI 学情分析",
    tag: "数据分析",
    desc: "汇总选课、学习进度与笔记数据，生成个性化的学情报告与改进建议。",
    caps: ["学情报告", "进度复盘"],
    href: "report.html",
    roles: ["student", "teacher"]
  },
  {
    key: "record",
    name: "AI 录课助手",
    tag: "教师工具",
    desc: "在线录制课程视频，录制完成后直接关联到你的课程课时。",
    caps: ["在线录制", "课时上传"],
    href: "video-record.html",
    roles: ["teacher"]
  },
  {
    key: "qa",
    name: "AI 答疑",
    tag: "问答",
    desc: "面向作业与习题的答疑智能体，给出分步解析与同类练习建议。",
    caps: ["分步解析", "举一反三"],
    href: "AIchat.html?agent=qa",
    roles: ["student", "teacher"]
  }
];

/** 读取本机最近使用记录 */
function readUsage() {
  try {
    const raw = window.localStorage.getItem(USAGE_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/** 写入一条最近使用记录 */
function pushUsage(key) {
  const list = readUsage().filter((item) => item?.key !== key);
  list.unshift({ key, time: Date.now() });
  try {
    window.localStorage.setItem(USAGE_STORAGE_KEY, JSON.stringify(list.slice(0, 6)));
  } catch {
    /* 隐私模式下写入失败可忽略 */
  }
}

/** 渲染智能体卡片 */
function renderAgents(role) {
  const grid = document.querySelector("#agent-grid");
  const visible = AGENTS.filter((agent) => agent.roles.includes(role) || role === "guest");
  document.querySelector("#hero-agent-count").textContent = String(visible.length);

  grid.innerHTML = visible
    .map(
      (agent) => `
      <article class="agent-card">
        <div class="agent-card__head">
          <span class="agent-card__icon" aria-hidden="true">${escapeHtml(agent.name.slice(0, 1))}</span>
          <div>
            <h3 class="agent-card__name">${escapeHtml(agent.name)}</h3>
            <p class="agent-card__tag">${escapeHtml(agent.tag)}</p>
          </div>
        </div>
        <p class="agent-card__desc">${escapeHtml(agent.desc)}</p>
        <div class="agent-card__caps">
          ${agent.caps.map((cap) => `<span class="tag">${escapeHtml(cap)}</span>`).join("")}
        </div>
        <div class="agent-card__foot">
          <a class="btn btn--primary btn--sm" href="${escapeHtml(agent.href)}" data-agent-key="${escapeHtml(
            agent.key
          )}">开始使用</a>
        </div>
      </article>`
    )
    .join("");

  grid.addEventListener("click", (event) => {
    const link = event.target.closest("[data-agent-key]");
    if (link) pushUsage(link.dataset.agentKey);
  });
}

/** 渲染最近使用列表 */
function renderUsage() {
  const host = document.querySelector("#usage-list");
  const usage = readUsage();
  if (!usage.length) {
    host.innerHTML = '<li class="usage-item"><span class="text-muted">还没有使用记录，先从上方选一个智能体开始吧。</span></li>';
    return;
  }

  const nameOf = (key) => AGENTS.find((agent) => agent.key === key)?.name ?? "未知智能体";
  const hrefOf = (key) => AGENTS.find((agent) => agent.key === key)?.href ?? "ai-agent.html";

  host.innerHTML = usage
    .map(
      (item) => `
      <li class="usage-item">
        <a href="${escapeHtml(hrefOf(item.key))}">${escapeHtml(nameOf(item.key))}</a>
        <span class="usage-item__time">${escapeHtml(formatDateTime(item.time))}</span>
      </li>`
    )
    .join("");
}

/** 统计顶部指标 */
async function loadStats(context) {
  const { role, student, teacher } = context;
  document.querySelector("#agent-role-hint").textContent =
    role === "teacher" ? "已按教师身份展示可用智能体" : role === "student" ? "已按学生身份展示可用智能体" : "未登录，展示通用智能体";

  const tasks = [];
  if (role === "teacher" && teacher?.name) {
    tasks.push(apiGet("/course/search", { teachingTeachers: teacher.name, page: 0, size: 1 }));
  } else if (student) {
    tasks.push(apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 1 }));
  } else {
    tasks.push(Promise.resolve(null));
  }
  tasks.push(student ? apiGet(`/notes/student/${student.id}`, { page: 0, size: 1 }) : Promise.resolve(null));

  try {
    const [coursePage, notePage] = await Promise.all(tasks);
    document.querySelector("#hero-course-count").textContent = String(
      Number(coursePage?.totalElements) || 0
    );
    document.querySelector("#hero-note-count").textContent = String(Number(notePage?.totalElements) || 0);
  } catch (error) {
    console.warn("统计信息加载失败", error);
    document.querySelector("#hero-course-count").textContent = "—";
    document.querySelector("#hero-note-count").textContent = "—";
  }
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  try {
    const context = await initPageShell({ active: "ai-agent", requireLogin: false });
    renderAgents(context.role);
    renderUsage();
    await loadStats(context);
  } catch (error) {
    console.error("AI 智能体首页初始化失败", error);
    toast("页面数据加载失败，请稍后重试", "error");
  }
}

document.addEventListener("DOMContentLoaded", main);
