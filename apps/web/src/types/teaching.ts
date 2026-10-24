// 教师 AI 服务的请求与规范化展示数据。
export interface TeachingInput {
  subject: string
  grade: string
  topic: string
  focus: string
  lessonType: string
  duration: number
  teachingStyle: string
  extraRequirements: string
}

export interface TeachingCatalog {
  subjects: { name: string }[]
  grades: string[]
  lessonTypes: string[]
}

export interface KnowledgeHandbook {
  chapterTitle: string
  overview: string
  knowledgePoints: {
    name: string
    explanation: string
    difficulty: string
    importance: string
    keyConcepts: string[]
    commonMistakes: string[]
  }[]
}

export interface LessonPlan {
  title: string
  lessonType: string
  duration: number
  teachingObjectives: { category: string; content: string }[]
  keyPoints: string[]
  difficultPoints: { point: string; strategy: string }[]
  teachingResources: string
  teachingProcess: { stageName: string; duration: number; teacherActivity: string; studentActivity: string; designIntent: string }[]
  boardDesign: string
  homework: { basic: string[]; improve: string[]; challenge: string[] }
  teachingReflection: string
}
