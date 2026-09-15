/**
 * 我的易课堂
 * 接口：
 *   GET    /api/student-courses/all-courses         已选全部课程
 *   GET    /api/student-courses/viewed-courses      已学完课程
 *   GET    /api/student-courses/not-viewed-courses  待学习课程
 *   PUT    /api/student-courses/update-viewed       切换学习状态
 *   DELETE /api/student-courses/drop                退选课程
 *   GET    /api/notes/student/{studentId}           最近笔记
 * 教师身份：改为展示本人开设的课程（/api/course/search?teachingTeachers=）。
 */
import {
  initPageShell,
  apiGet,
  apiPut,
  apiDelete,
  pageContent,
  courseCardHtml,
  renderPagination,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  formatDate,
  toast,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 8;

const state = {
  context: null,
  scope: "all",
  page: 0,
  totalPages: 0,
  totalElements: 0,
  counts: { all: 0, viewed: 0, pending: 0 },
  role: "student"
};

/** 依据当前筛选范围请求课程数据 */
async function fetchCourses() {
  const { student, teacher } = state.context;

  if (state.role === "teacher" && teacher?.name) {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: state.page,
      size: PAGE_SIZE
    });
    return pageData;
  }
  if (!student) return null;

  const pathByScope = {
    all: "/student-courses/all-courses",
    viewed: "/student-courses/viewed-courses",
    pending: "/student-courses/not-viewed-courses"
  };
  return apiGet(pathByScope[state.scope], {
    studentId: student.id,
    page: state.page,
    size: PAGE_SIZE
  });
}

