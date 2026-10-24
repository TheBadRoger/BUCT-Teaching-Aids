import type { CourseReview } from '@/types/content'

// 展示页面布局用的演示数据，不代表真实学生、课程或评价。
export const demoReviews: readonly CourseReview[] = [
  { id: 'demo-1', course: '人工智能基础（演示）', student: '示例学员 A', rating: 5, content: '课程讲解清晰，案例有助于理解知识点。希望增加更多实践练习。', createdAt: '2026-01-12T08:00:00+08:00' },
  { id: 'demo-2', course: 'Python 程序设计（演示）', student: '示例学员 B', rating: 4, content: '练习安排合理，建议增加代码调试过程的讲解。', createdAt: '2026-01-11T14:30:00+08:00' },
  { id: 'demo-3', course: '人工智能基础（演示）', student: '示例学员 C', rating: 3, content: '部分章节节奏较快，希望补充预习资料和课后答疑。', createdAt: '2026-01-10T10:00:00+08:00' },
]
