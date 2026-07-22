/**
 * 知识点手册 API 路由
 */

const { Router } = require('express');
const { generateKnowledgePoints, DeepSeekError } = require('../services/deepseek');

const router = Router();

/**
 * POST /api/knowledge/generate
 *
 * 请求体:
 * {
 *   "subject": "数学",
 *   "grade": "八年级",
 *   "topic": "一次函数",
 *   "focus": "函数图像的性质"   // 可选
 * }
 *
 * 返回:
 * {
 *   "success": true,
 *   "data": {
 *     "chapterTitle": "...",
 *     "overview": "...",
 *     "knowledgePoints": [...]
 *   }
 * }
 */
router.post('/generate', async (req, res) => {
  try {
    const { subject, grade, topic, focus } = req.body;

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

    const data = await generateKnowledgePoints({ subject, grade, topic, focus });

    return res.json({
      success: true,
      data,
    });
  } catch (err) {
    console.error('[知识点生成] 错误:', err.message);

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
 * GET /api/knowledge/subjects
 * 返回预设的学科/年级/教材版本列表（供前端下拉框使用）
 */
router.get('/subjects', (_req, res) => {
  const catalog = {
    grades: ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级', '七年级', '八年级', '九年级', '高一', '高二', '高三'],
    subjects: [
      { name: '语文', versions: ['统编版', '人教版'] },
      { name: '数学', versions: ['人教版', '北师大版', '苏教版', '沪教版'] },
      { name: '英语', versions: ['人教版', '外研版', '牛津版', '北师大版'] },
      { name: '物理', versions: ['人教版', '沪科版', '教科版', '北师大版'] },
      { name: '化学', versions: ['人教版', '沪教版', '鲁科版'] },
      { name: '生物', versions: ['人教版', '北师大版', '苏教版'] },
      { name: '历史', versions: ['统编版', '人教版'] },
      { name: '地理', versions: ['人教版', '湘教版', '中图版'] },
      { name: '道德与法治', versions: ['统编版'] },
      { name: '科学', versions: ['教科版', '苏教版'] },
    ],
    lessonTypes: ['新授课', '复习课', '习题课', '实验课', '讲评课', '活动课'],
  };

  // 按学段分组年级
  catalog.gradeGroups = {
    小学: catalog.grades.slice(0, 6),
    初中: catalog.grades.slice(6, 9),
    高中: catalog.grades.slice(9, 12),
  };

  return res.json({ success: true, data: catalog });
});

module.exports = router;
