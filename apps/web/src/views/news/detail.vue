<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNewsStore } from '@/stores/news'
const route = useRoute()
const router = useRouter()
const store = useNewsStore()
const draft = computed(() => typeof route.query.id === 'string' ? store.drafts.find(item => item.id === route.query.id) : undefined)
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">资讯详情</h2>
    <el-alert title="本机草稿预览：内容尚未发布，其他浏览器和用户无法查看。" type="info" show-icon :closable="false" />
    <el-alert v-if="store.error" :title="store.error" type="error" :closable="false" />
    <article v-if="draft" class="article">
      <h1>{{ draft.title }}</h1>
      <el-space wrap><el-tag>{{ draft.category }}</el-tag><el-tag type="info">本机草稿</el-tag><span>作者：{{ draft.author }}</span><time>{{ new Date(draft.createdAt).toLocaleString() }}</time></el-space>
      <p v-if="draft.summary" class="summary">{{ draft.summary }}</p>
      <p class="content">{{ draft.content }}</p>
      <el-button @click="router.push('/news/list')">返回资讯列表</el-button>
    </article>
    <el-empty v-else :description="route.query.id ? '未找到该本机草稿，可能已被删除或来自其他浏览器' : '请从资讯列表选择一条资讯查看'"><el-button type="primary" @click="router.push('/news/list')">资讯列表</el-button></el-empty>
  </section>
</template>

<style scoped>
.article { max-width: 900px; margin: 24px auto; overflow-wrap: anywhere; }
h1 { font-size: 26px; color: var(--app-header); }
.summary { padding: 16px; background: var(--app-background); white-space: pre-wrap; }
.content { margin: 28px 0; line-height: 1.9; white-space: pre-wrap; }
</style>
