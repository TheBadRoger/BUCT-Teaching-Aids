/**
 * 我的笔记
 * 接口：
 *   GET    /api/notes/student/{studentId}   我的笔记
 *   GET    /api/notes/public                公开笔记
 *   GET    /api/notes/search                搜索公开笔记
 *   GET    /api/notes/popular               热门笔记
 *   GET    /api/notes/{id}                  笔记详情
 *   POST   /api/notes/create                新建笔记（必须关联学生与课程）
 *   PUT    /api/notes/update/{id}           编辑笔记
 *   DELETE /api/notes/{noteId}?studentId=   删除笔记
 *   POST   /api/notes/{noteId}/like?studentId=  点赞 / 取消点赞
 *   GET    /api/student-courses/all-courses 可选课程来源
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  apiPut,
  apiDelete,
  pageContent,
  renderPagination,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  formatDateTime,
  toast,
  getParam,
  readForm,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 8;

const state = {
  context: null,
  scope: "mine",
  keyword: "",
  page: 0,
  totalPages: 0,
  totalElements: 0,
  editingId: null,
  courses: []
};

/** 加载可关联课程（仅已加入的课程） */
async function loadCourseOptions() {
  const select = document.querySelector("#modal-note-course");
  const student = state.context?.student;
  if (!student) return;

  try {
    const pageData = await apiGet("/student-courses/all-courses", {
      studentId: student.id,
      page: 0,
      size: 100
    });
    state.courses = pageContent(pageData)
      .map((item) => item?.course)
      .filter(Boolean);
    select.innerHTML =
      '<option value="">请选择课程</option>' +
      state.courses
        .map(
          (course) =>
            `<option value="${escapeHtml(course.id)}">${escapeHtml(course.courseName ?? "未命名课程")}</option>`
        )
        .join("");
    if (!state.courses.length) {
      select.innerHTML = '<option value="">暂无可关联课程，请先选课</option>';
    }
  } catch (error) {
    console.warn("课程选项加载失败", error);
  }
}

/** 请求笔记列表 */
async function fetchNotes() {
  const student = state.context?.student;
  if (state.scope === "mine") {
    if (!student) return null;
    return apiGet(`/notes/student/${student.id}`, { page: state.page, size: PAGE_SIZE });
  }
  if (state.scope === "popular") {
    return apiGet("/notes/popular", { page: state.page, size: PAGE_SIZE });
  }
  return apiGet("/notes/search", { keyword: state.keyword, page: state.page, size: PAGE_SIZE });
}

/** 渲染笔记列表 */
async function renderList() {
  const host = document.querySelector("#note-list");
  renderLoading(host, 3);
  try {
    const pageData = await fetchNotes();
    const notes = pageContent(pageData);
    state.currentNotes = notes;
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || notes.length;
    document.querySelector("#note-summary").textContent = state.totalElements
      ? `共 ${state.totalElements} 篇笔记`
      : "还没有符合条件的笔记";

    if (!notes.length) {
      renderEmpty(
        host,
        state.scope === "mine" ? "还没有笔记" : "没有找到公开笔记",
        state.scope === "mine" ? "点击右上角“写笔记”，记录你的学习心得。" : "换个关键词再搜索试试。"
      );
      renderPagination(document.querySelector("#note-pagination"), { page: 0, totalPages: 0 }, () => {});
      return;
    }

    host.innerHTML = notes.map((note) => noteCardHtml(note)).join("");
    renderPagination(
      document.querySelector("#note-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        renderList();
      }
    );
  } catch (error) {
    console.error("笔记加载失败", error);
    renderError(host, error, renderList);
  }
}

/** 生成单条笔记的 HTML */
function noteCardHtml(note) {
  const isOwner = Number(note?.student?.id) === Number(state.context?.student?.id);
  const excerpt = String(note.content ?? "").slice(0, 220);
  return `
    <article class="note-card" data-note-id="${escapeHtml(note.id ?? "")}">
      <div class="note-card__head">
        <h2 class="note-card__title">${escapeHtml(note.title ?? "无标题笔记")}</h2>
        <span class="tag${note.isPublic ? " tag--success" : ""}">${note.isPublic ? "公开" : "私有"}</span>
      </div>
      <p class="note-card__meta">
        <span>${escapeHtml(note.student?.name ?? "匿名")}</span>
        <span>${escapeHtml(note.course?.courseName ?? "未关联课程")}</span>
        <span>${formatDateTime(note.createdAt)}</span>
        <span>${Number(note.likeCount) || 0} 赞</span>
        <span>${Number(note.commentCount) || 0} 评论</span>
      </p>
      <p class="note-card__content">${escapeHtml(excerpt)}${
        String(note.content ?? "").length > 220 ? "…" : ""
      }</p>
      <div class="note-card__ops">
        <button class="btn btn--sm" type="button" data-action="toggle-content">展开全文</button>
        <button class="btn btn--sm" type="button" data-action="like" data-note-id="${escapeHtml(
          note.id ?? ""
        )}">点赞</button>
        ${
          isOwner
            ? `<button class="btn btn--sm" type="button" data-action="edit">编辑</button>
               <button class="btn btn--sm btn--danger" type="button" data-action="delete">删除</button>`
            : ""
        }
      </div>
      <div class="note-card__full hidden" data-role="full-content">
        <p class="rich-text">${escapeHtml(String(note.content ?? ""))}</p>
      </div>
    </article>`;
}

