import request from '@/utils/request'
import type { Account, Credentials } from '@/types/api'
// 管理员登录
export const loginApi = (data: Credentials) => {
  return request<Account>({
    url: '/api/admin/login',
    method: 'post',
    data: new URLSearchParams({ username: data.username, password: data.password }).toString(),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}
// 管理员注册
export const registerApi = (data: Credentials) => {
  return request<Account>({
    url: '/api/admin/register',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}
