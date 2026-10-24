import type { CourseLesson } from '@/types/learning'

/** 仅接受站内路径或 HTTP(S) 媒体；避免将脚本协议交给资源元素。 */
export function learningMediaUrl(value: string | undefined): string {
  const url = value?.trim() || ''
  if (!url || [...url].some(char => char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127) || url.includes('\\') || url.startsWith('//')) return ''
  if (/^https?:\/\//i.test(url)) return url
  if (/^[a-z][a-z\d+.-]*:/i.test(url)) return ''
  if (url.startsWith('/')) return url
  return `/${url.replace(/^\.\//, '')}`
}
export function parseCourseLessons(outline: string | undefined): CourseLesson[] {
  return (outline || '').split(/\r?\n|;|；/).map(line => line.replace(/^\s*\d+[.、)]\s*/, '').trim()).filter(Boolean).map(line => {
    const [title, videoUrl] = line.split('|').map(part => part.trim())
    return { title: title || '未命名课时', videoUrl: learningMediaUrl(videoUrl) }
  })
}
export function lessonIndex(value: unknown, count: number): number {
  const number = typeof value === 'string' ? Number(value) : NaN
  return Number.isInteger(number) && number > 0 ? Math.min(number - 1, Math.max(0, count - 1)) : 0
}
export function learningPage(value: unknown): number {
  const page = typeof value === 'string' ? Number(value) : NaN
  return Number.isSafeInteger(page) && page >= 1 && page <= 1000000 ? page : 1
}
