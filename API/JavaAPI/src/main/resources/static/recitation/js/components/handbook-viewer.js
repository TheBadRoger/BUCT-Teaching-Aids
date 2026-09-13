// ============================================================
// handbook-viewer.js — 背诵手册内容渲染器
// ============================================================

import { sanitizeHTML } from '../utils/sanitize.js';
import { ESSENTIAL_TYPE_ICONS, TEST_TYPE_LABELS } from '../config.js';
import { qs, qsa } from '../utils/dom.js';

/**
 * 挂载手册查看器组件
 * @param {Object} store - 应用状态 Store
 */
export function mountHandbookViewer(store) {
  // Tab 切换
  setupTabNavigation(store);

  // 订阅状态变化
  store.subscribe((state, changedKeys) => {
    if (changedKeys.includes('handbookEntry') && state.handbookEntry) {
      renderAllTabs(state.handbookEntry);
      renderMetadata(state.handbookEntry);
    }
    if (changedKeys.includes('activeTab')) {
      switchTab(state.activeTab);
    }
  });

  // 初始渲染
  const entry = store.get('handbookEntry');
  if (entry) {
    renderAllTabs(entry);
    renderMetadata(entry);
  }
}

function setupTabNavigation(store) {
  const tabNav = qs('#tabNav');
  if (!tabNav) return;

  tabNav.addEventListener('click', (e) => {
    const btn = e.target.closest('.tab-btn');
    if (!btn) return;
    const tabId = btn.dataset.tab;
    if (tabId) {
      store.setState({ activeTab: tabId });
    }
  });
}

function switchTab(activeTab) {
  // 更新 Tab 按钮状态
  qsa('.tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === activeTab);
  });
  // 更新面板状态
  qsa('.tab-panel').forEach(panel => {
    panel.classList.toggle('active', panel.id === `panel-${activeTab}`);
  });
}

function renderAllTabs(entry) {
  const { content } = entry;
  if (!content) return;

  renderSummary(qs('#panel-summary'), content.summary);
  renderMnemonics(qs('#panel-mnemonics'), content.mnemonics);
  renderEssentials(qs('#panel-essentials'), content.essentials);
  renderSelfTest(qs('#panel-selftest'), content.selfTest);
}

// ---- 各 Tab 渲染函数 ----

function renderSummary(container, summary) {
  if (!container) return;
  if (!summary) {
    container.innerHTML = '<p class="empty-tab-content">暂无知识归纳内容</p>';
    return;
  }
  const sanitized = sanitizeHTML(summary);
  container.innerHTML = `<div class="summary-content">${sanitized}</div>`;
}

function renderMnemonics(container, mnemonics) {
  if (!container) return;
  if (!mnemonics) {
    container.innerHTML = '<p class="empty-tab-content">暂无记忆口诀</p>';
    return;
  }

  const lines = mnemonics.split('\n').filter(line => line.trim());
  const html = lines.map((line, i) => {
    const trimmed = line.trim();
    return `<p class="mnemonic-line ${i % 2 === 0 ? 'even' : 'odd'}">${trimmed}</p>`;
  }).join('');

  container.innerHTML = `
    <div class="mnemonics-container">
      <div class="mnemonics-card">
        ${html}
      </div>
    </div>`;
}

function renderEssentials(container, essentials) {
  if (!container) return;
  if (!essentials || essentials.length === 0) {
    container.innerHTML = '<p class="empty-tab-content">暂无必背要点</p>';
    return;
  }

  const cards = essentials.map(item => {
    const icon = ESSENTIAL_TYPE_ICONS[item.type] || '📌';
    return `
      <div class="essential-card">
        <div class="essential-header">
          <span class="essential-icon">${icon}</span>
          <span class="essential-label">${escapeHTML(item.label)}</span>
        </div>
        <div class="essential-value">${escapeHTML(item.value)}</div>
        ${item.detail ? `<div class="essential-detail">${escapeHTML(item.detail)}</div>` : ''}
      </div>`;
  }).join('');

  container.innerHTML = `<div class="essentials-grid">${cards}</div>`;
}

