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
    url: '/api/teacher/add',
    method: 'post',
    data 
  })
}