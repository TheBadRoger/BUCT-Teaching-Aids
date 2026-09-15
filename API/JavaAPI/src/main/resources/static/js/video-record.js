/**
 * 在线录制视频
 * 接口：
 *   GET /api/course/search?teachingTeachers=  读取本人开设的课程（用于关联课时）
 *   PUT /api/course/update?id=                更新课程大纲（courseOutline）
 * 说明：录制使用浏览器 MediaRecorder API，视频文件在本地生成并下载；
 *      项目后端未提供视频文件上传接口，因此通过“下载 + 填写可访问地址”的方式
 *      把课时写入课程大纲，学生端“课时播放”页按 “课时名称|视频地址” 解析播放。
 */
import {
  initPageShell,
  apiGet,
  apiPut,
  pageContent,
  escapeHtml,
  toast,
  readForm,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  courses: [],
  stream: null,
  recorder: null,
  chunks: [],
  blobUrl: "",
  startedAt: 0,
  timerId: null,
  mimeType: ""
};

/** 选择浏览器支持的录制格式 */
function pickMimeType() {
  const candidates = [
    "video/webm;codecs=vp9,opus",
    "video/webm;codecs=vp8,opus",
    "video/webm",
    "video/mp4"
  ];
  return candidates.find((type) => window.MediaRecorder?.isTypeSupported?.(type)) ?? "";
}

/** 检测浏览器能力并在页面提示 */
function checkSupport() {
  const tip = document.querySelector("#support-tip");
  const supported =
    typeof window.MediaRecorder !== "undefined" &&
    Boolean(navigator.mediaDevices?.getUserMedia);

  if (!supported) {
    tip.classList.add("is-unsupported");
    tip.textContent =
      "当前浏览器不支持在线录制（需支持 MediaRecorder 与 getUserMedia）。请使用最新版 Chrome/Edge，或改用本地录制后上传视频地址。";
    document.querySelector("#start-record").disabled = true;
    return false;
  }
  tip.textContent = "浏览器支持在线录制，点击“开始录制”将请求摄像头与麦克风权限。";
  return true;
}

/** 格式化录制时长 */
function formatDuration(ms) {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

/** 开始录制 */
async function startRecording() {
  const withAudio = document.querySelector("#with-audio").checked;
  try {
    state.stream = await navigator.mediaDevices.getUserMedia({
      video: { width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: withAudio
    });
  } catch (error) {
    console.error("获取媒体设备失败", error);
    toast("无法访问摄像头或麦克风，请检查浏览器权限设置", "error");
    return;
  }

  const preview = document.querySelector("#record-preview");
  preview.srcObject = state.stream;
  preview.muted = true;
  document.querySelector("#record-placeholder").classList.add("hidden");

  state.mimeType = pickMimeType();
  state.chunks = [];
  state.recorder = new MediaRecorder(state.stream, state.mimeType ? { mimeType: state.mimeType } : undefined);
  state.recorder.addEventListener("dataavailable", (event) => {
    if (event.data?.size) state.chunks.push(event.data);
  });
  state.recorder.addEventListener("stop", handleRecordingStop);
  state.recorder.start();

  state.startedAt = Date.now();
  state.timerId = window.setInterval(() => {
    document.querySelector("#record-timer").textContent = formatDuration(Date.now() - state.startedAt);
  }, 500);

  document.querySelector("#start-record").disabled = true;
  document.querySelector("#stop-record").disabled = false;
  document.querySelector("#reset-record").disabled = true;
  toast("开始录制");
}

/** 停止录制并释放设备 */
function stopRecording() {
  if (state.recorder && state.recorder.state !== "inactive") {
    state.recorder.stop();
  }
  if (state.timerId) {
    window.clearInterval(state.timerId);
    state.timerId = null;
  }
  document.querySelector("#stop-record").disabled = true;
  document.querySelector("#reset-record").disabled = false;
}

/** 录制结束：生成可回放的 Blob 地址 */
function handleRecordingStop() {
  releaseStream();
  if (!state.chunks.length) {
    toast("没有采集到视频数据，请重试", "error");
    return;
  }

  const blob = new Blob(state.chunks, { type: state.mimeType || "video/webm" });
  if (state.blobUrl) URL.revokeObjectURL(state.blobUrl);
  state.blobUrl = URL.createObjectURL(blob);

  const playback = document.querySelector("#record-playback");
  playback.src = state.blobUrl;
  document.querySelector("#record-result").classList.remove("hidden");
  document.querySelector("#record-meta").textContent = `时长 ${formatDuration(
    Date.now() - state.startedAt
  )} · 大小 ${(blob.size / 1024 / 1024).toFixed(2)} MB · 格式 ${blob.type || "video/webm"}`;

  document.querySelector("#start-record").disabled = false;
  toast("录制完成，可下载或直接关联到课时");
}

/** 释放摄像头与麦克风 */
function releaseStream() {
  state.stream?.getTracks().forEach((track) => track.stop());
  state.stream = null;
  const preview = document.querySelector("#record-preview");
  preview.srcObject = null;
}

/** 重置录制状态 */
function resetRecording() {
  releaseStream();
  if (state.timerId) window.clearInterval(state.timerId);
  state.chunks = [];
  state.recorder = null;
  state.startedAt = 0;
  document.querySelector("#record-timer").textContent = "00:00";
  document.querySelector("#record-result").classList.add("hidden");
  document.querySelector("#record-placeholder").classList.remove("hidden");
  document.querySelector("#start-record").disabled = false;
  document.querySelector("#stop-record").disabled = true;
  document.querySelector("#reset-record").disabled = false;
}

/** 下载录制结果 */
function downloadRecording() {
  if (!state.blobUrl) return;
  const extension = (state.mimeType || "video/webm").includes("mp4") ? "mp4" : "webm";
  const lessonTitle = document.querySelector("#attach-lesson").value.trim() || "课时录制";
  const link = document.createElement("a");
  link.href = state.blobUrl;
  link.download = `${lessonTitle}.${extension}`;
  link.click();
  toast("已开始下载，请上传到服务器后回填视频地址");
}

/** 加载本人开设的课程 */
async function loadCourses() {
  const { teacher } = state.context;
  const select = document.querySelector("#attach-course");
  if (!teacher?.name) {
    select.innerHTML = '<option value="">仅教师身份可关联课程</option>';
    return;
  }

  try {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 100
    });
    state.courses = pageContent(pageData);
    select.innerHTML =
      '<option value="">请选择课程</option>' +
      state.courses
        .map(
          (course) =>
            `<option value="${escapeHtml(course.id)}">${escapeHtml(course.courseName ?? "未命名课程")}（${
              escapeHtml(course.courseNumber ?? "无编号")
            }）</option>`
        )
        .join("");
    if (!state.courses.length) {
      select.innerHTML = '<option value="">暂无课程，请先创建课程</option>';
    }
  } catch (error) {
    console.warn("课程加载失败", error);
    select.innerHTML = '<option value="">课程加载失败</option>';
  }
}

