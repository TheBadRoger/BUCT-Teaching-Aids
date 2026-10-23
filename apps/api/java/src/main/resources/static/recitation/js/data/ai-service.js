// ============================================================
// ai-service.js — AI 服务抽象层
// ============================================================
// 调用本系统后端接口生成背诵手册（后端内部再调用 polymas AI）。
// 前端不接触任何 AI 密钥，直接上传文件即可。

/** 请求超时时间（毫秒）。AI 生成较慢，给足 180s */
const REQUEST_TIMEOUT_MS = 180000;

/** 成功状态码（与后端 ApiResponse 约定一致） */
const SUCCESS_CODE = 2000;

/**
 * 调用后端接口生成背诵手册
 * @param {File} file - 用户上传的文件（.txt / .docx / .pdf）
 * @param {string} [topicName] - 可选的知识点名称
 * @param {Function} [onProgress] - 进度回调 (step: string) => void
 * @returns {Promise<Object>} HandbookEntry
 */
export async function generateHandbook(file, topicName = '', onProgress = null) {
  if (!file) {
    throw new Error('请先上传文件');
  }

  const formData = new FormData();
  formData.append('file', file);
  if (topicName && topicName.trim()) {
    formData.append('topicName', topicName.trim());
  }

  if (onProgress) onProgress('analyze');

  // 超时控制
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  if (onProgress) onProgress('generate');

  let response;
  try {
    response = await fetch('/api/handbook/generate', {
      method: 'POST',
      body: formData,
      signal: controller.signal
    });
  } catch (err) {
    clearTimeout(timer);
    if (err.name === 'AbortError') {
      throw new Error('生成超时，请稍后重试');
    }
    throw new Error('网络连接失败，请检查网络后重试');
  }
  clearTimeout(timer);

  // 后端返回标准 ApiResponse: { code, msg, timestamp, data }
  let body;
  try {
    body = await response.json();
  } catch (e) {
    throw new Error('服务返回格式异常，请稍后重试');
  }

  if (!response.ok || body.code !== SUCCESS_CODE) {
    const msg = (body && body.msg) ? body.msg : `请求失败 (${response.status})`;
    throw new Error(msg);
  }

  const entry = body.data;
  if (!entry || !entry.content) {
    throw new Error('AI 返回内容为空，请重试');
  }

  return entry;
}
