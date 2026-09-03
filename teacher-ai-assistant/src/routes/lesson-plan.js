/**
 * 智能教案 API 路由
 */

const { Router } = require('express');
const { generateLessonPlan, streamChat, DeepSeekError } = require('../services/deepseek');

const router = Router();

/**
 * POST /api/lesson-plan/generate
 *
 * 请求体:
 * {
 *   "subject": "物理",
 *   "grade": "初三",
 *   "topic": "浮力",
 *   "lessonType": "新授课",        // 可选，默认 "新授课"
 *   "duration": 45,                // 可选，默认 45
 *   "teachingStyle": "探究式教学",   // 可选
 *   "extraRequirements": "要有实验环节" // 可选
 * }
 *
 * 返回:
 * {
 *   "success": true,
 *   "data": {
 *     "title": "...",
 *     "lessonType": "...",
 *     "teachingObjectives": [...],
 *     "keyPoints": [...],
 *     "difficultPoints": [...],
 *     "teachingResources": "...",
 *     "teachingProcess": [...],
 *     "boardDesign": "...",
 *     "homework": {...},
 *     "teachingReflection": "..."
 *   }
 * }
 */
router.post('/generate', async (req, res) => {
  try {
    const { subject, grade, topic, lessonType, duration, teachingStyle, extraRequirements } = req.body;

    // 参数校验
    const missing = [];
    if (!subject) missing.push('subject');
    if (!grade) missing.push('grade');
    if (!topic) missing.push('topic');

    if (missing.length > 0) {
      return res.status(400).json({
        success: false,
        error: `缺少必填参数: ${missing.join(', ')}`,
      });
    }

    const data = await generateLessonPlan({
      subject,
      grade,
      topic,
      lessonType,
      duration,
      teachingStyle,
      extraRequirements,
    });

    return res.json({
      success: true,
      data,
    });
  } catch (err) {
    console.error('[教案生成] 错误:', err.message);

    if (err instanceof DeepSeekError) {
      return res.status(err.statusCode || 500).json({
        success: false,
        error: err.message,
        type: err.type,
      });
    }

    return res.status(500).json({
      success: false,
      error: '服务内部错误，请稍后重试',
    });
  }
});

/**
 * POST /api/lesson-plan/generate-stream
 *
 * 流式生成教案（SSE），请求体同 /generate
 * 事件类型:
 *   - data: 增量文本
 *   - done: 生成完毕
 *   - error: 出错
 */
router.post('/generate-stream', (req, res) => {
  const { subject, grade, topic, lessonType, duration, teachingStyle, extraRequirements } = req.body;

  const missing = [];
  if (!subject) missing.push('subject');
  if (!grade) missing.push('grade');
  if (!topic) missing.push('topic');

  if (missing.length > 0) {
    res.status(400).json({
      success: false,
      error: `缺少必填参数: ${missing.join(', ')}`,
    });
    return;
  }

  // 设置 SSE headers
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no',
  });

  const systemPrompt = `你是一位经验丰富的${subject}教师，具有20年以上教龄。请根据提供的信息，生成一份专业、完整的教案。

要求：
1. 用 JSON 格式输出
2. 包含以下字段：
   - title（课题名称）
   - lessonType（课型）
   - duration（课时/分钟）
   - teachingObjectives（教学目标数组，每项包含 category: knowledge/skill/emotion 和 content）
   - keyPoints（教学重点数组）
   - difficultPoints（教学难点数组，每项包含 point 和 strategy/突破策略）
   - teachingResources（教学资源/教具准备）
   - teachingProcess（教学过程数组）
   - boardDesign（板书设计）
   - homework（分层作业）
   - teachingReflection（教学反思）
3. 教学内容专业、准确
4. 教学过程以学生为中心
5. 时间分配合理`;

  let userPrompt = `学科：${subject}\n年级：${grade}\n课题：${topic}\n课型：${lessonType || '新授课'}\n课时：${duration || 45}分钟`;
  if (teachingStyle) userPrompt += `\n教学风格偏好：${teachingStyle}`;
  if (extraRequirements) userPrompt += `\n额外要求：${extraRequirements}`;

  const messages = [
    { role: 'system', content: systemPrompt },
    { role: 'user', content: userPrompt },
  ];

  streamChat(
    messages,
    { temperature: 0.5, maxTokens: 8192 },
    (chunk) => {
      res.write(`data: ${JSON.stringify({ type: 'chunk', content: chunk })}\n\n`);
    },
    () => {
      res.write(`data: ${JSON.stringify({ type: 'done' })}\n\n`);
      res.end();
    },
    (err) => {
      res.write(`data: ${JSON.stringify({ type: 'error', message: err.message })}\n\n`);
      res.end();
    }
  );
});

module.exports = router;
