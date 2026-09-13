/**
 * 学习分布
 * 接口：
 *   GET /api/student-courses/all-courses     已选课程（含加入时间）
 *   GET /api/student-courses/viewed-courses  已完成课程
 *   GET /api/notes/student/{studentId}       笔记产出（用于活跃度）
 *   GET /api/course/search?teachingTeachers= 教师身份下的开课分布
 * 说明：全部图表使用 CSS 绘制，不引入第三方图表库。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  renderEmpty,
  renderError,
  escapeHtml,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  courses: [],
  viewedIds: new Set(),
  notes: []
};

/** 统计出现次数并按降序返回 */
function countBy(items, picker) {
  const counter = new Map();
  items.forEach((item) => {
    const key = picker(item) || "未标注";
    counter.set(key, (counter.get(key) || 0) + 1);
  });
  return [...counter.entries()].sort((a, b) => b[1] - a[1]);
}

/** 渲染横向条形分布 */
function renderBars(host, entries, total, emptyText) {
  if (!entries.length) {
    renderEmpty(host, emptyText, "加入课程后即可看到分布统计。");
    return;
  }
  const max = Math.max(...entries.map(([, count]) => count));
  host.innerHTML = entries
    .map(
      ([label, count]) => `
      <div class="bar-row bar-row--wide">
        <span class="bar-row__label" title="${escapeHtml(label)}">${escapeHtml(label)}</span>
        <span class="progress">
          <span class="progress__bar" style="width:${Math.max(4, Math.round((count / max) * 100))}%"></span>
        </span>
        <span class="bar-row__value">${count} 门${
          total ? `（${Math.round((count / total) * 100)}%）` : ""
        }</span>
      </div>`
    )
    .join("");
}

/** 渲染月度活跃度柱状图 */
function renderMonthChart() {
  const host = document.querySelector("#month-chart");
  const now = new Date();
  const months = [];

  for (let offset = 5; offset >= 0; offset -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - offset, 1);
    months.push({
      key: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      label: `${date.getMonth() + 1}月`,
      value: 0
    });
  }

  const addToMonth = (time) => {
    if (!time) return;
    const date = new Date(time);
    if (Number.isNaN(date.getTime())) return;
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
    const target = months.find((month) => month.key === key);
    if (target) target.value += 1;
  };

  state.courses.forEach((record) => addToMonth(record?.createdAt ?? record?.course?.startDate));
  state.notes.forEach((note) => addToMonth(note.createdAt));

  const max = Math.max(1, ...months.map((month) => month.value));
  host.innerHTML = `
    <div class="chart" role="img" aria-label="近半年学习活跃度柱状图">
      ${months
        .map(
          (month) => `
        <div class="chart__col">
          <span class="chart__value">${month.value}</span>
          <span class="chart__bar" style="height:${Math.round((month.value / max) * 100)}%"></span>
          <span class="chart__label">${month.label}</span>
        </div>`
        )
        .join("")}
    </div>`;
}

/** 渲染明细表格 */
function renderDetail() {
  const host = document.querySelector("#detail-body");
  document.querySelector("#detail-count").textContent = `${state.courses.length} 门课程`;

  if (!state.courses.length) {
    host.innerHTML = '<tr><td class="is-wrap text-muted" colspan="5">暂无课程数据。</td></tr>';
    return;
  }

  host.innerHTML = state.courses
    .map((record) => {
      const course = record?.course ?? record;
      const done = state.viewedIds.has(String(course.id));
      return `
        <tr>
          <td class="is-wrap">
            <a href="course-info.html?courseNumber=${encodeURIComponent(course.courseNumber ?? "")}">${escapeHtml(
              course.courseName ?? "未命名课程"
            )}</a>
          </td>
          <td>${escapeHtml(course.teachingTeachers ?? "—")}</td>
          <td><span class="tag">${escapeHtml(course.courseStatus ?? "未标注")}</span></td>
          <td>
            <span class="state-dot ${done ? "state-dot--done" : "state-dot--doing"}">
              ${done ? "已完成" : "进行中"}
            </span>
          </td>
          <td class="is-wrap">${escapeHtml(course.courseTags ?? "—")}</td>
        </tr>`;
    })
    .join("");
}

/** 加载数据 */
async function loadData() {
  const { role, student, teacher } = state.context;

  if (role === "teacher" && teacher?.name) {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 200
    });
    state.courses = pageContent(pageData).map((course) => ({ course }));
    state.viewedIds = new Set(state.courses.map((item) => String(item.course.id)));
    state.notes = [];
    return;
  }

  if (!student) return;

  const [allPage, viewedPage, notePage] = await Promise.all([
    apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 200 }),
    apiGet("/student-courses/viewed-courses", { studentId: student.id, page: 0, size: 200 }),
    apiGet(`/notes/student/${student.id}`, { page: 0, size: 200 }).catch(() => null)
  ]);

  state.courses = pageContent(allPage);
  state.viewedIds = new Set(pageContent(viewedPage).map((item) => String(item?.course?.id)));
  state.notes = pageContent(notePage);
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  try {
    state.context = await initPageShell({ active: PAGE_KEY.STUDY_DISTRIBUTION });
    if (!state.context.user) return;

    await loadData();

    const total = state.courses.length;
    const viewed = state.viewedIds.size;
    const rate = total ? Math.round((viewed / total) * 100) : 0;

    document.querySelector("#stat-course").textContent = String(total);
    document.querySelector("#stat-viewed").textContent = String(viewed);
    document.querySelector("#stat-rate").innerHTML = `${rate}<span class="stat__unit">%</span>`;
    document.querySelector("#stat-note").textContent = String(state.notes.length);
    document.querySelector("#distribution-summary").textContent = total
      ? `共 ${total} 门课程，已完成 ${viewed} 门，累计产出 ${state.notes.length} 篇笔记`
      : "还没有课程数据，先去课程大全加入一门课程吧";

    renderBars(
      document.querySelector("#status-chart"),
      countBy(state.courses, (record) => record?.course?.courseStatus ?? record?.courseStatus),
      total,
      "暂无状态分布"
    );
    renderBars(
      document.querySelector("#tag-chart"),
      countBy(
        state.courses.flatMap((record) =>
          String(record?.course?.courseTags ?? record?.courseTags ?? "")
            .split(/[,，]/)
            .map((tag) => tag.trim())
            .filter(Boolean)
            .map((tag) => ({ tag }))
        ),
        (item) => item.tag
      ),
      total,
      "暂无标签分布"
    );
    renderMonthChart();
    renderDetail();
  } catch (error) {
    console.error("学习分布加载失败", error);
    renderError(document.querySelector("#status-chart"), error, () => window.location.reload());
  }
}

document.addEventListener("DOMContentLoaded", main);
