// Course 管理接口，保持后端 URL 与请求方式。
import request from '@/utils/request'
import type { Course, EntityId, Page, SearchParams } from '@/types/api'

// 课程搜索
export const getCourseList = (params: SearchParams, signal?: AbortSignal) => {
  return request<Page<Course>>({
    url: '/api/course/search',
    method: 'get',
    params,
    signal
  })
}

// 新增课程
export const addCourse = (data: Omit<Course, 'id'>) => {
  return request<Course>({
    url: '/api/course/add',
    method: 'post',
    data: {
      id: 0,
      viewCount: 0,
      ...data
    }
  })
}

// 编辑课程
export const updateCourse = (id: EntityId, data: Partial<Course>) => {
  return request<Course>({
    url: '/api/course/update',
    method: 'put',
    params: { id },
    data: {
      id,
      ...data
    }
  })
}

// 批量删除课程
export const deleteCourses = (data: EntityId[]) => {
  return request<void>({
    url: '/api/course/batch',
    method: 'delete',
    data
  })
}
