import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNewsStore } from './news'

const input = { title: ' 教学通知 ', author: ' 管理员 ', category: '教学通知' as const, summary: '摘要', content: '正文\n第二行' }
let data: Map<string, string>
let storage: { getItem: ReturnType<typeof vi.fn>; setItem: ReturnType<typeof vi.fn> }
beforeEach(() => {
  setActivePinia(createPinia())
  data = new Map()
  storage = { getItem: vi.fn((key: string) => data.get(key) || null), setItem: vi.fn((key: string, value: string) => data.set(key, value)) }
  vi.stubGlobal('localStorage', storage)
})
afterEach(() => vi.unstubAllGlobals())

describe('local news drafts', () => {
  it('saves trimmed text and reloads details from browser storage', () => {
    const store = useNewsStore()
    const id = store.add(input)
    expect(store.drafts[0]).toMatchObject({ id, title: '教学通知', author: '管理员', content: '正文\n第二行' })
    setActivePinia(createPinia())
    expect(useNewsStore().drafts[0]?.id).toBe(id)
  })
  it('removes a selected draft without deleting other articles', () => {
    const store = useNewsStore()
    const first = store.add(input)
    const second = store.add({ ...input, title: '第二条资讯' })
    store.remove(first)
    expect(store.drafts.map(item => item.id)).toEqual([second])
    expect(JSON.parse(data.get('buct-news-drafts') || '[]')).toHaveLength(1)
  })
  it('keeps the list unchanged when browser storage is full', () => {
    const store = useNewsStore()
    store.add(input)
    storage.setItem.mockImplementation(() => { throw new Error('quota') })
    expect(() => store.add(input)).toThrow('草稿未保存')
    expect(store.drafts).toHaveLength(1)
    expect(() => store.remove(store.drafts[0]!.id)).toThrow()
    expect(store.drafts).toHaveLength(1)
  })
  it.each(['{broken', '{}', '[null]', '[{"id":"1"}]'])('does not overwrite damaged storage: %s', value => {
    data.set('buct-news-drafts', value)
    const store = useNewsStore()
    expect(store.error).toContain('读取失败')
    expect(() => store.add(input)).toThrow()
    expect(storage.setItem).not.toHaveBeenCalled()
    data.set('buct-news-drafts', '[]')
    store.reload()
    expect(store.error).toBe('')
    expect(() => store.add(input)).not.toThrow()
  })
  it('does not report success when storage cannot be read', () => {
    storage.getItem.mockImplementation(() => { throw new Error('denied') })
    const store = useNewsStore()
    expect(() => store.add(input)).toThrow('读取失败')
    expect(store.drafts).toHaveLength(0)
  })
  it('rejects blank required fields without saving', () => {
    const store = useNewsStore()
    expect(() => store.add({ ...input, title: '  ' })).toThrow('请填写')
    expect(storage.setItem).not.toHaveBeenCalled()
  })
})
