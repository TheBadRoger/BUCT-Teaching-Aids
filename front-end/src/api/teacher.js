import request from '@/utils/request'

// 教师搜索
export const getTeacherList = (params) => {
  return request({
    url: '/api/teacher/search',
    method: 'post',
    params
  })
}

// 新增教师
export const addTeacher = (data) => {
  return request({
    url: '/api/teacher/add-with-user',
    method: 'post',
    data 
  })
}

// 教师详情
export const getTeacherDetail = (id) => {
  return request({
    url: `/api/teacher/${id}`,
    method: 'get'
  })
}

// 编辑教师
export const updateTeacher = (id, data) => {
  return request({
    url: '/api/teacher/update',
    method: 'put',
    params: { id },
    data
  })
}

// 批量删除教师
export const deleteTeachers = (data) => {
  return request({
    url: '/api/teacher/batch',
    method: 'delete',
    data
  })
}
