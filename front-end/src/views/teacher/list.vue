<script setup>
import { ref, onMounted } from 'vue'
import SearchForm from '@/components/searchform.vue'
import { getTeacherList } from '@/api/teacher'

// 查询参数
const query = ref({
  name: '',
  organization: '',
  gender: '',
  education: '',
  jointimeRange: [],
  page: 0,
  size: 10
})

// 表格数据
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 获取数据
const fetchData = async () => {
  loading.value = true

  try {
    const params = {
      name: query.value.name,
      organization: query.value.organization,
      gender: query.value.gender,
      education: query.value.education,
      page: query.value.page,
      size: query.value.size,
      // 👉 时间处理
      jointime: query.value.jointimeRange?.length
        ? query.value.jointimeRange.join(',')
        : ''
    }

    const res = await getTeacherList(params)

    tableData.value = res.content || []
    total.value = res.totalElements || 0

  } catch (err) {
    console.log(err)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  query.value.page = 0
  fetchData()
}

// 重置
const handleReset = () => {
  query.value = {
    name: '',
    organization: '',
    gender: '',
    education: '',
    jointimeRange: [],
    page: 0,
    size: 10
  }
  fetchData()
}

// 分页
const handleCurrentChange = (page) => {
  query.value.page = page - 1
  fetchData()
}

const handleSizeChange = (size) => {
  query.value.size = size
  query.value.page = 0
  fetchData()
}

// 删除（占位）
const handleDelete = () => {
  console.log('删除（后面接接口）')
}

// 导出（占位）
const handleExport = () => {
  console.log('导出（后面接接口）')
}

// 页面加载
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">

    <!-- 标题 -->
    <div class="page-header">
      <span class="title">教师列表</span>
    </div>

    <!-- 查询 -->
    <SearchForm
      :model="query"
      @search="handleSearch"
      @reset="handleReset"
      @delete="handleDelete"
      @export="handleExport"
    >
      <el-form-item label="姓名">
        <el-input v-model="query.name" />
      </el-form-item>

      <el-form-item label="所属单位/院系">
        <el-input v-model="query.organization" />
      </el-form-item>

      <el-form-item label="性别">
        <el-select v-model="query.gender" style="width: 120px">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <el-form-item label="学历">
        <el-select v-model="query.education" style="width: 120px">
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
      </el-form-item>

      <el-form-item label="入职时间">
        <el-date-picker
          v-model="query.jointimeRange"
          type="daterange"
          range-separator="至"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
    </SearchForm>

    <!-- 表格 -->
    <el-table :data="tableData" border v-loading="loading">
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="organization" label="所属单位/院系" />
      <el-table-column prop="gender" label="性别" />
      <el-table-column prop="education" label="学历" />
      <el-table-column prop="jointime" label="入职时间" />

      <el-table-column label="操作" width="180">
        <template #default>
          <el-button size="small">查看</el-button>
          <el-button size="small" type="primary">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page + 1"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>

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

.pagination {
  margin: 30px 0;
  text-align: right;
  display: flex;
  justify-content: center;
}
</style>