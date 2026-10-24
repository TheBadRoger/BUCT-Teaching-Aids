// 教师 AI 使用独立服务协议，不经过 Java 的 code=2000 响应解包。
import type { TeachingCatalog, TeachingInput } from '@/types/teaching'
import { normalizeKnowledge, normalizeLesson } from '@/utils/teaching-content'
import { readLessonStream } from '@/utils/lesson-stream'

const base = (import.meta.env.VITE_TEACHER_AI_BASE_URL || '/teacher-ai-api').replace(/\/$/, '')

/** 校验教师助手的 success/data 封装。 */
async function request(path: string, signal: AbortSignal, input?: Partial<TeachingInput>): Promise<unknown> {
  const response = await fetch(`${base}${path}`, {
    signal, method: input ? 'POST' : 'GET',
    headers: input ? { 'Content-Type': 'application/json' } : undefined,
    body: input ? JSON.stringify(input) : undefined,
  })
  const value: unknown = await response.json()
  if (!value || typeof value !== 'object' || !('success' in value) || value.success !== true || !response.ok) {
    throw new Error(value && typeof value === 'object' && 'error' in value && typeof value.error === 'string' ? value.error : '教师 AI 服务请求失败')
  }
  return 'data' in value ? value.data : undefined
}

/** 学科目录失败由页面明确展示并提供重试。 */
export async function getTeachingCatalog(signal: AbortSignal): Promise<TeachingCatalog> {
  const value = await request('/knowledge/subjects', signal)
  if (!value || typeof value !== 'object' || !('subjects' in value) || !Array.isArray(value.subjects) || !('grades' in value) || !Array.isArray(value.grades) || !('lessonTypes' in value) || !Array.isArray(value.lessonTypes)) throw new Error('学科目录格式不正确')
  return {
    subjects: value.subjects.filter((item: unknown): item is { name: string } => !!item && typeof item === 'object' && 'name' in item && typeof item.name === 'string'),
    grades: value.grades.filter((item: unknown): item is string => typeof item === 'string'),
    lessonTypes: value.lessonTypes.filter((item: unknown): item is string => typeof item === 'string'),
  }
}

/** 复用既有知识点生成接口。 */
export async function generateKnowledge(input: TeachingInput, signal: AbortSignal) {
  const { subject, grade, topic, focus } = input
  return normalizeKnowledge(await request('/knowledge/generate', signal, { subject, grade, topic, focus }))
}

/** 两种教案模式发送相同字段，不向教案接口附加知识点设置。 */
function lessonPayload(input: TeachingInput): Omit<TeachingInput, 'focus'> {
  const { subject, grade, topic, lessonType, duration, teachingStyle, extraRequirements } = input
  return { subject, grade, topic, lessonType, duration, teachingStyle, extraRequirements }
}

/** 非流式生成仍保留为可选模式。 */
export async function generateLesson(input: TeachingInput, signal: AbortSignal) {
  return normalizeLesson(await request('/lesson-plan/generate', signal, lessonPayload(input)))
}

/** Fetch 支持 POST 请求体和取消，保持既有 SSE 事件协议。 */
export async function streamLesson(input: TeachingInput, signal: AbortSignal, onChunk: (text: string) => void) {
  const response = await fetch(`${base}/lesson-plan/generate-stream`, {
    method: 'POST', signal, headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' }, body: JSON.stringify(lessonPayload(input)),
  })
  if (!response.ok) {
    const value: unknown = await response.json().catch(() => null)
    throw new Error(value && typeof value === 'object' && 'error' in value && typeof value.error === 'string' ? value.error : `生成失败（${response.status}）`)
  }
  if (!response.body || !response.headers.get('content-type')?.includes('text/event-stream')) throw new Error('服务未返回流式响应')
  await readLessonStream(response.body, onChunk, signal)
}

/** 健康检查仅返回可公开的状态信息，不传入密钥。 */
export async function getTeachingHealth(signal: AbortSignal): Promise<{ ready: boolean; model: string }> {
  const response = await fetch(`${base}/health`, { signal })
  const value: unknown = await response.json()
  if (!response.ok || !value || typeof value !== 'object' || !('status' in value) || value.status !== 'ok') throw new Error('教师 AI 服务未连接')
  return { ready: 'hasApiKey' in value && value.hasApiKey === true, model: 'model' in value && typeof value.model === 'string' ? value.model : '' }
}
