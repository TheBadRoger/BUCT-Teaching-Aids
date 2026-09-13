/**
 * 我已开的课 - 课程详情（二期） / 我要开课 - 上传课程（二期）
 * 页面模式：
 *   ?mode=create                       新增课程（对应“我要开课 - 上传课程”）
 *   ?courseNumber=xxx                  查看课程详情
 *   ?mode=edit&courseNumber=xxx        编辑课程（对应“我已开的课 - 课程详情”的维护入口）
 * 接口：
 *   POST /api/course/add                新增课程
 *   GET  /api/course/search             按课程编号读取课程
 *   PUT  /api/course/update?id=         更新课程
 *   GET  /api/course/view/{id}/count    课程访问量
 *   GET  /api/students/search?className= 开课班级学生数
 *   GET  /api/course/popularity/{id}/ranking  课程热度排名
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
  readForm,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  mode: "view",
  course: null
};

/** 提取表单中的课程字段 */
function collectCourseForm(form) {
  const values = readForm(form);
  return {
    courseName: values.courseName,
    courseNumber: values.courseNumber,
    courseIntroduction: values.courseIntroduction,
    startDate: values.startDate,
    teachingObjectives: values.teachingObjectives,
    duration: values.duration,
    teachingTeachers: values.teachingTeachers,
    teachingClasses: values.teachingClasses,
    targetAudience: values.targetAudience,
    classAddress: values.classAddress,
    coursePrice: values.coursePrice ? Number(values.coursePrice) : null,
    courseStatus: values.courseStatus,
    courseTags: values.courseTags,
    courseOutline: values.courseOutline,
    courseImage: values.courseImage
  };
}

