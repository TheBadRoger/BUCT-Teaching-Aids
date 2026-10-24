// 安全读取详情缓存；损坏或不完整的缓存视为无数据。
import type { EntityId } from '@/types/api'

/** 校验详情标识和关键文本字段，不把任意 JSON 直接交给页面。 */
export function readCachedDetail<T extends { id: EntityId }>(key: string, required: (keyof T)[]): T | null {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(key) || 'null')
    if (!value || typeof value !== 'object' || Array.isArray(value)) return null
    const record = value as Record<string, unknown>
    if (typeof record.id !== 'string' && typeof record.id !== 'number') return null
    if (!required.every(field => typeof record[String(field)] === 'string')) return null
    return value as T
  } catch { return null }
}