/** 打开编辑弹窗 */
function openModal(note = null) {
  state.editingId = note?.id ?? null;
  document.querySelector("#note-modal-title").textContent = note ? "编辑笔记" : "写笔记";
  document.querySelector("#modal-note-title").value = note?.title ?? "";
  document.querySelector("#modal-note-content").value = note?.content ?? "";
  document.querySelector("#modal-note-public").checked = Boolean(note?.isPublic);
  const courseSelect = document.querySelector("#modal-note-course");
  courseSelect.value = note?.course?.id ? String(note.course.id) : "";
  courseSelect.disabled = Boolean(note);
  document.querySelector("#note-modal").hidden = false;
  document.querySelector("#modal-note-title").focus();
}

/** 关闭编辑弹窗 */
function closeModal() {
  document.querySelector("#note-modal").hidden = true;
  document.querySelector("#note-form").reset();
  state.editingId = null;
}

/** 保存笔记 */
async function saveNote(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const values = readForm(form);
  const student = state.context?.student;
  const submitButton = document.querySelector("#note-save");

  if (!student) {
    toast("仅学生身份可创建笔记，请先绑定学号", "warn");
    return;
  }
  if (!values.title || !values.content) {
    toast("请填写标题与内容", "warn");
    return;
  }
  if (!state.editingId && !values.courseId) {
    toast("请选择关联课程", "warn");
    return;
  }

  submitButton.disabled = true;
  try {
    if (state.editingId) {
      await apiPut(`/notes/update/${state.editingId}`, {
        title: values.title,
        content: values.content,
        isPublic: form.querySelector("#modal-note-public").checked
      });
      toast("笔记已更新");
    } else {
      await apiPost("/notes/create", {
        title: values.title,
        content: values.content,
        isPublic: form.querySelector("#modal-note-public").checked,
        student: { id: student.id },
        course: { id: Number(values.courseId) }
      });
      toast("笔记已创建");
    }
    closeModal();
    state.page = 0;
    await renderList();
  } catch (error) {
    toast(error.message || "保存失败", "error");
  } finally {
    submitButton.disabled = false;
  }
}

/** 删除笔记 */
async function deleteNote(noteId) {
  if (!window.confirm("删除后不可恢复，确认删除这条笔记？")) return;
  try {
    await apiDelete(`/notes/${noteId}`, { studentId: state.context.student.id });
    toast("笔记已删除");
    await renderList();
  } catch (error) {
    toast(error.message || "删除失败", "error");
  }
}

/** 点赞 / 取消点赞 */
async function likeNote(noteId, button) {
  if (!state.context?.student) {
    toast("请先绑定学生身份", "warn");
    return;
  }
  button.disabled = true;
  try {
    const liked = await apiPost(`/notes/${noteId}/like`, null, {
      studentId: state.context.student.id
    });
    toast(liked ? "点赞成功" : "已取消点赞");
    await renderList();
  } catch (error) {
    button.disabled = false;
    toast(error.message || "操作失败", "error");
  }
}

/** 绑定列表事件（事件委托） */
function bindListEvents() {
  document.querySelector("#note-list").addEventListener("click", (event) => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const card = button.closest(".note-card");
    const noteId = Number(card?.dataset.noteId);
    const action = button.dataset.action;

    if (action === "toggle-content") {
      const full = card.querySelector('[data-role="full-content"]');
      const expanded = !full.classList.contains("hidden");
      full.classList.toggle("hidden", expanded);
      card.querySelector(".note-card__content").classList.toggle("hidden", !expanded);
      button.textContent = expanded ? "展开全文" : "收起";
      return;
    }
    if (action === "like") {
      likeNote(Number(button.dataset.noteId) || noteId, button);
      return;
    }
    if (action === "delete") {
      deleteNote(noteId);
      return;
    }
    if (action === "edit") {
      const note = state.currentNotes?.find((item) => Number(item.id) === noteId);
      if (note) openModal(note);
    }
  });
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.NOTE });
  if (!state.context.user) return;

  // 支持从其他页面携带 noteId 直接打开编辑
  const noteId = Number(getParam("noteId"));

  document.querySelector("#new-note-btn")?.addEventListener("click", () => openModal());
  document.querySelector("#note-modal-close")?.addEventListener("click", closeModal);
  document.querySelector("#note-modal-cancel")?.addEventListener("click", closeModal);
  document.querySelector("#note-form")?.addEventListener("submit", saveNote);
  document.querySelector("#note-modal")?.addEventListener("click", (event) => {
    if (event.target.id === "note-modal") closeModal();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !document.querySelector("#note-modal").hidden) closeModal();
  });

  document.querySelector("#note-filter")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    state.keyword = (data.get("keyword") ?? "").toString().trim();
    state.scope = data.get("scope") === "mine" ? "mine" : data.get("scope") === "popular" ? "popular" : "public";
    // 我的笔记模式下关键词用于前端过滤，其他模式走后端搜索
    state.page = 0;
    renderList();
  });

  document.querySelector("#note-reset")?.addEventListener("click", () => {
    window.setTimeout(() => {
      state.keyword = "";
      state.scope = "mine";
      document.querySelector("#note-scope").value = "mine";
      state.page = 0;
      renderList();
    }, 0);
  });

  bindListEvents();
  await loadCourseOptions();
  await renderList();

  if (noteId) {
    try {
      const note = await apiGet(`/notes/${noteId}`);
      if (note) openModal(note);
    } catch (error) {
      toast(error.message || "笔记不存在", "error");
    }
  }
}

document.addEventListener("DOMContentLoaded", main);
