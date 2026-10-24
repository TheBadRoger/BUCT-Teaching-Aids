<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNewsStore } from '@/stores/news'
import { newsCategories, type NewsDraft } from '@/types/content'

const router = useRouter()
const store = useNewsStore()
const keyword = ref('')
const category = ref('')
const page = ref(1)
const filtered = computed(() => store.drafts.filter(item => (!category.value || item.category === category.value) && `${item.title} ${item.author}`.includes(keyword.value.trim())))
const rows = computed(() => filtered.value.slice((page.value - 1) * 10, page.value * 10))
watch([keyword, category], () => { page.value = 1 })
watch(() => filtered.value.length, total => { page.value = Math.min(page.value, Math.max(1, Math.ceil(total / 10))) })
async function remove(item: NewsDraft) {
  try { await ElMessageBox.confirm(`删除本机草稿「${item.title}」？`, '删除资讯', { type: 'warning' }) }
  catch { return }
  try { store.remove(item.id); ElMessage.success('本机草稿已删除') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '删除失败') }
}
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">资讯列表</h2>
    <el-alert title="资讯接口待接入：此处仅管理当前浏览器的本机草稿，保存不会发布到平台，请勿填写敏感信息。" type="info" show-icon :closable="false" />
    <el-alert v-if="store.error" :title="store.error" type="error" :closable="false"><el-button link @click="store.reload">重试读取</el-button></el-alert>
    <el-form inline class="search-bar" @submit.prevent>
      <el-form-item label="标题 / 作者"><el-input v-model="keyword" placeholder="请输入关键词" clearable /></el-form-item>
      <el-form-item label="资讯分类"><el-select v-model="category" placeholder="全部分类" clearable style="width: 160px"><el-option v-for="item in newsCategories" :key="item" :label="item" :value="item" /></el-select></el-form-item>
      <el-form-item><el-button @click="keyword = ''; category = ''">重置</el-button><el-button type="primary" @click="router.push('/news/add')">新增资讯</el-button></el-form-item>
    </el-form>
    <el-table :data="rows" border empty-text="暂无匹配的本机草稿，可点击新增资讯">
      <el-table-column prop="title" label="资讯标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column label="状态" width="110"><el-tag type="info">本机草稿</el-tag></el-table-column>
      <el-table-column label="创建时间" min-width="180"><template #default="{ row }">{{ new Date(row.createdAt).toLocaleString() }}</template></el-table-column>
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="router.push({ path: '/news/detail', query: { id: row.id } })">详情</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" class="pagination" :total="filtered.length" :page-size="10" layout="total, prev, pager, next" />
  </section>
</template>

<style scoped>
.pagination { margin: 24px 0; justify-content: center; }
</style>
