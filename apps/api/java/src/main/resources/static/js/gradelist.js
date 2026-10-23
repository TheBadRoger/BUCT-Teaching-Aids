/**
 * 名单 - 列表
 * 接口：
 *   GET /api/course/search?teachingTeachers=   本人开设课程（含开课班级 teachingClasses）
 *   GET /api/students/search?className=        按班级统计学生人数
 * 说明：后端未单独维护“班级”实体，班级来源于课程的 teachingClasses 字段；
 *      一个课程的 teachingClasses 支持逗号分隔的多个班级，本页会逐个班级统计人数。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  rows: [],
  keyword: "",
  status: ""
};

/** 拆分开课班级字段 */
function splitClasses(text) {
  return String(text ?? "")
    .split(/[,，、;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

/** 加载班级名单概览 */
async function loadData() {
  const tbody = document.querySelector("#grade-body");
  renderLoading(tbody, 3);

  const teacher = state.context?.teacher;
  if (!teacher?.name) {
    tbody.innerHTML = `
      <tr>
        <td class="is-wrap" colspan="5">
          当前账号未绑定教师身份，无法查看班级名单。请先前往
          <a href="apply-teacher.html">申请成为老师</a>。
        </td>
      </tr>`;
    return;
  }

  try {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 200
    });
    const courses = pageContent(pageData);

    // 汇总班级 → 课程与状态
    const classMap = new Map();
    courses.forEach((course) => {
      splitClasses(course.teachingClasses).forEach((className) => {
        if (!classMap.has(className)) {
          classMap.set(className, { className, courses: [] });
        }
        classMap.get(className).courses.push(course);
      });
    });

    // 并行统计每个班级的学生人数
    const rows = await Promise.all(
      [...classMap.values()].map(async (entry) => {
        const studentPage = await apiGet("/students/search", {
          className: entry.className,
          page: 0,
          size: 1
        }).catch(() => null);
        return {
          ...entry,
          studentCount: Number(studentPage?.totalElements) || 0
        };
      })
    );

    state.rows = rows.sort((a, b) => b.studentCount - a.studentCount);
    renderTable();
    renderStats(courses);
  } catch (error) {
    console.error("名单数据加载失败", error);
    renderError(tbody, error, loadData);
  }
}

/** 渲染表格 */
function renderTable() {
  const tbody = document.querySelector("#grade-body");
  const filtered = state.rows
    .filter((row) => !state.keyword || row.className.includes(state.keyword))
    .filter((row) => {
      if (!state.status) return true;
      return row.courses.some((course) => course.courseStatus === state.status);
    });

  document.querySelector("#gradelist-summary").textContent = state.rows.length
    ? `共 ${state.rows.length} 个开课班级，当前展示 ${filtered.length} 个`
    : "暂时没有开课班级";

  if (!filtered.length) {
    renderEmpty(
      tbody,
      "没有符合条件的班级",
      "调整筛选条件，或先在课程中填写开课班级字段。"
    );
    return;
  }

  const maxCount = Math.max(1, ...filtered.map((row) => row.studentCount));

  tbody.innerHTML = filtered
    .map((row) => {
      const courseNames = row.courses.map((course) => course.courseName ?? "未命名课程");
      const statuses = [...new Set(row.courses.map((course) => course.courseStatus ?? "未标注"))];
      return `
        <tr>
          <td class="is-wrap">${escapeHtml(row.className)}</td>
          <td class="is-wrap">${escapeHtml(courseNames.join("、"))}</td>
          <td>
            <span class="class-scale">
              <span class="progress">
                <span class="progress__bar" style="width:${Math.round(
                  (row.studentCount / maxCount) * 100
                )}%"></span>
              </span>
              <span class="class-scale__value">${row.studentCount} 人</span>
            </span>
          </td>
          <td>${statuses.map((status) => `<span class="tag">${escapeHtml(status)}</span>`).join(" ")}</td>
          <td>
            <span class="row-ops">
              <a href="gradedetail-list.html?className=${encodeURIComponent(row.className)}">学生名单</a>
              <a href="gradedetail.html?className=${encodeURIComponent(row.className)}">名单详情</a>
            </span>
          </td>
        </tr>`;
    })
    .join("");
}

/** 渲染统计指标 */
function renderStats(courses) {
  const totalStudents = state.rows.reduce((sum, row) => sum + row.studentCount, 0);
  document.querySelector("#stat-class").textContent = String(state.rows.length);
  document.querySelector("#stat-course").textContent = String(courses.length);
  document.querySelector("#stat-student").textContent = String(totalStudents);
  document.querySelector("#stat-average").textContent = state.rows.length
    ? String(Math.round(totalStudents / state.rows.length))
    : "0";
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.GRADE_LIST });
  if (!state.context.user) return;

  document.querySelector("#gradelist-filter")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    state.keyword = (data.get("keyword") ?? "").toString().trim();
    state.status = (data.get("status") ?? "").toString();
    renderTable();
  });
  document.querySelector("#grade-reset")?.addEventListener("click", () => {
    window.setTimeout(() => {
      state.keyword = "";
      state.status = "";
      renderTable();
    }, 0);
  });

  await loadData();
}

document.addEventListener("DOMContentLoaded", main);