/** 生成课程表单 HTML */
function courseFormHtml(course = {}) {
  const isEdit = state.mode === "edit";
  return `
    <div class="mode-banner">
      ${
        isEdit
          ? "正在编辑已有课程，保存后将同步更新学生端展示内容。"
          : "上传课程后将出现在课程大全中，可继续补充课时、录制视频并维护班级名单。"
      }
    </div>
    <form id="course-form" novalidate>
      <h2 class="card__title mb-md">基础信息</h2>
      <div class="form-row">
        <div class="form-group">
          <label class="form-group__label form-group__label--required" for="f-name">课程名称</label>
          <input class="form-control" id="f-name" name="courseName" type="text" required
                 value="${escapeHtml(course.courseName ?? "")}" placeholder="例如：高等数学（上）">
        </div>
        <div class="form-group">
          <label class="form-group__label form-group__label--required" for="f-number">课程编号</label>
          <input class="form-control" id="f-number" name="courseNumber" type="text" required
                 value="${escapeHtml(course.courseNumber ?? "")}" placeholder="唯一编号，例如 MATH101">
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-group__label" for="f-teachers">授课教师</label>
          <input class="form-control" id="f-teachers" name="teachingTeachers" type="text"
                 value="${escapeHtml(course.teachingTeachers ?? state.context?.teacher?.name ?? "")}">
        </div>
        <div class="form-group">
          <label class="form-group__label" for="f-classes">开课班级</label>
          <input class="form-control" id="f-classes" name="teachingClasses" type="text"
                 value="${escapeHtml(course.teachingClasses ?? "")}" placeholder="多个班级可用逗号分隔">
        </div>
      </div>
      <div class="form-row form-row--3">
        <div class="form-group">
          <label class="form-group__label" for="f-status">课程状态</label>
          <select class="form-control" id="f-status" name="courseStatus">
            ${["未开始", "进行中", "已结课"]
              .map(
                (status) =>
                  `<option value="${status}"${
                    course.courseStatus === status ? " selected" : ""
                  }>${status}</option>`
              )
              .join("")}
          </select>
        </div>
        <div class="form-group">
          <label class="form-group__label" for="f-start">开课日期</label>
          <input class="form-control" id="f-start" name="startDate" type="date"
                 value="${escapeHtml(course.startDate ?? "")}">
        </div>
        <div class="form-group">
          <label class="form-group__label" for="f-duration">课程时长 / 学时</label>
          <input class="form-control" id="f-duration" name="duration" type="text"
                 value="${escapeHtml(course.duration ?? "")}" placeholder="例如：32 学时">
        </div>
      </div>
      <div class="form-row form-row--3">
        <div class="form-group">
          <label class="form-group__label" for="f-address">上课地点</label>
          <input class="form-control" id="f-address" name="classAddress" type="text"
                 value="${escapeHtml(course.classAddress ?? "")}">
        </div>
        <div class="form-group">
          <label class="form-group__label" for="f-audience">适用对象</label>
          <input class="form-control" id="f-audience" name="targetAudience" type="text"
                 value="${escapeHtml(course.targetAudience ?? "")}" placeholder="例如：本科一年级">
        </div>
        <div class="form-group">
          <label class="form-group__label" for="f-price">课程价格（元）</label>
          <input class="form-control" id="f-price" name="coursePrice" type="number" min="0" step="0.01"
                 value="${course.coursePrice ?? ""}" placeholder="0 表示免费">
        </div>
      </div>
      <div class="form-group">
        <label class="form-group__label" for="f-tags">课程标签</label>
        <input class="form-control" id="f-tags" name="courseTags" type="text"
               value="${escapeHtml(course.courseTags ?? "")}" placeholder="多个标签用逗号分隔，例如：数学,基础课">
      </div>

      <h2 class="card__title mb-md mt-xl">课程内容</h2>
      <div class="form-group">
        <label class="form-group__label" for="f-cover">课程封面图片地址</label>
        <input class="form-control" id="f-cover" name="courseImage" type="text"
               value="${escapeHtml(course.courseImage ?? "")}" placeholder="images/test-img.jpg 或外链地址">
        <img class="cover-preview" id="cover-preview" src="${escapeHtml(
          course.courseImage || "images/test-img.jpg"
        )}" alt="课程封面预览">
      </div>
      <div class="form-group">
        <label class="form-group__label" for="f-intro">课程简介</label>
        <textarea class="form-control" id="f-intro" name="courseIntroduction"
                  placeholder="介绍课程内容、适合人群与学习收获">${escapeHtml(
                    course.courseIntroduction ?? ""
                  )}</textarea>
      </div>
      <div class="form-group">
        <label class="form-group__label" for="f-objectives">教学目标</label>
        <textarea class="form-control" id="f-objectives" name="teachingObjectives"
                  placeholder="列出学生学完后应掌握的知识与能力">${escapeHtml(
                    course.teachingObjectives ?? ""
                  )}</textarea>
      </div>
      <div class="form-group">
        <label class="form-group__label" for="f-outline">课程大纲 / 课时</label>
        <textarea class="form-control" id="f-outline" name="courseOutline" rows="8"
                  placeholder="每行一个课时，格式：课时名称|视频地址（视频地址可留空）">${escapeHtml(
                    course.courseOutline ?? ""
                  )}</textarea>
        <p class="form-group__hint">
          每行一个课时；如需支持学生端在线播放，请写成“课时名称|视频地址”，视频地址可先用“在线录制视频”页生成并上传。
        </p>
      </div>

      <p class="form-message" id="form-message" role="alert" hidden></p>
      <div class="form-actions">
        <button class="btn btn--primary" id="save-btn" type="submit">${
          isEdit ? "保存修改" : "创建课程"
        }</button>
        <button class="btn" id="reset-btn" type="reset">重置</button>
        <a class="btn" href="my-course.html">返回我开的课</a>
      </div>
    </form>`;
}

