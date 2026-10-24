<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import type { CalendarEvent, LivePlan } from '@/types/application'
// 应用页面提供本地交互；邮件发送、直播推流需后端服务支持。
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const mode = computed(() => route.path.split('/').pop() || '')
const titles: Record<string, string> = { widgets: '小部件', email: '电子邮件', calendar: '日历', add: '新增直播', room: '直播间' }
const error = ref('')
const date = ref(new Date())
const eventDate = ref('')
const eventTitle = ref('')
const note = ref('')
const events = ref<CalendarEvent[]>([])
const lives = ref<LivePlan[]>([])
const mail = ref({ to: '', subject: '', body: '' })
const live = ref({ title: '', teacher: '', start: '', description: '' })
const liveForm = ref<FormInstance>()
const query = ref('')
const selected = ref<LivePlan | null>(null)
const rules: FormRules = Object.fromEntries(['title', 'teacher', 'start'].map(key => [key, [{ required: true, whitespace: true, message: '请填写此项', trigger: 'blur' }]]))
const rooms = computed(() => lives.value.filter(item => item.title.includes(query.value)))
try {
  const storedEvents = JSON.parse(localStorage.getItem('buct-calendar') || '[]')
  const storedLives = JSON.parse(localStorage.getItem('buct-lives') || '[]')
  events.value = Array.isArray(storedEvents) ? storedEvents.filter((item: Partial<CalendarEvent> | null): item is CalendarEvent => typeof item?.id === 'string' && typeof item.title === 'string' && typeof item.date === 'string') : []
  lives.value = Array.isArray(storedLives) ? storedLives.filter((item: Partial<LivePlan> | null): item is LivePlan => typeof item?.id === 'string' && typeof item.title === 'string' && typeof item.teacher === 'string' && typeof item.start === 'string' && typeof item.description === 'string') : []
  note.value = localStorage.getItem('buct-note') || ''
  const draft = JSON.parse(localStorage.getItem('buct-mail') || 'null')
  if (draft && ['to', 'subject', 'body'].every(key => typeof draft[key] === 'string')) mail.value = draft
} catch { error.value = '本地数据读取失败，可继续使用页面' }

/** 保存本地数据，失败时保留输入并提示。 */
function persist(key: string, value: unknown) {
  try { localStorage.setItem(key, typeof value === 'string' ? value : JSON.stringify(value)); return true }
  catch { error.value = '浏览器存储不可用，数据未保存'; return false }
}

/** 添加指定日期的日程。 */
function addEvent() {
  if (!eventDate.value || !eventTitle.value.trim()) return ElMessage.warning('请填写日期和日程')
  const next = [...events.value, { id: crypto.randomUUID(), date: eventDate.value, title: eventTitle.value.trim() }]
  if (persist('buct-calendar', next)) { events.value = next; eventTitle.value = '' }
}

/** 删除本地日程。 */
function removeEvent(id: string) {
  const next = events.value.filter(item => item.id !== id)
  if (persist('buct-calendar', next)) events.value = next
}

/** 保存直播计划，不产生真实直播间或推流地址。 */
async function addLive() {
  if (!await liveForm.value?.validate().catch(() => false)) return
  const next = [...lives.value, { ...live.value, title: live.value.title.trim(), id: crypto.randomUUID() }]
  if (!persist('buct-lives', next)) return
  lives.value = next
  live.value = { title: '', teacher: '', start: '', description: '' }
  ElMessage.success('直播计划已保存到本机')
  router.push('/application/live/room')
}

