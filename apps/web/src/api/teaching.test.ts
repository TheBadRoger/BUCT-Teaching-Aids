// 保持现有 Express API 请求协议，并隔离 Java 的响应封装。
import { afterEach, describe, expect, it, vi } from 'vitest'
import { generateKnowledge, getTeachingCatalog, getTeachingHealth, streamLesson } from './teaching'
import type { TeachingInput } from '@/types/teaching'

const input: TeachingInput = { subject: '数学', grade: '八年级', topic: '一次函数', focus: '', lessonType: '新授课', duration: 45, teachingStyle: '', extraRequirements: '' }
afterEach(() => vi.unstubAllGlobals())

describe('existing teacher service contract', () => {
  it('posts generation settings to the isolated teacher API namespace', async () => {
    const fetcher = vi.fn().mockResolvedValue(Response.json({ success: true, data: { chapterTitle: '一次函数', knowledgePoints: [] } }))
    vi.stubGlobal('fetch', fetcher)
    const controller = new AbortController()
    await generateKnowledge(input, controller.signal)
    const [url, config] = fetcher.mock.calls[0]!
    expect(url).toBe('/api/teacher-ai/knowledge/generate')
    expect(config.method).toBe('POST')
    expect(JSON.parse(config.body)).toEqual({ subject: input.subject, grade: input.grade, topic: input.topic, focus: input.focus })
    expect(config.signal).toBe(controller.signal)
  })

  it('surfaces the service error from a failed generation', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ success: false, error: '缺少必填参数' }, { status: 400 })))
    await expect(generateKnowledge(input, new AbortController().signal)).rejects.toThrow('缺少必填参数')
  })

  it('rejects the Java response envelope on the teacher service', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ code: 2000, data: {} })))
    await expect(generateKnowledge(input, new AbortController().signal)).rejects.toThrow('请求失败')
  })

  it('validates catalog entries and recognizes an unconfigured AI service', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(Response.json({ success: true, data: { subjects: [null, { name: '数学' }], grades: [1, '八年级'], lessonTypes: ['新授课'] } }))
      .mockResolvedValueOnce(Response.json({ status: 'ok', hasApiKey: false, model: 'deepseek-chat' })))
    expect(await getTeachingCatalog(new AbortController().signal)).toEqual({ subjects: [{ name: '数学' }], grades: ['八年级'], lessonTypes: ['新授课'] })
    expect(await getTeachingHealth(new AbortController().signal)).toEqual({ ready: false, model: 'deepseek-chat' })
  })

  it('uses the existing POST stream endpoint and forwards incremental content', async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response('data: {"type":"chunk","content":"教案"}\n\ndata: {"type":"done"}\n\n', { headers: { 'content-type': 'text/event-stream' } }))
    vi.stubGlobal('fetch', fetcher)
    const onChunk = vi.fn()
    await streamLesson(input, new AbortController().signal, onChunk)
    expect(fetcher.mock.calls[0]?.[0]).toBe('/api/teacher-ai/lesson-plan/generate-stream')
    expect(JSON.parse(fetcher.mock.calls[0]?.[1].body)).not.toHaveProperty('focus')
    expect(onChunk).toHaveBeenCalledExactlyOnceWith('教案')
  })
})
