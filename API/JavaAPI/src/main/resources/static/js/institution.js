/**
 * 机构专区
 * 接口：
 *   GET /api/organization/search             分页查询机构（按名称模糊搜索）
 *   GET /api/organization/{id}              机构详情
 *   GET /api/teacher/search?organization=   机构的师资列表
 *   GET /api/course/search?teachingTeachers= 按教师姓名检索机构开设的课程
 * 说明：后端 Course 实体未直接关联机构，课程归属通过“授课教师 → 所属院系”间接匹配。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  courseCardHtml,
  renderPagination,
  renderEmpty,
  renderError,
  renderLoading,
  escapeHtml,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const PAGE_SIZE = 12;

const state = {
  context: null,
  keyword: "",
  page: 0,
  totalPages: 0,
  totalElements: 0,
  activeOrg: null
};

/** 加载机构列表 */
async function loadOrganizations() {
  const grid = document.querySelector("#org-grid");
  renderLoading(grid, 3);

  try {
    const pageData = await apiGet("/organization/search", {
      name: state.keyword,
      page: state.page,
      size: PAGE_SIZE
    });
    const organizations = pageContent(pageData);
    state.totalPages = Number(pageData?.totalPages) || 0;
    state.totalElements = Number(pageData?.totalElements) || organizations.length;

    document.querySelector("#org-summary").textContent = state.totalElements
      ? `共收录 ${state.totalElements} 个机构`
      : "没有找到匹配的机构";

    if (!organizations.length) {
      renderEmpty(
        grid,
        "暂无机构资料",
        "机构账号可在“发布信息”页发布单位主页资料，发布后即可在此展示。"
      );
      renderPagination(document.querySelector("#org-pagination"), { page: 0, totalPages: 0 }, () => {});
      return;
    }

    grid.innerHTML = organizations.map((org) => orgCardHtml(org)).join("");
    renderPagination(
      document.querySelector("#org-pagination"),
      { page: state.page, totalPages: state.totalPages, totalElements: state.totalElements },
      (nextPage) => {
        state.page = nextPage;
        loadOrganizations();
      }
    );
  } catch (error) {
    console.error("机构列表加载失败", error);
    renderError(grid, error, loadOrganizations);
  }
}

/** 机构卡片 */
function orgCardHtml(org) {
  return `
    <article class="org-card">
      <img class="org-card__banner" src="${escapeHtml(
        org.bannerUrl || "images/main-background.jpg"
      )}" alt="${escapeHtml(org.name ?? "机构")} 的横幅" loading="lazy">
      <div class="org-card__body">
        <div class="org-card__head">
          <img class="org-card__logo" src="${escapeHtml(
            org.logo || "images/default.png"
          )}" alt="${escapeHtml(org.name ?? "机构")} 的标识" loading="lazy">
          <p class="org-card__name">${escapeHtml(org.name ?? "未命名机构")}</p>
        </div>
        <p class="org-card__info">${escapeHtml(org.info || "该机构暂未填写简介。")}</p>
        <div class="org-card__foot">
          <span class="text-muted">${org.honorCertUrl ? "已上传荣誉证书" : "暂无荣誉证书"}</span>
          <button class="btn btn--sm btn--primary" type="button" data-org-id="${escapeHtml(
            org.id ?? ""
          )}">查看详情</button>
        </div>
      </div>
    </article>`;
}

/** 打开机构详情面板 */
async function openDetail(orgId) {
  const panel = document.querySelector("#org-detail-panel");
  const body = document.querySelector("#org-detail-body");
  panel.hidden = false;
  body.innerHTML = '<div class="skeleton skeleton--block"></div>';

  try {
    const org = await apiGet(`/organization/${orgId}`);
    state.activeOrg = org;
    document.querySelector("#org-detail-heading").textContent = `${org.name ?? "机构"} · 详情`;

    // 并行加载机构师资与课程
    const teacherPage = await apiGet("/teacher/search", {
      organization: org.name ?? "",
      page: 0,
      size: 20
    }).catch(() => null);
    const teachers = pageContent(teacherPage);

    const courseResults = await Promise.all(
      teachers.slice(0, 8).map((teacher) =>
        apiGet("/course/search", {
          teachingTeachers: teacher.name ?? "",
          page: 0,
          size: 6
        }).catch(() => null)
      )
    );
    const courseMap = new Map();
    courseResults.forEach((pageData) => {
      pageContent(pageData).forEach((course) => courseMap.set(String(course.id), course));
    });
    const courses = [...courseMap.values()].slice(0, 8);

    body.innerHTML = `
      <div class="org-detail__head">
        <img class="org-detail__logo" src="${escapeHtml(
          org.logo || "images/default.png"
        )}" alt="${escapeHtml(org.name ?? "机构")} 的标识">
        <div>
          <p class="org-detail__name">${escapeHtml(org.name ?? "未命名机构")}</p>
          <p class="org-detail__meta">
            <span>收录教师 ${Number(teacherPage?.totalElements) || teachers.length} 位</span>
            <span>关联课程 ${courses.length} 门</span>
            ${org.honorCertUrl ? '<span>已上传荣誉证书</span>' : ""}
          </p>
        </div>
        ${
          org.honorCertUrl
            ? `<a class="btn btn--sm" href="${escapeHtml(org.honorCertUrl)}" target="_blank"
                  rel="noopener noreferrer">查看荣誉证书</a>`
            : ""
        }
      </div>
      <p class="rich-text mb-lg">${escapeHtml(org.info || "该机构暂未填写简介。")}</p>

      <h3 class="card__title mb-md">机构师资</h3>
      <ul class="list-plain mb-lg">
        ${
          teachers.length
            ? teachers
                .slice(0, 12)
                .map(
                  (teacher) => `
          <li class="flex-between">
            <span>${escapeHtml(teacher.name ?? "未知教师")} · ${escapeHtml(
              teacher.education ?? "学历未填写"
            )}</span>
            <a class="btn btn--sm" href="teacher-detail.html?id=${encodeURIComponent(
              teacher.id ?? ""
            )}">教师主页</a>
          </li>`
                )
                .join("")
            : '<li class="text-muted">该机构暂未收录教师信息。</li>'
        }
      </ul>

      <h3 class="card__title mb-md">机构课程</h3>
      <div class="grid-cards">
        ${
          courses.length
            ? courses.map((course) => courseCardHtml(course, { showStatus: true })).join("")
            : '<p class="text-muted">暂未检索到该机构的课程。</p>'
        }
      </div>`;

    panel.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    console.error("机构详情加载失败", error);
    body.innerHTML = `<p class="text-danger">${escapeHtml(error.message || "机构详情加载失败")}</p>`;
  }
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  state.context = await initPageShell({ active: PAGE_KEY.INSTITUTION, requireLogin: false });

  document.querySelector("#org-search")?.addEventListener("submit", (event) => {
    event.preventDefault();
    state.keyword = (new FormData(event.currentTarget).get("name") ?? "").toString().trim();
    state.page = 0;
    loadOrganizations();
  });

  document.querySelector("#org-grid")?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-org-id]");
    if (button) openDetail(button.dataset.orgId);
  });

  document.querySelector("#close-detail")?.addEventListener("click", () => {
    document.querySelector("#org-detail-panel").hidden = true;
    state.activeOrg = null;
  });

  await loadOrganizations();
}

document.addEventListener("DOMContentLoaded", main);
