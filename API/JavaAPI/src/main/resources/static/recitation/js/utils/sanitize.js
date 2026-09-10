// ============================================================
// sanitize.js — 基础 HTML 安全处理
// ============================================================

/** 允许的安全标签 */
const ALLOWED_TAGS = new Set([
  'h3', 'h4', 'p', 'ul', 'ol', 'li', 'strong', 'em', 'br',
  'span', 'div', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'mark', 'code', 'pre', 'blockquote', 'sub', 'sup'
]);

/** 允许的安全属性 */
const ALLOWED_ATTRS = new Set(['class', 'id']);

/**
 * 基本HTML净化 (仅允许白名单标签和属性)
 * 用于渲染模拟数据库中的HTML内容
 */
export function sanitizeHTML(html) {
  if (!html) return '';

  // 创建临时容器
  const temp = document.createElement('div');
  temp.innerHTML = html;

  // 递归清理节点
  cleanNode(temp);

  return temp.innerHTML;
}

function cleanNode(node) {
  const children = Array.from(node.childNodes);
  for (const child of children) {
    if (child.nodeType === Node.ELEMENT_NODE) {
      const tagName = child.tagName.toLowerCase();
      if (!ALLOWED_TAGS.has(tagName)) {
        // 移除不允许的标签，保留文本内容
        while (child.firstChild) {
          node.insertBefore(child.firstChild, child);
        }
        node.removeChild(child);
      } else {
        // 清理属性
        const attrs = Array.from(child.attributes);
        for (const attr of attrs) {
          if (!ALLOWED_ATTRS.has(attr.name)) {
            child.removeAttribute(attr.name);
          }
        }
        // 递归清理子节点
        cleanNode(child);
      }
    }
  }
}

/**
 * 转义HTML特殊字符（用于纯文本安全插入）
 */
export function escapeHTML(str) {
  if (!str) return '';
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  };
  return String(str).replace(/[&<>"']/g, c => map[c]);
}
