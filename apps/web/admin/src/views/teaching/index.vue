<script setup lang="ts">
// 教师 AI 工具迁入统一 Vue 前端，复用原有 Express 服务。
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { TeachingCatalog, TeachingInput } from '@/types/teaching'
import { getTeachingCatalog, getTeachingHealth } from '@/api/teaching'
import { knowledgeText, lessonText } from '@/utils/teaching-content'
import KnowledgeResult from '@/components/knowledge-result.vue'
import LessonResult from '@/components/lesson-result.vue'
import { useTeachingGeneration } from '@/composables/useTeachingGeneration'

const route = useRoute()
const isKnowledge = computed(() => route.path.endsWith('/knowledge'))
const title = computed(() => isKnowledge.value ? '知识点手册' : '智能教案')
const formRef = ref<FormInstance>()
const form = ref<TeachingInput>({ subject: '', grade: '', topic: '', focus: '', lessonType: '新授课', duration: 45, teachingStyle: '', extraRequirements: '' })
const rules: FormRules<TeachingInput> = Object.fromEntries(['subject', 'grade', 'topic'].map(key => [key, [{ required: true, whitespace: true, message: '请填写此项', trigger: 'blur' }]]))
const catalog = ref<TeachingCatalog>({ subjects: [], grades: [], lessonTypes: ['新授课', '复习课', '习题课', '实验课', '讲评课', '活动课'] })
const health = ref<{ ready: boolean; model: string }>({ ready: false, model: '' })
const connecting = ref(false)
const connectionError = ref('')
const useStream = ref(true)
const { busy, error, notice, raw, handbook, lesson, generate, cancel } = useTeachingGeneration({ formRef, form, isKnowledge, useStream })
const setupController = new AbortController()
const output = computed(() => isKnowledge.value ? handbook.value ? knowledgeText(handbook.value) : '' : lesson.value ? lessonText(lesson.value) : raw.value)

/** 刷新服务状态和学科目录；失败不使用虚假的就绪状态。 */
async function connect() {
  if (connecting.value) return
  connecting.value = true
  connectionError.value = ''
  const signal = AbortSignal.any([setupController.signal, AbortSignal.timeout(10000)])
  const results = await Promise.allSettled([getTeachingHealth(signal), getTeachingCatalog(signal)])
  if (!setupController.signal.aborted) {
    const [service, subjects] = results
    health.value = service.status === 'fulfilled' ? service.value : { ready: false, model: '' }
    if (subjects.status === 'fulfilled') catalog.value = subjects.value
    if (results.some(item => item.status === 'rejected')) connectionError.value = '教师 AI 服务连接或目录加载失败，可刷新重试；学科和年级也可直接输入。'
  }
  connecting.value = false
}

/** 剪贴板受浏览器权限限制时，仍可使用文本导出。 */
async function copy() {
  try { await navigator.clipboard.writeText(output.value); ElMessage.success('已复制') }
  catch { ElMessage.warning('复制失败，请使用导出文本') }
}

