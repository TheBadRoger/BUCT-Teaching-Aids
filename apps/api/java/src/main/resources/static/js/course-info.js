/**
 * 课程详情页
 * 接口：
 *   GET  /api/course/search?courseNumber|courseName   按编号/名称定位课程（后端未提供按 ID 查询接口）
 *   GET  /api/course/search?id=                       按主键回退查询
 *   POST /api/course/view/{courseId}/record           记录一次课程访问
 *   GET  /api/course/view/{courseId}/count            读取当前访问量
 *   GET  /api/notes/course/{courseId}                 课程下的公开/个人笔记
 *   POST /api/student-courses/select                  学生加入学习
 *   PUT  /api/student-courses/update-viewed           标记已学
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  apiPut,
  pageContent,
  renderError,
  escapeHtml,
  fromNow,
  toast,
  getParam,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  course: null,
  enrolled: false,
  viewed: false
};

/**
 * 解析课程大纲文本为条目数组
 * @param {string} outline 原始大纲文本（支持换行、序号、分号分隔）
 * @returns {string[]} 大纲条目
 */
function parseOutline(outline) {
  if (!outline) return [];
  return String(outline)
    .split(/\r?\n|;|；/)
    .map((line) => line.replace(/^\s*\d+[.、)]\s*/, "").trim())
    .filter(Boolean);
}

/**
 * 查询课程详情
 * 后端 /api/course/search 仅支持 courseName / courseNumber / teachingTeachers / courseStatus /
 * courseTags / startDate 作为筛选条件（不支持按主键 id 查询），因此详情页以课程编号为主键，
 * 并兼容通过 courseName 或列表页传入的 id（id 场景下退化为按课程编号精确匹配失败后返回 null）。
 * @returns {Promise<object|null>} 课程对象，未找到返回 null
 */
async function fetchCourse() {
  const courseNumber = getParam("courseNumber") || getParam("id");
  const courseName = getParam("courseName");

  const attempts = [];
  if (courseNumber) attempts.push({ courseNumber });
  if (courseName) attempts.push({ courseName });

  for (const params of attempts) {
    const pageData = await apiGet("/course/search", { ...params, page: 0, size: 1 });
    const [course] = pageContent(pageData);
    if (course) return course;
  }
  return null;
}

/** 判断当前学生是否已加入该课程 */
async function fetchEnrollment(studentId, courseId) {
  if (!studentId || !courseId) return { enrolled: false, viewed: false };
  try {
    const pageData = await apiGet("/student-courses/all-courses", {
      studentId,
      page: 0,
      size: 200
    });
    const record = pageContent(pageData).find((item) => Number(item?.course?.id) === Number(courseId));
    return { enrolled: Boolean(record), viewed: Boolean(record?.isViewed) };
  } catch {
    return { enrolled: false, viewed: false };
  }
}

