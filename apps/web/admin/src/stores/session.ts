// 登录展示状态；实际访问权限仍由后端 Cookie 会话控制。
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Account } from '@/types/api'

/** 只读取已知的非敏感展示字段，兼容旧版缓存。 */
export function readCachedAccount(storage: Pick<Storage, 'getItem'>): Account | null {
  try {
    const value: unknown = JSON.parse(storage.getItem('user') || 'null')
    if (storage.getItem('isLogin') !== '1' || !value || typeof value !== 'object' || !('username' in value) || typeof value.username !== 'string' || !value.username.trim()) return null
    const id = 'id' in value && (typeof value.id === 'string' || typeof value.id === 'number') ? value.id : undefined
    return { id, username: value.username }
  } catch { return null }
}

export const useSessionStore = defineStore('session', () => {
  let cached: Account | null = null
  try { cached = readCachedAccount(localStorage) } catch { /* 浏览器可能禁止访问存储。 */ }
  const user = ref<Account | null>(cached)
  const authenticated = computed(() => user.value !== null)

  /** 保存显示字段，不持久化后端返回的密码等额外字段。 */
  function setUser(account: Account) {
    if (typeof account?.username !== 'string' || !account.username.trim()) throw new Error('登录信息不完整')
    user.value = { id: account.id, username: account.username }
    try {
      localStorage.setItem('user', JSON.stringify(user.value))
      localStorage.setItem('isLogin', '1')
    } catch { /* 存储被禁用时，保留当前页面会话状态。 */ }
  }

  /** 清除客户端登录状态。 */
  function clear() {
    user.value = null
    try {
      localStorage.removeItem('user')
      localStorage.removeItem('isLogin')
    } catch { /* 存储被禁用时仍确保内存状态已清除。 */ }
  }

  // 升级旧缓存时同时移除以前可能持久化的密码等额外字段。
  if (cached) setUser(cached)
  else clear()
  return { user, authenticated, setUser, clear }
})
