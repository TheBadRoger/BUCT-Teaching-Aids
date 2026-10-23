/**
 * 课时播放 - 普通视频
 * 接口：
 *   GET  /api/course/search?courseNumber=   读取课程与大纲
 *   GET  /api/notes/course/{courseId}       读取课程笔记
 *   POST /api/notes/create                  保存课时笔记
 *   PUT  /api/student-courses/update-viewed 播放完成后标记已学
 *   POST /api/course/view/{courseId}/record 记录课程访问量
 * 说明：具体课时视频地址由课程大纲（courseOutline）逐行描述，格式支持
 *      “课时标题” 或 “课时标题|视频地址”；未提供地址时展示占位说明。
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  apiPut,
  pageContent,
  renderError,
  renderEmpty,
  escapeHtml,
  formatDateTime,
  toast,
  getParam,
  readForm,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  course: null,
  lessons: [],
  index: 0,
  viewed: false
};

/**
 * 解析课程大纲为课时数组
 * @param {string} outline 大纲文本
 * @returns {Array<{title:string, videoUrl:string}>} 课时列表
 */
function parseLessons(outline) {
  if (!outline) return [];
  return String(outline)
    .split(/\r?\n|;|；/)
    .map((line) => line.replace(/^\s*\d+[.、)]\s*/, "").trim())
    .filter(Boolean)
    .map((line) => {
      const [title, videoUrl = ""] = line.split("|").map((part) => part.trim());
      return { title: title || "未命名课时", videoUrl };
    });
}

/** 依据课时序号与课程编号定位课程 */
async function fetchCourse() {
  const courseNumber = getParam("courseNumber") || getParam("id");
  if (!courseNumber) return null;
  const pageData = await apiGet("/course/search", { courseNumber, page: 0, size: 1 });
  const [course] = pageContent(pageData);
  return course ?? null;
}

/** 渲染课程目录 */
function renderOutline() {
  const host = document.querySelector("#play-outline");
  document.querySelector("#outline-count").textContent = `${state.lessons.length} 个课时`;

  if (!state.lessons.length) {
    renderEmpty(host, "该课程暂无课时", "课程大纲尚未配置，请联系授课教师补充。");
    return;
  }

  host.innerHTML = state.lessons
    .map(
      (lesson, index) => `
      <li>
        <button class="play-outline__item${index === state.index ? " is-active" : ""}" type="button"
                data-index="${index}">
          <span class="play-outline__index">${String(index + 1).padStart(2, "0")}</span>
          <span class="flex-1 ellipsis">${escapeHtml(lesson.title)}</span>
        </button>
      </li>`
    )
    .join("");

  host.addEventListener("click", (event) => {
    const button = event.target.closest("[data-index]");
    if (!button) return;
    const target = Number(button.dataset.index);
    if (!Number.isNaN(target) && target !== state.index) {
      state.index = target;
      switchLesson();
    }
  });
}

/** 切换课时 */
function switchLesson() {
  const lesson = state.lessons[state.index];
  const player = document.querySelector("#lesson-player");
  const notice = document.querySelector("#player-notice");
  const courseNumber = state.course?.courseNumber ?? "";

  document.querySelector("#lesson-title").textContent = lesson?.title ?? "课时播放";
  document.querySelector("#crumb-lesson").textContent = lesson?.title ?? "课时播放";
  document.querySelector("#lesson-sub").textContent = state.course
    ? `${state.course.courseName ?? ""} · 授课教师 ${state.course.teachingTeachers ?? "—"} · 第 ${
        state.index + 1
      } 讲`
    : "—";

  if (lesson?.videoUrl) {
    player.src = lesson.videoUrl;
    player.poster = state.course?.courseImage || "images/test-img.jpg";
    notice.hidden = true;
  } else {
    player.removeAttribute("src");
    player.load();
    player.poster = state.course?.courseImage || "images/test-img.jpg";
    notice.hidden = false;
    notice.textContent =
      "该课时暂未配置视频地址，可联系授课教师上传，或前往“在线录制视频”页面自行录制并关联课时。";
  }

  // 同步 URL，保证课时可被分享
  const search = new URLSearchParams(window.location.search);
  search.set("courseNumber", courseNumber);
  search.set("lesson", String(state.index + 1));
  window.history.replaceState(null, "", `${window.location.pathname}?${search.toString()}`);

  // 刷新目录高亮
  document.querySelectorAll(".play-outline__item").forEach((item, index) => {
    item.classList.toggle("is-active", index === state.index);
  });

  document.querySelector("#prev-lesson").disabled = state.index <= 0;
  document.querySelector("#next-lesson").disabled = state.index >= state.lessons.length - 1;
}

