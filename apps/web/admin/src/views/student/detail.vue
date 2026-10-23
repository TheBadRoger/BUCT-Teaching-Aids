<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const student = ref(JSON.parse(localStorage.getItem('currentStudentDetail') || 'null'))

const userTypeLabel = computed(() => {
  if (student.value?.userType === 'TEACHER') return '教师'
  if (student.value?.userType === 'STUDENT') return '学生'
  return student.value?.userType || '未绑定'
})

const displayValue = (value) => value || '暂无'

const goBack = () => {
  router.push('/student/list')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">学生详情</span>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-empty v-if="!student" description="暂无学生详情数据，请从学生列表进入" />

    <template v-else>
      <el-descriptions title="基础信息" :column="2" border>
        <el-descriptions-item label="学生ID">
          {{ displayValue(student.id) }}
        </el-descriptions-item>
        <el-descriptions-item label="学号">
          {{ displayValue(student.studentNumber) }}
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          {{ displayValue(student.name) }}
        </el-descriptions-item>
        <el-descriptions-item label="班级">
          {{ displayValue(student.className) }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ displayValue(student.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="入学时间">
          {{ displayValue(student.admissionDate) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="账号绑定信息" :column="2" border class="detail-section">
        <el-descriptions-item label="用户名">
          {{ displayValue(student.username) }}
        </el-descriptions-item>
        <el-descriptions-item label="用户类型">
          {{ userTypeLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="电话">
          {{ displayValue(student.telephone) }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ displayValue(student.email) }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 22px;
  font-weight: bold;
  color: #333;
}

.detail-section {
  margin-top: 24px;
}
</style>
