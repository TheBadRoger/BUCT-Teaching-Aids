import { ref } from 'vue'
import { defineStore } from 'pinia'
import { newsCategories, type NewsDraft } from '@/types/content'

const key = 'buct-news-drafts'
function isDraft(value: unknown): value is NewsDraft {
  if (!value || typeof value !== 'object') return false
  const item = value as Record<string, unknown>
  return ['id', 'title', 'category', 'author', 'summary', 'content', 'createdAt'].every(field => typeof item[field] === 'string')
    && !!item.id && !!item.title && newsCategories.includes(item.category as NewsDraft['category'])
    && Number.isFinite(Date.parse(item.createdAt as string))
}

/** 本机草稿没有发布语义；写入成功后才更新列表。 */
export const useNewsStore = defineStore('news', () => {
  const drafts = ref<NewsDraft[]>([])
  const error = ref('')
  let readable = false
  function reload() {
    try {
      const value: unknown = JSON.parse(localStorage.getItem(key) || '[]')
      if (!Array.isArray(value) || !value.every(isDraft)) throw new Error('invalid cache')
      drafts.value = value
      readable = true
      error.value = ''
    } catch {
      readable = false
      error.value = '本机草稿读取失败。请检查浏览器存储后重试；为保护已有内容，暂不覆盖存储。'
    }
  }
  function persist(next: NewsDraft[]) {
    if (!readable) throw new Error(error.value)
    try { localStorage.setItem(key, JSON.stringify(next)) }
    catch { throw new Error('浏览器存储不可用或空间不足，草稿未保存，请保留输入后重试') }
    drafts.value = next
  }
  function add(input: Omit<NewsDraft, 'id' | 'createdAt'>) {
    if (!input.title.trim() || !input.author.trim() || !input.content.trim()) throw new Error('请填写标题、作者和正文')
    const draft: NewsDraft = { ...input, title: input.title.trim(), author: input.author.trim(), summary: input.summary.trim(), content: input.content.trim(), id: crypto.randomUUID(), createdAt: new Date().toISOString() }
    persist([draft, ...drafts.value])
    return draft.id
  }
  function remove(id: string) { persist(drafts.value.filter(item => item.id !== id)) }
  reload()
  return { drafts, error, reload, add, remove }
})