function renderSelfTest(container, selfTest) {
  if (!container) return;
  if (!selfTest || selfTest.length === 0) {
    container.innerHTML = '<p class="empty-tab-content">暂无自测题</p>';
    return;
  }

  const toggleBtn = `
    <div class="selftest-toolbar">
      <button class="btn btn-sm btn-outline" id="btnExpandAll">📖 全部展开</button>
      <button class="btn btn-sm btn-outline" id="btnCollapseAll" style="display:none;">📕 全部折叠</button>
      <span class="selftest-count">共 ${selfTest.length} 题</span>
    </div>`;

  const items = selfTest.map((item, i) => {
    const typeLabel = TEST_TYPE_LABELS[item.type] || item.type;
    return `
      <div class="selftest-item">
        <div class="selftest-question" data-index="${i}">
          <span class="question-number">${i + 1}</span>
          <span class="question-type-badge">${typeLabel}</span>
          <span class="question-text">${escapeHTML(item.question)}</span>
          <span class="question-toggle">▼</span>
        </div>
        <div class="selftest-answer" style="display:none;">
          <div class="answer-block">
            <span class="answer-label">✅ 答案：</span>
            <span class="answer-text">${escapeHTML(item.answer)}</span>
          </div>
          ${item.hint ? `
          <div class="hint-block">
            <span class="hint-label">💡 提示：</span>
            <span class="hint-text">${escapeHTML(item.hint)}</span>
          </div>` : ''}
        </div>
      </div>`;
  }).join('');

  container.innerHTML = toggleBtn + `<div class="selftest-list">${items}</div>`;

  // 绑定事件
  setupSelfTestInteractions(container);
}

function setupSelfTestInteractions(container) {
  // 展开/折叠单个题目
  container.addEventListener('click', (e) => {
    const question = e.target.closest('.selftest-question');
    if (!question) return;

    const answer = question.nextElementSibling;
    if (!answer) return;

    const isVisible = answer.style.display !== 'none';
    answer.style.display = isVisible ? 'none' : 'block';
    const toggle = question.querySelector('.question-toggle');
    if (toggle) {
      toggle.textContent = isVisible ? '▼' : '▲';
    }
  });

  // 全部展开
  const btnExpand = container.querySelector('#btnExpandAll');
  const btnCollapse = container.querySelector('#btnCollapseAll');
  if (btnExpand) {
    btnExpand.addEventListener('click', () => {
      container.querySelectorAll('.selftest-answer').forEach(a => { a.style.display = 'block'; });
      container.querySelectorAll('.question-toggle').forEach(t => { t.textContent = '▲'; });
      btnExpand.style.display = 'none';
      if (btnCollapse) btnCollapse.style.display = '';
    });
  }
  if (btnCollapse) {
    btnCollapse.addEventListener('click', () => {
      container.querySelectorAll('.selftest-answer').forEach(a => { a.style.display = 'none'; });
      container.querySelectorAll('.question-toggle').forEach(t => { t.textContent = '▼'; });
      btnCollapse.style.display = 'none';
      if (btnExpand) btnExpand.style.display = '';
    });
  }
}

// ---- 元数据渲染 ----

function renderMetadata(entry) {
  const metaEl = qs('#handbookMeta');
  if (!metaEl) return;

  const { topic, subject, gradeLevel, metadata } = entry;
  metaEl.innerHTML = `
    <div class="meta-bar">
      <span class="meta-item"><strong>📖 ${escapeHTML(topic)}</strong></span>
      ${metadata?.difficulty ? `<span class="meta-item">难度：${'⭐'.repeat(metadata.difficulty)}</span>` : ''}
      ${metadata?.tags?.length ? `<span class="meta-item">标签：${metadata.tags.map(t => escapeHTML(t)).join(' · ')}</span>` : ''}
      ${metadata?.source ? `<span class="meta-item">来源：${escapeHTML(metadata.source)}</span>` : ''}
    </div>`;
}

// ---- 辅助函数 ----

function escapeHTML(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
