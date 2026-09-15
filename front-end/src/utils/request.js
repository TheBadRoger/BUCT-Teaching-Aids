import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '',
  timeout: 5000,
  withCredentials: true //让浏览器自动带cookie
})

service.interceptors.request.use(config => {
  return config
})

service.interceptors.response.use(
  response => {
    const res = response.data

    if (res.code !== 2000) {
      const message = res.code === 4091 && response.config.url?.includes('/api/course')
        ? '课程编号已存在，请换一个课程编号'
        : (res.msg || '请求失败')
      ElMessage.error(message)

      // 登录过期
      if (res.code === 4012 || res.code === 4013) {
        localStorage.removeItem('isLogin')
        localStorage.removeItem('user')
        router.push('/login')
      }

      return Promise.reject(res)
    }

    return res.data
  },
  error => {
    const message = error.response?.data?.msg || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)


export default service
