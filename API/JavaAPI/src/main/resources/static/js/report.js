/**
 * 个性化学情报告页面
 * 数据来源（均为后端已实现接口）：
 *   GET /api/user/auth/current                     当前登录用户
 *   GET /api/user/binding/info                     身份绑定信息（学生 / 教师）
 *   GET /api/student-courses/all-courses           已选全部课程
 *   GET /api/student-courses/viewed-courses        已学课程
 *   GET /api/student-courses/not-viewed-courses    待学课程
 *   GET /api/notes/student/{studentId}             我的笔记（用于统计产出）
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  escapeHtml,
  renderEmpty,
  renderError,
  formatDateTime,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  courses: [],
  viewed: [],
  pending: [],
  notes: []
};

/** 取分页总条数，兼容后端未返回该字段的场景 */
function totalOf(pageData, list) {
  const total = Number(pageData?.totalElements);
  return Number.isFinite(total) ? total : list.length;
}

/** 拉取全部依赖数据 */
async function loadData() {
  const { student, teacher } = state.context;

  const [allPage, viewedPage, pendingPage, notePage] = await Promise.all([
    student
      ? apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 100 })
      : Promise.resolve(null),
    student
      ? apiGet("/student-courses/viewed-courses", { studentId: student.id, page: 0, size: 100 })
      : Promise.resolve(null),
    student
      ? apiGet("/student-courses/not-viewed-courses", { studentId: student.id, page: 0, size: 100 })
      : Promise.resolve(null),
    student
      ? apiGet(`/notes/student/${student.id}`, { page: 0, size: 100 })
      : Promise.resolve(null)
  ]);

  state.courses = pageContent(allPage);
  state.viewed = pageContent(viewedPage);
  state.pending = pageContent(pendingPage);
  state.notes = pageContent(notePage);

  // 教师身份下的数据口径：以本人开设的课程（按授课教师检索）作为报告主体
  if (!student && teacher?.name) {
    const taught = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 100
    });
    state.courses = pageContent(taught);
    state.viewed = state.courses;
    state.pending = [];
  }
}

/** 渲染头部与概览指标 */
function renderOverview() {
  const total = state.courses.length;
  const viewed = state.viewed.length;
  const pending = state.pending.length;
  const noteCount = state.notes.length;

  document.querySelector("#report-time").textContent = formatDateTime(new Date());
  document.querySelector("#stat-total").innerHTML = `${total}<span class="stat__unit">门</span>`;
  document.querySelector("#stat-viewed").innerHTML = `${viewed}<span class="stat__unit">门</span>`;
  document.querySelector("#stat-pending").innerHTML = `${pending}<span class="stat__unit">门</span>`;
  document.querySelector("#stat-notes").innerHTML = `${noteCount}<span class="stat__unit">篇</span>`;

  const rate = total ? Math.round((viewed / total) * 100) : 0;
  const noteScore = Math.min(100, noteCount * 10);
  const index = total ? Math.round(rate * 0.7 + noteScore * 0.3) : noteScore;

  const ring = document.querySelector("#score-ring");
  const degrees = Math.round((index / 100) * 360);
  ring.style.background = `conic-gradient(var(--color-primary) ${degrees}deg, var(--color-border) ${degrees}deg)`;
  ring.setAttribute("aria-label", `综合学习指数 ${index} 分，满分 100 分`);
  document.querySelector("#score-value").textContent = String(index);
}

/** 渲染学习分布（按课程状态聚合） */
function renderDistribution() {
  const host = document.querySelector("#distribution-chart");
  const source = state.courses.length ? state.courses : state.viewed;
  if (!source.length) {
    renderEmpty(host, "暂无学习分布数据", "选课或开课后，这里会显示课程状态分布。");
    return;
  }

  const counter = new Map();
  source.forEach((item) => {
    const status = item?.course?.courseStatus || item?.courseStatus || "未标注状态";
    counter.set(status, (counter.get(status) || 0) + 1);
  });

  const entries = [...counter.entries()].sort((a, b) => b[1] - a[1]);
  const max = Math.max(...entries.map(([, count]) => count));

  host.innerHTML = entries
    .map(
      ([status, count]) => `
      <div class="bar-row">
        <span class="bar-row__label" title="${escapeHtml(status)}">${escapeHtml(status)}</span>
        <span class="progress"><span class="progress__bar" style="width:${Math.round(
          (count / max) * 100
        )}%"></span></span>
        <span class="bar-row__value">${count} 门</span>
      </div>`
    )
    .join("");
}

