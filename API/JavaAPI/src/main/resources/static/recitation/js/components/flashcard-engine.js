// ============================================================
// flashcard-engine.js — 记忆卡片模式
// ============================================================

import { qs } from '../utils/dom.js';
import { TEST_TYPE_LABELS } from '../config.js';

export function mountFlashcardEngine(store) {
  const overlay = qs('#flashcardOverlay');
  const summary = qs('#flashcardSummary');
  const container = qs('#flashcardContainer');
  const stage = qs('#flashcardStage');
  const card = qs('#flashcardCard');

  // UI 元素
  const elProgress = qs('#flashcardProgress');
  const elProgressFill = qs('#flashcardProgressFill');
  const elCardTypeBadge = qs('#cardTypeBadge');
  const elQuestion = qs('#cardQuestion');
  const elAnswer = qs('#cardAnswer');
  const elHintWrap = qs('#cardHintWrap');
  const elHint = qs('#cardHint');

  let deck = [];
  let currentIndex = 0;
  let shuffled = false;
  let history = [];

  // =========== 打开/关闭 ===========

  function open() {
    const entry = store.get('handbookEntry');
    if (!entry || !entry.content || !entry.content.selfTest || entry.content.selfTest.length === 0) {
      alert('当前手册没有自测题，无法使用记忆卡片模式');
      return;
    }

    deck = [...entry.content.selfTest];
    currentIndex = 0;
    shuffled = false;
    history = [];

    overlay.style.display = 'flex';
    summary.style.display = 'none';
    container.style.display = '';

    store.setState({ isFlashcardMode: true });

    renderCard();
    updateProgress();
    bindKeyboard();
  }

  function close() {
    overlay.style.display = 'none';
    store.setState({ isFlashcardMode: false });
    unbindKeyboard();
  }

  // =========== 卡片渲染 ===========

  function renderCard() {
    if (currentIndex >= deck.length) {
      showSummary();
      return;
    }

    const item = deck[currentIndex];
    if (!item) return;

    elCardTypeBadge.textContent = TEST_TYPE_LABELS[item.type] || item.type;
    elQuestion.textContent = item.question;
    elAnswer.textContent = item.answer;

    if (item.hint) {
      elHintWrap.style.display = '';
      elHint.textContent = item.hint;
    } else {
      elHintWrap.style.display = 'none';
    }

    card.classList.remove('flipped');
    updateProgress();
  }

  function flipCard() {
    card.classList.toggle('flipped');
  }

  function updateProgress() {
    const total = deck.length;
    elProgress.textContent = `${currentIndex + 1} / ${total}`;
    const pct = total > 0 ? ((currentIndex + 1) / total) * 100 : 0;
    elProgressFill.style.width = `${pct}%`;
  }

  // =========== 卡片操作 ===========

  function markKnown() {
    if (currentIndex >= deck.length) return;
    history.push({ id: deck[currentIndex].id, known: true });
    currentIndex++;
    renderCard();
    checkComplete();
  }

  function markAgain() {
    if (currentIndex >= deck.length) return;
    const card = deck[currentIndex];
    history.push({ id: card.id, known: false });
    // 放到末尾
    deck.push(deck.splice(currentIndex, 1)[0]);
    renderCard();
  }

  function goNext() {
    if (currentIndex < deck.length - 1) {
      currentIndex++;
      renderCard();
    }
  }

  function goPrev() {
    if (currentIndex > 0) {
      currentIndex--;
      renderCard();
    }
  }

  function shuffleDeck() {
    for (let i = deck.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [deck[i], deck[j]] = [deck[j], deck[i]];
    }
    currentIndex = 0;
    shuffled = true;
    renderCard();
  }

  function resetDeck() {
    const entry = store.get('handbookEntry');
    if (entry?.content?.selfTest) {
      deck = [...entry.content.selfTest];
    }
    currentIndex = 0;
    history = [];
    shuffled = false;
    renderCard();
    summary.style.display = 'none';
    container.style.display = '';
  }

  function checkComplete() {
    if (currentIndex >= deck.length) {
      showSummary();
    }
  }

  // =========== 统计总结 ===========

  function showSummary() {
    container.style.display = 'none';
    summary.style.display = '';

    const known = history.filter(h => h.known).length;
    const again = history.filter(h => !h.known).length;
    const total = history.length;
    const pct = total > 0 ? Math.round((known / total) * 100) : 0;

    qs('#statKnown').textContent = known;
    qs('#statAgain').textContent = again;
    qs('#statTotal').textContent = total;
    qs('#ringPercent').textContent = `${pct}%`;

    // 更新圆环
    const circumference = 2 * Math.PI * 52; // r=52
    const offset = circumference - (pct / 100) * circumference;
    const ringFill = qs('#ringFill');
    ringFill.style.strokeDasharray = `${circumference}`;
    ringFill.style.strokeDashoffset = `${offset}`;
  }

  function retryWeak() {
    const weakIds = history.filter(h => !h.known).map(h => h.id);
    const entry = store.get('handbookEntry');
    if (!entry?.content?.selfTest) return;
    deck = entry.content.selfTest.filter(item => weakIds.includes(item.id));
    if (deck.length === 0) {
      alert('所有卡片都已掌握！');
      return;
    }
    currentIndex = 0;
    history = [];
    summary.style.display = 'none';
    container.style.display = '';
    renderCard();
  }

  function retryAll() {
    resetDeck();
  }

  // =========== 键盘事件 ===========

  function handleKeyDown(e) {
    if (!store.get('isFlashcardMode')) return;

    switch (e.key) {
      case ' ':
      case 'Enter':
        e.preventDefault();
        flipCard();
        break;
      case 'ArrowRight':
        e.preventDefault();
        if (summary.style.display === 'none') goNext();
        break;
      case 'ArrowLeft':
        e.preventDefault();
        if (summary.style.display === 'none') goPrev();
        break;
      case 'y':
      case 'Y':
        e.preventDefault();
        if (summary.style.display === 'none') markKnown();
        break;
      case 'n':
      case 'N':
        e.preventDefault();
        if (summary.style.display === 'none') markAgain();
        break;
      case 's':
      case 'S':
        if (!e.ctrlKey && !e.metaKey && summary.style.display === 'none') {
          e.preventDefault();
          shuffleDeck();
        }
        break;
      case 'Escape':
        e.preventDefault();
        close();
        break;
    }
  }

  function bindKeyboard() {
    document.addEventListener('keydown', handleKeyDown);
  }

  function unbindKeyboard() {
    document.removeEventListener('keydown', handleKeyDown);
  }

  // =========== 事件绑定 ===========

  // 打开
  qs('#btnFlashcardMode').addEventListener('click', open);

  // 关闭
  qs('#btnCloseFlashcard').addEventListener('click', close);
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) close();
  });

  // 翻转卡片
  card.addEventListener('click', flipCard);

  // 控制按钮
  qs('#btnKnown').addEventListener('click', markKnown);
  qs('#btnAgain').addEventListener('click', markAgain);
  qs('#btnNextCard').addEventListener('click', goNext);
  qs('#btnPrevCard').addEventListener('click', goPrev);
  qs('#btnShuffle').addEventListener('click', shuffleDeck);
  qs('#btnResetFlashcard').addEventListener('click', resetDeck);

  // 总结按钮
  qs('#btnRetryWeak').addEventListener('click', retryWeak);
  qs('#btnRetryAll').addEventListener('click', retryAll);
  qs('#btnExitSummary').addEventListener('click', close);

  // 触摸滑动支持
  let touchStartX = 0;
  card.addEventListener('touchstart', (e) => {
    touchStartX = e.touches[0].clientX;
  });

  card.addEventListener('touchend', (e) => {
    const deltaX = e.changedTouches[0].clientX - touchStartX;
    if (Math.abs(deltaX) > 60) {
      if (deltaX > 0) {
        markKnown();
      } else {
        markAgain();
      }
    }
  });
}
