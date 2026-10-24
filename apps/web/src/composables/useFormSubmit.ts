import { getCurrentScope, onScopeDispose, ref, type Ref } from 'vue'
import type { FormInstance } from 'element-plus'

/** 在异步校验开始前锁定提交；失败时保留表单，由页面展示错误。 */
export function useFormSubmit(form: Ref<Pick<FormInstance, 'validate'> | undefined>) {
  const submitting = ref(false)
  const error = ref('')
  let disposed = false
  if (getCurrentScope()) onScopeDispose(() => { disposed = true })
  async function submit(action: () => Promise<unknown>): Promise<boolean> {
    if (submitting.value || disposed) return false
    submitting.value = true
    error.value = ''
    try {
      if (!await form.value?.validate().catch(() => false) || disposed) return false
      await action()
      return !disposed
    } catch (reason) {
      if (!disposed) error.value = reason instanceof Error ? reason.message : '提交失败，请稍后重试'
      return false
    } finally { submitting.value = false }
  }
  return { submitting, error, submit }
}
