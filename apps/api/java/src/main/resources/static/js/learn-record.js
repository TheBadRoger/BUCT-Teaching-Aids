/**
 * 我的学习记录（学生 / 老师）
 * 接口：
 *   GET /api/student-courses/all-courses        选课记录（含 createdAt）
 *   GET /api/student-courses/viewed-courses     已学记录
 *   GET /api/notes/student/{studentId}          笔记产出记录
 *   GET /api/course/search?teachingTeachers=    教师身份下的开课记录
 * 说明：后端未提供独立的学习行为流水接口，本页以“选课时间 + 学习状态 + 笔记产出时间”
 *      聚合为可读的学习时间线。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  formatDate,
  formatDateTime,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  events: [],
  type: "all",
  range: "all",
  keyword: ""
};

/** 加载并归一化为统一的事件结构 */
async function loadEvents() {
  const { student, teacher, role } = state.context;
  const events = [];

  if (role === "teacher" && teacher?.name) {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 100
    });
    pageContent(pageData).forEach((course) => {
      events.push({
        kind: "course",
        time: course.startDate,
        title: `开课：${course.courseName ?? "未命名课程"}`,
        desc: course.courseIntroduction ?? "暂无课程简介",
        meta: [
          `课程编号 ${course.courseNumber ?? "—"}`,
          `状态 ${course.courseStatus ?? "未标注"}`,
          `${Number(course.viewCount) || 0} 次访问`
        ],
        href: `my-course-detail.html?courseNumber=${encodeURIComponent(course.courseNumber ?? "")}`
      });
    });
    state.events = events;
    return;
  }

  if (!student) {
    state.events = [];
    return;
  }

  const [allPage, viewedPage, notePage] = await Promise.all([
    apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 100 }),
    apiGet("/student-courses/viewed-courses", { studentId: student.id, page: 0, size: 100 }),
    apiGet(`/notes/student/${student.id}`, { page: 0, size: 100 })
  ]);

  const viewedIds = new Set(pageContent(viewedPage).map((item) => String(item?.course?.id)));

  pageContent(allPage).forEach((record) => {
    const course = record?.course ?? {};
    const isViewed = Boolean(record?.isViewed) || viewedIds.has(String(course.id));
    events.push({
      kind: "course",
      time: record?.createdAt ?? course.startDate,
      title: `${isViewed ? "学完" : "加入"}：${course.courseName ?? "未命名课程"}`,
      desc: isViewed
        ? "已完成该课程的学习，可前往课时播放页复看重点章节。"
        : "已加入学习计划，尚未完成全部课时。",
      meta: [
        `授课教师 ${course.teachingTeachers ?? "—"}`,
        `状态 ${course.courseStatus ?? "未标注"}`,
        isViewed ? "已完成" : "进行中"
      ],
      href: `course-info.html?courseNumber=${encodeURIComponent(course.courseNumber ?? "")}`
    });
  });

  pageContent(notePage).forEach((note) => {
    events.push({
      kind: "note",
      time: note.createdAt,
      title: `笔记：${note.title ?? "无标题笔记"}`,
      desc: String(note.content ?? "").slice(0, 120),
      meta: [
        `课程 ${note.course?.courseName ?? "未关联课程"}`,
        `${Number(note.likeCount) || 0} 赞`,
        note.isPublic ? "公开" : "私有"
      ],
      href: `my-note.html?noteId=${encodeURIComponent(note.id ?? "")}`
    });
  });

  state.events = events;
}

/** 依据筛选条件过滤事件 */
function applyFilters() {
  const now = Date.now();
  const rangeDays = Number(state.range);
  const keyword = state.keyword.toLowerCase();

  return state.events
    .filter((event) => state.type === "all" || event.kind === state.type)
    .filter((event) => {
      if (!Number.isFinite(rangeDays) || rangeDays <= 0) return true;
      const time = new Date(event.time).getTime();
      if (Number.isNaN(time)) return false;
      return now - time <= rangeDays * 24 * 60 * 60 * 1000;
    })
    .filter((event) => {
      if (!keyword) return true;
      return `${event.title} ${event.desc}`.toLowerCase().includes(keyword);
    })
    .sort((a, b) => {
      const timeA = new Date(a.time).getTime() || 0;
      const timeB = new Date(b.time).getTime() || 0;
      return timeB - timeA;
    });
}

/** 渲染时间线 */
function renderTimeline() {
  const host = document.querySelector("#record-timeline");
  const events = applyFilters();

  document.querySelector("#timeline-count").textContent = `${events.length} 条记录`;
  document.querySelector("#record-summary").textContent = state.events.length
    ? `共汇总 ${state.events.length} 条学习行为，当前筛选出 ${events.length} 条`
    : "暂无可用的学习记录";

  if (!events.length) {
    renderEmpty(host, "没有符合条件的记录", "调整筛选条件，或先去学习一门课程。");
    return;
  }

  host.innerHTML = events
    .map(
      (event) => `
      <li class="timeline__item">
        <span class="timeline__dot${event.kind === "note" ? " timeline__dot--note" : ""}" aria-hidden="true"></span>
        <p class="timeline__time">
          <time datetime="${escapeHtml(event.time ?? "")}">${formatDateTime(event.time)}</time>
          <span class="ml-sm">（${formatDate(event.time)}）</span>
        </p>
        <div class="timeline__card">
          <p class="timeline__title">
            <a href="${escapeHtml(event.href ?? "#")}">${escapeHtml(event.title)}</a>
          </p>
          <p class="timeline__desc">${escapeHtml(event.desc)}</p>
          <p class="timeline__meta">
            ${event.meta.map((item) => `<span>${escapeHtml(item)}</span>`).join("")}
          </p>
        </div>
      </li>`
    )
    .join("");
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  const host = document.querySelector("#record-timeline");
  try {
    state.context = await initPageShell({ active: PAGE_KEY.LEARN_RECORD });
    if (!state.context.user) return;
    renderLoading(host, 3);
    await loadEvents();
    renderTimeline();
  } catch (error) {
    console.error("学习记录加载失败", error);
    renderError(host, error, () => window.location.reload());
  }

  document.querySelector("#record-filter")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    state.type = (data.get("type") ?? "all").toString();
    state.range = (data.get("range") ?? "all").toString();
    state.keyword = (data.get("keyword") ?? "").toString().trim();
    renderTimeline();
  });

  document.querySelector("#record-reset")?.addEventListener("click", () => {
    window.setTimeout(() => {
      state.type = "all";
      state.range = "all";
      state.keyword = "";
      renderTimeline();
    }, 0);
  });
}

document.addEventListener("DOMContentLoaded", main);
