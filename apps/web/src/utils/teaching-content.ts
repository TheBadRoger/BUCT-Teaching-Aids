// AI 返回内容先规范化为文本和结构化字段，再通过 Vue 插值展示。
import type { KnowledgeHandbook, LessonPlan } from '@/types/teaching'

/** 将未知响应收窄为记录。 */
function record(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

/** 可选内容只接受文本；数组转换用于资源和作业的兼容格式。 */
function text(value: unknown): string { return typeof value === 'string' ? value : '' }
function texts(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : typeof value === 'string' ? [value] : []
}
function list(value: unknown): unknown[] { return Array.isArray(value) ? value : [] }
function minutes(value: unknown): number {
  const number = typeof value === 'number' || typeof value === 'string' ? Number(value) : 0
  return Number.isFinite(number) && number >= 0 ? number : 0
}

/** 校验知识点响应的必需字段，并补齐可选字段。 */
export function normalizeKnowledge(value: unknown): KnowledgeHandbook {
  const data = record(value)
  if (!text(data.chapterTitle).trim() || !Array.isArray(data.knowledgePoints)) throw new Error('知识点结果格式不完整，请重试')
  return {
    chapterTitle: text(data.chapterTitle), overview: text(data.overview),
    knowledgePoints: data.knowledgePoints.map(item => {
      const point = record(item)
      return {
        name: text(point.name), explanation: text(point.explanation),
        difficulty: text(point.difficulty), importance: text(point.importance),
        keyConcepts: texts(point.keyConcepts), commonMistakes: texts(point.commonMistakes),
      }
    }),
  }
}

/** 兼容模型返回的字符串和对象式重点、难点、分层作业。 */
export function normalizeLesson(value: unknown): LessonPlan {
  const data = record(value)
  if (!text(data.title).trim()) throw new Error('教案结果缺少课题标题，请重试')
  const homework = record(data.homework)
  return {
    title: text(data.title), lessonType: text(data.lessonType), duration: minutes(data.duration),
    teachingObjectives: list(data.teachingObjectives).map(item => {
      const objective = record(item)
      return { category: text(objective.category), content: typeof item === 'string' ? item : text(objective.content) }
    }),
    keyPoints: list(data.keyPoints).map(item => typeof item === 'string' ? item : text(record(item).point)),
    difficultPoints: list(data.difficultPoints).map(item => ({ point: typeof item === 'string' ? item : text(record(item).point), strategy: text(record(item).strategy) })),
    teachingResources: texts(data.teachingResources).join('\n'),
    teachingProcess: list(data.teachingProcess).map(item => {
      const stage = record(item)
      return { stageName: text(stage.stageName), duration: minutes(stage.duration), teacherActivity: text(stage.teacherActivity), studentActivity: text(stage.studentActivity), designIntent: text(stage.designIntent) }
    }),
    boardDesign: text(data.boardDesign),
    homework: { basic: texts(homework.basic), improve: texts(homework.improve), challenge: texts(homework.challenge) },
    teachingReflection: text(data.teachingReflection),
  }
}

/** 解析流式模型输出，兼容外围 Markdown JSON 代码围栏。 */
export function parseLessonText(content: string): LessonPlan {
  const json = content.trim().replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '')
  let value: unknown
  try { value = JSON.parse(json) } catch { throw new Error('教案未返回完整 JSON，可复制生成原文后重试') }
  return normalizeLesson(value)
}

export const difficultyLabels: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
export const importanceLabels: Record<string, string> = { required: '必学', extended: '拓展', enrichment: '选学' }
export const objectiveLabels: Record<string, string> = { knowledge: '知识与技能', skill: '过程与方法', emotion: '情感态度价值观' }

/** 导出保留完整教学内容，避免导出结果仅剩标题或摘要。 */
export function knowledgeText(data: KnowledgeHandbook): string {
  return [data.chapterTitle, data.overview, ...data.knowledgePoints.map((point, index) => [
    `${index + 1}. ${point.name}`, `难度：${difficultyLabels[point.difficulty] || point.difficulty}；重要性：${importanceLabels[point.importance] || point.importance}`,
    point.explanation, '核心概念', ...point.keyConcepts, '常见误区', ...point.commonMistakes,
  ].join('\n'))].join('\n\n')
}

/** 输出各教学环节、设计意图和三个层次的作业。 */
export function lessonText(data: LessonPlan): string {
  return [data.title, `课型：${data.lessonType}；课时：${data.duration} 分钟`,
    '教学目标', ...data.teachingObjectives.map(item => `${objectiveLabels[item.category] || item.category}：${item.content}`),
    '教学重点', ...data.keyPoints, '教学难点', ...data.difficultPoints.map(item => `${item.point}\n突破策略：${item.strategy}`),
    '教学资源', data.teachingResources, '教学过程', ...data.teachingProcess.map(stage => `${stage.stageName}（${stage.duration} 分钟）\n教师活动：${stage.teacherActivity}\n学生活动：${stage.studentActivity}\n设计意图：${stage.designIntent}`),
    '板书设计', data.boardDesign, '基础巩固', ...data.homework.basic, '能力提升', ...data.homework.improve,
    '挑战拓展', ...data.homework.challenge, '教学反思', data.teachingReflection,
  ].join('\n\n')
}
