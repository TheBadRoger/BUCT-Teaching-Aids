<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { EntityId } from '@/types/api'
import { createCourseNote, getCourseNotes } from '@/api/learning'
import { learningContextKey } from '@/composables/learningContext'
import { useAsyncResource } from '@/composables/useAsyncResource'
import { useFormSubmit } from '@/composables/useFormSubmit'

const props = defineProps<{ courseId: EntityId }>()
const context = inject(learningContextKey)
const formRef = ref<FormInstance>()
const form = ref({ title: '', content: '', isPublic: false })
const rules: FormRules = { title: [{ required: true, whitespace: true, message: '请输入标题', trigger: 'blur' }], content: [{ required: true, whitespace: true, message: '请输入笔记内容', trigger: 'blur' }] }
const { submitting, error: saveError, submit } = useFormSubmit(formRef)
const { data, error, loading, load } = useAsyncResource(signal => getCourseNotes(props.courseId, signal))
const notice = ref('')
async function save() {
  const student = context?.value?.student
  if (!student) return
  const courseId = props.courseId
  notice.value = ''
  if (await submit(() => createCourseNote(courseId, student.id, { ...form.value, title: form.value.title.trim(), content: form.value.content.trim() }))) {
    if (props.courseId !== courseId) return
    form.value = { title: '', content: '', isPublic: false }
    notice.value = '笔记已保存'
    await load()
  }
}
watch(() => props.courseId, () => { form.value = { title: '', content: '', isPublic: false }; notice.value = ''; saveError.value = ''; void load() }, { immediate: true })
</script>

<template>
  <section class="notes-panel"><h2>课程笔记</h2><el-alert v-if="error" :title="error" type="warning" :closable="false" /><el-button v-if="error" @click="load">重新加载笔记</el-button><el-skeleton v-else-if="loading" :rows="3" animated /><ul v-else-if="data?.content.length" class="notes-list"><li v-for="note in data.content" :key="note.id"><h3>{{ note.title }}</h3><p class="note-content">{{ note.content }}</p><small>{{ note.student?.name || '学生' }} · {{ note.createdAt || '' }} · {{ note.likeCount || 0 }} 赞</small></li></ul><el-empty v-else description="暂无课程笔记" />
    <el-alert v-if="saveError" :title="saveError" type="error" :closable="false" /><el-alert v-if="notice" :title="notice" type="success" :closable="false" />
    <el-form v-if="context?.student" ref="formRef" :model="form" :rules="rules" :disabled="submitting" label-position="top" @submit.prevent="save"><el-form-item label="笔记标题" prop="title"><el-input v-model="form.title" maxlength="200" /></el-form-item><el-form-item label="笔记内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item><el-form-item><el-checkbox v-model="form.isPublic">公开这篇笔记</el-checkbox></el-form-item><el-button native-type="submit" type="primary" :loading="submitting">保存笔记</el-button></el-form>
    <p v-else>登录并绑定学生身份后可以保存笔记。</p>
  </section>
</template>

<style scoped>
.notes-panel { margin-top: 24px; padding: 24px; background: #fff; border-radius: 8px; }
.notes-list { padding-left: 20px; }
.notes-list li { margin-bottom: 20px; }
.notes-list small { color: #646a73; }
.note-content { white-space: pre-wrap; overflow-wrap: anywhere; }
.notes-panel .el-alert { margin: 12px 0; }
</style>
