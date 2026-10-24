<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ref } from 'vue'
import { useFormSubmit } from '@/composables/useFormSubmit'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addStudent } from '@/api/student'

const router = useRouter()

const formRef = ref<FormInstance>()
const { submitting, error: submitError, submit } = useFormSubmit(formRef)
const form = ref({
  studentNumber: '',
  name: '',
  className: '',
  gender: '',
  admissionDate: '',
  username: '',
  password: '123456',
  telephone: '',
  email: ''
})

const rules: FormRules = {
  studentNumber: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入学生姓名', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  admissionDate: [{ required: true, message: '请选择入学时间', trigger: 'change' }],
  username: [{ required: true, message: '请输入登录用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

const handleSubmit = async () => {
  if (await submit(() => addStudent({
    student: {
      studentNumber: form.value.studentNumber.trim(),
      name: form.value.name.trim(),
      className: form.value.className.trim(),
      gender: form.value.gender,
      admissionDate: form.value.admissionDate
    },
    username: form.value.username.trim(),
    password: form.value.password,
    telephone: form.value.telephone.trim(),
    email: form.value.email.trim()
  }))) {
    ElMessage.success('新增成功')
    void router.push('/student/list')
  }
}

const handleReset = () => {
  if (submitting.value) return
  submitError.value = ''
  formRef.value?.resetFields()
}
</script>

<template>
  <div class="page-container">

    <div class="page-header">
      <span class="title">新增学生</span>
    </div>

    <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon />
    <el-form
      :disabled="submitting"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="form-box"
      style="margin: 0 auto;"
    >
      <el-divider content-position="left">基础信息</el-divider>

      <el-form-item label="学号" prop="studentNumber">
        <el-input v-model="form.studentNumber" clearable />
      </el-form-item>

      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" clearable />
      </el-form-item>

      <el-form-item label="班级" prop="className">
        <el-input v-model="form.className" clearable />
      </el-form-item>

      <el-form-item label="性别" prop="gender">
        <el-select v-model="form.gender" clearable>
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <el-form-item label="入学时间" prop="admissionDate">
        <el-date-picker
          v-model="form.admissionDate"
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
