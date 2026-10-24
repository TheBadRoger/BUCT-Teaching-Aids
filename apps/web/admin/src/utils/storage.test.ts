// 详情页不再因损坏的浏览器缓存而无法渲染。
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Teacher } from '@/types/api'
import { readCachedDetail } from './storage'

afterEach(() => vi.unstubAllGlobals())

describe('detail cache', () => {
  it.each(['{broken', '[]', 'null', '{"id":1}', '{"id":{},"name":"教师"}'])('ignores malformed or incomplete data: %s', raw => {
    vi.stubGlobal('localStorage', { getItem: () => raw })
    expect(readCachedDetail<Teacher>('currentTeacherDetail', ['name'])).toBeNull()
  })

  it('preserves a valid numeric identifier, including zero', () => {
    vi.stubGlobal('localStorage', { getItem: () => '{"id":0,"name":"教师"}' })
    expect(readCachedDetail<Teacher>('currentTeacherDetail', ['name'])).toEqual({ id: 0, name: '教师' })
  })
})
