/**
 * 教师AI全能助手 — 主应用逻辑
 * ────────────────────────────────────────────
 * 负责：
 *   - 标签页切换
 *   - API 健康检查
 *   - Toast 通知
 *   - 学科/年级下拉框初始化
 *   - 通用工具函数
 */

// ============================================================
//  全局状态
// ============================================================

const AppState = {
  currentTab: 'knowledge',
  apiOnline: false,
  catalog: null, // 学科/年级数据
};

// ============================================================
//  Toast 通知
// ============================================================

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  // 3 秒后自动移除
  setTimeout(() => {
    if (toast.parentNode) {
      toast.remove();
    }
  }, 3000);
}

// ============================================================
//  API 调用封装
// ============================================================

/**
 * 通用的 POST 请求
 * @param {string} url
 * @param {Object} body
 * @returns {Promise<Object>}
 */
async function apiPost(url, body) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const data = await response.json();

  if (!response.ok || !data.success) {
    throw new Error(data.error || `请求失败 (${response.status})`);
  }

  return data;
}

/**
 * 通用的 GET 请求
 * @param {string} url
 * @returns {Promise<Object>}
 */
async function apiGet(url) {
  const response = await fetch(url);
  const data = await response.json();

  if (!response.ok || !data.success) {
    throw new Error(data.error || `请求失败 (${response.status})`);
  }

  return data;
}

// ============================================================
//  健康检查
// ============================================================

async function checkHealth() {
  const dot = document.getElementById('statusDot');
  const text = document.getElementById('statusText');

  try {
    const res = await fetch('/api/health');
    const data = await res.json();
    if (data.status === 'ok') {
      AppState.apiOnline = true;
      dot.className = 'status-dot online';
      text.textContent = data.hasApiKey ? 'DeepSeek API 已就绪' : '⚠️ 请配置 API Key';
    }
  } catch (_) {
    AppState.apiOnline = false;
    dot.className = 'status-dot offline';
    text.textContent = '服务未连接';
  }
}

// ============================================================
//  初始化学科/年级下拉框
// ============================================================

async function initSelectOptions() {
  try {
    const res = await apiGet('/api/knowledge/subjects');
    AppState.catalog = res.data;

    const { subjects, grades } = res.data;

    // 填充所有学科下拉框
    const subjectSelects = ['k-subject', 'l-subject'];
    subjectSelects.forEach((id) => {
      const sel = document.getElementById(id);
      if (!sel) return;
      // 保留第一个 "请选择" option
      sel.innerHTML = '<option value="">请选择学科</option>';
      subjects.forEach((s) => {
        const opt = document.createElement('option');
        opt.value = s.name;
        opt.textContent = s.name;
        sel.appendChild(opt);
      });
    });

    // 填充所有年级下拉框
    const gradeSelects = ['k-grade', 'l-grade'];
    gradeSelects.forEach((id) => {
      const sel = document.getElementById(id);
      if (!sel) return;
      sel.innerHTML = '<option value="">请选择年级</option>';
      grades.forEach((g) => {
        const opt = document.createElement('option');
        opt.value = g;
        opt.textContent = g;
        sel.appendChild(opt);
      });
    });
  } catch (err) {
    console.warn('获取学科列表失败，使用默认选项:', err.message);
    // 使用硬编码的默认值
    const defaultSubjects = ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治', '科学'];
    const defaultGrades = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级', '七年级', '八年级', '九年级', '高一', '高二', '高三'];

    ['k-subject', 'l-subject'].forEach((id) => {
      const sel = document.getElementById(id);
      if (sel && sel.options.length <= 1) {
        defaultSubjects.forEach((s) => {
          const opt = document.createElement('option');
          opt.value = s;
          opt.textContent = s;
          sel.appendChild(opt);
        });
      }
    });

    ['k-grade', 'l-grade'].forEach((id) => {
      const sel = document.getElementById(id);
      if (sel && sel.options.length <= 1) {
        defaultGrades.forEach((g) => {
          const opt = document.createElement('option');
          opt.value = g;
          opt.textContent = g;
          sel.appendChild(opt);
        });
      }
    });
  }
}

// ============================================================
//  标签页切换
// ============================================================

function switchTab(tabName) {
  AppState.currentTab = tabName;

  // 更新导航按钮状态
  document.querySelectorAll('.nav-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === tabName);
  });

  // 切换面板
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    panel.classList.toggle('active', panel.id === `panel-${tabName}`);
  });
}

// ============================================================
//  复制到剪贴板
// ============================================================

function copyToClipboard(text) {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => {
      showToast('已复制到剪贴板', 'success');
    });
  } else {
    // Fallback
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    showToast('已复制到剪贴板', 'success');
  }
}

/**
 * 触发文本文件下载
 */
function downloadTextFile(content, filename) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  showToast('文件已下载', 'success');
}

// ============================================================
//  初始化
// ============================================================

function initApp() {
  // 标签页切换事件
  document.querySelectorAll('.nav-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      switchTab(btn.dataset.tab);
    });
  });

  // 初始化图标库（feather-icons 异步加载完成后渲染）
  if (typeof feather !== 'undefined') {
    feather.replace();
  }

  // 健康检查
  checkHealth();
  // 每 30 秒检查一次
  setInterval(checkHealth, 30000);

  // 加载学科/年级选项
  initSelectOptions();

  // 初始化业务模块
  if (typeof initKnowledgeHandbook === 'function') {
    initKnowledgeHandbook();
  }
  if (typeof initSmartLessonPlan === 'function') {
    initSmartLessonPlan();
  }
}

// DOM 加载完成后初始化
document.addEventListener('DOMContentLoaded', initApp);

// Feather Icons 加载完成后重新渲染
window.addEventListener('load', () => {
  if (typeof feather !== 'undefined') {
    feather.replace();
  }
});
