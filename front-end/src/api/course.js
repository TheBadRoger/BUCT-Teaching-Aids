import request from '@/utils/request'

// 课程搜索
export const getCourseList = (params) => {
  return request({
    url: '/api/course/search',
    method: 'get',
    params
  })
}

// 新增课程
export const addCourse = (data) => {
  return request({
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
export const updateCourse = (id, data) => {
  return request({
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
export const deleteCourses = (data) => {
  return request({
    url: '/api/course/batch',
    method: 'delete',
    data
  })
}
