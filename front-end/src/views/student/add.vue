<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addStudent } from '@/api/student'

const router = useRouter()

const formRef = ref()
const form = ref({
  studentNumber: '',
  name: '',
  className: '',
  gender: '',
  admissionDate: ''
})

const rules = {
  studentNumber: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入学生姓名', trigger: 'blur' }
  ],
  className: [
    { required: true, message: '请输入班级', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  admissionDate: [
    { required: true, message: '请选择入学时间', trigger: 'change' }
  ]
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      await addStudent(form.value)
      ElMessage.success('新增成功')
      router.push('/student/list')
    } catch (err) {
      console.log(err)
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
      <span class="title">新增学生</span>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="form-box"
      style="margin: 0 auto;"
    >
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

      <el-form-item>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
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
  width: 500px;
}
</style>
