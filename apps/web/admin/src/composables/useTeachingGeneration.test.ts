import { beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, ref } from 'vue'
import type { KnowledgeHandbook, TeachingInput } from '@/types/teaching'
import { generateKnowledge, streamLesson } from '@/api/teaching'
import { useTeachingGeneration } from './useTeachingGeneration'

vi.mock('@/api/teaching', () => ({ generateKnowledge: vi.fn(), generateLesson: vi.fn(), streamLesson: vi.fn() }))
const handbook = (title: string): KnowledgeHandbook => ({ chapterTitle: title, overview: '', knowledgePoints: [] })
function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(yes => { resolve = yes })
  return { promise, resolve }
}
function setup(validate = vi.fn(async () => true)) {
  const scope = effectScope()
  const isKnowledge = ref(true)
  const form = ref<TeachingInput>({ subject: ' 数学 ', grade: ' 高一 ', topic: ' 函数 ', focus: '', lessonType: '新授课', duration: 45, teachingStyle: '', extraRequirements: '' })
  const state = scope.run(() => useTeachingGeneration({ formRef: ref({ validate }), form, isKnowledge, useStream: ref(true) }))!
  return { state, scope, form, isKnowledge }
}

describe('teaching generation lifecycle', () => {
  beforeEach(() => vi.resetAllMocks())

  it('blocks double submission and cancels pending validation on route change', async () => {
    const validation = deferred<boolean>()
    const validate = vi.fn(() => validation.promise)
    const { state, scope, isKnowledge } = setup(validate)
    const pending = state.generate()
    await state.generate()
    expect(validate).toHaveBeenCalledOnce()
    isKnowledge.value = false
    validation.resolve(true)
    await pending
    expect(generateKnowledge).not.toHaveBeenCalled()
    expect(streamLesson).not.toHaveBeenCalled()
    expect(state.busy.value).toBe(false)
    expect(state.notice.value).toBe('')
    scope.stop()
  })

  it('does not start a request after unmounting during validation', async () => {
    const validation = deferred<boolean>()
    const { state, scope } = setup(vi.fn(() => validation.promise))
    const pending = state.generate()
    scope.stop()
    validation.resolve(true)
    await pending
    expect(generateKnowledge).not.toHaveBeenCalled()
  })

  it('ignores late results and completion from a canceled request while retrying', async () => {
    const old = deferred<KnowledgeHandbook>()
    const latest = deferred<KnowledgeHandbook>()
    vi.mocked(generateKnowledge).mockReturnValueOnce(old.promise).mockReturnValueOnce(latest.promise)
    const { state, scope } = setup()
    const first = state.generate()
    await Promise.resolve()
    await Promise.resolve()
    state.cancel()
    expect(vi.mocked(generateKnowledge).mock.calls[0]![1].aborted).toBe(true)
    expect(state.notice.value).toContain('已停止')
    const second = state.generate()
    old.resolve(handbook('旧结果'))
    await first
    expect(state.handbook.value).toBeNull()
    expect(state.busy.value).toBe(true)
    latest.resolve(handbook('新结果'))
    await second
    expect(state.handbook.value?.chapterTitle).toBe('新结果')
    expect(state.busy.value).toBe(false)
    scope.stop()
  })

  it('retains partial streamed text on cancellation and ignores later chunks', async () => {
    const stream = deferred<void>()
    let chunk!: (text: string) => void
    vi.mocked(streamLesson).mockImplementation((_input, _signal, onChunk) => { chunk = onChunk; return stream.promise })
    const { state, scope, isKnowledge } = setup()
    isKnowledge.value = false
    const pending = state.generate()
    await Promise.resolve()
    await Promise.resolve()
    chunk('部分内容')
    state.cancel()
    chunk('迟到内容')
    stream.resolve()
    await pending
    expect(state.raw.value).toBe('部分内容')
    expect(state.lesson.value).toBeNull()
    expect(state.error.value).toBe('')
    scope.stop()
  })

  it('keeps input after an error and retries with trimmed fields', async () => {
    vi.mocked(generateKnowledge).mockRejectedValueOnce(new Error('服务繁忙')).mockResolvedValueOnce(handbook('结果'))
    const { state, scope, form } = setup()
    await state.generate()
    expect(state.error.value).toBe('服务繁忙')
    expect(form.value.topic).toBe(' 函数 ')
    await state.generate()
    expect(state.error.value).toBe('')
    expect(generateKnowledge).toHaveBeenLastCalledWith(expect.objectContaining({ subject: '数学', grade: '高一', topic: '函数' }), expect.any(AbortSignal))
    scope.stop()
  })
})