/** 渲染课程详情页主体 */
function renderCourse(course, notes, viewCount) {
  const host = document.querySelector("#course-root");
  const title = course.courseName ?? "未命名课程";
  document.title = `${title} - 课程详情`;
  document.querySelector("#crumb-course").textContent = title;

  const tags = String(course.courseTags ?? "")
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean);
  const outline = parseOutline(course.courseOutline);
  const price = Number(course.coursePrice);
  const priceText = Number.isFinite(price) && price > 0 ? `¥ ${price.toFixed(2)}` : "免费";

  host.innerHTML = `
    <section class="course-hero">
      <img class="course-hero__cover" src="${escapeHtml(
        course.courseImage || "images/test-img.jpg"
      )}" alt="${escapeHtml(title)} 的课程封面" loading="lazy">
      <div>
        <h1 class="course-hero__title">${escapeHtml(title)}</h1>
        <div class="course-hero__tags">
          ${course.courseStatus ? `<span class="tag tag--primary">${escapeHtml(course.courseStatus)}</span>` : ""}
          ${tags.map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}
        </div>
        <dl class="course-hero__meta">
          <div><dt>课程编号</dt><dd>${escapeHtml(course.courseNumber ?? "—")}</dd></div>
          <div><dt>授课教师</dt><dd>${escapeHtml(course.teachingTeachers ?? "—")}</dd></div>
          <div><dt>开课班级</dt><dd>${escapeHtml(course.teachingClasses ?? "—")}</dd></div>
          <div><dt>上课地点</dt><dd>${escapeHtml(course.classAddress ?? "—")}</dd></div>
          <div><dt>开课日期</dt><dd>${escapeHtml(course.startDate ?? "—")}</dd></div>
          <div><dt>课程时长</dt><dd>${escapeHtml(course.duration ?? "—")}</dd></div>
          <div><dt>适用对象</dt><dd>${escapeHtml(course.targetAudience ?? "—")}</dd></div>
          <div><dt>访问量</dt><dd>${viewCount} 次</dd></div>
        </dl>
        <p class="course-price mb-lg">${escapeHtml(priceText)}</p>
        <div class="course-hero__actions">
          <button class="btn btn--primary btn--lg" id="enroll-btn" type="button">
            ${state.enrolled ? "已加入学习" : "加入学习"}
          </button>
          <a class="btn btn--lg" href="course-play.html?courseNumber=${encodeURIComponent(
            course.courseNumber ?? course.id ?? ""
          )}">开始上课</a>
        </div>
      </div>
    </section>

    <div class="course-body">
      <div class="flex-1">
        <section class="card" aria-labelledby="intro-heading">
          <div class="card__header">
            <h2 class="card__title" id="intro-heading">课程简介</h2>
          </div>
          <p class="rich-text">${escapeHtml(course.courseIntroduction || "该课程暂未填写简介。")}</p>
        </section>

        <section class="card mt-lg" aria-labelledby="objective-heading">
          <div class="card__header">
            <h2 class="card__title" id="objective-heading">教学目标</h2>
          </div>
          <p class="rich-text">${escapeHtml(course.teachingObjectives || "该课程暂未填写教学目标。")}</p>
        </section>

        <section class="card mt-lg" aria-labelledby="outline-heading">
          <div class="card__header">
            <h2 class="card__title" id="outline-heading">课程大纲</h2>
            <span class="card__subtitle">${outline.length} 个课时</span>
          </div>
          <ol class="outline-list">
            ${
              outline.length
                ? outline
                    .map(
                      (item, index) => `
              <li class="outline-item">
                <span class="outline-item__order">${index + 1}</span>
                <span class="outline-item__title">${escapeHtml(item)}</span>
                <a class="btn btn--sm" href="course-play.html?courseNumber=${encodeURIComponent(
                  course.courseNumber ?? course.id ?? ""
                )}&lesson=${index + 1}">播放</a>
              </li>`
                    )
                    .join("")
                : '<li class="outline-item"><span class="text-muted">该课程暂未填写大纲。</span></li>'
            }
          </ol>
        </section>
      </div>

      <aside class="flex-1">
        <section class="card" aria-labelledby="notes-heading">
          <div class="card__header">
            <h2 class="card__title" id="notes-heading">课程笔记</h2>
            <a class="card__extra" href="my-note.html">我的笔记</a>
          </div>
          <ul class="list-plain" id="course-notes"></ul>
        </section>

        <section class="card mt-lg" aria-labelledby="progress-heading">
          <div class="card__header">
            <h2 class="card__title" id="progress-heading">我的学习状态</h2>
          </div>
          <p class="text-secondary mb-md" id="viewed-text">${
            state.enrolled ? (state.viewed ? "已学完该课程" : "已加入，尚未开始学习") : "尚未加入该课程"
          }</p>
          <div class="progress mb-lg">
            <div class="progress__bar${state.viewed ? " progress__bar--success" : ""}" style="width:${
              state.viewed ? 100 : state.enrolled ? 30 : 0
            }%"></div>
          </div>
          <button class="btn btn--block" id="viewed-btn" type="button" ${
            state.enrolled ? "" : "disabled"
          }>${state.viewed ? "标记为未学" : "标记为已学"}</button>
        </section>
      </aside>
    </div>`;

  renderNotes(notes);
  bindActions(course);
}

