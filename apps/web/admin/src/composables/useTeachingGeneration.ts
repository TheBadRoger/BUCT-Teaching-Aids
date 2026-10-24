import { onScopeDispose, shallowReadonly, shallowRef, watch, type Ref } from 'vue'
import type { FormInstance } from 'element-plus'
import type { KnowledgeHandbook, LessonPlan, TeachingInput } from '@/types/teaching'
import { generateKnowledge, generateLesson, streamLesson } from '@/api/teaching'
import { parseLessonText } from '@/utils/teaching-content'

/** 生成任务拥有自己的取消与结果状态，页面只负责输入和呈现。 */
export function useTeachingGeneration(options: {
  formRef: Ref<Pick<FormInstance, 'validate'> | undefined>
  form: Ref<TeachingInput>
  isKnowledge: Readonly<Ref<boolean>>
  useStream: Ref<boolean>
}) {
  const busy = shallowRef(false)
  const error = shallowRef('')
  const notice = shallowRef('')
  const raw = shallowRef('')
  const handbook = shallowRef<KnowledgeHandbook | null>(null)
  const lesson = shallowRef<LessonPlan | null>(null)
  let controller: AbortController | undefined
  let disposed = false

  function cancel(showNotice = true) {
    controller?.abort()
    controller = undefined
    notice.value = showNotice && busy.value ? '已停止接收生成内容，可以调整设置后重新生成。' : ''
    error.value = ''
    busy.value = false
  }

  async function generate() {
    if (busy.value || disposed) return
    const task = new AbortController()
    controller = task
    const current = () => !disposed && controller === task && !task.signal.aborted
    const knowledge = options.isKnowledge.value
    const streaming = options.useStream.value
    busy.value = true
    error.value = ''
    notice.value = ''
    try {
      if (!await options.formRef.value?.validate().catch(() => false) || !current()) return
      const form = options.form.value
      const input = { ...form, subject: form.subject.trim(), grade: form.grade.trim(), topic: form.topic.trim() }
      if (knowledge) {
        handbook.value = null
        const result = await generateKnowledge(input, task.signal)
        if (current()) handbook.value = result
      } else {
        lesson.value = null
        raw.value = ''
        if (streaming) {
          await streamLesson(input, task.signal, chunk => { if (current()) raw.value += chunk })
          if (current()) lesson.value = parseLessonText(raw.value)
        } else {
          const result = await generateLesson(input, task.signal)
          if (current()) lesson.value = result
        }
      }
    } catch (reason) {
      if (current()) error.value = reason instanceof Error ? reason.message : '生成失败，请重试'
    } finally {
      if (current()) { busy.value = false; controller = undefined }
    }
  }

  watch(options.isKnowledge, () => cancel(false), { flush: 'sync' })
  onScopeDispose(() => { disposed = true; cancel(false) })
  return { busy: shallowReadonly(busy), error: shallowReadonly(error), notice: shallowReadonly(notice),
    raw: shallowReadonly(raw), handbook: shallowReadonly(handbook), lesson: shallowReadonly(lesson), generate, cancel }
}