/** 保存课时到课程大纲 */
async function attachLesson(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.querySelector("#attach-message");
  const submitButton = document.querySelector("#attach-submit");
  const values = readForm(form);

  const showMessage = (text, isError) => {
    message.hidden = false;
    message.classList.toggle("is-error", Boolean(isError));
    message.textContent = text;
  };

  if (!values.courseId) {
    showMessage("请选择要关联的课程。", true);
    return;
  }
  if (!values.lessonTitle) {
    showMessage("请填写课时名称。", true);
    return;
  }

  const course = state.courses.find((item) => String(item.id) === String(values.courseId));
  if (!course) {
    showMessage("未找到对应的课程，请刷新页面后重试。", true);
    return;
  }

  const entry = values.videoUrl ? `${values.lessonTitle}|${values.videoUrl}` : values.lessonTitle;
  const append = form.querySelector("#attach-append").checked;
  const nextOutline = append && course.courseOutline ? `${course.courseOutline}\n${entry}` : entry;

  submitButton.disabled = true;
  try {
    await apiPut("/course/update", { courseOutline: nextOutline }, { id: course.id });
    course.courseOutline = nextOutline;
    showMessage("课时已写入课程大纲，学生端“课时播放”页即可看到该课时。", false);
    toast("课时保存成功");
  } catch (error) {
    showMessage(error.message || "保存失败，请稍后重试。", true);
    toast(error.message || "保存失败", "error");
  } finally {
    submitButton.disabled = false;
  }
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.VIDEO_RECORD });
  if (!state.context.user) return;

  checkSupport();
  await loadCourses();

  document.querySelector("#start-record")?.addEventListener("click", startRecording);
  document.querySelector("#stop-record")?.addEventListener("click", stopRecording);
  document.querySelector("#reset-record")?.addEventListener("click", resetRecording);
  document.querySelector("#download-record")?.addEventListener("click", downloadRecording);
  document.querySelector("#discard-record")?.addEventListener("click", resetRecording);
  document.querySelector("#attach-form")?.addEventListener("submit", attachLesson);

  // 选择课程时自动带出课时序号建议
  document.querySelector("#attach-course")?.addEventListener("change", (event) => {
    const course = state.courses.find((item) => String(item.id) === event.target.value);
    if (!course) return;
    const lessonCount = String(course.courseOutline ?? "")
      .split(/\r?\n/)
      .filter((line) => line.trim()).length;
    const input = document.querySelector("#attach-lesson");
    if (!input.value) input.value = `第 ${lessonCount + 1} 课时`;
  });

  // 离开页面时释放设备，避免摄像头常亮
  window.addEventListener("beforeunload", releaseStream);
}

document.addEventListener("DOMContentLoaded", main);