/** 渲染课程笔记列表 */
function renderNotes(notes) {
  const host = document.querySelector("#course-notes");
  if (!notes.length) {
    host.innerHTML = '<li class="text-muted">该课程下暂无笔记，去<a href="my-note.html">写一篇</a>吧。</li>';
    return;
  }
  host.innerHTML = notes
    .slice(0, 6)
    .map(
      (note) => `
      <li class="list-item">
        <div class="list-item__body">
          <p class="list-item__title">${escapeHtml(note.title ?? "无标题笔记")}</p>
          <p class="list-item__meta">
            <span>${escapeHtml(note.student?.name ?? "匿名")}</span>
            <span>${Number(note.likeCount) || 0} 赞</span>
            <span>${escapeHtml(fromNow(note.createdAt))}</span>
          </p>
        </div>
      </li>`
    )
    .join("");
}

/** 绑定交互按钮 */
function bindActions(course) {
  const enrollButton = document.querySelector("#enroll-btn");
  const viewedButton = document.querySelector("#viewed-btn");
  const student = state.context?.student;

  enrollButton?.addEventListener("click", async () => {
    if (!student) {
      toast("请先以学生身份登录并绑定学号", "warn");
      return;
    }
    if (state.enrolled) {
      toast("你已经加入该课程了");
      return;
    }
    enrollButton.disabled = true;
    try {
      await apiPost("/student-courses/select", null, { studentId: student.id, courseId: course.id });
      state.enrolled = true;
      enrollButton.textContent = "已加入学习";
      document.querySelector("#viewed-text").textContent = "已加入，尚未开始学习";
      viewedButton.disabled = false;
      toast("已加入学习");
    } catch (error) {
      enrollButton.disabled = false;
      toast(error.message || "加入学习失败", "error");
    }
  });

  viewedButton?.addEventListener("click", async () => {
    if (!student) return;
    viewedButton.disabled = true;
    try {
      await apiPut(
        "/student-courses/update-viewed",
        null,
        { studentId: student.id, courseId: course.id, isViewed: !state.viewed }
      );
      state.viewed = !state.viewed;
      document.querySelector("#viewed-text").textContent = state.viewed
        ? "已学完该课程"
        : "已加入，尚未开始学习";
      document.querySelector(".progress__bar").style.width = state.viewed ? "100%" : "30%";
      document.querySelector(".progress__bar").classList.toggle("progress__bar--success", state.viewed);
      viewedButton.textContent = state.viewed ? "标记为未学" : "标记为已学";
      viewedButton.disabled = false;
      toast(state.viewed ? "已标记为已学" : "已标记为未学");
    } catch (error) {
      viewedButton.disabled = false;
      toast(error.message || "更新学习状态失败", "error");
    }
  });
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  const host = document.querySelector("#course-root");
  try {
    state.context = await initPageShell({ active: PAGE_KEY.COURSE_INFO, requireLogin: false });

    const course = await fetchCourse();
    if (!course) {
      renderError(host, "未找到对应的课程，请确认课程编号或返回课程大全重新选择。", () => {
        window.location.href = "course-list.html";
      });
      return;
    }
    state.course = course;

    const [enrollment, notesPage, viewCount] = await Promise.all([
      fetchEnrollment(state.context?.student?.id, course.id),
      apiGet(`/notes/course/${course.id}`, { page: 0, size: 6 }).catch(() => null),
      apiGet(`/course/view/${course.id}/count`).catch(() => Number(course.viewCount) || 0)
    ]);
    state.enrolled = enrollment.enrolled;
    state.viewed = enrollment.viewed;

    renderCourse(course, pageContent(notesPage), Number(viewCount) || 0);

    // 进入详情即记录一次访问量（失败不阻断页面）
    apiPost(`/course/view/${course.id}/record`).catch(() => {});
  } catch (error) {
    console.error("课程详情加载失败", error);
    renderError(host, error, () => window.location.reload());
  }
}

document.addEventListener("DOMContentLoaded", main);
