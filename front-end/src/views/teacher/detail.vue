<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const teacher = ref(JSON.parse(localStorage.getItem('currentTeacherDetail') || 'null'))

const userTypeLabel = computed(() => {
  if (teacher.value?.userType === 'TEACHER') return '教师'
  if (teacher.value?.userType === 'STUDENT') return '学生'
  return teacher.value?.userType || '未绑定'
})

const displayValue = (value) => value || '暂无'

const goBack = () => {
  router.push('/teacher/list')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">教师详情</span>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-empty v-if="!teacher" description="暂无教师详情数据，请从教师列表进入" />

    <template v-else>
      <el-descriptions title="基础信息" :column="2" border>
        <el-descriptions-item label="教师ID">
          {{ displayValue(teacher.id) }}
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          {{ displayValue(teacher.name) }}
        </el-descriptions-item>
        <el-descriptions-item label="所属单位/院系">
          {{ displayValue(teacher.organization) }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ displayValue(teacher.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="学历">
          {{ displayValue(teacher.education) }}
        </el-descriptions-item>
        <el-descriptions-item label="入职时间">
          {{ displayValue(teacher.jointime) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="账号绑定信息" :column="2" border class="detail-section">
        <el-descriptions-item label="用户名">
          {{ displayValue(teacher.username) }}
        </el-descriptions-item>
        <el-descriptions-item label="用户类型">
          {{ userTypeLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="电话">
          {{ displayValue(teacher.telephone) }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ displayValue(teacher.email) }}
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
