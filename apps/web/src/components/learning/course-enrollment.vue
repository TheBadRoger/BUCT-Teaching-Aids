<script setup lang="ts">
import { inject, onScopeDispose, ref, watch } from 'vue'
import type { Course } from '@/types/api'
import { enrollCourse, getEnrollments, markCourseViewed } from '@/api/learning'
import { learningContextKey } from '@/composables/learningContext'
import { useAsyncResource } from '@/composables/useAsyncResource'

const props = defineProps<{ course: Course }>()
const context = inject(learningContextKey)
const busy = ref(false)
const actionError = ref('')
let disposed = false
const { data: record, loading, error, load } = useAsyncResource(async signal => {
  const student = context?.value?.student
  if (!student) return null
  let page = 0
  while (true) {
    const result = await getEnrollments(student.id, page, signal)
    const found = result.content.find(item => String(item.course.id) === String(props.course.id))
    if (found) return found
    page++
    if (!result.content.length || page * 50 >= result.totalElements) return null
  }
})
async function update(viewed?: boolean) {
  const student = context?.value?.student
  if (!student || busy.value || loading.value || error.value || disposed) return
  const courseId = props.course.id
  busy.value = true
  actionError.value = ''
  try {
    const result = viewed === undefined ? await enrollCourse(student.id, courseId) : await markCourseViewed(student.id, courseId, viewed)
    if (!disposed && props.course.id === courseId && context?.value?.student?.id === student.id) record.value = result
  } catch (reason) {
    if (!disposed) actionError.value = reason instanceof Error ? reason.message : '学习状态更新失败'
  } finally { busy.value = false }
}
function markComplete() { if (record.value && !record.value.isViewed) return update(true) }
defineExpose({ markComplete })
watch(() => [props.course.id, context?.value?.student?.id], () => { actionError.value = ''; void load() }, { immediate: true })
onScopeDispose(() => { disposed = true })
</script>

<template>
  <section class="enrollment">
    <el-alert v-if="error || actionError" :title="error || actionError" type="error" :closable="false" /><el-button v-if="error" @click="load">重试学习状态</el-button>
    <p v-if="!context?.student">选课和标记学习状态需要登录并绑定学生身份。<a href="/personal-center.html">前往绑定</a></p>
    <el-button v-else-if="!record" type="primary" :disabled="!!error || loading" :loading="busy" @click="update()">加入学习</el-button>
    <el-space v-else><el-tag type="success">已加入学习</el-tag><el-button :loading="busy" :disabled="loading || !!error" @click="update(!record.isViewed)">{{ record.isViewed ? '标记为未学' : '标记为已学' }}</el-button></el-space>
  </section>
</template>

<style scoped>
.enrollment { margin: 20px 0; }
.enrollment a { color: #165dff; }
.enrollment .el-alert { margin-bottom: 12px; }
</style>
