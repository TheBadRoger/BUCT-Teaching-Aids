/**
 * 个人中心
 * 接口：
 *   GET  /api/user/auth/current     当前登录用户
 *   GET  /api/user/binding/info     身份绑定信息
 *   POST /api/user/auth/logout      退出登录
 *   POST /api/user/binding/unbind   解除身份绑定
 *   GET  /api/student-courses/...   学习情况统计
 *   GET  /api/notes/student/{id}    笔记数量
 * 说明：后端目前未提供资料修改与密码修改接口，因此本页资料字段为只读展示，
 *      避免出现无后端的假保存按钮。
 */
import {
  initPageShell,
  apiGet,
  apiPost,
  toast,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  binding: null
};

/** 渲染头部资料概览 */
function renderHero() {
  const { user, role, student, teacher } = state.context;
  const displayName = student?.name ?? teacher?.name ?? user?.username ?? "未登录用户";
  document.querySelector("#profile-hero-heading").textContent = displayName;
  document.querySelector("#profile-hero-desc").textContent =
    role === "teacher"
      ? `${teacher?.organization ?? "暂无单位信息"} · 教师账号`
      : role === "student"
        ? `${student?.className ?? "暂无班级信息"} · 学生账号`
        : "当前账号尚未绑定学生或教师身份";

  const roleTag = document.querySelector("#profile-role-tag");
  roleTag.textContent = role === "teacher" ? "教师" : role === "student" ? "学生" : "未绑定身份";
  roleTag.className = `tag${role === "teacher" ? " tag--success" : role === "student" ? " tag--primary" : " tag--warning"}`;

  document.querySelector("#profile-id-tag").textContent =
    role === "student"
      ? `学号 ${student?.studentNumber ?? "—"}`
      : role === "teacher"
        ? `单位 ${teacher?.organization ?? "—"}`
        : "待认证";

  document.querySelector("#profile-avatar").src = user?.avatar ?? "images/default.png";
  document.querySelector("#apply-entry").textContent =
    role === "teacher" ? "查看教师身份" : "申请成为老师";
}

/** 渲染资料字段 */
function renderProfileFields() {
  const { user, role, student, teacher } = state.context;
  document.querySelector("#profile-username").value = user?.username ?? "—";
  document.querySelector("#profile-type").value =
    role === "teacher" ? "教师账号（TEACHER）" : role === "student" ? "学生账号（STUDENT）" : "普通账号（未绑定）";
  document.querySelector("#profile-name").value = student?.name ?? teacher?.name ?? "—";
  document.querySelector("#profile-gender").value = student?.gender ?? teacher?.gender ?? "—";
  document.querySelector("#profile-class").value = student?.className ?? teacher?.organization ?? "—";
  document.querySelector("#profile-extra").value = student?.studentNumber ?? "—";
  document.querySelector("#profile-join").value = student?.admissionDate ?? teacher?.jointime ?? "—";
  document.querySelector("#profile-telephone").value = user?.telephone ?? "—";
  document.querySelector("#profile-email").value = user?.email ?? "—";
}

/** 渲染学习情况统计 */
async function renderLearning() {
  const { role, student, teacher } = state.context;
  const setValue = (id, value) => {
    document.querySelector(id).textContent = String(value);
  };

  if (role === "teacher" && teacher) {
    const [coursePage, notePage] = await Promise.all([
      apiGet("/course/search", { teachingTeachers: teacher.name, page: 0, size: 1 }).catch(() => null),
      apiGet("/teacher/search", { name: teacher.name, page: 0, size: 1 }).catch(() => null)
    ]);
    const total = Number(coursePage?.totalElements) || 0;
    setValue("#learning-course", total);
    setValue("#learning-viewed", total);
    setValue("#learning-note", Number(notePage?.totalElements) || 0);
    setValue("#learning-question", "—");
    return;
  }

  if (!student) {
    ["#learning-course", "#learning-viewed", "#learning-note", "#learning-question"].forEach((id) =>
      setValue(id, "—")
    );
    return;
  }

  const [allPage, viewedPage, notePage] = await Promise.all([
    apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 1 }).catch(() => null),
    apiGet("/student-courses/viewed-courses", { studentId: student.id, page: 0, size: 1 }).catch(() => null),
    apiGet(`/notes/student/${student.id}`, { page: 0, size: 1 }).catch(() => null)
  ]);

  setValue("#learning-course", Number(allPage?.totalElements) || 0);
  setValue("#learning-viewed", Number(viewedPage?.totalElements) || 0);
  const noteTotal = Number(notePage?.totalElements) || 0;
  setValue("#learning-note", noteTotal);
  setValue("#learning-question", noteTotal);
}

/** 渲染账号与安全面板 */
function renderAccount() {
  const { role, student, teacher } = state.context;
  const host = document.querySelector("#account-binding");
  if (role === "teacher") {
    host.textContent = `已绑定教师身份：${teacher?.name ?? "—"}（${teacher?.organization ?? "暂无单位"}）`;
  } else if (role === "student") {
    host.textContent = `已绑定学生身份：${student?.name ?? "—"}（学号 ${student?.studentNumber ?? "—"}）`;
  } else {
    host.textContent = "当前账号尚未绑定身份，可申请成为老师或联系教务绑定学号。";
  }
  document.querySelector("#unbind-btn").disabled = role === "guest";
}

/** 退出登录 */
async function logout() {
  try {
    await apiPost("/user/auth/logout");
  } catch {
    /* 服务端会话可能已过期，忽略异常继续清理本地状态 */
  }
  window.location.href = "enter.html";
}

/** 解除身份绑定 */
async function unbind() {
  if (!window.confirm("解除绑定后将失去与该身份关联的功能，确认继续？")) return;
  try {
    await apiPost("/user/binding/unbind");
    toast("已解除身份绑定");
    window.setTimeout(() => window.location.reload(), 600);
  } catch (error) {
    toast(error.message || "解除绑定失败", "error");
  }
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  state.context = await initPageShell({ active: PAGE_KEY.PERSONAL_CENTER });
  if (!state.context.user) return;

  renderHero();
  renderProfileFields();
  renderAccount();
  await renderLearning().catch((error) => console.warn("学习统计加载失败", error));

  document.querySelector("#profile-tabs")?.addEventListener("click", (event) => {
    const tab = event.target.closest("[data-panel]");
    if (!tab) return;
    document.querySelectorAll("#profile-tabs .tabs__item").forEach((item) => {
      const active = item === tab;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-selected", String(active));
    });
    document.querySelectorAll("[data-panel-body]").forEach((panel) => {
      panel.classList.toggle("hidden", panel.dataset.panelBody !== tab.dataset.panel);
    });
  });

  document.querySelector("#logout-btn")?.addEventListener("click", logout);
  document.querySelector("#logout-btn-2")?.addEventListener("click", logout);
  document.querySelector("#unbind-btn")?.addEventListener("click", unbind);
}

document.addEventListener("DOMContentLoaded", main);
