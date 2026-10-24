<script setup lang="ts">
// 通过文本插值展示 AI 内容，避免执行生成的 HTML。
import type { KnowledgeHandbook } from '@/types/teaching'
import { difficultyLabels, importanceLabels } from '@/utils/teaching-content'
defineProps<{ handbook: KnowledgeHandbook }>()
</script>

<template>
  <article class="teaching-result">
    <h2>{{ handbook.chapterTitle }}</h2>
    <p>{{ handbook.overview }}</p>
    <el-empty v-if="!handbook.knowledgePoints.length" description="未生成知识点，请调整主题后重试" />
    <el-card v-for="(point, index) in handbook.knowledgePoints" :key="index" shadow="never" class="point">
      <template #header><strong>{{ index + 1 }}. {{ point.name }}</strong><el-space class="badges"><el-tag>{{ difficultyLabels[point.difficulty] || point.difficulty || '未标注难度' }}</el-tag><el-tag type="info">{{ importanceLabels[point.importance] || point.importance || '未标注重要性' }}</el-tag></el-space></template>
      <p>{{ point.explanation }}</p>
      <h3>核心概念</h3><ul><li v-for="(item, i) in point.keyConcepts" :key="i">{{ item }}</li></ul>
      <h3>常见误区</h3><ul><li v-for="(item, i) in point.commonMistakes" :key="i">{{ item }}</li></ul>
    </el-card>
  </article>
</template>

<style scoped>
.point { margin-top: 20px; }
.badges { margin-left: 12px; }
p { white-space: pre-wrap; line-height: 1.8; }
h3 { margin-top: 16px; font-size: 15px; }
li { margin-top: 8px; line-height: 1.7; }
</style>