/** 调用系统邮件客户端，由用户确认发送。 */
function openMail() {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(mail.value.to)) return ElMessage.warning('请输入有效的收件人邮箱')
  window.location.href = `mailto:${encodeURIComponent(mail.value.to)}?subject=${encodeURIComponent(mail.value.subject)}&body=${encodeURIComponent(mail.value.body)}`
}
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">{{ titles[mode] }}</h2>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert title="本地前端功能：数据仅保存在当前浏览器，不会同步到服务器；邮件服务和直播服务尚未接入。请勿保存敏感信息。" type="info" :closable="false" show-icon />
    <div v-if="mode === 'widgets'" class="widgets">
      <el-card><template #header>快捷入口</template><el-space wrap><el-button @click="router.push('/organization/list')">机构管理</el-button><el-button @click="router.push('/application/calendar')">日程安排</el-button><el-button @click="router.push('/application/live/room')">直播计划</el-button></el-space></el-card>
      <el-card><template #header>便签</template><el-input v-model="note" type="textarea" :rows="6" aria-label="便签内容" /><el-button class="action" type="primary" @click="persist('buct-note', note) && ElMessage.success('便签已保存')">保存便签</el-button></el-card>
      <el-card><template #header>日程概览</template><p>已安排 {{ events.length }} 项日程</p><p>已保存 {{ lives.length }} 场直播计划</p></el-card>
    </div>
    <el-form v-else-if="mode === 'email'" label-width="100px" class="form-box">
      <el-form-item label="收件人"><el-input v-model="mail.to" type="email" /></el-form-item>
      <el-form-item label="主题"><el-input v-model="mail.subject" /></el-form-item>
      <el-form-item label="正文"><el-input v-model="mail.body" type="textarea" :rows="10" /></el-form-item>
      <el-form-item><el-button type="primary" @click="openMail">打开邮件客户端</el-button><el-button @click="persist('buct-mail', mail) && ElMessage.success('草稿已保存')">保存本地草稿</el-button></el-form-item>
    </el-form>
    <template v-else-if="mode === 'calendar'">
      <el-form inline class="action"><el-form-item label="日期"><el-date-picker v-model="eventDate" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="日程"><el-input v-model="eventTitle" @keyup.enter="addEvent" /></el-form-item><el-form-item><el-button type="primary" @click="addEvent">添加日程</el-button></el-form-item></el-form>
      <el-calendar v-model="date"><template #date-cell="{ data }"><p>{{ data.day.slice(-2) }}</p><p v-for="event in events.filter(item => item.date === data.day)" :key="event.id" class="event">{{ event.title }}</p></template></el-calendar>
      <el-table :data="events" border><el-table-column prop="date" label="日期" /><el-table-column prop="title" label="日程" /><el-table-column label="操作"><template #default="{ row }"><el-button type="danger" size="small" @click="removeEvent(row.id)">删除</el-button></template></el-table-column></el-table>
    </template>
    <el-form v-else-if="mode === 'add'" ref="liveForm" :model="live" :rules="rules" label-width="100px" class="form-box">
      <el-form-item label="直播名称" prop="title"><el-input v-model="live.title" /></el-form-item>
      <el-form-item label="主讲教师" prop="teacher"><el-input v-model="live.teacher" /></el-form-item>
      <el-form-item label="开始时间" prop="start"><el-date-picker v-model="live.start" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      <el-form-item label="直播介绍"><el-input v-model="live.description" type="textarea" :rows="5" /></el-form-item>
      <el-form-item><el-button type="primary" @click="addLive">保存本地计划</el-button><el-button @click="liveForm?.resetFields(); live.description = ''">重置</el-button></el-form-item>
    </el-form>
    <template v-else-if="mode === 'room'">
      <el-form inline class="action"><el-form-item label="直播名称"><el-input v-model="query" clearable /></el-form-item><el-form-item><el-button type="primary" @click="router.push('/application/live/add')">新增直播</el-button></el-form-item></el-form>
      <el-table :data="rooms" border><el-table-column prop="title" label="直播名称" /><el-table-column prop="teacher" label="主讲教师" /><el-table-column prop="start" label="计划时间" /><el-table-column label="状态"><el-tag type="info">本地计划</el-tag></el-table-column><el-table-column label="操作"><template #default="{ row }"><el-button size="small" @click="selected = row">查看</el-button></template></el-table-column></el-table>
      <el-dialog :model-value="!!selected" title="直播间" width="min(600px, 90vw)" @close="selected = null"><template v-if="selected"><h3>{{ selected.title }}</h3><p>主讲：{{ selected.teacher }}</p><p>计划时间：{{ selected.start }}</p><p class="description">{{ selected.description }}</p><el-empty description="直播推流和播放服务尚未接入" /></template></el-dialog>
    </template>
  </section>
</template>

<style scoped>
.widgets { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 20px; margin-top: 20px; }
.form-box { max-width: 680px; margin: 24px auto; }
.action { margin-top: 20px; }
.event { overflow: hidden; color: #325a7d; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.description { white-space: pre-wrap; }
</style>
