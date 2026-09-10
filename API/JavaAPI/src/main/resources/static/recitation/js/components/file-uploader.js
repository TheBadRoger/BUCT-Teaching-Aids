// ============================================================
// file-uploader.js — 文件上传处理组件
// ============================================================

import { qs } from '../utils/dom.js';

/**
 * 挂载文件上传组件
 * @param {Object} store
 * @returns {Object} { getFile, clearFile }
 */
export function mountFileUploader(store) {
  const uploadZone = qs('#uploadZone');
  const fileInput = qs('#fileInput');
  const uploadFileInfo = qs('#uploadFileInfo');
  const fileName = qs('#fileName');
  const fileSize = qs('#fileSize');
  const btnRemoveFile = qs('#btnRemoveFile');
  const btnGenerate = qs('#btnGenerate');
  const topicNameInput = qs('#topicNameInput');

  let currentFile = null;

  /**
   * 处理文件（保留 File 对象，交由后端提取内容）
   */
  function handleFile(file) {
    if (!file) return;

    // 验证文件类型
    const validExts = ['.txt', '.docx', '.pdf'];
    const fileNameLower = file.name.toLowerCase();
    const hasValidExt = validExts.some(ext => fileNameLower.endsWith(ext));

    if (!hasValidExt) {
      showError('请上传 .txt / .docx / .pdf 文件');
      return;
    }

    // 限制文件大小 10MB（docx/pdf 可稍大）
    if (file.size > 10 * 1024 * 1024) {
      showError('文件大小不能超过 10MB');
      return;
    }

    currentFile = file;

    // 显示文件信息
    fileName.textContent = file.name;
    fileSize.textContent = formatFileSize(file.size);

    uploadFileInfo.style.display = 'block';
    uploadZone.style.display = 'none';

    store.setState({
      file: currentFile,
      fileName: file.name,
      error: null
    });

    updateGenerateButton();
  }

  function showError(msg) {
    store.setState({ error: msg });
    setTimeout(() => {
      if (store.get('error') === msg) {
        store.setState({ error: null });
      }
    }, 3000);
  }

  /**
   * 清除已上传的文件
   */
  function clearFile() {
    currentFile = null;
    fileInput.value = '';
    uploadFileInfo.style.display = 'none';
    uploadZone.style.display = '';

    store.setState({
      file: null,
      fileName: '',
      handbookEntry: null
    });

    updateGenerateButton();
  }

  /**
   * 更新生成按钮状态
   */
  function updateGenerateButton() {
    btnGenerate.disabled = !currentFile;
  }

  // ---- 事件绑定 ----

  // 点击上传区域
  uploadZone.addEventListener('click', () => {
    fileInput.click();
  });

  // 文件选择
  fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) handleFile(file);
  });

  // 拖拽上传
  uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('dragover');
  });

  uploadZone.addEventListener('dragleave', () => {
    uploadZone.classList.remove('dragover');
  });

  uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('dragover');
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  });

  // 全局拖拽（避免浏览器打开文件）
  document.addEventListener('dragover', (e) => {
    e.preventDefault();
  });
  document.addEventListener('drop', (e) => {
    e.preventDefault();
  });

  // 移除文件按钮
  btnRemoveFile.addEventListener('click', () => {
    clearFile();
  });

  // 监听知识点名称输入
  topicNameInput.addEventListener('input', () => {
    store.setState({ topicName: topicNameInput.value });
  });

  return {
    getFile: () => currentFile,
    clearFile
  };
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
