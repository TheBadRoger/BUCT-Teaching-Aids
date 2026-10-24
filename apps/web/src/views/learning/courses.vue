<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchLearningCourses } from '@/api/learning'
import { useAsyncResource } from '@/composables/useAsyncResource'
import CourseCards from '@/components/learning/course-cards.vue'
import { learningPage } from '@/utils/learning'

const route = useRoute()
const router = useRouter()
const page = computed(() => learningPage(route.query.page))
const filters = reactive({ courseName: '', teachingTeachers: '', courseStatus: '', courseTags: '', sort: 'id' })
const { data, error, loading, load } = useAsyncResource(signal => searchLearningCourses({
  courseName: typeof route.query.keyword === 'string' ? route.query.keyword : '',
  teachingTeachers: typeof route.query.teacher === 'string' ? route.query.teacher : '',
  courseStatus: typeof route.query.status === 'string' ? route.query.status : '',
  courseTags: typeof route.query.tags === 'string' ? route.query.tags : '',
  sort: ['id', 'viewCount', 'courseName'].includes(String(route.query.sort)) ? String(route.query.sort) : 'id',
  page: page.value - 1, size: 12,
}, signal))
function search(page = 1) {
  const location = { path: '/learn/courses', query: { keyword: filters.courseName.trim(), teacher: filters.teachingTeachers.trim(), status: filters.courseStatus, tags: filters.courseTags.trim(), sort: filters.sort, page: String(page) } }
  if (router.resolve(location).fullPath === route.fullPath) void load()
  else void router.push(location)
}
function reset() { Object.assign(filters, { courseName: '', teachingTeachers: '', courseStatus: '', courseTags: '', sort: 'id' }); search() }
watch(() => route.query, () => {
  filters.courseName = String(route.query.keyword || '')
  filters.teachingTeachers = String(route.query.teacher || '')
  filters.courseStatus = String(route.query.status || '')
  filters.courseTags = String(route.query.tags || '')
  filters.sort = String(route.query.sort || 'id')
  void load()
}, { immediate: true })
</script>

<template>
  <h1>课程大全</h1>
  <el-form inline @submit.prevent="search()"><el-form-item label="课程名称"><el-input v-model="filters.courseName" clearable /></el-form-item><el-form-item label="授课教师"><el-input v-model="filters.teachingTeachers" clearable /></el-form-item><el-form-item label="课程状态"><el-select v-model="filters.courseStatus" clearable style="width: 130px"><el-option v-for="status in ['进行中', '未开始', '已结课']" :key="status" :label="status" :value="status" /></el-select></el-form-item><el-form-item label="标签"><el-input v-model="filters.courseTags" clearable /></el-form-item><el-form-item label="排序"><el-select v-model="filters.sort" style="width: 150px"><el-option label="默认顺序" value="id" /><el-option label="访问量" value="viewCount" /><el-option label="课程名称" value="courseName" /></el-select></el-form-item><el-form-item><el-button type="primary" native-type="submit">筛选</el-button><el-button @click="reset">重置</el-button></el-form-item></el-form>
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button v-if="error" @click="load">重新加载</el-button>
  <el-skeleton v-else-if="loading" :rows="6" animated />
  <template v-else><p>共 {{ data?.totalElements || 0 }} 门课程</p><CourseCards v-if="data?.content.length" :courses="data.content" /><el-empty v-else description="没有符合条件的课程" /><el-pagination v-if="data?.totalElements" class="learning-pagination" :current-page="page" :page-size="12" :total="data.totalElements" layout="total, prev, pager, next" @current-change="search" /></template>
</template>

<style scoped>
.learning-pagination { margin-top: 24px; justify-content: center; }
</style>
