<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addTeacher } from '@/api/teacher'
import { useRouter } from 'vue-router'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const form = ref({
  name: '',
  organization: '',
  gender: '',
  education: '',
  jointime: '',
  username: '',
  password: '123456',
  telephone: '',
  email: ''
})

const rules = {
  name: [{ required: true, message: '请输入教师姓名', trigger: 'blur' }],
  organization: [{ required: true, message: '请输入所属单位/院系', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  education: [{ required: true, message: '请选择学历', trigger: 'change' }],
  jointime: [{ required: true, message: '请选择入职时间', trigger: 'change' }],
  username: [{ required: true, message: '请输入登录用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (submitting.value) return

    try {
      submitting.value = true
      await addTeacher({
        teacher: {
          name: form.value.name.trim(),
          organization: form.value.organization.trim(),
          gender: form.value.gender,
          education: form.value.education,
          jointime: form.value.jointime
        },
        username: form.value.username.trim(),
        password: form.value.password,
        telephone: form.value.telephone.trim(),
        email: form.value.email.trim()
      })
      ElMessage.success('新增成功')
      router.push('/teacher/list')
    } catch (err) {
      console.log(err)
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  formRef.value.resetFields()
}
</script>

<template>
  <div class="page-container">

    <div class="page-header">
      <span class="title">新增教师</span>
    </div>

    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-width="120px"
      class="form-box"
      style="margin: 0 auto;"
    >
      <el-divider content-position="left">基础信息</el-divider>

      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" clearable />
      </el-form-item>

      <el-form-item label="所属单位/院系" prop="organization">
        <el-input v-model="form.organization" clearable />
      </el-form-item>

      <el-form-item label="性别" prop="gender">
        <el-select v-model="form.gender" clearable>
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <el-form-item label="学历" prop="education">
        <el-select v-model="form.education" clearable>
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
      </el-form-item>

      <el-form-item label="入职时间" prop="jointime">
        <el-date-picker
          v-model="form.jointime"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
        />
      </el-form-item>

      <el-divider content-position="left">账号信息</el-divider>

      <el-form-item label="登录用户名" prop="username">
        <el-input v-model="form.username" clearable />
      </el-form-item>

      <el-form-item label="初始密码" prop="password">
        <el-input v-model="form.password" type="password" show-password clearable />
      </el-form-item>

      <el-form-item label="电话">
        <el-input v-model="form.telephone" clearable />
      </el-form-item>

      <el-form-item label="邮箱">
        <el-input v-model="form.email" clearable />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>

    </el-form>

  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.title {
  font-size: 22px;
  font-weight: bold;
  color: #333;
}

.form-box {
  width: 560px;
}
</style>
