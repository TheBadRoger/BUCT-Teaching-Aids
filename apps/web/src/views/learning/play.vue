<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLearningCourse } from '@/composables/useLearningCourse'
import { learningMediaUrl, lessonIndex, parseCourseLessons } from '@/utils/learning'
import CourseEnrollment from '@/components/learning/course-enrollment.vue'
import CourseNotes from '@/components/learning/course-notes.vue'

const route = useRoute()
const router = useRouter()
const { data: course, loading, error, load } = useLearningCourse()
const lessons = computed(() => parseCourseLessons(course.value?.courseOutline))
const index = computed(() => lessonIndex(route.query.lesson, lessons.value.length))
const current = computed(() => lessons.value[index.value])
const enrollment = ref<InstanceType<typeof CourseEnrollment>>()
const videoError = ref('')
watch(() => [course.value?.id, index.value], () => { videoError.value = '' })
function selectLesson(next: number) {
  videoError.value = ''
  void router.replace({ path: '/learn/play', query: { courseNumber: course.value?.courseNumber, lesson: next + 1 } })
}
</script>

<template>
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button v-if="error" @click="load">重试</el-button><el-skeleton v-else-if="loading" :rows="8" animated />
  <template v-else-if="course"><RouterLink :to="{ path: '/learn/course', query: { courseNumber: course.courseNumber } }">返回课程详情</RouterLink><h1>{{ current?.title || course.courseName }}</h1><p>{{ course.courseName }} · {{ course.teachingTeachers }}</p>
    <div class="player-layout"><section><video v-if="current?.videoUrl" :key="`${course.id}-${index}`" :src="current.videoUrl" :poster="learningMediaUrl(course.courseImage)" controls preload="metadata" class="lesson-video" @ended="enrollment?.markComplete()" @error="videoError = '视频暂时无法播放，请检查媒体地址或联系授课教师。'" /><el-empty v-else description="本课时未配置视频地址，请联系授课教师" /><el-alert v-if="videoError" :title="videoError" type="error" :closable="false" /><el-space class="lesson-controls"><el-button :disabled="index <= 0" @click="selectLesson(index - 1)">上一课时</el-button><el-button :disabled="index >= lessons.length - 1" @click="selectLesson(index + 1)">下一课时</el-button></el-space></section><aside><h2>课程目录</h2><el-empty v-if="!lessons.length" description="暂无课时" /><ol v-else><li v-for="(lesson, position) in lessons" :key="position"><button :aria-current="position === index ? 'true' : undefined" class="lesson-item" @click="selectLesson(position)">{{ position + 1 }}. {{ lesson.title }}</button></li></ol></aside></div>
    <CourseEnrollment :key="String(course.id)" ref="enrollment" :course="course" /><CourseNotes :key="String(course.id)" :course-id="course.id" />
  </template>
</template>

<style scoped>
.player-layout { display: grid; grid-template-columns: minmax(0, 1fr); gap: 24px; }
.lesson-video { width: 100%; max-height: 70vh; background: #111; border-radius: 8px; }
.lesson-controls { margin-top: 16px; }
.player-layout aside { padding: 20px; background: #fff; border-radius: 8px; }
.player-layout ol { list-style: none; padding: 0; }
.lesson-item { border: 0; background: transparent; padding: 12px; width: 100%; text-align: left; cursor: pointer; overflow-wrap: anywhere; }
.lesson-item[aria-current="true"] { background: #e8f0ff; color: #165dff; }
@media (min-width: 1000px) { .player-layout { grid-template-columns: minmax(0, 3fr) minmax(240px, 1fr); } }
</style>
