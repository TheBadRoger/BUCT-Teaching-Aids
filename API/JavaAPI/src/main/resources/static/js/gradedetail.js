/**
 * 名单 - 详情
 * 接口：
 *   GET /api/students/search?className=            班级学生名单
 *   GET /api/course/search?teachingTeachers=       本人开设课程（构建课程下拉）
 *   GET /api/student-courses/all-courses?studentId= 单个学生的学习进度
 * 说明：为控制请求量，仅对当前页的前 N 名学生并行统计学习进度，避免一次性打满接口。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  renderError,
  escapeHtml,
  formatDate,
  getParam,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

/** 单页需要统计学习进度的学生数量上限 */
const PROGRESS_SAMPLE_LIMIT = 10;

const state = {
  context: null,
  className: "",
  students: [],
  courses: []
};

/**
 * 统计某个学生的课程学习进度
 * @param {object} student 学生对象
 * @param {number} courseCount 该班级关联课程数量
 * @returns {Promise<{total:number,done:number}>} 进度统计
 */
async function fetchProgress(student, courseCount) {
  try {
    const pageData = await apiGet("/student-courses/all-courses", {
      studentId: student.id,
      page: 0,
      size: 100
    });
    const records = pageContent(pageData);
    return {
      total: records.length,
      done: records.filter((record) => record?.isViewed).length,
      courseCount
    };
  } catch {
    return { total: 0, done: 0, courseCount };
  }
}

/** 渲染页面主体 */
function render(progressList) {
  const host = document.querySelector("#grade-root");
  const total = state.students.length;
  const genderCount = state.students.reduce(
    (acc, student) => {
      if (student.gender === "男") acc.male += 1;
      else if (student.gender === "女") acc.female += 1;
      else acc.unknown += 1;
      return acc;
    },
    { male: 0, female: 0, unknown: 0 }
  );

  document.querySelector("#crumb-class-list").href = `gradedetail-list.html?className=${encodeURIComponent(
    state.className
  )}`;
  document.title = `${state.className || "班级"} - 名单详情`;

  host.innerHTML = `
    <section class="grade-hero">
      <div>
        <h1 class="grade-hero__title">${escapeHtml(state.className || "全部班级")} · 名单详情</h1>
        <p class="grade-hero__meta">
          <span>学生 ${total} 人</span>
          <span>男生 ${genderCount.male} 人</span>
          <span>女生 ${genderCount.female} 人</span>
          ${genderCount.unknown ? `<span>性别未填 ${genderCount.unknown} 人</span>` : ""}
          <span>关联课程 ${state.courses.length} 门</span>
        </p>
      </div>
      <div class="flex-wrap flex gap-sm">
        <a class="btn" href="gradedetail-list.html?className=${encodeURIComponent(
          state.className
        )}">查看完整名单</a>
        <a class="btn" id="export-link" href="/api/students/export?className=${encodeURIComponent(
          state.className
        )}" download>导出名单</a>
      </div>
    </section>

    <section class="card mt-lg" aria-labelledby="class-course-heading">
      <div class="card__header">
        <h2 class="card__title" id="class-course-heading">班级关联课程</h2>
        <span class="card__subtitle">${state.courses.length} 门</span>
      </div>
      <ul class="list-plain">
        ${
          state.courses.length
            ? state.courses
                .map(
                  (course) => `
          <li class="flex-between">
            <span>${escapeHtml(course.courseName ?? "未命名课程")} · ${escapeHtml(
              course.courseStatus ?? "未标注"
            )}</span>
            <a class="btn btn--sm" href="my-course-detail.html?courseNumber=${encodeURIComponent(
              course.courseNumber ?? ""
            )}">课程详情</a>
          </li>`
                )
                .join("")
            : '<li class="text-muted">该班级暂未关联课程。</li>'
        }
      </ul>
    </section>

    <section class="card mt-lg" aria-labelledby="student-progress-heading">
      <div class="card__header">
        <h2 class="card__title" id="student-progress-heading">学生学习进度</h2>
        <span class="card__subtitle">按班级名单顺序展示，仅统计前 ${PROGRESS_SAMPLE_LIMIT} 名学生的进度</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <caption class="sr-only">班级学生学习进度</caption>
          <thead>
          <tr>
            <th scope="col">学号</th>
            <th scope="col">姓名</th>
            <th scope="col">班级</th>
            <th scope="col">入学日期</th>
            <th scope="col">已选课程</th>
            <th scope="col">已完成</th>
            <th scope="col">完成率</th>
            <th scope="col">联系方式</th>
          </tr>
          </thead>
          <tbody>
          ${
            state.students.length
              ? state.students
                  .map((student, index) => {
                    const progress = progressList[index] ?? { total: 0, done: 0 };
                    const rate = progress.total ? Math.round((progress.done / progress.total) * 100) : 0;
                    return `
            <tr>
              <td>${escapeHtml(student.studentNumber ?? "—")}</td>
              <td class="is-wrap">${escapeHtml(student.name ?? "—")}</td>
              <td class="is-wrap">${escapeHtml(student.className ?? "—")}</td>
              <td>${escapeHtml(formatDate(student.admissionDate))}</td>
              <td>${progress.total}</td>
              <td>${progress.done}</td>
              <td>
                <span class="flex-center gap-sm">
                  <span class="progress" style="min-width:80px">
                    <span class="progress__bar progress__bar--${
                      rate >= 80 ? "success" : rate >= 40 ? "" : "warning"
                    }" style="width:${rate}%"></span>
                  </span>
                  <span class="text-muted">${rate}%</span>
                </span>
              </td>
              <td class="is-wrap">${escapeHtml(student.telephone ?? student.email ?? "—")}</td>
            </tr>`;
                  })
                  .join("")
              : '<tr><td class="is-wrap text-muted" colspan="8">该班级暂无学生。</td></tr>'
          }
          </tbody>
        </table>
      </div>
    </section>`;
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  state.context = await initPageShell({ active: PAGE_KEY.GRADE_LIST });
  if (!state.context.user) return;

  const host = document.querySelector("#grade-root");
  state.className = getParam("className");

  try {
    const teacher = state.context.teacher;
    const [studentPage, coursePage] = await Promise.all([
      apiGet("/students/search", {
        className: state.className,
        page: 0,
        size: 200
      }),
      teacher?.name
        ? apiGet("/course/search", { teachingTeachers: teacher.name, page: 0, size: 200 }).catch(() => null)
        : Promise.resolve(null)
    ]);

    state.students = pageContent(studentPage);
    state.courses = pageContent(coursePage).filter(
      (course) =>
        !state.className ||
        String(course.teachingClasses ?? "")
          .split(/[,，、;；]/)
          .map((item) => item.trim())
          .includes(state.className)
    );

    const sample = state.students.slice(0, PROGRESS_SAMPLE_LIMIT);
    const progressList = await Promise.all(
      sample.map((student) => fetchProgress(student, state.courses.length))
    );

    render(progressList);
  } catch (error) {
    console.error("名单详情加载失败", error);
    renderError(host, error, () => window.location.reload());
  }
}

document.addEventListener("DOMContentLoaded", main);