/** 渲染课程列表 */
async function renderList() {
  const grid = document.querySelector("#class-course-grid");
  if (state.role === "teacher") {
    document.querySelector("#course-tabs-heading").textContent = "我开设的课程";
  }
  renderLoading(grid, 3);

  try {
    const pageData = await fetchCourses();
    const records = pageContent(pageData);
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || records.length;

    document.querySelector("#course-count").textContent = `共 ${state.totalElements} 门`;

    if (!records.length) {
      const tips = {
        all: ["还没有加入任何课程", "前往课程大全挑选课程，加入后即可在这里看到学习进度。"],
        viewed: ["还没有学完的课程", "把在学课程标记为已学后，会在这里归档。"],
        pending: ["没有待学习的课程", "所有已加入的课程都已开始学习，保持节奏！"]
      };
      renderEmpty(grid, ...(tips[state.scope] ?? tips.all));
      renderPagination(document.querySelector("#class-pagination"), { page: 0, totalPages: 0 }, () => {});
      return;
    }

    grid.innerHTML = records
      .map((item) => {
        const course = item?.course ?? item;
        const isViewed = Boolean(item?.isViewed);
        const stateText =
          state.role === "teacher"
            ? `课程状态：${escapeHtml(course.courseStatus ?? "未标注")}`
            : isViewed
              ? "已学完"
              : "待学习";
        return `
          <div class="class-card">
            ${courseCardHtml(course, { showStatus: true })}
            <div class="class-card__state">
              <span>${stateText}</span>
              <span class="class-card__ops">
                ${
                  state.role === "teacher"
                    ? `<a class="btn btn--sm" href="my-course-detail.html?courseNumber=${encodeURIComponent(
                        course.courseNumber ?? ""
                      )}">管理课程</a>`
                    : `<button class="btn btn--sm" type="button" data-action="toggle-viewed"
                        data-course-id="${escapeHtml(course.id ?? "")}"
                        data-viewed="${isViewed}">${isViewed ? "标记未学" : "标记已学"}</button>
                       <button class="btn btn--sm btn--danger" type="button" data-action="drop"
                        data-course-id="${escapeHtml(course.id ?? "")}">退课</button>`
                }
              </span>
            </div>
          </div>`;
      })
      .join("");

    bindCardActions();
    renderPagination(
      document.querySelector("#class-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        renderList();
      }
    );
  } catch (error) {
    console.error("我的课程加载失败", error);
    renderError(grid, error, renderList);
  }
}

/** 绑定卡片上的学习状态与退课操作 */
function bindCardActions() {
  const grid = document.querySelector("#class-course-grid");
  grid.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const courseId = Number(button.dataset.courseId);
    const student = state.context?.student;
    if (!student || !courseId) return;

    if (button.dataset.action === "toggle-viewed") {
      button.disabled = true;
      try {
        await apiPut("/student-courses/update-viewed", null, {
          studentId: student.id,
          courseId,
          isViewed: button.dataset.viewed !== "true"
        });
        toast("学习状态已更新");
        await Promise.all([renderList(), renderOverview()]);
      } catch (error) {
        button.disabled = false;
        toast(error.message || "更新失败", "error");
      }
    }

    if (button.dataset.action === "drop") {
      if (!window.confirm("退课后该课程将从我的易课堂移除，确认退课？")) return;
      button.disabled = true;
      try {
        await apiDelete("/student-courses/drop", { studentId: student.id, courseId });
        toast("已退课");
        await Promise.all([renderList(), renderOverview()]);
      } catch (error) {
        button.disabled = false;
        toast(error.message || "退课失败", "error");
      }
    }
  });
}

/** 渲染顶部统计 */
async function renderOverview() {
  const { student, teacher } = state.context;

  if (state.role === "teacher" && teacher?.name) {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 1
    }).catch(() => null);
    const total = Number(pageData?.totalElements) || 0;
    document.querySelector("#hero-total").textContent = String(total);
    document.querySelector("#hero-viewed").textContent = String(total);
    document.querySelector("#hero-pending").textContent = "0";
    document.querySelector("#class-hero-desc").textContent = `欢迎回来，${teacher.name ?? "老师"}，这里汇总了你开设的全部课程。`;
    return;
  }

  if (!student) {
    document.querySelector("#class-hero-desc").textContent =
      "当前账号未绑定学生身份，绑定后即可查看选课与学习进度。";
    return;
  }

  const [allPage, viewedPage, pendingPage] = await Promise.all([
    apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 1 }),
    apiGet("/student-courses/viewed-courses", { studentId: student.id, page: 0, size: 1 }),
    apiGet("/student-courses/not-viewed-courses", { studentId: student.id, page: 0, size: 1 })
  ]);

  state.counts = {
    all: Number(allPage?.totalElements) || 0,
    viewed: Number(viewedPage?.totalElements) || 0,
    pending: Number(pendingPage?.totalElements) || 0
  };

  document.querySelector("#hero-total").textContent = String(state.counts.all);
  document.querySelector("#hero-viewed").textContent = String(state.counts.viewed);
  document.querySelector("#hero-pending").textContent = String(state.counts.pending);
  document.querySelector("#class-hero-desc").textContent = `欢迎回来，${
    student.name ?? "同学"
  }！你已加入 ${state.counts.all} 门课程，其中 ${state.counts.viewed} 门已完成学习。`;
}

/** 渲染最近笔记 */
async function renderRecentNotes() {
  const host = document.querySelector("#recent-notes");
  const student = state.context?.student;
  if (!student) {
    host.innerHTML = '<li class="text-muted">绑定学生身份后可查看笔记。</li>';
    return;
  }
  try {
    const pageData = await apiGet(`/notes/student/${student.id}`, { page: 0, size: 5 });
    const notes = pageContent(pageData);
    if (!notes.length) {
      host.innerHTML = '<li class="text-muted">还没有笔记，去课时播放页写一条吧。</li>';
      return;
    }
    host.innerHTML = notes
      .map(
        (note) => `
        <li>
          <a href="my-note.html?noteId=${encodeURIComponent(note.id ?? "")}">${escapeHtml(
            note.title ?? "无标题笔记"
          )}</a>
          <p class="list-item__meta mt-sm">
            <span>${escapeHtml(note.course?.courseName ?? "未关联课程")}</span>
            <span>${formatDate(note.createdAt)}</span>
          </p>
        </li>`
      )
      .join("");
  } catch (error) {
    console.warn("最近笔记加载失败", error);
    host.innerHTML = '<li class="text-muted">笔记暂时无法加载。</li>';
  }
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  state.context = await initPageShell({ active: PAGE_KEY.MY_CLASS });
  if (!state.context.user) return;
  state.role = state.context.role;

  document.querySelector("#course-tabs")?.addEventListener("click", (event) => {
    const tab = event.target.closest("[data-scope]");
    if (!tab || tab.classList.contains("is-active")) return;
    document.querySelectorAll("#course-tabs .tabs__item").forEach((item) => {
      const active = item === tab;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-selected", String(active));
    });
    state.scope = tab.dataset.scope;
    state.page = 0;
    renderList();
  });

  await Promise.all([renderOverview(), renderList(), renderRecentNotes()]);
}

document.addEventListener("DOMContentLoaded", main);
