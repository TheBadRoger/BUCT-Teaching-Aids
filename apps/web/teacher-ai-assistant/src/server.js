/**
 * 教师AI全能助手 - 服务端入口
 * ────────────────────────────────────────────
 * 提供：
 *   - 静态文件服务（前端页面）
 *   - 知识点手册生成 API
 *   - 智能教案生成 API（含流式 SSE）
 *   - DeepSeek API 代理
 */

require('dotenv').config();

const express = require('express');
const cors = require('cors');
const path = require('path');

const knowledgeRoutes = require('./routes/knowledge');
const lessonPlanRoutes = require('./routes/lesson-plan');

// ============================================================
//  Express 应用配置
// ============================================================

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(cors());
app.use(express.json({ limit: '1mb' }));

// 静态文件 — public 目录
app.use(express.static(path.join(__dirname, '..', 'public')));

// ============================================================
//  API 路由
// ============================================================

app.use('/api/knowledge', knowledgeRoutes);
app.use('/api/lesson-plan', lessonPlanRoutes);

// 健康检查
app.get('/api/health', (_req, res) => {
  res.json({
    status: 'ok',
    model: process.env.DEEPSEEK_MODEL || 'deepseek-chat',
    hasApiKey: !!process.env.DEEPSEEK_API_KEY,
  });
});

// ============================================================
//  SPA 回退 — 所有非 API 请求返回 index.html
// ============================================================

app.get('*', (_req, res) => {
  res.sendFile(path.join(__dirname, '..', 'public', 'index.html'));
});

// ============================================================
//  全局错误处理
// ============================================================

app.use((err, _req, res, _next) => {
  console.error('[服务器] 未捕获错误:', err);
  res.status(500).json({
    success: false,
    error: '服务器内部错误',
  });
});

// ============================================================
//  启动
// ============================================================

app.listen(PORT, () => {
  console.log('');
  console.log('  ╔══════════════════════════════════════════╗');
  console.log('  ║      📚 教师AI全能助手 已启动             ║');
  console.log(`  ║      地址: http://localhost:${PORT}          ║`);
  console.log('  ║                                          ║');
  console.log('  ║  功能:                                    ║');
  console.log('  ║    📖 知识点手册生成                      ║');
  console.log('  ║    📝 智能教案生成                        ║');
  console.log('  ║                                          ║');
  console.log(`  ║  AI模型: ${(process.env.DEEPSEEK_MODEL || 'deepseek-chat').padEnd(32)}║`);
  console.log(`  ║  API配置: ${(process.env.DEEPSEEK_API_KEY ? '✅ 已配置' : '❌ 未配置').padEnd(32)}║`);
  console.log('  ╚══════════════════════════════════════════╝');
  console.log('');
});
