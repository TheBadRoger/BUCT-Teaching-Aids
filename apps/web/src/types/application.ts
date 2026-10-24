// 本地应用页面使用的数据类型。
export interface CalendarEvent {
  id: string
  date: string
  title: string
}

export interface LivePlan {
  id: string
  title: string
  teacher: string
  start: string
  description: string
}
