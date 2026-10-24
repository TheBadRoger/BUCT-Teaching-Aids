<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { demoReviews } from './demo'
const router = useRouter()
const keyword = ref('')
const rating = ref<number | ''>('')
const rows = computed(() => demoReviews.filter(item => (!rating.value || item.rating === rating.value) && `${item.course} ${item.student} ${item.content}`.includes(keyword.value.trim())))
</script>

<template>
  <section class="page-container">
    <h2 class="page-title">评价列表</h2>
    <el-alert title="课程评价接口待接入：以下全部为演示数据，不代表真实评价；暂不提供审核、回复或删除操作。" type="info" show-icon :closable="false" />
    <el-form inline class="search-bar" @submit.prevent>
      <el-form-item label="课程 / 学员 / 内容"><el-input v-model="keyword" placeholder="请输入关键词" clearable /></el-form-item>
      <el-form-item label="评分"><el-select v-model="rating" placeholder="全部评分" clearable style="width: 140px"><el-option v-for="score in 5" :key="score" :label="`${score} 星`" :value="score" /></el-select></el-form-item>
      <el-form-item><el-button @click="keyword = ''; rating = ''">重置</el-button></el-form-item>
    </el-form>
    <el-table :data="rows" border empty-text="暂无匹配的演示评价">
      <el-table-column prop="course" label="课程名称" min-width="200" />
      <el-table-column prop="student" label="评价学员" width="130" />
      <el-table-column label="评分" width="160"><template #default="{ row }"><el-rate :model-value="row.rating" disabled :aria-label="`${row.rating} 星`" /></template></el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="数据来源" width="110"><el-tag type="warning">演示数据</el-tag></el-table-column>
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="router.push({ path: '/review/detail', query: { id: row.id } })">详情</el-button></template></el-table-column>
    </el-table>
    <p class="count">共 {{ rows.length }} 条演示评价</p>
  </section>
</template>

<style scoped>
.count { color: #606266; text-align: right; }
</style>
