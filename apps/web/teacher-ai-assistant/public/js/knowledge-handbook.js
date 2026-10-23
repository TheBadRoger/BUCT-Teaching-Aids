/**
 * 知识点手册模块
 * ────────────────────────────────────────────
 * 负责：
 *   - 收集表单参数
 *   - 调用生成 API
 *   - 渲染知识点卡片
 *   - 复制/导出功能
 */

// ============================================================
//  DOM 元素引用
// ============================================================

const K = {
  subject: () => document.getElementById('k-subject'),
  grade: () => document.getElementById('k-grade'),
  topic: () => document.getElementById('k-topic'),
  focus: () => document.getElementById('k-focus'),
  btn: () => document.getElementById('btnGenerateKnowledge'),
  placeholder: () => document.getElementById('knowledgePlaceholder'),
  loading: () => document.getElementById('knowledgeLoading'),
  error: () => document.getElementById('knowledgeError'),
  result: () => document.getElementById('knowledgeResult'),
  chapterTitle: () => document.getElementById('k-chapterTitle'),
  overview: () => document.getElementById('k-overview'),
  points: () => document.getElementById('k-points'),
  btnCopy: () => document.getElementById('btnCopyKnowledge'),
  btnExport: () => document.getElementById('btnExportKnowledge'),
};

// ============================================================
//  状态切换
// ============================================================

function showKnowledgeState(state) {
  K.placeholder().style.display = state === 'placeholder' ? '' : 'none';
  K.loading().style.display = state === 'loading' ? '' : 'none';
  K.error().style.display = state === 'error' ? '' : 'none';
  K.result().style.display = state === 'result' ? '' : 'none';
}

function setKnowledgeLoading(loading) {
  K.btn().disabled = loading;
  K.btn().innerHTML = loading
    ? '<span class="spinner" style="width:16px;height:16px;border-width:2px;"></span> 生成中...'
    : '<i data-feather="zap" class="btn-icon"></i> 生成知识点手册';
  if (!loading && typeof feather !== 'undefined') {
    feather.replace();
  }
}

// ============================================================
//  渲染知识点
// ============================================================

function renderKnowledgePoints(data) {
  const { chapterTitle, overview, knowledgePoints } = data;

  K.chapterTitle().textContent = chapterTitle || '';
  K.overview().textContent = overview || '';

  const container = K.points();
  container.innerHTML = '';

  if (!knowledgePoints || knowledgePoints.length === 0) {
    container.innerHTML = '<p style="text-align:center;color:var(--color-text-muted);">未生成知识点</p>';
    return;
  }

  knowledgePoints.forEach((kp, idx) => {
    const card = document.createElement('div');
    card.className = 'kp-card';

    const diffLabel = { easy: '简单', medium: '中等', hard: '困难' }[kp.difficulty] || kp.difficulty;
    const impLabel = { required: '必学', extended: '拓展', enrichment: '选学' }[kp.importance] || kp.importance;

    card.innerHTML = `
      <div class="kp-header">
        <span class="kp-number">${idx + 1}</span>
        <span class="kp-name">${escapeHtml(kp.name || '')}</span>
        <div class="kp-badges">
          <span class="badge badge-difficulty-${kp.difficulty || 'medium'}">${diffLabel}</span>
          <span class="badge badge-importance-${kp.importance || 'required'}">${impLabel}</span>
        </div>
      </div>
      <div class="kp-explanation">${escapeHtml(kp.explanation || '')}</div>
      <div class="kp-detail-grid">
        <div class="kp-detail-box">
          <h4>📌 核心概念</h4>
          <ul>${(kp.keyConcepts || []).map((c) => `<li>${escapeHtml(c)}</li>`).join('')}</ul>
        </div>
        <div class="kp-detail-box">
          <h4>⚠️ 常见误区</h4>
          <ul>${(kp.commonMistakes || []).map((m) => `<li>${escapeHtml(m)}</li>`).join('')}</ul>
        </div>
      </div>
    `;

    container.appendChild(card);
  });
}

// ============================================================
//  构建纯文本导出
// ============================================================

