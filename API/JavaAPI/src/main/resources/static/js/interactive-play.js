/**
 * 课时播放 - 互动视频（二期）
 * 接口：
 *   GET  /api/course/search?courseNumber=        读取课程与课时大纲
 *   POST /api/student-courses/select             加入学习
 *   PUT  /api/student-courses/update-viewed      标记已学
 *   POST /api/notes/create                       记录互动答题笔记（可选）
 *   GET  /api/comments/note/{noteId}             课时讨论
 * 说明：互动视频需要“视频时间轴 + 互动点（题目/投票/分支）”的数据结构支撑，
 *      后端尚无对应实体与接口（课程大纲仅有“课时名称|视频地址”两级信息）。
 *      本页因此先实现可运行的播放骨架，互动点数据按约定格式从大纲的第三段解析：
 *      “课时名称|视频地址|00:30:选择题:题干|选项A;选项B”
 *      当大纲未提供互动点时，页面会明确提示二期能力待后端支持。
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  apiPut,
  pageContent,
  renderError,
  escapeHtml,
  toast,
  getParam,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  course: null,
  lesson: null,
  markers: [],
  viewed: false
};

/** 将 mm:ss 或 hh:mm:ss 转为秒 */
function toSeconds(text) {
  const parts = String(text ?? "")
    .split(":")
    .map((part) => Number(part));
  if (parts.some((value) => Number.isNaN(value))) return 0;
  return parts.reduce((sum, value) => sum * 60 + value, 0);
}

/** 将秒转为 mm:ss */
function toClock(seconds) {
  const total = Math.max(0, Math.floor(seconds));
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}

/**
 * 解析课时中的互动点
 * @param {string} outlineLine 大纲中的一行课时定义
 * @returns {Array<{time:string,seconds:number,type:string,question:string,options:string[]}>} 互动点
 */
function parseMarkers(outlineLine) {
  const segments = String(outlineLine ?? "").split("|").map((part) => part.trim());
  if (segments.length <= 2) return [];
  return segments
    .slice(2)
    .map((segment) => {
      const [time = "00:00", type = "提示", question = "", options = ""] = segment.split(":");
      return {
        time,
        seconds: toSeconds(time),
        type: type || "提示",
        question: question || type,
        options: options ? options.split(";").filter(Boolean) : []
      };
    })
    .filter((marker) => marker.question)
    .sort((a, b) => a.seconds - b.seconds);
}

