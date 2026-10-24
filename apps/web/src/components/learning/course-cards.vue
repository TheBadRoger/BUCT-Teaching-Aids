<script setup lang="ts">
import type { Course } from '@/types/api'
import { learningMediaUrl } from '@/utils/learning'
defineProps<{ courses: Course[] }>()
</script>

<template>
  <div class="course-grid">
    <article v-for="course in courses" :key="course.id" class="course-card">
      <RouterLink :to="{ path: '/learn/course', query: { courseNumber: course.courseNumber } }">
        <el-image :src="learningMediaUrl(course.courseImage)" :alt="course.courseName" fit="cover" loading="lazy" class="course-image"><template #error><span class="cover-placeholder">课程封面</span></template></el-image>
        <div class="course-body"><el-tag v-if="course.courseStatus" size="small">{{ course.courseStatus }}</el-tag><h3>{{ course.courseName }}</h3><p>{{ course.teachingTeachers || '讲师待公布' }}</p><p>{{ course.viewCount || 0 }} 次访问<span v-if="course.courseTags"> · {{ course.courseTags }}</span></p></div>
      </RouterLink>
    </article>
  </div>
</template>

<style scoped>
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(240px, 100%), 1fr)); gap: 20px; }
.course-card { overflow: hidden; border: 1px solid #e1e5eb; border-radius: 8px; background: #fff; }
.course-card:hover { border-color: #165dff; }
.course-image { width: 100%; height: 160px; background: #e8f0ff; }
.cover-placeholder { display: grid; height: 100%; place-items: center; color: #646a73; }
.course-body { padding: 16px; }
.course-body h3 { font-size: 18px; margin: 8px 0; }
.course-body p { color: #646a73; font-size: 14px; margin: 8px 0 0; }
</style>
