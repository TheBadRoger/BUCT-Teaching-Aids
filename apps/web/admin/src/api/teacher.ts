// Teacher 管理接口，保持后端 URL 与请求方式。
import request from '@/utils/request'
import type { Teacher, EntityId, Page, SearchParams, Registration } from '@/types/api'

// 教师搜索
export const getTeacherList = (params: SearchParams, signal?: AbortSignal) => {
  return request<Page<Teacher>>({
    url: '/api/teacher/search',
    method: 'post',
    params,
    signal
  })
}

// 新增教师
export const addTeacher = (data: Registration & { teacher: Omit<Teacher, 'id'> }) => {
  return request<Teacher>({
    url: '/api/teacher/add-with-user',
    method: 'post',
    data
  })
}

// 教师详情
export const getTeacherDetail = (id: EntityId) => {
  return request<Teacher>({
    url: `/api/teacher/${id}`,
    method: 'get'
  })
}

// 编辑教师
export const updateTeacher = (id: EntityId, data: Partial<Teacher>) => {
  return request<Teacher>({
    url: '/api/teacher/update',
    method: 'put',
    params: { id },
    data
  })
}

// 批量删除教师
export const deleteTeachers = (data: EntityId[]) => {
  return request<void>({
    url: '/api/teacher/batch',
    method: 'delete',
    data
  })
}
