import type { Course, EntityId } from './api'

export interface LearningContext {
  username: string
  student: { id: EntityId; name?: string } | null
}
export interface CourseNote {
  id: EntityId
  title: string
  content?: string
  createdAt?: string
  likeCount?: number
  student?: { name?: string }
}
export interface Enrollment {
  course: Course
  isViewed: boolean
}
export interface CourseLesson { title: string; videoUrl: string }
