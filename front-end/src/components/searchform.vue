<script setup>
defineProps({
  model: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['search', 'reset', 'delete', 'export'])
</script>

<template>
  <div class="search-bar">
    <el-form :inline="true" :model="model">
      <!-- 查询条件 -->
      <slot />

      <el-form-item>
        <el-button type="primary" @click="emit('search')">
          查询
        </el-button>

        <el-button @click="emit('reset')">
          重置
        </el-button>

        <el-button type="danger" @click="emit('delete')">
          删除
        </el-button>

        <el-button type="success" @click="emit('export')">
          导出
        </el-button>
      </el-form-item>

    </el-form>
  </div>
</template>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}
</style>