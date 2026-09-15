/**
 * 好友动态
 * 接口：
 *   GET    /api/notes/public                  公开笔记动态流
 *   GET    /api/notes/popular                 热门笔记
 *   GET    /api/notes/search?keyword=         按关键词搜索公开笔记
 *   POST   /api/notes/{noteId}/like           点赞 / 取消点赞
 *   GET    /api/notes/{noteId}/liked          查询点赞状态
 *   GET    /api/comments/note/{noteId}        读取笔记评论（含回复）
 *   POST   /api/comments/add                  发表评论
 *   POST   /api/comments/reply/{parentId}     回复评论
 * 说明：后端暂无“关注关系”接口，动态流以公开笔记作为同学动态来源。
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  pageContent,
  renderPagination,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  fromNow,
  toast,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 10;

const state = {
  context: null,
  mode: "latest",
  keyword: "",
  page: 0,
  totalPages: 0,
  totalElements: 0,
  notes: [],
  likedIds: new Set()
};

/** 按当前模式请求动态数据 */
async function fetchFeed() {
  if (state.mode === "popular") {
    return apiGet("/notes/popular", { page: state.page, size: PAGE_SIZE });
  }
  if (state.keyword) {
    return apiGet("/notes/search", { keyword: state.keyword, page: state.page, size: PAGE_SIZE });
  }
  return apiGet("/notes/public", { page: state.page, size: PAGE_SIZE });
}

