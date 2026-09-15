/**
 * 我开的课（老师、机构）
 * 接口：
 *   GET    /api/course/search?teachingTeachers=  按授课教师检索本人开设的课程
 *   DELETE /api/course/batch                     批量删除课程
 *   GET    /api/course/view/{courseId}/count     读取课程访问量
 *   GET    /api/students/search?className=       按开课班级统计学生名单（用于名单页跳转）
 * 说明：教师与机构共用本页；机构账号可通过“发布信息”维护机构主页资料。
 */
import {
  initPageShell,
  apiGet,
  apiDelete,
  pageContent,
  renderPagination,
  renderError,
  renderLoading,
  escapeHtml,
  toast,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 10;

const state = {
  context: null,
  page: 0,
  totalPages: 0,
  totalElements: 0,
  courses: [],
  selected: new Set()
};

/** 当前查询主体名称：教师姓名 */
function ownerName() {
  return state.context?.teacher?.name ?? "";
}

/** 加载课程列表 */
async function loadCourses() {
  const tbody = document.querySelector("#course-body");
  renderLoading(tbody, 3);

  const form = document.querySelector("#course-filter");
  const data = new FormData(form);
  const name = ownerName();
  if (!name) {
    tbody.innerHTML = `
      <tr>
        <td class="is-wrap" colspan="7">
          当前账号未绑定教师身份，无法管理课程。请先前往
          <a href="apply-teacher.html">申请成为老师</a> 完成认证。
        </td>
      </tr>`;
    return;
  }

  try {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: name,
      courseName: (data.get("courseName") ?? "").toString().trim(),
      courseStatus: (data.get("courseStatus") ?? "").toString(),
      sort: (data.get("sort") ?? "id").toString(),
      page: state.page,
      size: PAGE_SIZE
    });

    state.courses = pageContent(pageData);
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || state.courses.length;
    document.querySelector("#course-count").textContent = `共 ${state.totalElements} 门课程`;

    if (!state.courses.length) {
      tbody.innerHTML = `
        <tr>
          <td class="is-wrap text-muted" colspan="7">
            还没有开设课程，点击右上角“我要开课 - 上传课程”创建第一门课程。
          </td>
        </tr>`;
      renderPagination(document.querySelector("#course-pagination"), { page: 0, totalPages: 0 }, () => {});
      renderSummary();
      return;
    }

    tbody.innerHTML = state.courses
      .map(
        (course) => `
        <tr>
          <td>
            <label class="choice">
              <input type="checkbox" data-select-id="${escapeHtml(course.id ?? "")}"
                     ${state.selected.has(String(course.id)) ? "checked" : ""}>
              <span class="sr-only">选择 ${escapeHtml(course.courseName ?? "课程")}</span>
            </label>
          </td>
          <td class="is-wrap">
            <a href="my-course-detail.html?courseNumber=${encodeURIComponent(
              course.courseNumber ?? ""
            )}">${escapeHtml(course.courseName ?? "未命名课程")}</a>
          </td>
          <td>${escapeHtml(course.courseNumber ?? "—")}</td>
          <td><span class="tag">${escapeHtml(course.courseStatus ?? "未标注")}</span></td>
          <td>${escapeHtml(course.duration ?? "—")}</td>
          <td data-role="view-count" data-course-id="${escapeHtml(course.id ?? "")}">${
            Number(course.viewCount) || 0
          }</td>
          <td>
            <span class="row-ops">
              <a href="my-course-detail.html?courseNumber=${encodeURIComponent(
                course.courseNumber ?? ""
              )}">课程详情</a>
              <a href="my-course-detail.html?mode=edit&courseNumber=${encodeURIComponent(
                course.courseNumber ?? ""
              )}">编辑</a>
              <a href="gradelist.html?className=${encodeURIComponent(course.teachingClasses ?? "")}">名单</a>
              <a href="video-record.html">录课</a>
              <button class="is-danger" type="button" data-action="delete"
                      data-course-id="${escapeHtml(course.id ?? "")}">删除</button>
            </span>
          </td>
        </tr>`
      )
      .join("");

    renderPagination(
      document.querySelector("#course-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        loadCourses();
      }
    );
    renderSummary();
  } catch (error) {
    console.error("课程列表加载失败", error);
    renderError(tbody, error, loadCourses);
  }
}