/** 渲染课程完成度面板 */
function renderProgress() {
  const host = document.querySelector("#progress-panel");
  const total = state.courses.length;
  if (!total) {
    renderEmpty(host, "暂无课程进度", "加入课程后即可查看完成度。");
    return;
  }

  const viewed = state.viewed.length;
  const pending = Math.max(0, total - viewed);
  const rate = Math.round((viewed / total) * 100);

  host.innerHTML = `
    <p class="flex-between mb-md">
      <span class="text-secondary">整体完成度</span>
      <strong class="text-primary">${rate}%</strong>
    </p>
    <div class="progress mb-lg" role="progressbar" aria-valuenow="${rate}" aria-valuemin="0" aria-valuemax="100">
      <div class="progress__bar progress__bar--${
        rate >= 80 ? "success" : rate >= 40 ? "" : "warning"
      }" style="width:${rate}%"></div>
    </div>
    <div class="chart" role="img" aria-label="已学与待学课程对比">
      <div class="chart__col">
        <span class="chart__bar" style="height:${Math.max(4, rate)}%"></span>
        <span class="chart__label">已学 ${viewed}</span>
      </div>
      <div class="chart__col">
        <span class="chart__bar chart__bar--muted" style="height:${Math.max(4, 100 - rate)}%"></span>
        <span class="chart__label">待学 ${pending}</span>
      </div>
    </div>`;
}

/** 渲染学习建议 */
function renderSuggestions() {
  const host = document.querySelector("#suggestion-list");
  const total = state.courses.length;
  const viewed = state.viewed.length;
  const pending = state.pending.length;
  const noteCount = state.notes.length;
  const rate = total ? Math.round((viewed / total) * 100) : 0;

  const items = [];
  if (!total) {
    items.push({
      type: "",
      title: "先加入一门课程",
      desc: "前往课程大全挑选感兴趣的课程，系统会在你选课后开始生成学情数据。"
    });
  } else {
    if (pending > 0) {
      items.push({
        type: "warning",
        title: `还有 ${pending} 门课程未开始学习`,
        desc: "建议先推进最近加入的课程，保持每周固定的学习节奏。"
      });
    } else {
      items.push({
        type: "success",
        title: "已选课程全部开始学习",
        desc: "进度保持得不错，可以尝试把重点课程复看一遍加深理解。"
      });
    }
    items.push({
      type: rate >= 60 ? "success" : "",
      title: `课程完成度 ${rate}%`,
      desc:
        rate >= 60
          ? "完成度处于良好水平，继续保持即可。"
          : "建议每周至少完成 2 门课程的进度推进，把完成度提升到 60% 以上。"
    });
  }

  items.push({
    type: noteCount >= 3 ? "success" : "warning",
    title: noteCount >= 3 ? `已沉淀 ${noteCount} 篇笔记` : "笔记数量偏少",
    desc:
      noteCount >= 3
        ? "保持记录习惯，复习时可直接回到笔记页回顾要点。"
        : "建议每学完一节课就记录一条笔记，方便后续复习与提问。"
  });

  host.innerHTML = items
    .map(
      (item) => `
      <li class="suggestion-item${item.type ? ` suggestion-item--${item.type}` : ""}">
        <div>
          <p class="suggestion-item__title">${escapeHtml(item.title)}</p>
          <p class="suggestion-item__desc">${escapeHtml(item.desc)}</p>
        </div>
      </li>`
    )
    .join("");
}

/** 渲染待推进课程表格 */
function renderPending() {
  const host = document.querySelector("#pending-body");
  if (!state.pending.length) {
    host.innerHTML = `
      <tr>
        <td class="is-wrap text-muted" colspan="4">太棒了，当前没有待推进的课程。</td>
      </tr>`;
    return;
  }

  host.innerHTML = state.pending
    .slice(0, 8)
    .map((item) => {
      const course = item?.course ?? item;
      return `
        <tr>
          <td class="is-wrap">${escapeHtml(course?.courseName ?? "未命名课程")}</td>
          <td>${escapeHtml(course?.teachingTeachers ?? "—")}</td>
          <td><span class="tag">${escapeHtml(course?.courseStatus ?? "未标注")}</span></td>
          <td><a href="course-info.html?id=${encodeURIComponent(course?.id ?? "")}">查看课程</a></td>
        </tr>`;
    })
    .join("");
}

/** 页面初始化 */
async function main() {
  const host = document.querySelector("#pending-body");
  try {
    state.context = await initPageShell({ active: PAGE_KEY.REPORT });
    if (!state.context.user) return;
    await loadData();
    renderOverview();
    renderDistribution();
    renderProgress();
    renderSuggestions();
    renderPending();
  } catch (error) {
    console.error("学情报告加载失败", error);
    renderError(host, error, () => window.location.reload());
  }
}

document.addEventListener("DOMContentLoaded", main);