/** 文件名移除特殊字符，导出内容保持纯文本。 */
function download() {
  const name = (isKnowledge.value ? handbook.value?.chapterTitle : lesson.value?.title) || title.value
  const link = document.createElement('a')
  const url = URL.createObjectURL(new Blob([output.value], { type: 'text/plain;charset=utf-8' }))
  link.href = url
  link.download = `${name.replace(/[<>:"/\\|?*]|\p{Cc}/gu, '_').slice(0, 80)}.txt`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

watch(isKnowledge, () => formRef.value?.clearValidate())
onMounted(connect)
onUnmounted(() => setupController.abort())
</script>

<template>
  <section class="page-container">
    <header class="teaching-header"><h2 class="page-title">{{ title }}</h2><el-space><el-tag :type="health.ready ? 'success' : 'info'">{{ health.ready ? `${health.model} · 已就绪` : 'AI 服务待就绪' }}</el-tag><el-button :loading="connecting" @click="connect">刷新连接</el-button></el-space></header>
    <el-alert v-if="connectionError" :title="connectionError" type="warning" :closable="false" show-icon />
    <el-alert v-else-if="!connecting && !health.ready" title="服务尚未配置 AI 能力，请联系平台管理员。" type="info" :closable="false" show-icon />
    <div class="teaching-layout">
      <aside>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="busy" @submit.prevent="generate">
          <el-form-item label="学科" prop="subject"><el-select v-model="form.subject" filterable allow-create default-first-option placeholder="选择或输入学科"><el-option v-for="subject in catalog.subjects" :key="subject.name" :label="subject.name" :value="subject.name" /></el-select></el-form-item>
          <el-form-item label="年级" prop="grade"><el-select v-model="form.grade" filterable allow-create default-first-option placeholder="选择或输入年级"><el-option v-for="grade in catalog.grades" :key="grade" :label="grade" :value="grade" /></el-select></el-form-item>
          <el-form-item :label="isKnowledge ? '章节 / 主题' : '课题'" prop="topic"><el-input v-model="form.topic" maxlength="200" placeholder="例如：一次函数的图像与性质" @keyup.enter="generate" /></el-form-item>
          <el-form-item v-if="isKnowledge" label="重点关注"><el-input v-model="form.focus" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
          <template v-else>
            <el-form-item label="课型"><el-select v-model="form.lessonType"><el-option v-for="type in catalog.lessonTypes" :key="type" :label="type" :value="type" /></el-select></el-form-item>
            <el-form-item label="课时（分钟）"><el-input-number v-model="form.duration" :min="1" :max="240" :value-on-clear="45" /></el-form-item>
            <el-form-item label="教学风格"><el-select v-model="form.teachingStyle" clearable><el-option v-for="style in ['探究式教学', '启发式教学', '合作学习', '翻转课堂', 'PBL 项目式学习']" :key="style" :label="style" :value="style" /></el-select></el-form-item>
            <el-form-item label="额外要求"><el-input v-model="form.extraRequirements" type="textarea" :rows="4" maxlength="2000" placeholder="如：包含实验环节、分层作业" /></el-form-item>
            <el-form-item label="逐步显示生成内容"><el-switch v-model="useStream" /></el-form-item>
          </template>
          <el-button type="primary" :loading="busy" @click="generate">生成{{ title }}</el-button>
        </el-form>
        <el-button v-if="busy" class="cancel" @click="cancel()">取消生成</el-button>
      </aside>
      <section class="result-panel" aria-live="polite" :aria-busy="busy">
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon /><el-button v-if="error" class="retry" :disabled="busy" @click="generate">重试</el-button>
        <el-alert v-if="notice" :title="notice" type="info" :closable="false" />
        <el-space v-if="output" class="result-actions"><el-button :disabled="busy" @click="copy">复制{{ !isKnowledge && !lesson ? '原文' : '' }}</el-button><el-button :disabled="busy" @click="download">导出文本</el-button></el-space>
        <KnowledgeResult v-if="isKnowledge && handbook" :handbook="handbook" />
        <LessonResult v-else-if="!isKnowledge && lesson" :lesson="lesson" />
        <pre v-else-if="!isKnowledge && raw" class="raw-output">{{ raw }}</pre>
        <el-empty v-else :description="busy ? 'AI 正在生成，请稍候……' : `填写设置后生成${title}`" />
      </section>
    </div>
  </section>
</template>

<style scoped>
.teaching-header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 20px; }
.page-title { margin-bottom: 0; }
.teaching-layout { display: grid; grid-template-columns: minmax(0, 1fr); gap: 24px; margin-top: 24px; }
.result-panel { min-width: 0; padding: 20px; border: 1px solid var(--el-border-color); border-radius: 6px; text-align: left; }
.cancel, .retry { margin-top: 12px; }
.result-actions { margin-bottom: 20px; }
.raw-output { white-space: pre-wrap; overflow-wrap: anywhere; font-family: inherit; line-height: 1.8; }
@media (min-width: 1100px) { .teaching-layout { grid-template-columns: 300px minmax(0, 1fr); } }
</style>
