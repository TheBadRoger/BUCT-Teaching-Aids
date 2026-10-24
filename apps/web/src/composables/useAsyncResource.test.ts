import { describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { useAsyncResource } from './useAsyncResource'

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: Error) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
describe('learning resource requests', () => {
  it('keeps only the latest route result, even when cancellation is ignored by the server', async () => {
    const first = deferred<string>()
    const next = deferred<string>()
    const loader = vi.fn().mockReturnValueOnce(first.promise).mockReturnValueOnce(next.promise)
    const scope = effectScope()
    const resource = scope.run(() => useAsyncResource<string>(loader))!
    const old = resource.load()
    const latest = resource.load()
    expect(loader.mock.calls[0]![0].aborted).toBe(true)
    first.reject(new Error('旧错误'))
    await old
    expect(resource.error.value).toBe('')
    expect(resource.loading.value).toBe(true)
    next.resolve('新课程')
    await latest
    expect(resource.data.value).toBe('新课程')
    expect(resource.loading.value).toBe(false)
    scope.stop()
  })
  it('clears stale content on reload, exposes failures and allows retry', async () => {
    const loader = vi.fn().mockResolvedValueOnce('课程').mockRejectedValueOnce(new Error('未登录')).mockResolvedValueOnce('重试结果')
    const scope = effectScope()
    const resource = scope.run(() => useAsyncResource<string>(loader))!
    await resource.load()
    const failed = resource.load()
    expect(resource.data.value).toBeNull()
    await failed
    expect(resource.error.value).toBe('未登录')
    await resource.load()
    expect(resource.data.value).toBe('重试结果')
    expect(resource.error.value).toBe('')
    scope.stop()
  })
  it('aborts on leaving the page and does not apply late results or start more requests', async () => {
    const pending = deferred<string>()
    const loader = vi.fn(() => pending.promise)
    const scope = effectScope()
    const resource = scope.run(() => useAsyncResource(loader))!
    const request = resource.load()
    scope.stop()
    pending.resolve('迟到结果')
    await request
    await resource.load()
    expect(resource.data.value).toBeNull()
    expect(loader).toHaveBeenCalledOnce()
  })
})