/** 定位课程与课时 */
async function fetchLesson() {
  const courseNumber = getParam("courseNumber") || getParam("id");
  if (!courseNumber) return null;

  const pageData = await apiGet("/course/search", { courseNumber, page: 0, size: 1 });
  const [course] = pageContent(pageData);
  if (!course) return null;

  const lines = String(course.courseOutline ?? "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  const lessonIndex = Math.max(0, (Number(getParam("lesson")) || 1) - 1);
  const line = lines[lessonIndex] ?? lines[0] ?? "";
  const [title = "未命名课时", videoUrl = ""] = line.split("|").map((part) => part.trim());

  return { course, title, videoUrl, rawLine: line, lessonIndex };
}

/** 渲染页面 */
function render() {
  const host = document.querySelector("#interactive-root");
  const course = state.course;
  const courseNumber = course.courseNumber ?? "";

  document.title = `${course.courseName ?? "课程"} - 互动视频课时`;
  document.querySelector("#crumb-course").textContent = course.courseName ?? "课程详情";
  document.querySelector("#crumb-course").href = `course-info.html?courseNumber=${encodeURIComponent(
    courseNumber
  )}`;

  host.innerHTML = `
    <header class="flex-between mb-lg">
      <div>
        <h1 class="page-title">${escapeHtml(state.lesson.title)}</h1>
        <p class="page-desc mb-sm">
          ${escapeHtml(course.courseName ?? "")} · 授课教师 ${escapeHtml(course.teachingTeachers ?? "—")} ·
          互动课时（二期）
        </p>
      </div>
      <div class="flex gap-sm">
        <a class="btn" href="course-play.html?courseNumber=${encodeURIComponent(
          courseNumber
        )}&lesson=${state.lesson.lessonIndex + 1}">切换普通播放</a>
        <a class="btn btn--primary" href="course-info.html?courseNumber=${encodeURIComponent(
          courseNumber
        )}">返回课程</a>
      </div>
    </header>

    <div class="interactive-hero">
      <section class="card card--flush">
        <div class="interactive-player">
          <video id="interactive-video" controls playsinline preload="metadata"
                 poster="${escapeHtml(course.courseImage || "images/test-img.jpg")}"
                 aria-label="互动视频播放器"></video>
          <div class="interactive-marker" id="interactive-marker" hidden>
            <p class="interactive-marker__bubble" id="marker-bubble"></p>
          </div>
        </div>
        <div class="card__body">
          <div class="flex-wrap flex gap-sm mb-md">
            <button class="btn btn--sm" id="toggle-viewed" type="button">${
              state.viewed ? "已标记已学" : "标记已学"
            }</button>
            <button class="btn btn--sm" id="save-note" type="button">把互动记录存为笔记</button>
          </div>
          <p class="phase-note">
            互动视频（二期）：需要后端提供“课时时间轴 + 互动点（选择题 / 投票 / 分支跳转）”数据结构，
            当前后端课程大纲仅支持“课时名称|视频地址”。本页已按
            <code>课时名称|视频地址|00:30:选择题:题干|选项A;选项B</code> 的约定解析互动点，
            后端补充字段后即可直接对接，无需改动页面结构。
          </p>
        </div>
      </section>

      <aside class="card" aria-labelledby="marker-heading">
        <div class="card__header">
          <h2 class="card__title" id="marker-heading">互动点</h2>
          <span class="card__subtitle">${state.markers.length} 个</span>
        </div>
        <ol class="marker-list" id="marker-list"></ol>
      </aside>
    </div>`;

  renderMarkers();
  bindEvents();
}

/** 渲染互动点列表 */
function renderMarkers() {
  const host = document.querySelector("#marker-list");
  if (!state.markers.length) {
    host.innerHTML =
      '<li class="text-muted">当前课时未配置互动点。可在课程大纲中按约定格式补充，或等待二期后端支持。</li>';
    return;
  }
  host.innerHTML = state.markers
    .map(
      (marker, index) => `
      <li class="marker-item">
        <span class="marker-item__time">${escapeHtml(marker.time)}</span>
        <span class="ellipsis">${escapeHtml(marker.question)}</span>
        <span class="tag marker-item__type">${escapeHtml(marker.type)}</span>
        <button class="btn btn--sm" type="button" data-marker-index="${index}">跳转</button>
      </li>`
    )
    .join("");
}

/** 绑定播放与互动交互 */
function bindEvents() {
  const video = document.querySelector("#interactive-video");
  const markerBox = document.querySelector("#interactive-marker");
  const bubble = document.querySelector("#marker-bubble");
  const lesson = state.lesson;

  if (lesson.videoUrl) {
    video.src = lesson.videoUrl;
  } else {
    video.poster = state.course.courseImage || "images/test-img.jpg";
    toast("该课时未配置视频地址，可先在“在线录制视频”页录制并关联", "warn");
  }

  // 到达互动点时弹出提示（同一互动点仅提示一次）
  const shown = new Set();
  video?.addEventListener("timeupdate", () => {
    const current = video.currentTime;
    const hit = state.markers.find(
      (marker) => !shown.has(marker.time) && current >= marker.seconds && current < marker.seconds + 1.2
    );
    if (!hit) return;
    shown.add(hit.time);
    bubble.textContent = `【${hit.type}】${hit.question}${
      hit.options.length ? `（选项：${hit.options.join(" / ")}）` : ""
    }`;
    markerBox.hidden = false;
    window.setTimeout(() => {
      markerBox.hidden = true;
    }, 6000);
  });

  document.querySelector("#marker-list")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-marker-index]");
    if (!button) return;
    const marker = state.markers[Number(button.dataset.markerIndex)];
    if (marker && video) {
      video.currentTime = marker.seconds;
      video.play().catch(() => {});
    }
  });

  document.querySelector("#toggle-viewed")?.addEventListener("click", async (event) => {
    const student = state.context?.student;
    if (!student) {
      toast("请先以学生身份绑定学号", "warn");
      return;
    }
    event.currentTarget.disabled = true;
    try {
      await apiPost("/student-courses/select", null, {
        studentId: student.id,
        courseId: state.course.id
      }).catch(() => {});
      await updateViewed(student.id, !state.viewed);
    } catch (error) {
      toast(error.message || "更新失败", "error");
    } finally {
      event.currentTarget.disabled = false;
    }
  });

  document.querySelector("#save-note")?.addEventListener("click", async () => {
    const student = state.context?.student;
    if (!student) {
      toast("请先以学生身份绑定学号", "warn");
      return;
    }
    try {
      await apiPost("/notes/create", {
        title: `${state.lesson.title} · 互动视频学习记录`,
        content: `课程：${state.course.courseName ?? ""}\n课时：${state.lesson.title}\n互动点数量：${
          state.markers.length
        }\n\n${state.markers
          .map((marker) => `- ${marker.time} 【${marker.type}】${marker.question}`)
          .join("\n")}`,
        isPublic: false,
        student: { id: student.id },
        course: { id: state.course.id }
      });
      toast("已保存为笔记");
    } catch (error) {
      toast(error.message || "保存笔记失败", "error");
    }
  });
}

/** 更新已学状态 */
async function updateViewed(studentId, isViewed) {
  await apiPut("/student-courses/update-viewed", null, {
    studentId,
    courseId: state.course.id,
    isViewed
  });
  state.viewed = isViewed;
  document.querySelector("#toggle-viewed").textContent = isViewed ? "已标记已学" : "标记已学";
  toast(isViewed ? "已标记为已学" : "已取消已学标记");
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  const host = document.querySelector("#interactive-root");
  try {
    state.context = await initPageShell({ active: PAGE_KEY.COURSE_PLAY, requireLogin: false });

    const result = await fetchLesson();
    if (!result) {
      renderError(host, "未找到对应的课程或课时，请从课程详情页进入。", () => {
        window.location.href = "course-list.html";
      });
      return;
    }
    state.course = result.course;
    state.lesson = result;
    state.markers = parseMarkers(result.rawLine);

    // 读取当前学习状态
    if (state.context?.student) {
      const pageData = await apiGet("/student-courses/all-courses", {
        studentId: state.context.student.id,
        page: 0,
        size: 200
      }).catch(() => null);
      const record = pageContent(pageData).find(
        (item) => Number(item?.course?.id) === Number(result.course.id)
      );
      state.viewed = Boolean(record?.isViewed);
    }

    render();
  } catch (error) {
    console.error("互动视频课时加载失败", error);
    renderError(host, error, () => window.location.reload());
  }
}

document.addEventListener("DOMContentLoaded", main);
