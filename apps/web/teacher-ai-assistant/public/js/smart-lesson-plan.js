/**
 * 智能教案模块
 * ────────────────────────────────────────────
 * 负责：
 *   - 收集教案表单参数
 *   - 调用生成 API
 *   - 渲染教案内容
 *   - 复制/导出功能
 */

// ============================================================
//  DOM 元素引用
// ============================================================

const L = {
  subject: () => document.getElementById('l-subject'),
  grade: () => document.getElementById('l-grade'),
  topic: () => document.getElementById('l-topic'),
  lessonType: () => document.getElementById('l-lessonType'),
  duration: () => document.getElementById('l-duration'),
  teachingStyle: () => document.getElementById('l-teachingStyle'),
  extra: () => document.getElementById('l-extra'),
  btn: () => document.getElementById('btnGenerateLesson'),
  placeholder: () => document.getElementById('lessonPlaceholder'),
  loading: () => document.getElementById('lessonLoading'),
  error: () => document.getElementById('lessonError'),
  result: () => document.getElementById('lessonResult'),
  title: () => document.getElementById('l-title'),
  content: () => document.getElementById('l-content'),
  btnCopy: () => document.getElementById('btnCopyLesson'),
  btnExport: () => document.getElementById('btnExportLesson'),
};

// ============================================================
//  状态切换
// ============================================================

function showLessonState(state) {
  L.placeholder().style.display = state === 'placeholder' ? '' : 'none';
  L.loading().style.display = state === 'loading' ? '' : 'none';
  L.error().style.display = state === 'error' ? '' : 'none';
  L.result().style.display = state === 'result' ? '' : 'none';
}

function setLessonLoading(loading) {
  L.btn().disabled = loading;
  L.btn().innerHTML = loading
    ? '<span class="spinner" style="width:16px;height:16px;border-width:2px;"></span> 生成中...'
    : '<i data-feather="zap" class="btn-icon"></i> 生成智能教案';
  if (!loading && typeof feather !== 'undefined') {
    feather.replace();
  }
}

// ============================================================
//  渲染教案
// ============================================================

