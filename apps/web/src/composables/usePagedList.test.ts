import { describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { Page } from '@/types/api'
import { usePagedList } from './usePagedList'

vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
type Row = { id: number }
const page = (id: number, totalElements = 1): Page<Row> => ({ content: [{ id }], totalElements })
function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: Error) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function setup(load = vi.fn(async () => page(1, 100)), remove = vi.fn(async () => {})) {
  const scope = effectScope()
  const state = scope.run(() => usePagedList({
    createQuery: () => ({ name: '', page: 0, size: 10 }), load, remove, label: '教师',
  }))!
  return { state, scope, load, remove }
}

describe('paged lists', () => {
  it('does not delete if the page was closed while confirmation was open', async () => {
    const confirmation = deferred<Awaited<ReturnType<typeof ElMessageBox.confirm>>>()
    vi.mocked(ElMessageBox.confirm).mockReturnValueOnce(confirmation.promise)
    const { state, scope, remove } = setup()
    state.handleSelectionChange([{ id: 1 }])
    const pending = state.handleDelete()
    scope.stop()
    confirmation.resolve('confirm' as Awaited<ReturnType<typeof ElMessageBox.confirm>>)
    await pending
    expect(remove).not.toHaveBeenCalled()
  })
  it('keeps the latest result even if the old server response arrives last', async () => {
    const first = deferred<Page<Row>>()
    const second = deferred<Page<Row>>()
    const load = vi.fn().mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const { state, scope } = setup(load)
    const older = state.fetchData()
    state.query.value.name = '新条件'
    const newer = state.handleSearch()
    expect(load.mock.calls[0]![1].aborted).toBe(true)
    second.resolve(page(2))
    await newer
    first.resolve(page(1))
    await older
    expect(state.tableData.value).toEqual([{ id: 2 }])
    expect(state.error.value).toBe('')
    scope.stop()
  })

  it('ignores stale failures without clearing the current loading indicator', async () => {
    const first = deferred<Page<Row>>()
    const second = deferred<Page<Row>>()
    const { state, scope } = setup(vi.fn().mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise))
    const older = state.fetchData()
    const newer = state.fetchData()
    first.reject(new Error('旧请求失败'))
    await older
    expect(state.loading.value).toBe(true)
    expect(state.error.value).toBe('')
    second.reject(new Error('当前请求失败'))
    await newer
    expect(state.loading.value).toBe(false)
    expect(state.error.value).toBe('当前请求失败')
    expect(state.tableData.value).toEqual([])
    scope.stop()
  })

  it('resets filters and pagination and clears selection when reloading', async () => {
    const { state, scope, load } = setup()
    state.query.value.name = '张'
    await state.handleCurrentChange(3)
    expect(load).toHaveBeenLastCalledWith({ name: '张', page: 2, size: 10 }, expect.any(AbortSignal))
    state.handleSelectionChange([{ id: 1 }])
    await state.handleSizeChange(20)
    expect(state.query.value.page).toBe(0)
    await state.handleReset()
    expect(state.query.value).toEqual({ name: '', page: 0, size: 10 })
    scope.stop()
  })

  it('prevents repeated deletion and returns to the last available page', async () => {
    const confirmation = deferred<Awaited<ReturnType<typeof ElMessageBox.confirm>>>()
    vi.mocked(ElMessageBox.confirm).mockReturnValueOnce(confirmation.promise)
    const load = vi.fn().mockResolvedValueOnce(page(11, 10)).mockResolvedValueOnce(page(1, 10))
    const { state, scope, remove } = setup(load)
    state.query.value.page = 1
    state.handleSelectionChange([{ id: 11 }])
    const pending = state.handleDelete()
    await state.handleDelete()
    expect(ElMessageBox.confirm).toHaveBeenCalledOnce()
    confirmation.resolve('confirm' as Awaited<ReturnType<typeof ElMessageBox.confirm>>)
    await pending
    expect(remove).toHaveBeenCalledExactlyOnceWith([11])
    expect(state.query.value.page).toBe(0)
    expect(state.tableData.value).toEqual([{ id: 1 }])
    expect(state.deleting.value).toBe(false)
    scope.stop()
  })

  it('does not delete after cancel and stops applying results after leaving the page', async () => {
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')
    const result = deferred<Page<Row>>()
    const { state, scope, remove } = setup(vi.fn().mockReturnValue(result.promise))
    state.handleSelectionChange([{ id: 1 }])
    await state.handleDelete()
    expect(remove).not.toHaveBeenCalled()
    const pending = state.fetchData()
    scope.stop()
    result.resolve(page(2))
    await pending
    expect(state.tableData.value).toEqual([])
  })
})
