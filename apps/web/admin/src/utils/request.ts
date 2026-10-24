// 把 HTTP 层与管理端会话和提示组件连接。
import { ElMessage } from 'element-plus'
import router from '@/router'
import { pinia } from '@/stores'
import { useSessionStore } from '@/stores/session'
import { createHttpClient } from './http'

const request = createHttpClient(() => {
  useSessionStore(pinia).clear()
  if (router.currentRoute.value.path !== '/login') void router.replace('/login')
}, message => { ElMessage.error(message) })

export default request
