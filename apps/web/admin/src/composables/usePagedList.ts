import { onScopeDispose, ref, shallowRef, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { EntityId, Page, SearchParams } from '@/types/api'

export function usePagedList<T extends { id: EntityId }, Q extends SearchParams & { page: number; size: number }>(options: {
  createQuery: () => Q
  load: (query: Q, signal: AbortSignal) => Promise<Page<T>>
  remove: (ids: EntityId[]) => Promise<unknown>
  label: string
}) {
  const query = ref(options.createQuery()) as Ref<Q>
  const tableData = shallowRef<T[]>([])
  const selectedRows = shallowRef<T[]>([])
  const total = ref(0)
  const loading = ref(false)
  const deleting = ref(false)
  const error = ref('')
  let sequence = 0
  let controller: AbortController | undefined
  let disposed = false

  async function fetchData(): Promise<void> {
    if (disposed) return
    const current = ++sequence
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    error.value = ''
    selectedRows.value = []
    try {
      const result = await options.load({ ...query.value }, controller.signal)
      if (current !== sequence || disposed) return
      total.value = result.totalElements || 0
      const lastPage = Math.max(0, Math.ceil(total.value / query.value.size) - 1)
      if (query.value.page > lastPage) {
        query.value.page = lastPage
        await fetchData()
        return
      }
      tableData.value = result.content || []
    } catch (reason) {
      if (current !== sequence || disposed) return
      tableData.value = []
      total.value = 0
      error.value = reason instanceof Error ? reason.message : '列表加载失败，请重试'
    } finally {
      if (current === sequence && !disposed) loading.value = false
    }
  }
  const handleSearch = () => { query.value.page = 0; return fetchData() }
  const handleReset = () => { query.value = options.createQuery(); return fetchData() }
  const handleCurrentChange = (page: number) => { query.value.page = page - 1; return fetchData() }
  const handleSizeChange = (size: number) => { query.value.size = size; return handleSearch() }
  const handleSelectionChange = (rows: T[]) => { selectedRows.value = rows }
  async function handleDelete() {
    if (deleting.value || loading.value) return
    const ids = selectedRows.value.map(row => row.id)
    if (!ids.length) { ElMessage.warning(`请先选择要删除的${options.label}`); return }
    deleting.value = true
    try {
      try { await ElMessageBox.confirm(`确定要删除选中的${options.label}吗？`, '提示', { type: 'warning' }) }
      catch { return }
      if (disposed) return
      await options.remove(ids)
      if (disposed) return
      ElMessage.success('删除成功')
      await fetchData()
    } catch (reason) {
      error.value = reason instanceof Error ? reason.message : '删除失败，请重试'
    } finally { deleting.value = false }
  }
  onScopeDispose(() => { disposed = true; sequence++; controller?.abort() })
  return { query, tableData, total, loading, deleting, error, fetchData, handleSearch, handleReset,
    handleCurrentChange, handleSizeChange, handleSelectionChange, handleDelete }
}
