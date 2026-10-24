<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useNewsStore } from '@/stores/news'
import { useSessionStore } from '@/stores/session'
import { useFormSubmit } from '@/composables/useFormSubmit'
import { newsCategories, type NewsDraft } from '@/types/content'

const router = useRouter()
const store = useNewsStore()
const formRef = ref<FormInstance>()
const form = ref<Omit<NewsDraft, 'id' | 'createdAt'>>({ title: '', author: useSessionStore().user?.username || '', category: '校园动态', summary: '', content: '' })
const rules: FormRules = Object.fromEntries(['title', 'author', 'content'].map(field => [field, [{ required: true, whitespace: true, message: '请填写此项', trigger: 'blur' }]]))
const { submit, submitting, error } = useFormSubmit(formRef)
async function save() {
  let id = ''
  if (await submit(async () => { id = store.add(form.value) })) {
    ElMessage.success('已保存到本机，尚未发布')
    await router.push({ path: '/news/detail', query: { id } })
  }
}
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">新增资讯</h2>
    <el-alert title="资讯接口待接入：仅保存本机草稿，不会发布或同步到服务器。" type="info" show-icon :closable="false" />
    <el-alert v-if="error || store.error" :title="error || store.error" type="error" show-icon :closable="false" />
    <el-form ref="formRef" :model="form" :rules="rules" :disabled="submitting" label-width="100px" class="news-form" @submit.prevent="save">
      <el-form-item label="资讯标题" prop="title"><el-input v-model="form.title" maxlength="120" show-word-limit placeholder="请输入资讯标题" /></el-form-item>
      <el-form-item label="资讯分类"><el-select v-model="form.category"><el-option v-for="item in newsCategories" :key="item" :label="item" :value="item" /></el-select></el-form-item>
      <el-form-item label="作者" prop="author"><el-input v-model="form.author" maxlength="50" /></el-form-item>
      <el-form-item label="资讯摘要"><el-input v-model="form.summary" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item>
      <el-form-item label="资讯正文" prop="content"><el-input v-model="form.content" type="textarea" :rows="12" maxlength="20000" show-word-limit placeholder="请输入正文，支持换行" /></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit" :loading="submitting">保存本机草稿</el-button><el-button @click="router.push('/news/list')">返回列表</el-button></el-form-item>
    </el-form>
  </section>
</template>

<style scoped>
.news-form { max-width: 860px; margin: 24px auto; }
</style>
