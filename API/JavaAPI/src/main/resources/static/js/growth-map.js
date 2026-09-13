/**
 * 成长地图（学生 / 老师）
 * 接口：
 *   GET /api/student-courses/all-courses     已选课程（统计广度与完成度）
 *   GET /api/student-courses/viewed-courses  已完成课程
 *   GET /api/notes/student/{studentId}       笔记产出（统计沉淀与互动）
 *   GET /api/notes/{noteId}/liked            点赞情况
 *   GET /api/course/search?teachingTeachers= 教师身份下的教研分布
 * 说明：后端未提供成长等级接口，本页所有维度均基于上述真实数据折算，规则在页面内可追溯。
 */
import {
  initPageShell,
  apiGet,
  pageContent,
  renderEmpty,
  escapeHtml,
  installImageFallback,
  PAGE_KEY
} from "./frontend-common.js";

const state = {
  context: null,
  courses: [],
  viewedCount: 0,
  notes: [],
  totalLikes: 0
};

/** 成长阶段定义 */
const STAGES = [
  { key: "start", title: "启程", desc: "加入第一门课程，建立学习计划", require: (m) => m.courseCount >= 1 },
  { key: "steady", title: "稳固", desc: "完成 3 门课程并沉淀 3 篇笔记", require: (m) => m.viewedCount >= 3 && m.noteCount >= 3 },
  { key: "deep", title: "深耕", desc: "完成 6 门课程，笔记获 10 次点赞", require: (m) => m.viewedCount >= 6 && m.totalLikes >= 10 },
  { key: "expert", title: "领航", desc: "完成 10 门课程并保持 15 篇以上笔记", require: (m) => m.viewedCount >= 10 && m.noteCount >= 15 }
];

/** 由数据折算各项指标 */
function buildMetrics() {
  const courseCount = state.courses.length;
  const viewedCount = state.viewedCount;
  const noteCount = state.notes.length;
  const totalLikes = state.totalLikes;
  const rate = courseCount ? viewedCount / courseCount : 0;

  /** 线性折算为 0-100 分，超过目标值按满分计 */
  const score = (value, target) => Math.max(0, Math.min(100, Math.round((value / target) * 100)));

  return {
    courseCount,
    viewedCount,
    noteCount,
    totalLikes,
    rate,
    overall: Math.round(rate * 45 + (score(noteCount, 15) * 0.35 + score(totalLikes, 20) * 0.2) * 0.55)
  };
}

/** 渲染总体进度与等级 */
function renderHero(metrics) {
  const progress = Math.max(0, Math.min(100, metrics.overall));
  document.querySelector("#map-progress").innerHTML = `${progress}<span class="stat__unit">%</span>`;
  document.querySelector("#map-progress-bar").style.width = `${progress}%`;

  const achieved = STAGES.filter((stage) => stage.require(metrics));
  const level = achieved.length ? achieved.at(-1) : null;
  document.querySelector("#map-level-tag").textContent = level ? `当前阶段：${level.title}` : "尚未启程";
  document.querySelector("#map-level-hint").textContent = level
    ? `已完成 ${achieved.length} / ${STAGES.length} 个阶段目标`
    : "加入第一门课程即可开启成长地图";

  document.querySelector("#map-summary").textContent =
    `已加入 ${metrics.courseCount} 门课程，完成 ${metrics.viewedCount} 门，产出 ${metrics.noteCount} 篇笔记，累计获得 ${metrics.totalLikes} 次点赞。`;
}

/** 渲染能力维度 */
function renderAbilities(metrics) {
  const host = document.querySelector("#ability-grid");
  const score = (value, target) => Math.max(0, Math.min(100, Math.round((value / target) * 100)));

  const abilities = [
    {
      name: "学习广度",
      value: score(metrics.courseCount, 8),
      hint: `已选 ${metrics.courseCount} 门课程，目标 8 门`
    },
    {
      name: "学习完成度",
      value: Math.round(metrics.rate * 100),
      hint: `已完成 ${metrics.viewedCount} / ${metrics.courseCount || 0} 门`
    },
    {
      name: "知识沉淀",
      value: score(metrics.noteCount, 15),
      hint: `已写 ${metrics.noteCount} 篇笔记，目标 15 篇`
    },
    {
      name: "分享影响力",
      value: score(metrics.totalLikes, 20),
      hint: `笔记累计获赞 ${metrics.totalLikes} 次，目标 20 次`
    }
  ];

  host.innerHTML = abilities
    .map(
      (ability) => `
      <div class="ability-card">
        <div class="ability-card__head">
          <span class="ability-card__name">${escapeHtml(ability.name)}</span>
          <span class="ability-card__score">${ability.value}</span>
        </div>
        <div class="progress" role="progressbar" aria-valuenow="${ability.value}" aria-valuemin="0"
             aria-valuemax="100" aria-label="${escapeHtml(ability.name)}">
          <div class="progress__bar progress__bar--${
            ability.value >= 80 ? "success" : ability.value >= 40 ? "" : "warning"
          }" style="width:${ability.value}%"></div>
        </div>
        <p class="ability-card__hint">${escapeHtml(ability.hint)}</p>
      </div>`
    )
    .join("");
}

