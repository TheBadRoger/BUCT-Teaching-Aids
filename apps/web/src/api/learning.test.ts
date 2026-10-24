import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createCourseNote, enrollCourse, findLearningCourse, getLearningContext, markCourseViewed } from './learning'

const transport = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), put: vi.fn() }))
vi.mock('@/utils/http', () => ({ createHttpClient: () => transport }))
describe('migrated student API contracts', () => {
  beforeEach(() => vi.resetAllMocks())
  it('locates an exact course number across pages instead of taking the first fuzzy match', async () => {
    transport.get.mockResolvedValueOnce({ content: [{ id: 1, courseNumber: 'ABC-1' }], totalElements: 51 }).mockResolvedValueOnce({ content: [{ id: 2, courseNumber: 'ABC' }], totalElements: 51 })
    const signal = new AbortController().signal
    await expect(findLearningCourse('ABC', signal)).resolves.toEqual({ id: 2, courseNumber: 'ABC' })
    expect(transport.get).toHaveBeenLastCalledWith('/api/course/search', { params: { courseNumber: 'ABC', page: 1, size: 50 }, signal })
  })
  it('does not substitute another course when the requested number is missing', async () => {
    transport.get.mockResolvedValue({ content: [{ courseNumber: 'ABC-1' }], totalElements: 1 })
    await expect(findLearningCourse('ABC', new AbortController().signal)).resolves.toBeNull()
  })
  it('takes student identity from the server binding rather than a local administrator account', async () => {
    transport.get.mockResolvedValueOnce({ username: 'student-user' }).mockResolvedValueOnce({ studentInfo: { id: 7 } })
    await expect(getLearningContext(new AbortController().signal)).resolves.toEqual({ username: 'student-user', student: { id: 7 } })
  })
  it('retains query parameters for selection and viewed status, and entity references for notes', async () => {
    await enrollCourse(7, 12)
    expect(transport.post).toHaveBeenCalledWith('/api/student-courses/select', {}, { params: { studentId: 7, courseId: 12 } })
    await markCourseViewed(7, 12, true)
    expect(transport.put).toHaveBeenCalledWith('/api/student-courses/update-viewed', {}, { params: { studentId: 7, courseId: 12, isViewed: true } })
    await createCourseNote(12, 7, { title: '笔记', content: '正文', isPublic: false })
    expect(transport.post).toHaveBeenCalledWith('/api/notes/create', { title: '笔记', content: '正文', isPublic: false, student: { id: 7 }, course: { id: 12 } })
  })
})