/** 渲染动态流 */
async function renderFeed() {
  const host = document.querySelector("#activity-feed");
  renderLoading(host, 3);

  try {
    const pageData = await fetchFeed();
    const notes = pageContent(pageData);
    state.notes = notes;
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || notes.length;

    document.querySelector("#activity-summary").textContent = state.totalElements
      ? `共 ${state.totalElements} 条同学动态`
      : "暂时没有同学公开笔记";

    if (!notes.length) {
      renderEmpty(host, "暂无动态", "还没有同学公开笔记，去写一篇并设为公开吧。");
      renderPagination(document.querySelector("#activity-pagination"), { page: 0, totalPages: 0 }, () => {});
      return;
    }

    host.innerHTML = notes.map((note) => activityCardHtml(note)).join("");

    // 并行查询本人点赞状态
    const studentId = state.context?.student?.id;
    if (studentId) {
      const results = await Promise.all(
        notes.map((note) =>
          apiGet(`/notes/${note.id}/liked`, { studentId }).catch(() => false)
        )
      );
      results.forEach((liked, index) => {
        if (!liked) return;
        state.likedIds.add(String(notes[index].id));
        const button = host.querySelector(
          `[data-note-id="${notes[index].id}"] [data-action="like"]`
        );
        button?.classList.add("is-liked");
        button && (button.textContent = "已赞");
      });
    }

    renderPagination(
      document.querySelector("#activity-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        renderFeed();
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    );
  } catch (error) {
    console.error("好友动态加载失败", error);
    renderError(host, error, renderFeed);
  }
}

/** 生成单条动态卡片 */
function activityCardHtml(note) {
  return `
    <article class="activity-card" data-note-id="${escapeHtml(note.id ?? "")}">
      <header class="activity-card__head">
        <img class="activity-card__avatar" src="images/default.png"
             alt="${escapeHtml(note.student?.name ?? "同学")} 的头像">
        <div class="flex-1">
          <p class="activity-card__author">${escapeHtml(note.student?.name ?? "匿名同学")}</p>
          <p class="activity-card__time">
            ${escapeHtml(note.course?.courseName ?? "未关联课程")} · ${escapeHtml(fromNow(note.createdAt))}
          </p>
        </div>
        <span class="tag${note.isPublic ? " tag--success" : ""}">${note.isPublic ? "公开" : "私有"}</span>
      </header>
      <h2 class="activity-card__title">${escapeHtml(note.title ?? "无标题笔记")}</h2>
      <p class="activity-card__content">${escapeHtml(String(note.content ?? "").slice(0, 320))}${
        String(note.content ?? "").length > 320 ? "…" : ""
      }</p>
      <div class="activity-card__ops">
        <button class="btn btn--sm" type="button" data-action="like">点赞 ${
          Number(note.likeCount) || 0
        }</button>
        <button class="btn btn--sm" type="button" data-action="toggle-comment">评论 ${
          Number(note.commentCount) || 0
        }</button>
        <a class="btn btn--sm" href="course-info.html?courseNumber=${encodeURIComponent(
          note.course?.courseNumber ?? ""
        )}">查看课程</a>
      </div>
      <div class="comment-panel hidden" data-role="comment-panel"></div>
    </article>`;
}

/** 加载并渲染某条笔记的评论区 */
async function loadComments(card) {
  const noteId = card?.dataset.noteId;
  const panel = card?.querySelector('[data-role="comment-panel"]');
  if (!noteId || !panel) return;

  panel.innerHTML = '<p class="text-muted">评论加载中…</p>';
  try {
    const comments = await apiGet(`/comments/note/${noteId}`);
    const list = Array.isArray(comments) ? comments : [];
    const topLevel = list.filter((item) => !item.parentComment);
    const repliesOf = (parentId) =>
      list.filter((item) => Number(item.parentComment?.id) === Number(parentId));

    panel.innerHTML = `
      <div class="comment-list">
        ${
          topLevel.length
            ? topLevel
                .map(
                  (comment) => `
          <div class="comment-item">
            <p class="comment-item__meta">${escapeHtml(comment.student?.name ?? "匿名")} · ${escapeHtml(
              fromNow(comment.createdAt)
            )}</p>
            <p>${escapeHtml(comment.content ?? "")}</p>
            ${
              repliesOf(comment.id).length
                ? repliesOf(comment.id)
                    .map(
                      (reply) => `
              <div class="comment-reply">
                <p class="comment-item__meta">${escapeHtml(reply.student?.name ?? "匿名")} 回复 · ${escapeHtml(
                  fromNow(reply.createdAt)
                )}</p>
                <p>${escapeHtml(reply.content ?? "")}</p>
              </div>`
                    )
                    .join("")
                : ""
            }
            <button class="btn btn--sm btn--ghost mt-sm" type="button" data-action="reply"
                    data-parent-id="${escapeHtml(comment.id ?? "")}">回复</button>
          </div>`
                )
                .join("")
            : '<p class="text-muted">还没有评论，来说两句吧。</p>'
        }
      </div>
      <form class="comment-form" data-role="comment-form">
        <label class="sr-only" for="comment-${escapeHtml(noteId)}">发表评论</label>
        <textarea class="form-control" id="comment-${escapeHtml(noteId)}" name="content"
                  placeholder="友善交流，写下你的想法…" required></textarea>
        <p class="form-group__hint hidden" data-role="reply-hint"></p>
        <div class="flex gap-sm">
          <button class="btn btn--primary btn--sm" type="submit">发表评论</button>
          <button class="btn btn--sm hidden" type="button" data-action="cancel-reply">取消回复</button>
        </div>
      </form>`;
  } catch (error) {
    console.warn("评论加载失败", error);
    panel.innerHTML = '<p class="text-muted">评论暂时无法加载。</p>';
  }
}

/** 点赞切换 */
async function toggleLike(card, button) {
  const noteId = card?.dataset.noteId;
  const studentId = state.context?.student?.id;
  if (!studentId) {
    toast("请先绑定学生身份后再点赞", "warn");
    return;
  }
  button.disabled = true;
  try {
    const liked = await apiPost(`/notes/${noteId}/like`, null, { studentId });
    state.likedIds[liked ? "add" : "delete"](String(noteId));
    button.textContent = liked
      ? "已赞"
      : `点赞 ${Number(state.notes.find((note) => String(note.id) === String(noteId))?.likeCount) || 0}`;
    button.classList.toggle("is-liked", Boolean(liked));
    toast(liked ? "点赞成功" : "已取消点赞");
  } catch (error) {
    toast(error.message || "操作失败", "error");
  } finally {
    button.disabled = false;
  }
}

/** 发表评论或回复 */
async function submitComment(card, form, parentId) {
  const noteId = card?.dataset.noteId;
  const studentId = state.context?.student?.id;
  const content = (new FormData(form).get("content") ?? "").toString().trim();

  if (!studentId) {
    toast("请先绑定学生身份后再评论", "warn");
    return;
  }
  if (!content) {
    toast("评论内容不能为空", "warn");
    return;
  }

  const payload = { content, note: { id: Number(noteId) }, student: { id: studentId } };
  try {
    if (parentId) {
      await apiPost(`/comments/reply/${parentId}`, payload);
    } else {
      await apiPost("/comments/add", payload);
    }
    toast("评论已发表");
    await loadComments(card);
  } catch (error) {
    toast(error.message || "评论失败", "error");
  }
}

/** 绑定动态流事件（事件委托） */
function bindFeedEvents() {
  const host = document.querySelector("#activity-feed");
  let replyParentId = null;

  host.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const card = button.closest(".activity-card");
    const action = button.dataset.action;

    if (action === "like") {
      toggleLike(card, button);
      return;
    }
    if (action === "toggle-comment") {
      const panel = card.querySelector('[data-role="comment-panel"]');
      const opening = panel.classList.contains("hidden");
      panel.classList.toggle("hidden", !opening);
      if (opening) await loadComments(card);
      return;
    }
    if (action === "reply") {
      replyParentId = button.dataset.parentId;
      const hint = card.querySelector('[data-role="reply-hint"]');
      const cancel = card.querySelector('[data-action="cancel-reply"]');
      hint.textContent = "正在回复该评论，发表后将作为回复展示。";
      hint.classList.remove("hidden");
      cancel.classList.remove("hidden");
      card.querySelector('[data-role="comment-form"] textarea')?.focus();
    }
    if (action === "cancel-reply") {
      replyParentId = null;
      const hint = card.querySelector('[data-role="reply-hint"]');
      hint.classList.add("hidden");
      button.classList.add("hidden");
    }
  });

  host.addEventListener("submit", (event) => {
    const form = event.target.closest('[data-role="comment-form"]');
    if (!form) return;
    event.preventDefault();
    const card = form.closest(".activity-card");
    submitComment(card, form, replyParentId).then(() => {
      replyParentId = null;
    });
  });
}

/** 页面初始化 */
async function main() {
  state.context = await initPageShell({ active: PAGE_KEY.FRIEND_ACTIVITY });
  if (!state.context.user) return;

  bindFeedEvents();

  document.querySelector("#activity-filter")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    state.mode = (data.get("mode") ?? "latest").toString();
    state.keyword = (data.get("keyword") ?? "").toString().trim();
    state.page = 0;
    renderFeed();
  });

  await renderFeed();
}

document.addEventListener("DOMContentLoaded", main);
