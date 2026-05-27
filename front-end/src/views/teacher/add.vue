<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addTeacher } from '@/api/teacher'
import { useRouter } from 'vue-router'

const router = useRouter()

// 表单数据
const form = ref({
  name: '',
  organization: '',
  gender: '',
  education: '',
  jointime: ''
})

// 表单规则
const rules = {
  name: [{ required: true, message: '请输入教师姓名', trigger: 'blur' }]
}

const formRef = ref()

// 提交
const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      await addTeacher(form.value)
      ElMessage.success('新增成功')

      router.push('/teacher/list')
    } catch (err) {
      console.log(err)
    }
  })
}

// 重置
const handleReset = () => {
  formRef.value.resetFields()
}
</script>

<template>
  <div class="page-container">

    <!-- 标题 -->
    <div class="page-header">
      <span class="title">新增教师</span>
    </div>

    <!-- 表单 -->
    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-width="120px"
      class="form-box"
      style="margin: 0 auto;"
    >

      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>

      <el-form-item label="所属单位/院系">
        <el-input v-model="form.organization" />
      </el-form-item>

      <el-form-item label="性别">
        <el-select v-model="form.gender">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <el-form-item label="学历">
        <el-select v-model="form.education">
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
      </el-form-item>

      <el-form-item label="入职时间">
        <el-date-picker
          v-model="form.jointime"
          type="date"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <!-- 按钮 -->
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