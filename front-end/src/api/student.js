import request from '@/utils/request'

// 学生搜索
export const getStudentList = (params) => {
  return request({
    url: '/api/students/search',
    method: 'get',
    params
  })
}

// 新增学生
export const addStudent = (data) => {
  return request({
    url: '/api/students/add',
    method: 'post',
    data
  })
}

// 编辑学生
export const updateStudent = (id, data) => {
  return request({
    url: '/api/students/update',
    method: 'put',
    params: { id },
    data
  })
}

// 批量删除学生
export const deleteStudents = (data) => {
  return request({
    url: '/api/students/batch',
    method: 'delete',
    data
  })
}
