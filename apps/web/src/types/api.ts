// 管理端与现有 Java API 的数据契约。
export type EntityId = number | string

export interface Page<T> {
  content: T[]
  totalElements: number
}

export interface Account {
  id?: EntityId
  username: string
}

export interface Credentials {
  username: string
  password: string
}

export interface Profile {
  id: EntityId
  name: string
  gender?: string
  username?: string
  telephone?: string
  email?: string
  userType?: 'TEACHER' | 'STUDENT' | null
}

export interface Teacher extends Profile {
  organization?: string
  education?: string
  jointime?: string
}

export interface Student extends Profile {
  className?: string
  studentNumber?: string
  admissionDate?: string
}

export interface Course {
  id: EntityId
  courseName: string
  courseNumber: string
  courseIntroduction?: string
  startDate?: string
  teachingObjectives?: string
  duration?: string
  teachingTeachers?: string
  teachingClasses?: string
  targetAudience?: string
  classAddress?: string
  coursePrice?: number
  courseStatus?: string
  courseTags?: string
  courseOutline?: string
  courseImage?: string
  viewCount?: number
}

export interface Organization {
  id?: EntityId
  name: string
  logo: string
  bannerUrl: string
  info: string
  honorCertUrl: string
  createdTime?: string
}

export interface Registration extends Credentials {
  telephone?: string
  email?: string
}

export type SearchParams = Record<string, string | number | undefined>
