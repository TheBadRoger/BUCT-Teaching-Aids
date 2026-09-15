<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const course = ref(JSON.parse(localStorage.getItem('currentCourseDetail') || 'null'))

const displayValue = (value) => {
  if (value === 0) return 0
  return value || '暂无'
}

const goBack = () => {
  router.push('/course/list')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">课程详情</span>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-empty v-if="!course" description="暂无课程详情数据，请从课程列表进入" />

    <template v-else>
      <el-descriptions title="基础信息" :column="2" border>
        <el-descriptions-item label="课程ID">
          {{ displayValue(course.id) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程编号">
          {{ displayValue(course.courseNumber) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程名称">
          {{ displayValue(course.courseName) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程状态">
          {{ displayValue(course.courseStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="开课日期">
          {{ displayValue(course.startDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程时长">
          {{ displayValue(course.duration) }}
        </el-descriptions-item>
        <el-descriptions-item label="授课教师">
          {{ displayValue(course.teachingTeachers) }}
        </el-descriptions-item>
        <el-descriptions-item label="授课班级">
          {{ displayValue(course.teachingClasses) }}
        </el-descriptions-item>
        <el-descriptions-item label="目标人群">
          {{ displayValue(course.targetAudience) }}
        </el-descriptions-item>
        <el-descriptions-item label="上课地点">
          {{ displayValue(course.classAddress) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程价格">
          {{ displayValue(course.coursePrice) }}
        </el-descriptions-item>
        <el-descriptions-item label="浏览量">
          {{ displayValue(course.viewCount) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程标签" :span="2">
          {{ displayValue(course.courseTags) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程图片" :span="2">
          {{ displayValue(course.courseImage) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="课程内容" :column="1" border class="detail-section">
        <el-descriptions-item label="课程简介">
          <div class="long-text">{{ displayValue(course.courseIntroduction) }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="教学目标">
          <div class="long-text">{{ displayValue(course.teachingObjectives) }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="课程大纲">
          <div class="long-text">{{ displayValue(course.courseOutline) }}</div>
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

.long-text {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
