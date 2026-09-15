/**
 * 发布信息（机构）（三期）
 * 接口：
 *   POST   /api/organization/add       新增机构资料
 *   PUT    /api/organization/{id}      更新机构资料
 *   GET    /api/organization/search    查询机构列表（按名称模糊匹配）
 *   GET    /api/organization/{id}      读取单个机构详情
 *   DELETE /api/organization/{id}      删除机构资料
 * 说明：当前登录用户的机构归属由教师信息的 organization 字段（院系/单位）表示，
 *      本页据此匹配已发布机构，匹配不到则可新建。
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  apiPut,
  apiDelete,
  pageContent,
  escapeHtml,
  formatDateTime,
  toast,
  readForm,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  orgId: null,
  organizations: []
};

/** 渲染已发布机构列表 */
function renderList() {
  const tbody = document.querySelector("#org-body");
  document.querySelector("#org-count").textContent = `${state.organizations.length} 个机构`;

  if (!state.organizations.length) {
    tbody.innerHTML = '<tr><td class="is-wrap text-muted" colspan="4">暂无机构资料，填写上方表单发布第一个机构。</td></tr>';
    return;
  }

  tbody.innerHTML = state.organizations
    .map(
      (org) => `
      <tr>
        <td class="is-wrap">${escapeHtml(org.name ?? "未命名机构")}</td>
        <td class="is-wrap">${escapeHtml(String(org.info ?? "—").slice(0, 60))}</td>
        <td>${escapeHtml(formatDateTime(org.updatedTime ?? org.createdTime))}</td>
        <td>
          <span class="row-ops">
            <button type="button" data-action="edit" data-org-id="${escapeHtml(org.id ?? "")}">载入编辑</button>
            <button class="is-danger" type="button" data-action="delete" data-org-id="${escapeHtml(
              org.id ?? ""
            )}">删除</button>
          </span>
        </td>
      </tr>`
    )
    .join("");
}

/** 渲染预览区 */
function renderPreview() {
  const form = document.querySelector("#org-form");
  const name = form.querySelector("#org-name").value.trim() || "机构名称";
  const info = form.querySelector("#org-info").value.trim() || "机构简介将显示在这里。";
  const logo = form.querySelector("#org-logo").value.trim() || "images/default.png";
  const banner = form.querySelector("#org-banner").value.trim() || "images/main-background.jpg";

  document.querySelector("#preview-name").textContent = name;
  document.querySelector("#preview-info").textContent = info;
  document.querySelector("#preview-logo").src = logo;
  document.querySelector("#preview-banner").src = banner;
}

/** 表单回填 */
function fillForm(org) {
  const form = document.querySelector("#org-form");
  form.querySelector("#org-name").value = org?.name ?? "";
  form.querySelector("#org-logo").value = org?.logo ?? "";
  form.querySelector("#org-banner").value = org?.bannerUrl ?? "";
  form.querySelector("#org-honor").value = org?.honorCertUrl ?? "";
  form.querySelector("#org-info").value = org?.info ?? "";
  state.orgId = org?.id ?? null;
  document.querySelector("#publish-mode").textContent = org
    ? `正在编辑已发布机构：${org.name}`
    : "尚未发布机构资料，填写后点击“发布信息”即可创建。";
  renderPreview();
}

/** 加载机构列表与当前机构 */
async function loadOrganizations() {
  const { teacher } = state.context;
  const pageData = await apiGet("/organization/search", { page: 0, size: 50 });
  state.organizations = pageContent(pageData);
  renderList();

  const ownName = teacher?.organization ?? "";
  if (ownName) {
    const matched = state.organizations.find((org) => org.name === ownName);
    if (matched) {
      fillForm(matched);
      return;
    }
    const form = document.querySelector("#org-form");
    form.querySelector("#org-name").value = ownName;
  }
  fillForm(null);
}

/** 提交机构资料 */
async function submitForm(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.querySelector("#org-message");
  const submitButton = document.querySelector("#org-submit");
  const values = readForm(form);

  const showMessage = (text, isError) => {
    message.hidden = false;
    message.classList.toggle("is-error", Boolean(isError));
    message.textContent = text;
  };

  if (!values.name) {
    showMessage("机构名称为必填项。", true);
    return;
  }

  const payload = {
    name: values.name,
    logo: values.logo || null,
    bannerUrl: values.bannerUrl || null,
    honorCertUrl: values.honorCertUrl || null,
    info: values.info || null
  };

  submitButton.disabled = true;
  try {
    if (state.orgId) {
      const updated = await apiPut(`/organization/${state.orgId}`, payload);
      showMessage("机构信息已更新。", false);
      state.organizations = state.organizations.map((org) =>
        String(org.id) === String(state.orgId) ? updated : org
      );
      fillForm(updated);
    } else {
      const created = await apiPost("/organization/add", payload);
      showMessage("机构信息发布成功。", false);
      state.organizations = [created, ...state.organizations];
      fillForm(created);
    }
    renderList();
    toast("发布成功");
  } catch (error) {
    showMessage(error.message || "发布失败，请稍后重试。", true);
    toast(error.message || "发布失败", "error");
  } finally {
    submitButton.disabled = false;
  }
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  state.context = await initPageShell({ active: PAGE_KEY.PUBLISH_INFO });
  if (!state.context.user) return;

  const form = document.querySelector("#org-form");
  form?.addEventListener("submit", submitForm);
  form?.addEventListener("input", renderPreview);
  document.querySelector("#org-clear")?.addEventListener("click", () => {
    window.setTimeout(() => {
      state.orgId = null;
      renderPreview();
    }, 0);
  });

  document.querySelector("#org-body")?.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const orgId = button.dataset.orgId;

    if (button.dataset.action === "edit") {
      try {
        const org = await apiGet(`/organization/${orgId}`);
        fillForm(org);
        window.scrollTo({ top: 0, behavior: "smooth" });
      } catch (error) {
        toast(error.message || "机构信息加载失败", "error");
      }
      return;
    }

    if (button.dataset.action === "delete") {
      if (!window.confirm("删除后机构主页信息将被移除，确认删除？")) return;
      try {
        await apiDelete(`/organization/${orgId}`);
        state.organizations = state.organizations.filter((org) => String(org.id) !== String(orgId));
        if (String(state.orgId) === String(orgId)) fillForm(null);
        renderList();
        toast("机构已删除");
      } catch (error) {
        toast(error.message || "删除失败", "error");
      }
    }
  });

  await loadOrganizations().catch((error) => {
    console.error("机构资料加载失败", error);
    document.querySelector("#publish-mode").textContent = "机构资料加载失败，请稍后刷新重试。";
  });
}

document.addEventListener("DOMContentLoaded", main);
