// ============================================================
// dom.js — 轻量级 DOM 辅助函数
// ============================================================

/**
 * 快捷选择单个元素
 */
export function qs(selector, parent = document) {
  return parent.querySelector(selector);
}

/**
 * 快捷选择多个元素
 */
export function qsa(selector, parent = document) {
  return Array.from(parent.querySelectorAll(selector));
}

/**
 * 创建元素并设置属性
 * @param {string} tag - HTML标签名
 * @param {Object} attrs - 属性键值对
 * @param {(string|Node|Node[])} [children] - 子内容
 * @returns {HTMLElement}
 */
export function createElement(tag, attrs = {}, children = null) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (key === 'className') {
      el.className = value;
    } else if (key === 'dataset') {
      Object.assign(el.dataset, value);
    } else if (key.startsWith('on') && typeof value === 'function') {
      el.addEventListener(key.slice(2).toLowerCase(), value);
    } else if (key === 'style' && typeof value === 'object') {
      Object.assign(el.style, value);
    } else if (key === 'html') {
      el.innerHTML = value;
    } else {
      el.setAttribute(key, value);
    }
  }
  if (children) {
    if (Array.isArray(children)) {
      children.forEach(child => {
        if (typeof child === 'string') {
          el.appendChild(document.createTextNode(child));
        } else if (child instanceof Node) {
          el.appendChild(child);
        }
      });
    } else if (typeof children === 'string') {
      el.textContent = children;
    } else if (children instanceof Node) {
      el.appendChild(children);
    }
  }
  return el;
}

/**
 * 事件委托辅助
 * @param {HTMLElement} parent - 父元素
 * @param {string} selector - CSS选择器
 * @param {string} eventType - 事件类型
 * @param {Function} handler - 处理函数
 */
export function delegate(parent, selector, eventType, handler) {
  parent.addEventListener(eventType, function(e) {
    const target = e.target.closest(selector);
    if (target && parent.contains(target)) {
      handler.call(target, e);
    }
  });
}
