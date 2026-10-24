<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { demoReviews } from './demo'
const route = useRoute()
const router = useRouter()
const review = computed(() => typeof route.query.id === 'string' ? demoReviews.find(item => item.id === route.query.id) : undefined)
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">评价详情</h2>
    <el-alert title="课程评价接口待接入：当前仅展示演示数据，不代表真实学生评价。" type="info" show-icon :closable="false" />
    <template v-if="review">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="数据来源"><el-tag type="warning">演示数据</el-tag></el-descriptions-item>
        <el-descriptions-item label="课程名称">{{ review.course }}</el-descriptions-item>
        <el-descriptions-item label="评价学员">{{ review.student }}</el-descriptions-item>
        <el-descriptions-item label="课程评分"><el-rate :model-value="review.rating" disabled show-score :aria-label="`${review.rating} 星`" /></el-descriptions-item>
        <el-descriptions-item label="评价时间">{{ new Date(review.createdAt).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="评价内容"><p class="content">{{ review.content }}</p></el-descriptions-item>
      </el-descriptions>
      <el-button class="back" @click="router.push('/review/list')">返回评价列表</el-button>
    </template>
    <el-empty v-else :description="route.query.id ? '未找到该评价，请返回列表重新选择' : '请从评价列表选择一条评价查看'"><el-button type="primary" @click="router.push('/review/list')">评价列表</el-button></el-empty>
  </section>
</template>

<style scoped>
.content { white-space: pre-wrap; overflow-wrap: anywhere; }
.back { margin-top: 24px; }
</style>