function renderLessonPlan(data) {
  L.title().textContent = data.title || '';

  const container = L.content();
  container.innerHTML = '';

  // ── 基本信息 ──
  const metaSection = createSection('📋 基本信息');
  const metaBody = metaSection.querySelector('.lp-section-body');
  metaBody.innerHTML = `
    <div class="lp-meta">
      <div class="lp-meta-item"><span class="label">课型：</span>${escapeHtml(data.lessonType || '')}</div>
      <div class="lp-meta-item"><span class="label">课时：</span>${data.duration || ''} 分钟</div>
    </div>
  `;
  container.appendChild(metaSection);

  // ── 教学目标 ──
  if (data.teachingObjectives && data.teachingObjectives.length > 0) {
    const objSection = createSection('🎯 教学目标');
    const objBody = objSection.querySelector('.lp-section-body');
    objBody.innerHTML = `
      <div class="objective-list">
        ${data.teachingObjectives.map((obj) => {
          const catMap = { knowledge: 'obj-knowledge', skill: 'obj-skill', emotion: 'obj-emotion' };
          const catLabel = { knowledge: '知识与技能', skill: '过程与方法', emotion: '情感态度价值观' };
          const catClass = catMap[obj.category] || 'obj-knowledge';
          const catText = catLabel[obj.category] || obj.category;
          return `
            <div class="objective-item">
              <span class="objective-category ${catClass}">${escapeHtml(catText)}</span>
              <span class="objective-content">${escapeHtml(obj.content || '')}</span>
            </div>
          `;
        }).join('')}
      </div>
    `;
    container.appendChild(objSection);
  }

  // ── 教学重点 ──
  if (data.keyPoints && data.keyPoints.length > 0) {
    const kpSection = createSection('⭐ 教学重点');
    const kpBody = kpSection.querySelector('.lp-section-body');
    kpBody.innerHTML = `
      <div class="point-list">
        ${data.keyPoints.map((p) => `<div class="point-item"><span class="point-label">🔴</span>${escapeHtml(typeof p === 'string' ? p : p.point || p)}</div>`).join('')}
      </div>
    `;
    container.appendChild(kpSection);
  }

  // ── 教学难点 ──
  if (data.difficultPoints && data.difficultPoints.length > 0) {
    const dpSection = createSection('💎 教学难点');
    const dpBody = dpSection.querySelector('.lp-section-body');
    dpBody.innerHTML = `
      <div class="point-list">
        ${data.difficultPoints.map((p) => {
          const pointText = typeof p === 'string' ? p : p.point;
          const strategy = typeof p === 'object' ? p.strategy : '';
          return `
            <div class="point-item">
              <span class="point-label">🟡</span>${escapeHtml(pointText || '')}
              ${strategy ? `<div class="point-strategy">${escapeHtml(strategy)}</div>` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
    container.appendChild(dpSection);
  }

  // ── 教学资源 ──
  if (data.teachingResources) {
    const resSection = createSection('🔧 教学资源 / 教具准备');
    const resBody = resSection.querySelector('.lp-section-body');
    resBody.innerHTML = `<p style="font-size:14px;">${escapeHtml(data.teachingResources)}</p>`;
    container.appendChild(resSection);
  }

  // ── 教学过程 ──
  if (data.teachingProcess && data.teachingProcess.length > 0) {
    const procSection = createSection('📖 教学过程');
    const procBody = procSection.querySelector('.lp-section-body');
    procBody.innerHTML = `
      <div class="process-timeline">
        ${data.teachingProcess.map((stage) => `
          <div class="process-stage">
            <div class="stage-header">
              <span class="stage-name">${escapeHtml(stage.stageName || '')}</span>
              <span class="stage-duration">${stage.duration || 0} 分钟</span>
            </div>
            <div class="stage-detail">
              <div class="stage-detail-item">
                <span class="detail-label">👨‍🏫 教师活动</span>
                <span>${escapeHtml(stage.teacherActivity || '')}</span>
              </div>
              <div class="stage-detail-item">
                <span class="detail-label">👩‍🎓 学生活动</span>
                <span>${escapeHtml(stage.studentActivity || '')}</span>
              </div>
            </div>
            ${stage.designIntent ? `<div class="stage-intent">${escapeHtml(stage.designIntent)}</div>` : ''}
          </div>
        `).join('')}
      </div>
    `;
    container.appendChild(procSection);
  }

  // ── 板书设计 ──
  if (data.boardDesign) {
    const boardSection = createSection('🖊️ 板书设计');
    const boardBody = boardSection.querySelector('.lp-section-body');
    boardBody.innerHTML = `<div class="board-design">${escapeHtml(data.boardDesign)}</div>`;
    container.appendChild(boardSection);
  }

  // ── 作业布置 ──
  if (data.homework) {
    const hwSection = createSection('📝 作业布置');
    const hwBody = hwSection.querySelector('.lp-section-body');
    const hw = data.homework;
    hwBody.innerHTML = `
      <div class="homework-grid">
        ${hw.basic ? `
          <div class="homework-level homework-basic">
            <h4>🟢 基础巩固</h4>
            <ul>${(Array.isArray(hw.basic) ? hw.basic : [hw.basic]).map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul>
          </div>
        ` : ''}
        ${hw.improve ? `
          <div class="homework-level homework-improve">
            <h4>🔵 能力提升</h4>
            <ul>${(Array.isArray(hw.improve) ? hw.improve : [hw.improve]).map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul>
          </div>
        ` : ''}
        ${hw.challenge ? `
          <div class="homework-level homework-challenge">
            <h4>🔴 挑战拓展</h4>
            <ul>${(Array.isArray(hw.challenge) ? hw.challenge : [hw.challenge]).map((item) => `<li>${escapeHtml(item)}</li>`).join('')}</ul>
          </div>
        ` : ''}
      </div>
    `;
    container.appendChild(hwSection);
  }

  // ── 教学反思 ──
  if (data.teachingReflection) {
    const reflSection = createSection('💭 教学反思');
    const reflBody = reflSection.querySelector('.lp-section-body');
    reflBody.innerHTML = `<div class="teaching-reflection">${escapeHtml(data.teachingReflection)}</div>`;
    container.appendChild(reflSection);
  }
}

/**
 * 创建一个教案内容区块
 */
function createSection(title) {
  const section = document.createElement('div');
  section.className = 'lp-section';
  section.innerHTML = `
    <div class="lp-section-header">
      ${escapeHtml(title)}
    </div>
    <div class="lp-section-body"></div>
  `;
  return section;
}

// ============================================================
//  构建纯文本导出
// ============================================================

function buildLessonText(data) {
  let text = '';

  text += `═══════════════════════════════════\n`;
  text += `  智能教案\n`;
  text += `═══════════════════════════════════\n\n`;

  if (data.title) text += `【课题】${data.title}\n`;
  if (data.lessonType) text += `【课型】${data.lessonType}\n`;
  if (data.duration) text += `【课时】${data.duration} 分钟\n`;
  text += `\n`;

  // 教学目标
  if (data.teachingObjectives && data.teachingObjectives.length > 0) {
    text += `───────────────────────────────────\n`;
    text += `一、教学目标\n\n`;
    data.teachingObjectives.forEach((obj) => {
      const label = { knowledge: '知识与技能', skill: '过程与方法', emotion: '情感态度价值观' };
      text += `  [${label[obj.category] || obj.category}] ${obj.content}\n`;
    });
    text += `\n`;
  }

  // 教学重点
  if (data.keyPoints && data.keyPoints.length > 0) {
    text += `二、教学重点\n`;
    data.keyPoints.forEach((p) => {
      text += `  • ${typeof p === 'string' ? p : p.point || p}\n`;
    });
    text += `\n`;
  }

  // 教学难点
  if (data.difficultPoints && data.difficultPoints.length > 0) {
    text += `三、教学难点\n`;
    data.difficultPoints.forEach((p) => {
      const pt = typeof p === 'string' ? p : p.point;
      text += `  • ${pt}\n`;
      if (typeof p === 'object' && p.strategy) {
        text += `    突破策略：${p.strategy}\n`;
      }
    });
    text += `\n`;
  }

  // 教学资源
  if (data.teachingResources) {
    text += `四、教学资源\n  ${data.teachingResources}\n\n`;
  }

  // 教学过程
  if (data.teachingProcess && data.teachingProcess.length > 0) {
    text += `五、教学过程\n\n`;
    data.teachingProcess.forEach((stage) => {
      text += `  ▸ ${stage.stageName}（${stage.duration || 0}分钟）\n`;
      text += `    教师活动：${stage.teacherActivity || ''}\n`;
      text += `    学生活动：${stage.studentActivity || ''}\n`;
      if (stage.designIntent) {
        text += `    设计意图：${stage.designIntent}\n`;
      }
      text += `\n`;
    });
  }

  // 板书设计
  if (data.boardDesign) {
    text += `六、板书设计\n${data.boardDesign}\n\n`;
  }

  // 作业
  if (data.homework) {
    text += `七、作业布置\n`;
    const hw = data.homework;
    if (hw.basic) {
      text += `  【基础巩固】\n`;
      (Array.isArray(hw.basic) ? hw.basic : [hw.basic]).forEach((item) => (text += `    ☐ ${item}\n`));
    }
    if (hw.improve) {
      text += `  【能力提升】\n`;
      (Array.isArray(hw.improve) ? hw.improve : [hw.improve]).forEach((item) => (text += `    ☐ ${item}\n`));
    }
    if (hw.challenge) {
      text += `  【挑战拓展】\n`;
      (Array.isArray(hw.challenge) ? hw.challenge : [hw.challenge]).forEach((item) => (text += `    ☐ ${item}\n`));
    }
    text += `\n`;
  }

  // 教学反思
  if (data.teachingReflection) {
    text += `八、教学反思\n${data.teachingReflection}\n\n`;
  }

  text += `───────────────────────────────────\n`;
  text += `由 教师AI全能助手 生成 | DeepSeek 驱动\n`;

  return text;
}

// ============================================================
//  生成流程
// ============================================================

let lastLessonData = null;

async function handleGenerateLesson() {
  const subject = L.subject().value.trim();
  const grade = L.grade().value.trim();
  const topic = L.topic().value.trim();
  const lessonType = L.lessonType().value;
  const duration = parseInt(L.duration().value, 10) || 45;
  const teachingStyle = L.teachingStyle().value;
  const extraRequirements = L.extra().value.trim();

  // 校验
  if (!subject || !grade || !topic) {
    showToast('请填写学科、年级和课题', 'error');
    if (!subject) L.subject().style.borderColor = 'var(--color-error)';
    if (!grade) L.grade().style.borderColor = 'var(--color-error)';
    if (!topic) L.topic().style.borderColor = 'var(--color-error)';
    return;
  }

  // 重置边框
  [L.subject(), L.grade(), L.topic()].forEach((el) => {
    el.style.borderColor = '';
  });

  // 显示加载状态
  showLessonState('loading');
  setLessonLoading(true);

  try {
    const res = await apiPost('/api/lesson-plan/generate', {
      subject,
      grade,
      topic,
      lessonType: lessonType || undefined,
      duration,
      teachingStyle: teachingStyle || undefined,
      extraRequirements: extraRequirements || undefined,
    });

    lastLessonData = res.data;
    renderLessonPlan(res.data);
    showLessonState('result');

    // 滚动到顶部
    L.result().scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (err) {
    L.error().querySelector('.error-message').textContent = err.message;
    showLessonState('error');
  } finally {
    setLessonLoading(false);
  }
}

// ============================================================
//  初始化
// ============================================================

function initSmartLessonPlan() {
  // 生成按钮
  L.btn().addEventListener('click', handleGenerateLesson);

  // Enter 键快捷提交
  [L.topic(), L.extra()].forEach((el) => {
    el().addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey && e.target !== L.extra()) {
        e.preventDefault();
        handleGenerateLesson();
      }
    });
  });

  // 实时清除错误边框
  [L.subject(), L.grade(), L.topic()].forEach((el) => {
    el().addEventListener('input', () => { el().style.borderColor = ''; });
    el().addEventListener('change', () => { el().style.borderColor = ''; });
  });

  // 重试按钮
  L.error().querySelector('.retry-btn').addEventListener('click', handleGenerateLesson);

  // 复制按钮
  L.btnCopy().addEventListener('click', () => {
    if (lastLessonData) {
      copyToClipboard(buildLessonText(lastLessonData));
    }
  });

  // 导出按钮
  L.btnExport().addEventListener('click', () => {
    if (lastLessonData) {
      const filename = `教案_${lastLessonData.title || '未命名'}.txt`;
      downloadTextFile(buildLessonText(lastLessonData), filename);
    }
  });
}
