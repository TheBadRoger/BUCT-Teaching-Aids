<script setup lang="ts">
import { computed } from 'vue'
import { useLearningCourse } from '@/composables/useLearningCourse'
import { learningMediaUrl, parseCourseLessons } from '@/utils/learning'
import CourseEnrollment from '@/components/learning/course-enrollment.vue'
import CourseNotes from '@/components/learning/course-notes.vue'

const { data: course, loading, error, load } = useLearningCourse()
const lessons = computed(() => parseCourseLessons(course.value?.courseOutline))
</script>

<template>
  <RouterLink to="/learn/courses">返回课程大全</RouterLink>
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button v-if="error" @click="load">重试</el-button><el-skeleton v-else-if="loading" :rows="8" animated />
  <template v-else-if="course">
    <section class="course-hero"><el-image class="course-cover" :src="learningMediaUrl(course.courseImage)" :alt="course.courseName" fit="cover"><template #error>课程封面</template></el-image><div><h1>{{ course.courseName }}</h1><el-tag v-if="course.courseStatus">{{ course.courseStatus }}</el-tag><p>{{ course.courseTags }}</p><p>编号：{{ course.courseNumber }} · 授课教师：{{ course.teachingTeachers || '待公布' }}</p><p>{{ course.startDate }} · {{ course.duration }} · {{ course.classAddress }}</p><p>适用对象：{{ course.targetAudience || '未填写' }} · 开课班级：{{ course.teachingClasses || '未填写' }}</p><p>{{ Number(course.coursePrice) > 0 ? `¥ ${Number(course.coursePrice).toFixed(2)}` : '免费' }} · {{ course.viewCount || 0 }} 次访问</p><CourseEnrollment :key="String(course.id)" :course="course" /><RouterLink :to="{ path: '/learn/play', query: { courseNumber: course.courseNumber } }"><el-button type="primary">开始上课</el-button></RouterLink></div></section>
    <section class="course-description"><h2>课程简介</h2><p>{{ course.courseIntroduction || '暂无课程简介' }}</p><h2>教学目标</h2><p>{{ course.teachingObjectives || '暂无教学目标' }}</p><h2>课程大纲</h2><ol v-if="lessons.length"><li v-for="(lesson, index) in lessons" :key="index"><RouterLink :to="{ path: '/learn/play', query: { courseNumber: course.courseNumber, lesson: index + 1 } }">{{ lesson.title }}</RouterLink></li></ol><el-empty v-else description="暂无课时" /></section>
    <CourseNotes :key="String(course.id)" :course-id="course.id" />
  </template>
</template>

<style scoped>
.course-hero { margin-top: 20px; display: grid; grid-template-columns: minmax(0, 1fr); gap: 24px; padding: 24px; background: #fff; border-radius: 8px; }
.course-cover { width: 100%; height: 260px; background: #e8f0ff; border-radius: 8px; }
.course-description { margin-top: 24px; padding: 24px; background: #fff; border-radius: 8px; }
.course-description p { white-space: pre-wrap; overflow-wrap: anywhere; }
.course-description li { margin-bottom: 12px; color: #165dff; }
@media (min-width: 900px) { .course-hero { grid-template-columns: minmax(280px, 2fr) 3fr; } }
</style>