/** 渲染顶部统计 */
function renderSummary() {
  const doing = state.courses.filter((course) => course.courseStatus === "进行中").length;
  const views = state.courses.reduce((sum, course) => sum + (Number(course.viewCount) || 0), 0);
  document.querySelector("#stat-total").textContent = String(state.totalElements);
  document.querySelector("#stat-doing").textContent = String(doing);
  document.querySelector("#stat-views").textContent = String(views);
  document.querySelector("#my-course-desc").textContent = ownerName()
    ? `以 ${ownerName()} 的教师身份管理课程、课时与班级名单`
    : "当前账号尚未绑定教师身份";
}

/** 删除单门课程 */
async function deleteCourse(courseId) {
  if (!window.confirm("删除后课程数据不可恢复，确认删除？")) return;
  try {
    await apiDelete("/course/batch", null, [Number(courseId)]);
    toast("课程已删除");
    state.selected.delete(String(courseId));
    await loadCourses();
  } catch (error) {
    toast(error.message || "删除失败", "error");
  }
}

/** 批量删除 */
async function batchDelete() {
  const ids = [...state.selected];
  if (!ids.length) {
    toast("请先勾选要删除的课程", "warn");
    return;
  }
  if (!window.confirm(`确认删除选中的 ${ids.length} 门课程？`)) return;
  try {
    await apiDelete("/course/batch", null, ids.map(Number));
    toast("批量删除成功");
    state.selected.clear();
    await loadCourses();
  } catch (error) {
    toast(error.message || "批量删除失败", "error");
  }
}

/** 查询真实访问量（Redis 计数） */
async function refreshViewCounts() {
  const cells = document.querySelectorAll('[data-role="view-count"]');
  await Promise.all(
    [...cells].map(async (cell) => {
      try {
        const count = await apiGet(`/course/view/${cell.dataset.courseId}/count`);
        cell.textContent = String(Number(count) || 0);
      } catch {
        /* 访问量接口异常时保留列表原始值 */
      }
    })
  );
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.MY_COURSE });
  if (!state.context.user) return;

  document.querySelector("#course-filter")?.addEventListener("submit", (event) => {
    event.preventDefault();
    state.page = 0;
    loadCourses();
  });
  document.querySelector("#filter-reset")?.addEventListener("click", () => {
    window.setTimeout(() => {
      state.page = 0;
      loadCourses();
    }, 0);
  });
  document.querySelector("#select-all")?.addEventListener("change", (event) => {
    const checked = event.target.checked;
    document.querySelectorAll("[data-select-id]").forEach((checkbox) => {
      checkbox.checked = checked;
      if (checked) {
        state.selected.add(checkbox.dataset.selectId);
      } else {
        state.selected.delete(checkbox.dataset.selectId);
      }
    });
  });

  const tbody = document.querySelector("#course-body");
  tbody.addEventListener("change", (event) => {
    const checkbox = event.target.closest("[data-select-id]");
    if (!checkbox) return;
    if (checkbox.checked) {
      state.selected.add(checkbox.dataset.selectId);
    } else {
      state.selected.delete(checkbox.dataset.selectId);
    }
  });
  tbody.addEventListener("click", (event) => {
    const button = event.target.closest('[data-action="delete"]');
    if (button) deleteCourse(button.dataset.courseId);
  });
  document.querySelector("#batch-delete")?.addEventListener("click", batchDelete);

  await loadCourses();
  refreshViewCounts();
}

document.addEventListener("DOMContentLoaded", main);
