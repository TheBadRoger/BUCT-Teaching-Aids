import type { Course, EntityId, Page, SearchParams } from '@/types/api'
import type { CourseNote, Enrollment, LearningContext } from '@/types/learning'
import { createHttpClient } from '@/utils/http'

// 学生会话由后端 Cookie 决定，不读取或修改管理员的本地登录缓存。
const request = createHttpClient(() => {}, () => {})
export const searchLearningCourses = (params: SearchParams, signal: AbortSignal) =>
  request.get<Page<Course>>('/api/course/search', { params, signal })
export const getPopularCourses = (signal: AbortSignal) => request.get<Course[]>('/api/course/view/popular', { params: { limit: 8 }, signal })
export const getRecommendedCourses = (signal: AbortSignal) => request.get<Course[]>('/api/course/view/top10', { signal })

export async function getLearningContext(signal: AbortSignal): Promise<LearningContext> {
  const user = await request.get<{ username: string; student?: LearningContext['student'] }>('/api/user/auth/current', { signal })
  const binding = await request.get<{ studentInfo?: LearningContext['student'] }>('/api/user/binding/info', { signal })
  return { username: user.username, student: binding.studentInfo ?? user.student ?? null }
}

/** 查询按编号精确定位，避免模糊搜索的第一条记录串课。 */
export async function findLearningCourse(courseNumber: string, signal: AbortSignal): Promise<Course | null> {
  if (!courseNumber) return null
  let page = 0
  let result: Page<Course>
  do {
    result = await searchLearningCourses({ courseNumber, page, size: 50 }, signal)
    const exact = result.content.find(course => course.courseNumber === courseNumber)
    if (exact) return exact
    page++
  } while (result.content.length && page * 50 < result.totalElements)
  return null
}
export const getCourseNotes = (courseId: EntityId, signal: AbortSignal) =>
  request.get<Page<CourseNote>>(`/api/notes/course/${encodeURIComponent(courseId)}`, { params: { page: 0, size: 10 }, signal })
export const createCourseNote = (courseId: EntityId, studentId: EntityId, input: { title: string; content: string; isPublic: boolean }) =>
  request.post('/api/notes/create', { ...input, student: { id: studentId }, course: { id: courseId } })
export const getEnrollments = (studentId: EntityId, page: number, signal: AbortSignal) =>
  request.get<Page<Enrollment>>('/api/student-courses/all-courses', { params: { studentId, page, size: 50 }, signal })
export const enrollCourse = (studentId: EntityId, courseId: EntityId) =>
  request.post<Enrollment>('/api/student-courses/select', {}, { params: { studentId, courseId } })
export const markCourseViewed = (studentId: EntityId, courseId: EntityId, isViewed: boolean) =>
  request.put<Enrollment>('/api/student-courses/update-viewed', {}, { params: { studentId, courseId, isViewed } })
export const recordCourseView = (courseId: EntityId, signal: AbortSignal) =>
  request.post(`/api/course/view/${encodeURIComponent(courseId)}/record`, {}, { signal })
