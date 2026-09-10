// ============================================================
// app.js — 应用主入口
// ============================================================

import { createStore, initialState } from './store.js';
import { mountFileUploader } from './components/file-uploader.js';
import { mountHandbookViewer } from './components/handbook-viewer.js';
import { mountFlashcardEngine } from './components/flashcard-engine.js';
import { mountPrintTrigger } from './components/print-trigger.js';
import { generateHandbook } from './data/ai-service.js';
import { qs } from './utils/dom.js';

// ---- 初始化 Store ----
const initState = {
  ...initialState,
  // 扩展：文件上传相关状态
  file: null,
  fileName: '',
  topicName: ''
};

const store = createStore(initState);

// ---- 挂载组件 ----
const fileUploader = mountFileUploader(store);
mountHandbookViewer(store);
mountFlashcardEngine(store);
mountPrintTrigger(store);

// ---- 获取 DOM 引用 ----
const btnGenerate = qs('#btnGenerate');
const btnFlashcardMode = qs('#btnFlashcardMode');
const btnPrint = qs('#btnPrint');
const btnToggleSidebar = qs('#btnToggleSidebar');
const sidebar = qs('#sidebar');
const sidebarOverlay = qs('#sidebarOverlay');
const emptyState = qs('#emptyState');
const loadingState = qs('#loadingState');
const errorState = qs('#errorState');
const handbookContent = qs('#handbookContent');
const errorTitle = qs('#errorTitle');
const errorMessage = qs('#errorMessage');
const loadingDetail = qs('#loadingDetail');
const stepAnalyze = qs('#stepAnalyze');
const stepGenerate = qs('#stepGenerate');
const errorIcon = qs('#errorIcon');
const btnRetry = qs('#btnRetry');
const btnBackToUpload = qs('#btnBackToUpload');

// ---- 侧边栏切换（移动端） ----
function toggleSidebar() {
  const isOpen = !store.get('isSidebarOpen');
  store.setState({ isSidebarOpen: isOpen });
}

btnToggleSidebar.addEventListener('click', toggleSidebar);
sidebarOverlay.addEventListener('click', toggleSidebar);

store.subscribe((state, changedKeys) => {
  if (changedKeys.includes('isSidebarOpen')) {
    sidebar.classList.toggle('open', state.isSidebarOpen);
    sidebarOverlay.classList.toggle('open', state.isSidebarOpen);
  }
});

// ---- 显示状态切换 ----
function showState(state) {
  emptyState.style.display = state === 'empty' ? '' : 'none';
  loadingState.style.display = state === 'loading' ? '' : 'none';
  errorState.style.display = state === 'error' ? '' : 'none';
  handbookContent.style.display = state === 'handbook' ? '' : 'none';
}

// ---- 生成手册 ----
btnGenerate.addEventListener('click', async () => {
  const file = store.get('file');
  const topicName = store.get('topicName') || '';

  if (!file) {
    alert('请先上传文件');
    return;
  }

  // 显示加载状态
  showState('loading');
  btnGenerate.disabled = true;
  loadingDetail.textContent = '正在调用 AI 接口分析文件内容...';
  stepAnalyze.classList.remove('active', 'done');
  stepGenerate.classList.remove('active', 'done');

  try {
    // 进度回调
    const onProgress = (step) => {
      if (step === 'analyze') {
        stepAnalyze.classList.add('active');
        loadingDetail.textContent = 'AI 正在分析文件内容，提炼知识点...';
      } else if (step === 'generate') {
        stepAnalyze.classList.remove('active');
        stepAnalyze.classList.add('done');
        stepGenerate.classList.add('active');
        loadingDetail.textContent = '正在生成知识归纳、记忆口诀、必背要点和自测题...';
      }
    };

    const entry = await generateHandbook(file, topicName, onProgress);

    // 标记完成
    stepGenerate.classList.remove('active');
    stepGenerate.classList.add('done');
    loadingDetail.textContent = '生成完成！';

    // 短暂延迟让用户看到完成状态
    await sleep(500);

    // 更新状态
    store.setState({
      handbookEntry: entry,
      error: null,
      isLoading: false,
      activeTab: 'summary'
    });

    // 显示手册
    showState('handbook');
    btnFlashcardMode.disabled = false;
    btnPrint.disabled = false;

  } catch (err) {
    console.error('生成失败:', err);
    store.setState({ error: err.message, isLoading: false });

    errorIcon.textContent = '😔';
    errorTitle.textContent = '生成失败';
    errorMessage.textContent = err.message || '请检查文件内容或稍后重试';
    showState('error');
  }

  btnGenerate.disabled = false;
});

// ---- 错误恢复 ----
btnRetry.addEventListener('click', () => {
  btnGenerate.click();
});

btnBackToUpload.addEventListener('click', () => {
  fileUploader.clearFile();
  store.setState({ handbookEntry: null, error: null });
  showState('empty');
  btnFlashcardMode.disabled = true;
  btnPrint.disabled = true;
});

// ---- 监听手册状态更新按钮 ----
store.subscribe((state, changedKeys) => {
  if (changedKeys.includes('handbookEntry')) {
    const hasHandbook = !!state.handbookEntry;
    btnFlashcardMode.disabled = !hasHandbook;
    btnPrint.disabled = !hasHandbook;
  }
});

// ---- 初始化 ----
showState('empty');

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
