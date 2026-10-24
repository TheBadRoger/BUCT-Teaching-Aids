<script setup lang="ts">
// 教案的结构化展示保留全部教学环节和分层作业。
import type { LessonPlan } from '@/types/teaching'
import { objectiveLabels } from '@/utils/teaching-content'
defineProps<{ lesson: LessonPlan }>()
const homeworkLabels = { basic: '基础巩固', improve: '能力提升', challenge: '挑战拓展' }
</script>

<template>
  <article>
    <h2>{{ lesson.title }}</h2>
    <p>课型：{{ lesson.lessonType }} · 课时：{{ lesson.duration }} 分钟</p>
    <h3>教学目标</h3><ul><li v-for="(item, index) in lesson.teachingObjectives" :key="index"><strong>{{ objectiveLabels[item.category] || item.category }}</strong> {{ item.content }}</li></ul>
    <h3>教学重点</h3><ul><li v-for="(item, index) in lesson.keyPoints" :key="index">{{ item }}</li></ul>
    <h3>教学难点</h3><section v-for="(item, index) in lesson.difficultPoints" :key="index"><p>{{ item.point }}</p><p>突破策略：{{ item.strategy }}</p></section>
    <h3>教学资源</h3><p>{{ lesson.teachingResources || '未提供' }}</p>
    <h3>教学过程</h3>
    <el-card v-for="(stage, index) in lesson.teachingProcess" :key="index" shadow="never" class="stage">
      <template #header>{{ stage.stageName }}（{{ stage.duration }} 分钟）</template>
      <p><strong>教师活动：</strong>{{ stage.teacherActivity }}</p><p><strong>学生活动：</strong>{{ stage.studentActivity }}</p><p><strong>设计意图：</strong>{{ stage.designIntent }}</p>
    </el-card>
    <h3>板书设计</h3><p>{{ lesson.boardDesign || '未提供' }}</p>
    <h3>分层作业</h3><section v-for="(label, key) in homeworkLabels" :key="key"><h4>{{ label }}</h4><ul><li v-for="(item, index) in lesson.homework[key]" :key="index">{{ item }}</li></ul></section>
    <h3>教学反思</h3><p>{{ lesson.teachingReflection || '未提供' }}</p>
  </article>
</template>

<style scoped>
h3 { margin-top: 24px; margin-bottom: 12px; font-size: 17px; }
h4 { margin: 12px 0; }
p, li { white-space: pre-wrap; line-height: 1.8; }
.stage { margin: 12px 0; }
</style>
