export const newsCategories = ['校园动态', '教学通知', '平台公告'] as const
export type NewsCategory = typeof newsCategories[number]
export interface NewsDraft {
  id: string
  title: string
  category: NewsCategory
  author: string
  summary: string
  content: string
  createdAt: string
}
export interface CourseReview {
  id: string
  course: string
  student: string
  rating: number
  content: string
  createdAt: string
}
