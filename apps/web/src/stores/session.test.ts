// 验证旧登录缓存迁移、密码脱敏以及损坏缓存处理。
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { readCachedAccount, useSessionStore } from './session'

const values = new Map<string, string>()
const storage = {
  getItem: (key: string) => values.get(key) ?? null,
  setItem: (key: string, value: string) => { values.set(key, value) },
  removeItem: (key: string) => { values.delete(key) },
}

beforeEach(() => {
  values.clear()
  vi.stubGlobal('localStorage', storage)
  setActivePinia(createPinia())
})

describe('session migration', () => {
  it('discards password fields from a legacy cached account', () => {
    storage.setItem('isLogin', '1')
    storage.setItem('user', JSON.stringify({ id: 1, username: 'teacher', password: 'old-secret' }))
    const session = useSessionStore()
    expect(session.authenticated).toBe(true)
    expect(JSON.parse(storage.getItem('user')!)).toEqual({ id: 1, username: 'teacher' })
  })

  it.each(['{broken', 'null', '[]', '{"username":123}', '{"username":" "}'])('rejects invalid cached identity: %s', (value) => {
    storage.setItem('isLogin', '1')
    storage.setItem('user', value)
    expect(readCachedAccount(storage)).toBeNull()
    expect(useSessionStore().authenticated).toBe(false)
    expect(storage.getItem('isLogin')).toBeNull()
  })

  it('clears both reactive and persistent state on expiry', () => {
    const session = useSessionStore()
    session.setUser({ id: 2, username: 'admin' })
    session.clear()
    expect(session.authenticated).toBe(false)
    expect(storage.getItem('user')).toBeNull()
  })

  it('keeps the current session usable when browser storage is blocked', () => {
    vi.stubGlobal('localStorage', { ...storage, getItem: () => { throw new Error('blocked') }, setItem: () => { throw new Error('blocked') } })
    const session = useSessionStore()
    session.setUser({ username: 'admin' })
    expect(session.authenticated).toBe(true)
  })
})
