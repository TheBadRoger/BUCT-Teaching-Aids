<script setup lang="ts">
import type { Teacher } from '@/types/api'
import type { FormInstance, FormRules } from 'element-plus'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchForm from '@/components/searchform.vue'
import { getTeacherList, updateTeacher, deleteTeachers } from '@/api/teacher'
import { ElMessage } from 'element-plus'

import { usePagedList } from '@/composables/usePagedList'
import { useFormSubmit } from '@/composables/useFormSubmit'

const router = useRouter()

const { query, tableData, total, loading, deleting, error: listError, fetchData,
  handleSearch, handleReset, handleSelectionChange, handleDelete, handleCurrentChange, handleSizeChange } = usePagedList({
  createQuery: () => ({
    name: '',
    organization: '',
    gender: '',
    education: '',
    jointime: '',
    username: '',
    telephone: '',
    email: '',
    userType: '',
    page: 0,
    size: 10
  }),
  load: (params, signal) => getTeacherList({ ...params }, signal),
  remove: deleteTeachers,
  label: '教师'
})

const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const { submitting: editSubmitting, error: editError, submit: submitEdit } = useFormSubmit(editFormRef)
const editForm = ref<Teacher>({
  id: '',
  name: '',
  organization: '',
  gender: '',
  education: '',
  jointime: ''
})

const editRules: FormRules = {
  name: [{ required: true, message: '请输入教师姓名', trigger: 'blur' }]
}

const handleExport = () => {
  const params = new URLSearchParams()
  const exportKeys: (keyof typeof query.value)[] = [
    'name',
    'organization',
    'gender',
    'education',
    'jointime',
    'username',
    'telephone',
    'email',
    'userType'
  ]

  exportKeys.forEach((key) => {
    if (query.value[key]) {
      params.append(key, String(query.value[key]))
    }
  })

  const queryString = params.toString()
  window.open(`/api/teacher/export${queryString ? `?${queryString}` : ''}`)
}

const handleEdit = (row: Teacher) => {
  if (editSubmitting.value) return
  editError.value = ''
  editForm.value = {
    id: row.id,
    name: row.name || '',
    organization: row.organization || '',
    gender: row.gender || '',
    education: row.education || '',
    jointime: row.jointime || ''
  }
  editDialogVisible.value = true
}

const handleView = (row: Teacher) => {
  localStorage.setItem('currentTeacherDetail', JSON.stringify(row))
  router.push({
    path: '/teacher/detail',
    query: { id: row.id }
  })
}

const handleEditSubmit = async () => {
  const saved = await submitEdit(() => {
    const { id, ...payload } = editForm.value
    return updateTeacher(id, payload)
  })
  if (saved) {
    ElMessage.success('编辑成功')
    editDialogVisible.value = false
    await fetchData()
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">

    <div class="page-header">
      <span class="title">教师列表</span>
    </div>

    <el-alert v-if="listError" :title="listError" type="error" :closable="false" show-icon />
    <el-button v-if="listError" :disabled="loading" @click="fetchData">重新加载</el-button>

    <SearchForm
      :model="query"
      :loading="loading"
      :deleting="deleting"
      @search="handleSearch"
      @reset="handleReset"
      @delete="handleDelete"
      @export="handleExport"
    >
      <el-form-item label="姓名">
        <el-input v-model="query.name" clearable />
      </el-form-item>

      <el-form-item label="所属单位/院系">
        <el-input v-model="query.organization" clearable />
      </el-form-item>

      <el-form-item label="性别">
        <el-select v-model="query.gender" clearable style="width: 120px">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <el-form-item label="学历">
        <el-select v-model="query.education" clearable style="width: 120px">
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
      </el-form-item>

      <el-form-item label="入职时间">
        <el-date-picker
          v-model="query.jointime"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
        />
      </el-form-item>

      <el-form-item label="用户名">
        <el-input v-model="query.username" clearable />
      </el-form-item>

      <el-form-item label="电话">
        <el-input v-model="query.telephone" clearable />
      </el-form-item>

      <el-form-item label="邮箱">
        <el-input v-model="query.email" clearable />
      </el-form-item>

      <el-form-item label="用户类型">
        <el-select v-model="query.userType" clearable style="width: 120px">
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
      </el-form-item>
    </SearchForm>

    <el-table
      :data="tableData"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="organization" label="所属单位/院系" />
      <el-table-column prop="gender" label="性别" />
      <el-table-column prop="education" label="学历" />
      <el-table-column prop="jointime" label="入职时间" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="telephone" label="电话" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="userType" label="用户类型" />

      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleView(row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <el-dialog :close-on-click-modal="!editSubmitting" :close-on-press-escape="!editSubmitting" :show-close="!editSubmitting" v-model="editDialogVisible" title="编辑教师" width="520px">
      <el-alert v-if="editError" :title="editError" type="error" :closable="false" show-icon />
      <el-form
        ref="editFormRef"
        :model="editForm"
        :disabled="editSubmitting"
        :rules="editRules"
        label-width="120px"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" />
        </el-form-item>

        <el-form-item label="所属单位/院系">
          <el-input v-model="editForm.organization" />
        </el-form-item>

        <el-form-item label="性别">
          <el-select v-model="editForm.gender" clearable>
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>

        <el-form-item label="学历">
          <el-select v-model="editForm.education" clearable>
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>

        <el-form-item label="入职时间">
          <el-date-picker
            v-model="editForm.jointime"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="editSubmitting" @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

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
  display: flex;
  justify-content: center;
}
</style>
