import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { findLearningCourse, recordCourseView } from '@/api/learning'
import { useAsyncResource } from './useAsyncResource'

export function useLearningCourse() {
  const route = useRoute()
  const resource = useAsyncResource(async signal => {
    const course = await findLearningCourse(typeof route.query.courseNumber === 'string' ? route.query.courseNumber : '', signal)
    if (!course) throw new Error('未找到课程，请从课程大全重新选择')
    // 访问统计不阻塞课程内容，也不将失败解释为课程不可用。
    void recordCourseView(course.id, signal).catch(() => {})
    return course
  })
  watch(() => route.query.courseNumber, resource.load, { immediate: true })
  return resource
}
