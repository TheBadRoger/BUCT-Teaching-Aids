<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import type { Organization, Page } from '@/types/api'
// 机构列表、新增及介绍共用后端机构接口。
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useFormSubmit } from '@/composables/useFormSubmit'

const route = useRoute()
const router = useRouter()
const mode = computed(() => route.path.split('/').pop() || '')
const rows = ref<Organization[]>([])
const total = ref(0)
const page = ref(1)
const name = ref('')
const busy = ref(false)
const error = ref('')
const formRef = ref<FormInstance>()
const { submitting, error: submitError, submit } = useFormSubmit(formRef)
const empty = () => ({ name: '', logo: '', bannerUrl: '', info: '', honorCertUrl: '' })
const form = ref<Organization>(empty())
const rules: FormRules = { name: [{ required: true, whitespace: true, message: '请输入机构名称', trigger: 'blur' }] }
const titles: Record<string, string> = { list: '机构列表', add: '新增机构', detail: '机构介绍' }
const title = computed(() => titles[mode.value])

/** 按路由加载列表或机构介绍。 */
async function load() {
  error.value = ''
  submitError.value = ''
  form.value = empty()
  busy.value = true
  try {
    if (mode.value === 'list') {
      const data = await request.get<Page<Organization>>('/api/organization/search', { params: { name: name.value, page: page.value - 1, size: 10 } })
      rows.value = Array.isArray(data?.content) ? data.content : []
      total.value = data?.totalElements || 0
    } else if (mode.value === 'detail' && route.query.id) {
      form.value = await request.get<Organization>(`/api/organization/${encodeURIComponent(String(route.query.id))}`)
    }
  } catch {
    error.value = '机构信息加载失败，请重试'
    rows.value = []
  } finally { busy.value = false }
}

/** 校验机构信息并提交。 */
async function save() {
  if (busy.value) return
  if (await submit(() => request.post('/api/organization/add', { ...form.value, name: form.value.name.trim() }))) {
    ElMessage.success('机构新增成功')
    void router.push('/organization/list')
  }
}

/** 确认后删除指定机构。 */
async function remove(row: Organization) {
  try { await ElMessageBox.confirm(`确定删除机构“${row.name}”吗？`, '删除机构', { type: 'warning' }) }
  catch { return }
  try {
    await request.delete(`/api/organization/${row.id}`)
    if (rows.value.length === 1 && page.value > 1) page.value--
    await load()
    ElMessage.success('删除成功')
  } catch { error.value = '删除失败，请重试' }
}

/** 从第一页开始查询。 */
function search() { page.value = 1; load() }
watch(() => route.fullPath, load, { immediate: true })
</script>

<template>
  <section class="page-container" v-loading="busy">
    <h2 class="page-title">{{ title }}</h2>
    <el-alert v-if="error || submitError" :title="error || submitError" type="error" show-icon :closable="false" />
    <template v-if="mode === 'list'">
      <el-form inline class="search-bar" @submit.prevent="search">
        <el-form-item label="机构名称"><el-input v-model="name" clearable @keyup.enter="search" /></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="name = ''; search()">重置</el-button><el-button @click="router.push('/organization/add')">新增机构</el-button></el-form-item>
      </el-form>
      <el-table :data="rows" border>
        <el-table-column prop="id" label="编号" width="90" />
        <el-table-column prop="name" label="机构名称" />
        <el-table-column prop="info" label="机构介绍" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="创建时间" />
        <el-table-column label="操作" width="180"><template #default="{ row }"><el-button size="small" @click="router.push({ path: '/organization/detail', query: { id: row.id } })">查看</el-button><el-button type="danger" size="small" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="page" :total="total" :page-size="10" layout="total, prev, pager, next" @current-change="load" />
    </template>
    <el-form v-else-if="mode === 'add'" :disabled="submitting" ref="formRef" :model="form" :rules="rules" label-width="110px" class="form-box">
      <el-form-item label="机构名称" prop="name"><el-input v-model="form.name" maxlength="200" /></el-form-item>
      <el-form-item label="机构标志"><el-input v-model="form.logo" maxlength="500" placeholder="图片地址" /></el-form-item>
      <el-form-item label="横幅图片"><el-input v-model="form.bannerUrl" maxlength="500" placeholder="图片地址" /></el-form-item>
      <el-form-item label="机构介绍"><el-input v-model="form.info" type="textarea" :rows="6" /></el-form-item>
      <el-form-item label="荣誉证书"><el-input v-model="form.honorCertUrl" maxlength="500" placeholder="图片地址" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="submitting" @click="save">提交</el-button><el-button @click="form = empty(); submitError = ''; formRef?.clearValidate()">重置</el-button></el-form-item>
    </el-form>
    <el-empty v-else-if="!route.query.id" description="请从机构列表选择机构查看介绍"><el-button @click="router.push('/organization/list')">机构列表</el-button></el-empty>
    <el-descriptions v-else-if="!error" :column="1" border>
      <el-descriptions-item label="机构名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="机构介绍"><p class="intro">{{ form.info || '暂无介绍' }}</p></el-descriptions-item>
      <el-descriptions-item label="机构标志">{{ form.logo || '未设置' }}</el-descriptions-item>
      <el-descriptions-item label="横幅图片">{{ form.bannerUrl || '未设置' }}</el-descriptions-item>
      <el-descriptions-item label="荣誉证书">{{ form.honorCertUrl || '未设置' }}</el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<style scoped>
.form-box { max-width: 560px; margin: 20px auto; }
.pagination { margin: 30px 0; justify-content: center; }
.intro { white-space: pre-wrap; }
</style>
