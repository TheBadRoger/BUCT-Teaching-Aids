<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPopularCourses, getRecommendedCourses } from '@/api/learning'
import { useAsyncResource } from '@/composables/useAsyncResource'
import CourseCards from '@/components/learning/course-cards.vue'

const router = useRouter()
const keyword = ref('')
const popular = useAsyncResource(getPopularCourses)
const recommended = useAsyncResource(getRecommendedCourses)
function search() { void router.push({ path: '/learn/courses', query: { keyword: keyword.value.trim() } }) }
onMounted(() => { void popular.load(); void recommended.load() })
</script>

<template>
  <section class="learning-hero"><h1>发现好课，开启学习</h1><p>课程、学习笔记与课堂内容，一站访问。</p><el-form inline @submit.prevent="search"><el-form-item label="搜索课程"><el-input v-model="keyword" placeholder="输入课程名称" clearable /></el-form-item><el-form-item><el-button type="primary" native-type="submit">查找课程</el-button></el-form-item></el-form></section>
  <section v-for="section in [{ title: '热门课程', resource: popular }, { title: '推荐课程', resource: recommended }]" :key="section.title" class="course-section" :aria-busy="section.resource.loading.value">
    <header><h2>{{ section.title }}</h2><RouterLink to="/learn/courses">查看全部</RouterLink></header>
    <el-alert v-if="section.resource.error.value" :title="section.resource.error.value" type="error" :closable="false" /><el-button v-if="section.resource.error.value" @click="section.resource.load">重试</el-button>
    <el-skeleton v-else-if="section.resource.loading.value" :rows="4" animated />
    <CourseCards v-else-if="section.resource.data.value?.length" :courses="section.resource.data.value" />
    <el-empty v-else description="暂无课程" />
  </section>
</template>

<style scoped>
.learning-hero { padding: 32px; background: #e8f0ff; border-radius: 12px; margin-bottom: 32px; }
.learning-hero h1 { margin: 0 0 12px; font-size: 30px; }
.learning-hero p { color: #646a73; margin-bottom: 24px; }
.course-section { margin: 32px 0; }
.course-section header { display: flex; align-items: center; justify-content: space-between; }
.course-section header a { color: #165dff; }
</style>