/** 渲染成长阶段 */
function renderStages(metrics) {
  const host = document.querySelector("#stage-track");
  host.innerHTML = STAGES.map((stage, index) => {
    const done = stage.require(metrics);
    return `
      <li class="stage-item${done ? " is-done" : ""}">
        <span class="stage-item__index">${done ? "✓" : index + 1}</span>
        <div>
          <p class="stage-item__title">${escapeHtml(stage.title)}</p>
          <p class="stage-item__desc">${escapeHtml(stage.desc)}</p>
          <p class="ability-card__hint">${done ? "已达成" : "进行中"}</p>
        </div>
      </li>`;
  }).join("");
}

/** 生成推荐任务 */
function renderTasks(metrics) {
  const host = document.querySelector("#task-list");
  const tasks = [];

  if (metrics.courseCount < 3) {
    tasks.push({
      title: "再选 1-2 门课程",
      desc: `当前已选 ${metrics.courseCount} 门，建议扩充到 3 门以上形成学习节奏。`,
      href: "course-list.html",
      action: "去选课"
    });
  }
  if (metrics.rate < 0.6 && metrics.courseCount > 0) {
    tasks.push({
      title: "推进在学课程进度",
      desc: "把进度落后的课程安排到本周计划，完成率提升到 60% 以上。",
      href: "my-class.html",
      action: "查看待学课程"
    });
  }
  if (metrics.noteCount < 5) {
    tasks.push({
      title: "每节课后写一条笔记",
      desc: `当前 ${metrics.noteCount} 篇笔记，建议学完一节课就沉淀一条要点。`,
      href: "my-note.html",
      action: "写笔记"
    });
  }
  if (metrics.totalLikes < 5 && metrics.noteCount > 0) {
    tasks.push({
      title: "把优质笔记设为公开",
      desc: "公开笔记可以被同学看到并点赞，有助于形成学习社区互动。",
      href: "my-note.html",
      action: "管理笔记"
    });
  }
  if (state.context?.role === "teacher") {
    tasks.push({
      title: "维护课程资料",
      desc: "补充课程大纲与课时视频，帮助学生更好地安排学习。",
      href: "my-course.html",
      action: "管理我的课程"
    });
  }

  if (!tasks.length) {
    tasks.push({
      title: "保持当前节奏",
      desc: "你的学习数据表现良好，继续保持并尝试帮助同学答疑。",
      href: "friend-activity.html",
      action: "看看好友动态"
    });
  }

  document.querySelector("#task-count").textContent = `${tasks.length} 项`;
  host.innerHTML = tasks
    .map(
      (task) => `
      <li class="task-item">
        <div>
          <p class="task-item__title">${escapeHtml(task.title)}</p>
          <p class="task-item__desc">${escapeHtml(task.desc)}</p>
        </div>
        <a class="btn btn--sm btn--primary" href="${escapeHtml(task.href)}">${escapeHtml(task.action)}</a>
      </li>`
    )
    .join("");
}

/** 加载数据 */
async function loadData() {
  const { role, student, teacher } = state.context;

  if (role === "teacher" && teacher?.name) {
    const pageData = await apiGet("/course/search", {
      teachingTeachers: teacher.name,
      page: 0,
      size: 200
    });
    state.courses = pageContent(pageData);
    state.viewedCount = state.courses.length;
    state.notes = [];
    state.totalLikes = 0;
    return;
  }

  if (!student) return;

  const [allPage, viewedPage, notePage] = await Promise.all([
    apiGet("/student-courses/all-courses", { studentId: student.id, page: 0, size: 200 }),
    apiGet("/student-courses/viewed-courses", { studentId: student.id, page: 0, size: 200 }),
    apiGet(`/notes/student/${student.id}`, { page: 0, size: 200 }).catch(() => null)
  ]);

  state.courses = pageContent(allPage);
  state.viewedCount = pageContent(viewedPage).length;
  state.notes = pageContent(notePage);
  state.totalLikes = state.notes.reduce((sum, note) => sum + (Number(note.likeCount) || 0), 0);
}

/** 页面初始化 */
async function main() {
  installImageFallback();
  try {
    state.context = await initPageShell({ active: PAGE_KEY.GROWTH_MAP });
    if (!state.context.user) return;

    await loadData();
    const metrics = buildMetrics();

    renderHero(metrics);
    renderAbilities(metrics);
    renderStages(metrics);
    renderTasks(metrics);
  } catch (error) {
    console.error("成长地图加载失败", error);
    renderEmpty(document.querySelector("#ability-grid"), "成长数据加载失败", "请稍后刷新页面重试。");
  }
}

document.addEventListener("DOMContentLoaded", main);
