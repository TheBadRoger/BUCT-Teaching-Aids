/**
 * 名单 - 列表详情
 * 接口：
 *   GET /api/course/search?teachingTeachers=  取本人开课班级，构成左侧班级列表
 *   GET /api/students/search                  按班级 / 姓名 / 学号 / 性别分页查询学生
 *   GET /api/students/export                  导出名单 Excel（文件流，直接下载）
 * 说明：学生名单以 StudentDTO（含关联账号的电话与邮箱）呈现，便于教师联系与核对。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  renderPagination,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  formatDate,
  getParam,
  buildQuery,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 12;

const state = {
  context: null,
  classes: [],
  activeClass: "",
  page: 0,
  totalPages: 0,
  totalElements: 0
};

/** 拆分课程的班级字段 */
function splitClasses(text) {
  return String(text ?? "")
    .split(/[,，、;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

/** 加载班级列表（来源：本人开课的 teachingClasses） */
async function loadClasses() {
  const nav = document.querySelector("#class-nav");
  const teacher = state.context?.teacher;
  if (!teacher?.name) {
    document.querySelector("#class-count").textContent = "不可用";
    nav.innerHTML = `
      <li class="text-muted">
        名单管理面向教师身份开放。当前账号未绑定教师身份，请先前往
        <a href="apply-teacher.html">申请成为老师</a>，或返回
        <a href="my-class.html">我的易课堂</a> 查看个人学习数据。
      </li>`;
    return false;
  }

  try {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 200
    });
    const names = new Set();
    pageContent(pageData).forEach((course) => {
      splitClasses(course.teachingClasses).forEach((name) => names.add(name));
    });
    state.classes = [...names].sort();

    document.querySelector("#class-count").textContent = `${state.classes.length} 个班级`;

    if (!state.classes.length) {
      nav.innerHTML = '<li class="text-muted">暂无班级，请先在课程中填写开课班级。</li>';
      return false;
    }

    nav.innerHTML = state.classes
      .map(
        (name) => `
        <li>
          <button class="class-nav__item${name === state.activeClass ? " is-active" : ""}" type="button"
                  data-class-name="${escapeHtml(name)}">
            <span class="ellipsis">${escapeHtml(name)}</span>
            <span class="class-nav__count" data-role="class-count">—</span>
          </button>
        </li>`
      )
      .join("");

    // 逐个班级统计人数（并发请求）
    await Promise.all(
      [...nav.querySelectorAll("[data-class-name]")].map(async (button) => {
        const badge = button.querySelector('[data-role="class-count"]');
        try {
          const result = await apiGet("/students/search", {
            className: button.dataset.className,
            page: 0,
            size: 1
          });
          badge.textContent = `${Number(result?.totalElements) || 0} 人`;
        } catch {
          badge.textContent = "—";
        }
      })
    );
  } catch (error) {
    console.error("班级列表加载失败", error);
    nav.innerHTML = '<li class="text-danger">班级加载失败</li>';
  }
}

/** 加载学生名单 */
async function loadStudents() {
  const tbody = document.querySelector("#student-body");
  const form = document.querySelector("#student-filter");
  const data = new FormData(form);
  const params = {
    className: state.activeClass,
    name: (data.get("name") ?? "").toString().trim(),
    studentNumber: (data.get("studentNumber") ?? "").toString().trim(),
    gender: (data.get("gender") ?? "").toString(),
    page: state.page,
    size: PAGE_SIZE
  };

  renderLoading(tbody, 4);
  try {
    const pageData = await apiGet("/students/search", params);
    const students = pageContent(pageData);
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || students.length;

    document.querySelector("#list-summary").textContent = state.totalElements
      ? `${state.activeClass || "全部班级"} · 共 ${state.totalElements} 名学生`
      : `${state.activeClass || "全部班级"} · 暂无学生`;

    // 导出链接同步当前筛选条件
    const exportLink = document.querySelector("#export-link");
    exportLink.href = `/api/students/export${buildQuery({
      className: state.activeClass,
      name: params.name,
      studentNumber: params.studentNumber,
      gender: params.gender
    })}`;

    if (!students.length) {
      renderEmpty(tbody, "没有符合条件的学生", "调整筛选条件，或确认该班级是否已导入学生。");
      renderPagination(document.querySelector("#student-pagination"), { page: 0, totalPages: 0 }, () => {});
      return;
    }

    tbody.innerHTML = students
      .map(
        (student) => `
        <tr>
          <td>${escapeHtml(student.studentNumber ?? "—")}</td>
          <td class="is-wrap">${escapeHtml(student.name ?? "—")}</td>
          <td class="is-wrap">${escapeHtml(student.className ?? "—")}</td>
          <td>${escapeHtml(student.gender ?? "—")}</td>
          <td>${escapeHtml(formatDate(student.admissionDate))}</td>
          <td>${escapeHtml(student.telephone ?? "—")}</td>
          <td class="is-wrap">${escapeHtml(student.email ?? "—")}</td>
        </tr>`
      )
      .join("");

    renderPagination(
      document.querySelector("#student-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        loadStudents();
      }
    );
  } catch (error) {
    console.error("学生名单加载失败", error);
    renderError(tbody, error, loadStudents);
  }
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.GRADE_LIST });
  if (!state.context.user) return;

  state.activeClass = getParam("className");
  const available = await loadClasses();

  // 非教师身份不请求学生名单，避免越权读取真实数据
  if (!available) {
    document.querySelector("#list-summary").textContent = "名单管理仅对教师身份开放";
    document.querySelector("#student-body").innerHTML =
      '<tr><td class="is-wrap text-muted" colspan="7">请先完成教师身份认证后再查看班级学生名单。</td></tr>';
    document.querySelector("#export-link").classList.add("hidden");
    return;
  }

  // 未通过 URL 指定班级时默认选择第一个班级
  if (!state.activeClass && state.classes.length) {
    state.activeClass = state.classes[0];
    document.querySelector("#class-nav .class-nav__item")?.classList.add("is-active");
  }

  await loadStudents();

  document.querySelector("#class-nav")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-class-name]");
    if (!button) return;
    document.querySelectorAll("#class-nav .class-nav__item").forEach((item) => {
      item.classList.toggle("is-active", item === button);
    });
    state.activeClass = button.dataset.className;
    state.page = 0;
    loadStudents();
  });

  document.querySelector("#student-filter")?.addEventListener("submit", (event) => {
    event.preventDefault();
    state.page = 0;
    loadStudents();
  });
  document.querySelector("#student-reset")?.addEventListener("click", () => {
    window.setTimeout(() => {
      state.page = 0;
      loadStudents();
    }, 0);
  });
}

document.addEventListener("DOMContentLoaded", main);
