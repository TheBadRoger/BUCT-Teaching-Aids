// Student 管理接口，保持后端 URL 与请求方式。
import request from '@/utils/request'
import type { Student, EntityId, Page, SearchParams, Registration } from '@/types/api'

// 学生搜索
export const getStudentList = (params: SearchParams, signal?: AbortSignal) => {
  return request<Page<Student>>({
    url: '/api/students/search',
    method: 'get',
    params,
    signal
  })
}

// 新增学生
export const addStudent = (data: Registration & { student: Omit<Student, 'id'> }) => {
  return request<Student>({
    url: '/api/students/add-with-user',
    method: 'post',
    data
  })
}

// 编辑学生
export const updateStudent = (id: EntityId, data: Partial<Student>) => {
  return request<Student>({
    url: '/api/students/update',
    method: 'put',
    params: { id },
    data
  })
}

// 批量删除学生
export const deleteStudents = (data: EntityId[]) => {
  return request<void>({
    url: '/api/students/batch',
    method: 'delete',
    data
  })
}
