import request from '@/utils/request'
import qs from 'qs'
// 管理员登录
export const loginApi = (data) => {
  return request({
    url: '/api/admin/login',
    method: 'post',
    data: qs.stringify(data),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}
// 管理员注册
export const registerApi = (data) => {
  return request({
    url: '/api/admin/register',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}