/** 渲染课程笔记 */
async function loadNotes() {
  const host = document.querySelector("#lesson-notes");
  if (!state.course?.id) return;
  try {
    const pageData = await apiGet(`/notes/course/${state.course.id}`, { page: 0, size: 10 });
    const notes = pageContent(pageData);
    document.querySelector("#note-count").textContent = `共 ${notes.length} 篇`;
    if (!notes.length) {
      host.innerHTML = '<li class="text-muted">还没有笔记，写下第一条吧。</li>';
      return;
    }
    host.innerHTML = notes
      .map(
        (note) => `
        <li>
          <p class="list-item__title">${escapeHtml(note.title ?? "无标题笔记")}</p>
          <p class="list-item__meta">
            <span>${escapeHtml(note.student?.name ?? "我")}</span>
            <span>${formatDateTime(note.createdAt)}</span>
            <span>${Number(note.likeCount) || 0} 赞</span>
          </p>
        </li>`
      )
      .join("");
  } catch (error) {
    console.warn("笔记加载失败", error);
    host.innerHTML = '<li class="text-muted">笔记暂时无法加载。</li>';
  }
}

/** 保存课时笔记 */
async function submitNote(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submitButton = document.querySelector("#note-submit");
  const values = readForm(form);

  if (!state.context?.student) {
    toast("仅学生身份可创建笔记，请先在个人中心绑定学号", "warn");
    return;
  }
  if (!values.title || !values.content) {
    toast("请填写笔记标题与内容", "warn");
    return;
  }

  submitButton.disabled = true;
  try {
    await apiPost("/notes/create", {
      title: values.title,
      content: values.content,
      isPublic: form.querySelector("#note-public").checked,
      student: { id: state.context.student.id },
      course: state.course?.id ? { id: state.course.id } : undefined
    });
    form.reset();
    toast("笔记已保存");
    await loadNotes();
  } catch (error) {
    toast(error.message || "笔记保存失败", "error");
  } finally {
    submitButton.disabled = false;
  }
}

/** 标记课程为已学 */
async function markViewed(forceValue) {
  const student = state.context?.student;
  if (!student || !state.course?.id) {
    toast("请先以学生身份绑定学号后再标记学习状态", "warn");
    return;
  }
  const next = typeof forceValue === "boolean" ? forceValue : !state.viewed;
  try {
    await apiPut("/student-courses/update-viewed", null, {
      studentId: student.id,
      courseId: state.course.id,
      isViewed: next
    });
    state.viewed = next;
    document.querySelector("#mark-viewed").textContent = next ? "已标记已学" : "标记已学";
    toast(next ? "已标记为已学" : "已取消已学标记");
  } catch (error) {
    toast(error.message || "更新学习状态失败", "error");
  }
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  try {
    state.context = await initPageShell({ active: PAGE_KEY.COURSE_PLAY, requireLogin: false });

    const course = await fetchCourse();
    if (!course) {
      renderError(
        document.querySelector(".play-main"),
        "未找到对应的课程，请从课程大全重新进入。",
        () => {
          window.location.href = "course-list.html";
        }
      );
      return;
    }
    state.course = course;
    state.lessons = parseLessons(course.courseOutline);

    const lessonParam = Number(getParam("lesson"));
    state.index = Number.isFinite(lessonParam) && lessonParam > 0 ? lessonParam - 1 : 0;
    state.index = Math.max(0, Math.min(state.index, Math.max(0, state.lessons.length - 1)));

    document.title = `${course.courseName ?? "课程"} - 课时播放`;
    document.querySelector("#crumb-course").textContent = course.courseName ?? "课程详情";
    document.querySelector("#crumb-course").href = `course-info.html?courseNumber=${encodeURIComponent(
      course.courseNumber ?? ""
    )}`;
    document.querySelector("#detail-link").href = `course-info.html?courseNumber=${encodeURIComponent(
      course.courseNumber ?? ""
    )}`;

    renderOutline();
    switchLesson();
    await loadNotes();

    // 判断当前学习状态
    if (state.context?.student) {
      const pageData = await apiGet("/student-courses/all-courses", {
        studentId: state.context.student.id,
        page: 0,
        size: 200
      });
      const record = pageContent(pageData).find(
        (item) => Number(item?.course?.id) === Number(course.id)
      );
      state.viewed = Boolean(record?.isViewed);
      document.querySelector("#mark-viewed").textContent = state.viewed ? "已标记已学" : "标记已学";
    }

    // 播放结束自动标记已学
    document.querySelector("#lesson-player")?.addEventListener("ended", () => markViewed(true));
    document.querySelector("#mark-viewed")?.addEventListener("click", () => markViewed());
    document.querySelector("#prev-lesson")?.addEventListener("click", () => {
      if (state.index > 0) {
        state.index -= 1;
        switchLesson();
      }
    });
    document.querySelector("#next-lesson")?.addEventListener("click", () => {
      if (state.index < state.lessons.length - 1) {
        state.index += 1;
        switchLesson();
      }
    });
    document.querySelector("#note-form")?.addEventListener("submit", submitNote);

    // 记录一次课程访问
    apiPost(`/course/view/${course.id}/record`).catch(() => {});
  } catch (error) {
    console.error("课时播放页初始化失败", error);
    toast("页面加载失败，请稍后重试", "error");
  }
}

document.addEventListener("DOMContentLoaded", main);
