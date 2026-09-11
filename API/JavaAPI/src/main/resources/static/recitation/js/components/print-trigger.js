// ============================================================
// print-trigger.js — 打印/导出功能
// ============================================================

import { qs, qsa } from '../utils/dom.js';

export function mountPrintTrigger(store) {
  const btnPrint = qs('#btnPrint');

  btnPrint.addEventListener('click', () => {
    const entry = store.get('handbookEntry');
    if (!entry) return;

    prepareForPrint();
    window.print();
    restoreAfterPrint();
  });

  // 监听 afterprint 事件做恢复
  window.addEventListener('afterprint', () => {
    restoreAfterPrint();
  });

  // 备用：超时后恢复（某些浏览器不支持 afterprint）
  window.matchMedia('print').addEventListener('change', (e) => {
    if (!e.matches) {
      setTimeout(restoreAfterPrint, 500);
    }
  });
}

function prepareForPrint() {
  document.body.classList.add('preparing-print');

  // 展开所有自测题答案
  qsa('.selftest-answer').forEach(el => {
    el.dataset.wasHidden = el.style.display === 'none' ? 'true' : 'false';
    el.style.display = 'block';
  });

  // 显示所有 Tab 面板
  qsa('.tab-panel').forEach(el => {
    el.dataset.wasInactive = !el.classList.contains('active') ? 'true' : 'false';
    el.classList.add('active');
    el.style.display = '';
  });
}

function restoreAfterPrint() {
  document.body.classList.remove('preparing-print');

  // 恢复自测题状态
  qsa('.selftest-answer').forEach(el => {
    if (el.dataset.wasHidden === 'true') {
      el.style.display = 'none';
    }
    delete el.dataset.wasHidden;
  });

  // 恢复 Tab 面板状态
  qsa('.tab-panel').forEach(el => {
    if (el.dataset.wasInactive === 'true') {
      el.classList.remove('active');
    }
    delete el.dataset.wasInactive;
  });

  // 恢复折叠按钮状态
  const btnExpand = qs('#btnExpandAll');
  const btnCollapse = qs('#btnCollapseAll');
  if (btnExpand) btnExpand.style.display = '';
  if (btnCollapse) btnCollapse.style.display = 'none';
}