/** 渲染查看模式 */
function renderView(course, extra) {
  const host = document.querySelector("#detail-root");
  const outline = String(course.courseOutline ?? "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [title, videoUrl = ""] = line.split("|").map((part) => part.trim());
      return { index: index + 1, title: title || "未命名课时", videoUrl };
    });

  document.querySelector("#crumb-current").textContent = course.courseName ?? "课程详情";
  document.title = `${course.courseName ?? "课程"} - 课程详情与维护`;

  host.innerHTML = `
    <section class="detail-hero">
      <img class="detail-hero__cover" src="${escapeHtml(
        course.courseImage || "images/test-img.jpg"
      )}" alt="${escapeHtml(course.courseName ?? "课程")} 的课程封面">
      <div>
        <h1 class="detail-hero__title">${escapeHtml(course.courseName ?? "未命名课程")}</h1>
        <div class="flex-wrap flex gap-sm">
          <span class="tag tag--primary">${escapeHtml(course.courseStatus ?? "未标注状态")}</span>
          ${String(course.courseTags ?? "")
            .split(/[,，]/)
            .map((tag) => tag.trim())
            .filter(Boolean)
            .map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`)
            .join("")}
        </div>
        <dl class="detail-meta">
          <div><dt>课程编号</dt><dd>${escapeHtml(course.courseNumber ?? "—")}</dd></div>
          <div><dt>开课班级</dt><dd>${escapeHtml(course.teachingClasses ?? "—")}</dd></div>
          <div><dt>访问量</dt><dd>${extra.viewCount} 次</dd></div>
          <div><dt>热度排名</dt><dd>${extra.ranking ? `第 ${extra.ranking} 名` : "暂无排名"}</dd></div>
          <div><dt>班级学生数</dt><dd>${extra.studentCount || "—"}</dd></div>
          <div><dt>课程时长</dt><dd>${escapeHtml(course.duration ?? "—")}</dd></div>
        </dl>
        <div class="flex-wrap flex gap-sm">
          <a class="btn btn--primary" href="gradelist.html?className=${encodeURIComponent(
            course.teachingClasses ?? ""
          )}">管理班级名单</a>
          <a class="btn" href="video-record.html">录制课时视频</a>
          <a class="btn" href="my-course-detail.html?mode=edit&courseNumber=${encodeURIComponent(
            course.courseNumber ?? ""
          )}">编辑课程</a>
          <a class="btn" href="course-info.html?courseNumber=${encodeURIComponent(
            course.courseNumber ?? ""
          )}">学生端预览</a>
        </div>
      </div>
    </section>

    <section class="card mt-lg" aria-labelledby="detail-intro-heading">
      <div class="card__header">
        <h2 class="card__title" id="detail-intro-heading">课程简介</h2>
      </div>
      <p class="rich-text">${escapeHtml(course.courseIntroduction || "暂未填写课程简介。")}</p>
      <h3 class="card__title mt-lg mb-sm">教学目标</h3>
      <p class="rich-text">${escapeHtml(course.teachingObjectives || "暂未填写教学目标。")}</p>
    </section>

    <section class="card mt-lg" aria-labelledby="detail-outline-heading">
      <div class="card__header">
        <h2 class="card__title" id="detail-outline-heading">课时列表</h2>
        <span class="card__subtitle">${outline.length} 个课时</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <caption class="sr-only">课程课时列表</caption>
          <thead>
          <tr>
            <th scope="col">序号</th>
            <th scope="col">课时名称</th>
            <th scope="col">视频地址</th>
            <th scope="col">操作</th>
          </tr>
          </thead>
          <tbody>
          ${
            outline.length
              ? outline
                  .map(
                    (lesson) => `
            <tr>
              <td>${lesson.index}</td>
              <td class="is-wrap">${escapeHtml(lesson.title)}</td>
              <td class="is-wrap">${
                lesson.videoUrl ? escapeHtml(lesson.videoUrl) : '<span class="text-muted">未配置</span>'
              }</td>
              <td>
                <a href="course-play.html?courseNumber=${encodeURIComponent(
                  course.courseNumber ?? ""
                )}&lesson=${lesson.index}">试看</a>
              </td>
            </tr>`
                  )
                  .join("")
              : '<tr><td class="is-wrap text-muted" colspan="4">暂未配置课时，可在编辑页的大纲中逐行添加。</td></tr>'
          }
          </tbody>
        </table>
      </div>
    </section>

    <section class="card mt-lg" aria-labelledby="detail-ops-heading">
      <div class="card__header">
        <h2 class="card__title" id="detail-ops-heading">课程维护</h2>
      </div>
      <ul class="list-plain">
        <li class="flex-between">
          <span>补充或调整课程大纲与课时视频</span>
          <a class="btn btn--sm" href="my-course-detail.html?mode=edit&courseNumber=${encodeURIComponent(
            course.courseNumber ?? ""
          )}">编辑大纲</a>
        </li>
        <li class="flex-between">
          <span>维护开课班级的学生名单</span>
          <a class="btn btn--sm" href="gradelist.html?className=${encodeURIComponent(
            course.teachingClasses ?? ""
          )}">进入名单</a>
        </li>
        <li class="flex-between">
          <span>发布课程通知与机构信息</span>
          <a class="btn btn--sm" href="publish-info.html">去发布</a>
        </li>
      </ul>
    </section>`;
}

/** 绑定表单交互 */
function bindForm(course) {
  const form = document.querySelector("#course-form");
  const coverInput = document.querySelector("#f-cover");
  const preview = document.querySelector("#cover-preview");
  const message = document.querySelector("#form-message");

  coverInput?.addEventListener("input", () => {
    preview.src = coverInput.value.trim() || "images/test-img.jpg";
  });

  document.querySelector("#reset-btn")?.addEventListener("click", (event) => {
    event.preventDefault();
    form.reset();
    if (course) {
      form.querySelector("#f-name").value = course.courseName ?? "";
      form.querySelector("#f-number").value = course.courseNumber ?? "";
    }
    message.hidden = true;
  });

  form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitButton = document.querySelector("#save-btn");
    const payload = collectCourseForm(form);

    if (!payload.courseName || !payload.courseNumber) {
      message.hidden = false;
      message.classList.add("is-error");
      message.textContent = "课程名称与课程编号为必填项。";
      return;
    }

    submitButton.disabled = true;
    try {
      if (state.mode === "edit" && state.course?.id) {
        await apiPut("/course/update", payload, { id: state.course.id });
        toast("课程已更新");
        window.location.href = `my-course-detail.html?courseNumber=${encodeURIComponent(
          payload.courseNumber
        )}`;
      } else {
        const created = await apiPost("/course/add", payload);
        toast("课程创建成功");
        window.location.href = `my-course-detail.html?courseNumber=${encodeURIComponent(
          created?.courseNumber ?? payload.courseNumber
        )}`;
      }
    } catch (error) {
      message.hidden = false;
      message.classList.add("is-error");
      message.textContent = error.message || "保存失败，请稍后重试。";
      toast(error.message || "保存失败", "error");
      submitButton.disabled = false;
    }
  });
}

/** 加载查看模式所需的附加数据 */
async function loadExtras(course) {
  const [viewCount, ranking, students] = await Promise.all([
    apiGet(`/course/view/${course.id}/count`).catch(() => Number(course.viewCount) || 0),
    apiGet(`/course/popularity/${course.id}/ranking`).catch(() => 0),
    course.teachingClasses
      ? apiGet("/students/search", { className: course.teachingClasses, page: 0, size: 1 }).catch(() => null)
      : Promise.resolve(null)
  ]);
  return {
    viewCount: Number(viewCount) || 0,
    ranking: Number(ranking) || 0,
    studentCount: Number(students?.totalElements) || 0
  };
}

/** 页面初始化 */
async function main() {
  installImageFallback();

  // 先解析页面模式，再初始化外壳，保证侧边栏高亮正确
  const modeParam = getParam("mode");
  const courseNumber = getParam("courseNumber");
  state.mode = modeParam === "create" ? "create" : modeParam === "edit" ? "edit" : "view";

  state.context = await initPageShell({
    active: state.mode === "view" ? PAGE_KEY.MY_COURSE : PAGE_KEY.MY_COURSE_DETAIL
  });
  if (!state.context.user) return;

  const host = document.querySelector("#detail-root");
  const inEditMode = state.mode === "create" || state.mode === "edit";
  document.querySelector("#crumb-current").textContent =
    state.mode === "create" ? "我要开课 - 上传课程" : state.mode === "edit" ? "编辑课程" : "课程详情";

  try {
    if (inEditMode && state.mode === "create") {
      document.title = "我要开课 - 上传课程";
      host.innerHTML = `<section class="card">${courseFormHtml()}</section>`;
      bindForm(null);
      return;
    }

    if (!courseNumber) {
      renderError(host, "缺少课程编号参数，请从我开的课列表进入。", () => {
        window.location.href = "my-course.html";
      });
      return;
    }

    const pageData = await apiGet("/course/search", { courseNumber, page: 0, size: 1 });
    const [course] = pageContent(pageData);
    if (!course) {
      renderError(host, "未找到该课程，可能已被删除。", () => {
        window.location.href = "my-course.html";
      });
      return;
    }
    state.course = course;

    if (state.mode === "edit") {
      document.title = `编辑课程 - ${course.courseName ?? ""}`;
      host.innerHTML = `<section class="card">${courseFormHtml(course)}</section>`;
      bindForm(course);
      return;
    }

    const extras = await loadExtras(course);
    renderView(course, extras);
  } catch (error) {
    console.error("课程详情加载失败", error);
    renderError(host, error, () => window.location.reload());
  }
}

document.addEventListener("DOMContentLoaded", main);
