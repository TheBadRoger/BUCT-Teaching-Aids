/**
 * 申请成为老师页面
 * 接口：
 *   GET  /api/user/binding/info    读取当前身份绑定情况
 *   POST /api/user/binding/bind    提交教师身份绑定（实名认证）
 *   POST /api/user/binding/unbind  解除当前绑定
 * 说明：后端通过姓名 + 身份证号 + 工号进行实名认证，认证通过后 userType 变为 TEACHER。
 */
import {
  initPageShell,
  apiPost,
  getBindingInfo,
  escapeHtml,
  toast,
  readForm,
  PAGE_KEY
} from "./frontend-common.js";

/** 业务状态码与提示文案映射 */
const ERROR_MESSAGE = {
  4013: "登录已过期，请重新登录后再提交申请。",
  4101: "实名认证失败，请确认姓名、身份证号与工号是否与教务登记一致。",
  4095: "该账号已绑定身份，请先在个人中心解除绑定。",
  4096: "该工号已被其他账号绑定，请联系教务管理员处理。",
  4098: "绑定类型与已有身份冲突，请先解除原有身份。",
  4001: "请补全所有必填项后再提交。"
};

/** 渲染当前身份面板 */
function renderIdentity(binding) {
  const host = document.querySelector("#identity-panel");
  const hint = document.querySelector("#identity-hint");
  const userType = binding?.userType;

  if (userType === "TEACHER") {
    const teacher = binding?.teacherInfo ?? {};
    hint.textContent = "已认证为教师";
    host.innerHTML = `
      <div class="identity-card">
        <img class="identity-card__avatar" src="images/default.png" alt="">
        <div>
          <p class="identity-card__name">${escapeHtml(teacher.name ?? "教师")}</p>
          <p class="identity-card__meta">${escapeHtml(teacher.organization ?? "暂无单位信息")}</p>
        </div>
        <span class="tag tag--success">教师身份已生效</span>
      </div>
      <dl class="identity-list">
        <div><dt>性别</dt><dd>${escapeHtml(teacher.gender ?? "—")}</dd></div>
        <div><dt>学历</dt><dd>${escapeHtml(teacher.education ?? "—")}</dd></div>
        <div><dt>入职时间</dt><dd>${escapeHtml(teacher.jointime ?? "—")}</dd></div>
      </dl>
      <div class="form-actions">
        <a class="btn btn--primary" href="my-course.html">前往我开的课</a>
        <button class="btn btn--danger" id="unbind-btn" type="button">解除教师绑定</button>
      </div>`;
    document.querySelector("#unbind-btn")?.addEventListener("click", handleUnbind);
  } else if (userType === "STUDENT") {
    const student = binding?.studentInfo ?? {};
    hint.textContent = "当前为学生身份";
    host.innerHTML = `
      <div class="identity-card">
        <img class="identity-card__avatar" src="images/default.png" alt="">
        <div>
          <p class="identity-card__name">${escapeHtml(student.name ?? "学生")}</p>
          <p class="identity-card__meta">${escapeHtml(student.className ?? "暂无班级信息")} · 学号 ${escapeHtml(
            student.studentNumber ?? "—"
          )}</p>
        </div>
        <span class="tag tag--primary">学生身份</span>
      </div>
      <p class="form-group__hint mt-md">
        当前账号已绑定学生身份，需先解除绑定后才能申请教师身份。
      </p>
      <div class="form-actions">
        <button class="btn btn--danger" id="unbind-btn" type="button">解除学生绑定</button>
      </div>`;
    document.querySelector("#unbind-btn")?.addEventListener("click", handleUnbind);
  } else {
    hint.textContent = "尚未绑定身份";
    host.innerHTML = `
      <div class="identity-card">
        <img class="identity-card__avatar" src="images/default.png" alt="">
        <div>
          <p class="identity-card__name">未绑定身份</p>
          <p class="identity-card__meta">绑定教师身份后即可开课、录课与管理名单。</p>
        </div>
        <span class="tag tag--warning">待认证</span>
      </div>`;
  }
}

/** 校验表单，返回错误提示或 null */
function validate(values) {
  if (!values.name) return "请填写真实姓名。";
  if (!/^\d{17}[\dXx]$/.test(values.idCard ?? "")) return "身份证号格式不正确，应为 18 位。";
  if (!values.employeeNumber) return "请填写教师工号。";
  return null;
}

/** 解除绑定 */
async function handleUnbind() {
  if (!window.confirm("解除绑定后将失去教师/学生身份相关功能，确认继续？")) return;
  try {
    await apiPost("/user/binding/unbind");
    toast("已解除身份绑定");
    window.setTimeout(() => window.location.reload(), 600);
  } catch (error) {
    toast(error.message || "解除绑定失败", "error");
  }
}

/** 提交认证申请 */
async function handleSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.querySelector("#form-message");
  const submitButton = document.querySelector("#submit-btn");
  const values = readForm(form);

  const invalid = validate(values);
  if (invalid) {
    message.hidden = false;
    message.classList.remove("is-success");
    message.textContent = invalid;
    return;
  }

  message.hidden = true;
  submitButton.disabled = true;
  submitButton.textContent = "提交中…";

  try {
    const result = await apiPost("/user/binding/bind", {
      bindingType: "TEACHER",
      name: values.name,
      idCard: values.idCard,
      employeeNumber: values.employeeNumber
    });
    message.hidden = false;
    message.classList.add("is-success");
    message.textContent = "认证通过，教师身份已生效，正在跳转…";
    toast("认证成功，欢迎加入教师团队");
    renderIdentity({
      userType: result?.userType ?? "TEACHER",
      teacherInfo: result?.teacherInfo ?? null
    });
    window.setTimeout(() => {
      window.location.href = "my-course.html";
    }, 1200);
  } catch (error) {
    message.hidden = false;
    message.classList.remove("is-success");
    message.textContent = ERROR_MESSAGE[error.code] ?? error.message ?? "提交失败，请稍后重试。";
    toast(message.textContent, "error");
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "提交认证申请";
  }
}

/** 页面初始化 */
async function main() {
  const context = await initPageShell({ active: PAGE_KEY.APPLY_TEACHER });
  if (!context.user) return;

  const binding = await getBindingInfo();
  renderIdentity(binding);

  // 已是教师身份时不再允许重复提交
  if (binding?.userType === "TEACHER") {
    document.querySelector("#apply-form").classList.add("hidden");
    document.querySelector("#form-heading").textContent = "教师身份认证（已完成）";
    return;
  }

  document.querySelector("#apply-form")?.addEventListener("submit", handleSubmit);
  document.querySelector("#reset-btn")?.addEventListener("click", () => {
    const message = document.querySelector("#form-message");
    message.hidden = true;
    message.textContent = "";
  });

  // 身份证号仅允许数字与末位 X
  document.querySelector("#apply-idcard")?.addEventListener("input", (event) => {
    event.target.value = event.target.value.replace(/[^\dXx]/g, "").toUpperCase();
  });
}

document.addEventListener("DOMContentLoaded", main);
