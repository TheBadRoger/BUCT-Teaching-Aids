import { onScopeDispose, shallowRef } from 'vue'

/** 页面资源：只提交最新请求的结果，离开作用域时中止请求。 */
export function useAsyncResource<T>(loader: (signal: AbortSignal) => Promise<T>) {
  const data = shallowRef<T | null>(null)
  const loading = shallowRef(false)
  const error = shallowRef('')
  let controller: AbortController | undefined
  let disposed = false
  async function load() {
    if (disposed) return
    controller?.abort()
    const task = new AbortController()
    controller = task
    data.value = null
    error.value = ''
    loading.value = true
    try {
      const result = await loader(task.signal)
      if (!disposed && controller === task) data.value = result
    } catch (reason) {
      if (!disposed && controller === task) error.value = reason instanceof Error ? reason.message : '加载失败，请重试'
    } finally {
      if (!disposed && controller === task) loading.value = false
    }
  }
  onScopeDispose(() => { disposed = true; controller?.abort() })
  return { data, loading, error, load }
}
