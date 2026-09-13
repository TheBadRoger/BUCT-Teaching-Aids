// ============================================================
// store.js — 发布/订阅式状态管理
// ============================================================

/**
 * 创建一个简易响应式 Store
 * 组件通过 subscribe 监听变化，通过 setState 更新状态
 */
export function createStore(initialState = {}) {
  let state = { ...initialState };
  const listeners = new Set();

  /**
   * 获取当前全部状态（返回浅拷贝）
   */
  function getState() {
    return { ...state };
  }

  /**
   * 部分更新状态，并通知所有监听器
   * @param {Object} partial - 要合并的状态片段
   */
  function setState(partial) {
    const prevState = { ...state };
    const changedKeys = [];

    for (const key of Object.keys(partial)) {
      if (state[key] !== partial[key]) {
        changedKeys.push(key);
      }
    }

    if (changedKeys.length === 0) return;

    state = { ...state, ...partial };

    for (const listener of listeners) {
      try {
        listener(state, changedKeys, prevState);
      } catch (err) {
        console.error('[Store] Listener error:', err);
      }
    }
  }

  /**
   * 订阅状态变化
   * @param {Function} callback - (state, changedKeys, prevState) => void
   * @returns {Function} unsubscribe function
   */
  function subscribe(callback) {
    listeners.add(callback);
    return () => listeners.delete(callback);
  }

  /**
   * 获取单个状态值
   */
  function get(key) {
    return state[key];
  }

  return { getState, setState, subscribe, get };
}

// ---- 初始应用状态 ----

export const initialState = {
  // 选择状态
  selectedSubject: null,
  selectedGrade: null,
  selectedTopic: null,
  customTopic: '',

  // 数据状态
  handbookEntry: null,
  isLoading: false,
  error: null,
  suggestions: null,

  // 视图状态
  activeTab: 'summary',
  isFlashcardMode: false,
  isSidebarOpen: true,

  // 记忆卡片状态
  flashcardIndex: 0,
  flashcardShuffled: false,
  flashcardDeck: [],
  flashcardHistory: [],  // [{ cardId, known: bool }]
};
