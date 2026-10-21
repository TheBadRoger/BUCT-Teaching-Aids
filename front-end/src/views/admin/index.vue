<script setup>
// 管理员新增接入现有注册接口，列表仅展示当前会话和本次新增结果。
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const adding = computed(() => route.path.endsWith('/add'))
const formRef = ref()
const form = ref({ username: '', password: '', confirm: '' })
const busy = ref(false)
const error = ref('')
const keyword = ref('')
const created = ref([])
let current = {}
try { current = JSON.parse(localStorage.getItem('user') || '{}') || {} } catch { current = {} }
const rows = computed(() => [current, ...created.value].filter(item => item.username && item.username.includes(keyword.value)))
const rules = {
  username: [{ required: true, whitespace: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirm: [{ validator: (_rule, value, callback) => callback(value && value === form.value.password ? undefined : new Error('两次密码输入不一致')), trigger: 'blur' }]
}

/** 新增管理员，不在浏览器存储密码。 */
async function save() {
  if (busy.value || !await formRef.value.validate().catch(() => false)) return
  busy.value = true
  error.value = ''
  try {
    const user = await registerApi({ username: form.value.username.trim(), password: form.value.password })
    created.value.push({ id: user?.id, username: user?.username || form.value.username.trim() })
    form.value = { username: '', password: '', confirm: '' }
    ElMessage.success('管理员新增成功')
    router.push('/admin/list')
  } catch { error.value = '新增失败，请检查用户名后重试' }
  finally { busy.value = false }
}
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">{{ adding ? '新增管理员' : '管理员列表' }}</h2>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form v-if="adding" ref="formRef" :model="form" :rules="rules" label-width="110px" class="form-box">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" autocomplete="off" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" /></el-form-item>
      <el-form-item label="确认密码" prop="confirm"><el-input v-model="form.confirm" type="password" show-password autocomplete="new-password" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="busy" @click="save">提交</el-button><el-button @click="formRef.resetFields()">重置</el-button></el-form-item>
    </el-form>
    <template v-else>
      <el-alert title="尚无管理员列表接口，当前仅显示登录账号及本次新增账号，刷新后新增记录需后端列表接口查询。" type="info" :closable="false" show-icon />
      <el-form inline class="search-bar"><el-form-item label="用户名"><el-input v-model="keyword" clearable /></el-form-item><el-form-item><el-button type="primary" @click="router.push('/admin/add')">新增管理员</el-button></el-form-item></el-form>
      <el-table :data="rows" border><el-table-column prop="id" label="编号" /><el-table-column prop="username" label="用户名" /></el-table>
    </template>
  </section>
</template>

<style scoped>
.form-box { max-width: 560px; margin: 20px auto; }
.search-bar { margin-top: 20px; }
</style>
