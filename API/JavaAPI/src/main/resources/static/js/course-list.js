/**
 * 课程大全列表页
 * 接口：
 *   GET /api/course/search                  多条件分页搜索课程
 *   GET /api/course/popularity/ranking      热门课程排行（附全局统计）
 *   POST /api/student-courses/select        学生选课（学生身份下可用）
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  pageContent,
  courseCardHtml,
  renderPagination,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  toast,
  debounce,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 12;

const state = {
  context: null,
  keyword: "",
  page: 0,
  totalPages: 0,
  totalElements: 0,
  sort: "id",
  loading: false
};

/** 从表单读取筛选条件 */
function readFilters() {
  const form = document.querySelector("#filter-form");
  const data = new FormData(form);
  return {
    courseName: (data.get("courseName") ?? "").toString().trim(),
    teachingTeachers: (data.get("teachingTeachers") ?? "").toString().trim(),
    courseStatus: (data.get("courseStatus") ?? "").toString().trim(),
    courseTags: (data.get("courseTags") ?? "").toString().trim()
  };
}

/** 加载课程列表 */
async function loadCourses() {
  const grid = document.querySelector("#course-grid");
  if (state.loading) return;
  state.loading = true;
  renderLoading(grid, 4);

  try {
    const filters = readFilters();
    const pageData = await apiGet("/course/search", {
      ...filters,
      page: state.page,
      size: PAGE_SIZE,
      sort: state.sort
    });
    const list = pageContent(pageData);
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || list.length;

    document.querySelector("#list-summary").textContent = state.totalElements
      ? `共找到 ${state.totalElements} 门课程`
      : "没有符合条件的课程";

    if (!list.length) {
      renderEmpty(grid, "暂无课程", "换个关键词或清空筛选条件后再试试。");
    } else {
      grid.innerHTML = list.map((course) => courseCardHtml(course, { showStatus: true })).join("");
      bindSelectButtons(list);
    }

    renderPagination(
      document.querySelector("#course-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        loadCourses();
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    );
  } catch (error) {
    console.error("课程列表加载失败", error);
    renderError(document.querySelector("#course-grid"), error, loadCourses);
  } finally {
    state.loading = false;
  }
}

/** 为课程卡片补充“加入学习”按钮（仅学生身份） */
function bindSelectButtons(list) {
  if (state.context?.role !== "student" || !state.context?.student) return;
  const cards = document.querySelectorAll("#course-grid .course-card");
  list.forEach((course, index) => {
    const target = cards[index]?.querySelector(".course-card__body");
    if (!target) return;
    const button = document.createElement("button");
    button.className = "btn btn--sm mt-sm";
    button.type = "button";
    button.textContent = "加入学习";
    button.addEventListener("click", async () => {
      button.disabled = true;
      try {
        await apiPost("/student-courses/select", null, {
          studentId: state.context.student.id,
          courseId: course.id
        });
        button.textContent = "已加入";
        toast(`已将《${course.courseName ?? "课程"}》加入学习`);
      } catch (error) {
        button.disabled = false;
        toast(error.message || "加入学习失败", "error");
      }
    });
    target.append(button);
  });
}

/** 加载热门课程排行 */
async function loadRanking() {
  const host = document.querySelector("#ranking-list");
  try {
    const data = await apiGet("/course/popularity/ranking", { limit: 10 });
    const items = Array.isArray(data?.items) ? data.items : [];
    if (!items.length) {
      host.innerHTML = '<li class="ranking-item"><span class="text-muted">暂无排行数据</span></li>';
      return;
    }
    // 排行接口只返回 courseId，详情页需要课程编号，因此按 id 回查一次课程名称对应的编号
    const detailed = await apiGet("/course/search", { page: 0, size: 100 }).catch(() => null);
    const numberById = new Map(
      pageContent(detailed).map((course) => [String(course.id), course.courseNumber ?? ""])
    );

    host.innerHTML = items
      .map((item) => {
        const courseNumber = numberById.get(String(item.courseId)) ?? "";
        const href = courseNumber
          ? `course-info.html?courseNumber=${encodeURIComponent(courseNumber)}`
          : `course-list.html?keyword=${encodeURIComponent(item.courseName ?? "")}`;
        return `
        <li class="ranking-item">
          <span class="ranking-item__index">${Number(item.ranking) || 0}</span>
          <a class="ranking-item__name" href="${href}">${escapeHtml(item.courseName ?? "未命名课程")}</a>
          <span class="ranking-item__views">${Number(item.viewCount) || 0} 次访问</span>
        </li>`;
      })
      .join("");
  } catch (error) {
    console.warn("热门排行加载失败", error);
    host.innerHTML = '<li class="ranking-item"><span class="text-muted">排行数据暂不可用</span></li>';
  }
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.COURSE_LIST, requireLogin: false });

  // 从 URL 读取初始关键词，便于首页搜索框跳转
  const keyword = new URLSearchParams(window.location.search).get("keyword") ?? "";
  if (keyword) document.querySelector("#filter-name").value = keyword;

  document.querySelector("#filter-form")?.addEventListener("submit", (event) => {
    event.preventDefault();
    state.page = 0;
    loadCourses();
  });

  document.querySelector("#filter-form")?.addEventListener("reset", () => {
    // 重置为初始状态后再查询
    window.setTimeout(() => {
      state.page = 0;
      loadCourses();
    }, 0);
  });

  document.querySelector("#filter-name")?.addEventListener(
    "input",
    debounce(() => {
      state.page = 0;
      loadCourses();
    }, 420)
  );

  document.querySelector("#sort-select")?.addEventListener("change", (event) => {
    state.sort = event.target.value;
    state.page = 0;
    loadCourses();
  });

  await Promise.all([loadCourses(), loadRanking()]);
}

document.addEventListener("DOMContentLoaded", main);
