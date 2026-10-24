<script setup lang="ts">
import { onMounted, provide } from 'vue'
import { getLearningContext } from '@/api/learning'
import { useAsyncResource } from '@/composables/useAsyncResource'
import { learningContextKey } from '@/composables/learningContext'
import logo from '@shared-images/BUCT-Logo-blue.png'

const { data: context, error, loading, load } = useAsyncResource(getLearningContext)
provide(learningContextKey, context)
onMounted(load)
</script>

<template>
  <div class="learning-shell">
    <header class="learning-header">
      <RouterLink to="/learn"><img :src="logo" alt="北京化工大学教学辅助系统" class="learning-logo" /></RouterLink>
      <nav aria-label="学习导航"><RouterLink to="/learn">学习首页</RouterLink><RouterLink to="/learn/courses">课程大全</RouterLink><a href="/personal-center.html">个人中心</a><RouterLink to="/teacher/list">管理后台</RouterLink></nav>
      <span v-if="context">{{ context.username }}</span><a v-else href="/enter.html">学生 / 教师登录</a>
    </header>
    <main class="learning-main">
      <el-alert v-if="error" title="尚未确认学生会话。请登录后刷新身份；未绑定学号时无法选课或保存笔记。" type="info" :closable="false" show-icon />
      <el-button v-if="error" :loading="loading" @click="load">刷新身份</el-button>
      <RouterView />
    </main>
    <footer class="learning-footer">北京化工大学 · 教学辅助系统</footer>
  </div>
</template>

<style scoped>
.learning-shell { min-height: 100vh; color: #1f2329; background: #f5f7fa; }
.learning-header { display: flex; flex-wrap: wrap; align-items: center; gap: 24px; padding: 16px max(20px, calc((100% - 1200px) / 2)); background: #fff; border-bottom: 1px solid #e1e5eb; }
.learning-logo { width: 180px; height: 45px; object-fit: contain; }
.learning-header nav { display: flex; flex-wrap: wrap; gap: 20px; flex: 1; }
.learning-header a:hover, .learning-header nav .router-link-exact-active { color: #165dff; }
.learning-main { max-width: 1240px; margin: 0 auto; padding: 24px 20px; }
.learning-main > .el-alert { margin-bottom: 12px; }
.learning-footer { padding: 24px; text-align: center; color: #646a73; }
</style>