function buildKnowledgeText(data) {
  const { chapterTitle, overview, knowledgePoints } = data;
  let text = '';

  text += `═══════════════════════════════════\n`;
  text += `  知识点手册\n`;
  text += `═══════════════════════════════════\n\n`;

  if (chapterTitle) {
    text += `【章节】${chapterTitle}\n\n`;
  }

  if (overview) {
    text += `【概述】${overview}\n\n`;
  }

  text += `───────────────────────────────────\n\n`;

  (knowledgePoints || []).forEach((kp, idx) => {
    const diffLabel = { easy: '简单', medium: '中等', hard: '困难' }[kp.difficulty] || kp.difficulty;
    const impLabel = { required: '必学', extended: '拓展', enrichment: '选学' }[kp.importance] || kp.importance;

    text += `知识点 ${idx + 1}：${kp.name || ''}\n`;
    text += `难度：${diffLabel}  |  重要性：${impLabel}\n`;
    text += `讲解：${kp.explanation || ''}\n`;

    if (kp.keyConcepts && kp.keyConcepts.length > 0) {
      text += `核心概念：\n`;
      kp.keyConcepts.forEach((c) => (text += `  • ${c}\n`));
    }

    if (kp.commonMistakes && kp.commonMistakes.length > 0) {
      text += `常见误区：\n`;
      kp.commonMistakes.forEach((m) => (text += `  • ${m}\n`));
    }

    text += `\n`;
  });

  text += `───────────────────────────────────\n`;
  text += `由 教师AI全能助手 生成 | DeepSeek 驱动\n`;

  return text;
}

// ============================================================
//  生成流程
// ============================================================

let lastKnowledgeData = null; // 缓存最近一次生成结果

async function handleGenerateKnowledge() {
  const subject = K.subject().value.trim();
  const grade = K.grade().value.trim();
  const topic = K.topic().value.trim();
  const focus = K.focus().value.trim();

  // 校验
  if (!subject || !grade || !topic) {
    showToast('请填写学科、年级和章节', 'error');
    // 高亮空字段
    if (!subject) K.subject().style.borderColor = 'var(--color-error)';
    if (!grade) K.grade().style.borderColor = 'var(--color-error)';
    if (!topic) K.topic().style.borderColor = 'var(--color-error)';
    return;
  }

  // 重置边框
  [K.subject(), K.grade(), K.topic()].forEach((el) => {
    el.style.borderColor = '';
  });

  // 显示加载状态
  showKnowledgeState('loading');
  setKnowledgeLoading(true);

  try {
    const res = await apiPost('/api/knowledge/generate', {
      subject,
      grade,
      topic,
      focus: focus || undefined,
    });

    lastKnowledgeData = res.data;
    renderKnowledgePoints(res.data);
    showKnowledgeState('result');
  } catch (err) {
    K.error().querySelector('.error-message').textContent = err.message;
    showKnowledgeState('error');
  } finally {
    setKnowledgeLoading(false);
  }
}

// ============================================================
//  初始化
// ============================================================

function initKnowledgeHandbook() {
  // 生成按钮
  K.btn().addEventListener('click', handleGenerateKnowledge);

  // Enter 键快捷提交
  [K.topic(), K.focus()].forEach((el) => {
    el().addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleGenerateKnowledge();
      }
    });
  });

  // 实时清除错误边框
  [K.subject(), K.grade(), K.topic()].forEach((el) => {
    el().addEventListener('input', () => {
      el().style.borderColor = '';
    });
    el().addEventListener('change', () => {
      el().style.borderColor = '';
    });
  });

  // 重试按钮
  K.error().querySelector('.retry-btn').addEventListener('click', handleGenerateKnowledge);

  // 复制按钮
  K.btnCopy().addEventListener('click', () => {
    if (lastKnowledgeData) {
      copyToClipboard(buildKnowledgeText(lastKnowledgeData));
    }
  });

  // 导出按钮
  K.btnExport().addEventListener('click', () => {
    if (lastKnowledgeData) {
      const filename = `知识点手册_${lastKnowledgeData.chapterTitle || '未命名'}.txt`;
      downloadTextFile(buildKnowledgeText(lastKnowledgeData), filename);
    }
  });
}

// ============================================================
//  HTML 转义工具
// ============================================================

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
