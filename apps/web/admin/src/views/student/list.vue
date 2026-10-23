<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchForm from '@/components/searchform.vue'
import { getStudentList, updateStudent, deleteStudents } from '@/api/student'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const query = ref({
  name: '',
  studentNumber: '',
  className: '',
  gender: '',
  telephone: '',
  email: '',
  page: 0,
  size: 10
})

const tableData = ref([])
const selectedRows = ref([])
const total = ref(0)
const loading = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref()
const editForm = ref({
  id: '',
  studentNumber: '',
  name: '',
  className: '',
  gender: '',
  admissionDate: ''
})

const editRules = {
  studentNumber: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入学生姓名', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  admissionDate: [{ required: true, message: '请选择入学时间', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true

  try {
    const res = await getStudentList({
      name: query.value.name,
      studentNumber: query.value.studentNumber,
      className: query.value.className,
      gender: query.value.gender,
      telephone: query.value.telephone,
      email: query.value.email,
      page: query.value.page,
      size: query.value.size,
      sort: 'id'
    })

    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } catch (err) {
    console.log(err)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.page = 0
  fetchData()
}

const handleReset = () => {
  query.value = {
    name: '',
    studentNumber: '',
    className: '',
    gender: '',
    telephone: '',
    email: '',
    page: 0,
    size: 10
  }
  fetchData()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要删除的学生')
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除选中的学生吗？', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteStudents(selectedRows.value.map(item => item.id))
    ElMessage.success('删除成功')
    fetchData()
  } catch (err) {
    console.log(err)
  }
}

const handleExport = () => {
  const params = new URLSearchParams()
  const exportKeys = ['name', 'studentNumber', 'className', 'gender', 'telephone', 'email']

  exportKeys.forEach((key) => {
    if (query.value[key]) {
      params.append(key, query.value[key])
    }
  })

  const queryString = params.toString()
  window.open(`/api/students/export${queryString ? `?${queryString}` : ''}`)
}

const handleEdit = (row) => {
  editForm.value = {
    id: row.id,
    studentNumber: row.studentNumber || '',
    name: row.name || '',
    className: row.className || '',
    gender: row.gender || '',
    admissionDate: row.admissionDate || ''
  }
  editDialogVisible.value = true
}

const handleEditSubmit = () => {
  editFormRef.value.validate(async (valid) => {
    if (!valid) return

    const { id, ...payload } = editForm.value
    try {
      await updateStudent(id, payload)
      ElMessage.success('编辑成功')
      editDialogVisible.value = false
      fetchData()
    } catch (err) {
      console.log(err)
    }
  })
}

const handleView = (row) => {
  localStorage.setItem('currentStudentDetail', JSON.stringify(row))
  router.push({
    path: '/student/detail',
    query: { id: row.id }
  })
}

const handleCurrentChange = (page) => {
  query.value.page = page - 1
  fetchData()
}

const handleSizeChange = (size) => {
  query.value.size = size
  query.value.page = 0
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">

    <div class="page-header">
      <span class="title">学生列表</span>
    </div>

    <SearchForm
      :model="query"
      @search="handleSearch"
      @reset="handleReset"
      @delete="handleDelete"
      @export="handleExport"
    >
      <el-form-item label="姓名">
        <el-input v-model="query.name" clearable />
      </el-form-item>

      <el-form-item label="学号">
        <el-input v-model="query.studentNumber" clearable />
      </el-form-item>

      <el-form-item label="班级">
        <el-input v-model="query.className" clearable />
      </el-form-item>

      <el-form-item label="性别">
        <el-select v-model="query.gender" clearable style="width: 120px">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <el-form-item label="电话">
        <el-input v-model="query.telephone" clearable />
      </el-form-item>

      <el-form-item label="邮箱">
        <el-input v-model="query.email" clearable />
      </el-form-item>
    </SearchForm>

    <el-table
      :data="tableData"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="studentNumber" label="学号" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="className" label="班级" />
      <el-table-column prop="gender" label="性别" width="90" />
      <el-table-column prop="admissionDate" label="入学时间" />
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

    <el-dialog v-model="editDialogVisible" title="编辑学生" width="520px">
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="120px"
      >
        <el-form-item label="学号" prop="studentNumber">
          <el-input v-model="editForm.studentNumber" clearable />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" clearable />
        </el-form-item>

        <el-form-item label="班级" prop="className">
          <el-input v-model="editForm.className" clearable />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="editForm.gender" clearable>
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>

        <el-form-item label="入学时间" prop="admissionDate">
          <el-date-picker
            v-model="editForm.admissionDate"
            type="date"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
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
