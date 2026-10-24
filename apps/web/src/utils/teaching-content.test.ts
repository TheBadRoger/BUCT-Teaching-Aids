// 验证模型不同输出格式的兼容与完整文本导出。
import { describe, expect, it } from 'vitest'
import { knowledgeText, lessonText, normalizeKnowledge, normalizeLesson, parseLessonText } from './teaching-content'

describe('teaching content', () => {
  it('rejects responses missing the core knowledge structure', () => {
    expect(() => normalizeKnowledge({ chapterTitle: '浮力' })).toThrow()
    expect(() => normalizeKnowledge(null)).toThrow()
  })

  it('preserves explanation, concepts, mistakes and difficulty in exports', () => {
    const result = normalizeKnowledge({ chapterTitle: '浮力', overview: '概述', knowledgePoints: [{ name: '阿基米德原理', explanation: '<b>讲解</b>', difficulty: 'hard', importance: 'required', keyConcepts: ['排开液体'], commonMistakes: ['误区'] }] })
    const output = knowledgeText(result)
    for (const text of ['阿基米德原理', '<b>讲解</b>', '困难', '必学', '排开液体', '误区']) expect(output).toContain(text)
  })

  it('normalizes string and object variants without dropping lesson sections', () => {
    const lesson = normalizeLesson({
      title: '浮力', duration: '45', teachingObjectives: [{ category: 'skill', content: '实验技能' }],
      keyPoints: [{ point: '重点' }], difficultPoints: [{ point: '难点', strategy: '突破策略' }],
      teachingResources: ['烧杯', '水'], teachingProcess: [{ stageName: '实验', duration: '10', teacherActivity: '示范', studentActivity: '操作', designIntent: '理解原理' }],
      boardDesign: '板书', homework: { basic: '基础题', improve: ['提升题'], challenge: ['挑战题'] }, teachingReflection: '反思',
    })
    expect(lesson.duration).toBe(45)
    const output = lessonText(lesson)
    for (const text of ['实验技能', '突破策略', '烧杯', '示范', '操作', '理解原理', '板书', '基础题', '提升题', '挑战题', '反思']) expect(output).toContain(text)
  })

  it('parses fenced JSON and rejects incomplete output', () => {
    expect(parseLessonText('```json\n{"title":"教案"}\n```').title).toBe('教案')
    expect(() => parseLessonText('{"title":')).toThrow('完整 JSON')
  })

  it('handles invalid optional fields without a render-time crash', () => {
    const data = normalizeLesson({ title: '教案', duration: 'not-a-number', teachingProcess: [null], homework: null })
    expect(data.duration).toBe(0)
    expect(data.homework.basic).toEqual([])
    expect(data.teachingProcess[0]?.stageName).toBe('')
  })
})
