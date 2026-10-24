<script setup lang="ts">
import type { Course } from '@/types/api'
import type { FormInstance, FormRules } from 'element-plus'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchForm from '@/components/searchform.vue'
import { getCourseList, updateCourse, deleteCourses } from '@/api/course'
import { ElMessage } from 'element-plus'

import { usePagedList } from '@/composables/usePagedList'
import { useFormSubmit } from '@/composables/useFormSubmit'

const router = useRouter()

const { query, tableData, total, loading, deleting, error: listError, fetchData,
  handleSearch, handleReset, handleSelectionChange, handleDelete, handleCurrentChange, handleSizeChange } = usePagedList({
  createQuery: () => ({
    courseName: '',
    courseNumber: '',
    teachingTeachers: '',
    courseStatus: '',
    courseTags: '',
    startDate: '',
    page: 0,
    size: 10
  }),
  load: (params, signal) => getCourseList({ ...params, sort: 'id' }, signal),
  remove: deleteCourses,
  label: '课程'
})

const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const { submitting: editSubmitting, error: editError, submit: submitEdit } = useFormSubmit(editFormRef)
const editForm = ref<Course>({
  id: '',
  courseName: '',
  courseNumber: '',
  courseIntroduction: '',
  startDate: '',
  teachingObjectives: '',
  duration: '',
  teachingTeachers: '',
  teachingClasses: '',
  targetAudience: '',
  classAddress: '',
  coursePrice: 0,
  courseStatus: '',
  courseTags: '',
  courseOutline: '',
  courseImage: ''
})

const rules: FormRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseNumber: [{ required: true, message: '请输入课程编号', trigger: 'blur' }]
}

const handleExport = () => {
  window.open('/api/course/export')
}

const handleEdit = (row: Course) => {
  if (editSubmitting.value) return
  editError.value = ''
  editForm.value = {
    id: row.id,
    courseName: row.courseName || '',
    courseNumber: row.courseNumber || '',
    courseIntroduction: row.courseIntroduction || '',
    startDate: row.startDate || '',
    teachingObjectives: row.teachingObjectives || '',
    duration: row.duration || '',
    teachingTeachers: row.teachingTeachers || '',
    teachingClasses: row.teachingClasses || '',
    targetAudience: row.targetAudience || '',
    classAddress: row.classAddress || '',
    coursePrice: row.coursePrice ?? 0,
    courseStatus: row.courseStatus || '',
    courseTags: row.courseTags || '',
    courseOutline: row.courseOutline || '',
    courseImage: row.courseImage || ''
  }
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  const saved = await submitEdit(() => {
    const { id, ...payload } = editForm.value
    return updateCourse(id, payload)
  })
  if (saved) {
    ElMessage.success('编辑成功')
    editDialogVisible.value = false
    await fetchData()
  }
}

const handleView = (row: Course) => {
  localStorage.setItem('currentCourseDetail', JSON.stringify(row))
  router.push({
    path: '/course/detail',
    query: { id: row.id }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">课程列表</span>
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
      <el-form-item label="课程名称">
        <el-input v-model="query.courseName" clearable />
      </el-form-item>

      <el-form-item label="课程编号">
        <el-input v-model="query.courseNumber" clearable />
      </el-form-item>

      <el-form-item label="授课教师">
        <el-input v-model="query.teachingTeachers" clearable />
      </el-form-item>

      <el-form-item label="课程状态">
        <el-select v-model="query.courseStatus" clearable style="width: 140px">
          <el-option label="未开始" value="未开始" />
          <el-option label="进行中" value="进行中" />
          <el-option label="已结束" value="已结束" />
        </el-select>
      </el-form-item>

      <el-form-item label="课程标签">
        <el-input v-model="query.courseTags" clearable />
      </el-form-item>

      <el-form-item label="开课日期">
        <el-date-picker
          v-model="query.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
        />
      </el-form-item>
    </SearchForm>

    <el-table
      :data="tableData"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="courseNumber" label="课程编号" min-width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="teachingTeachers" label="授课教师" min-width="140" />
      <el-table-column prop="teachingClasses" label="授课班级" min-width="140" />
      <el-table-column prop="startDate" label="开课日期" min-width="110" />
      <el-table-column prop="duration" label="课程时长" min-width="100" />
      <el-table-column prop="courseStatus" label="状态" min-width="90" />
      <el-table-column prop="coursePrice" label="价格" min-width="90" />
      <el-table-column prop="viewCount" label="浏览量" min-width="90" />

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

    <el-dialog :close-on-click-modal="!editSubmitting" :close-on-press-escape="!editSubmitting" :show-close="!editSubmitting" v-model="editDialogVisible" title="编辑课程" width="760px">
      <el-alert v-if="editError" :title="editError" type="error" :closable="false" show-icon />
      <el-form
        ref="editFormRef"
        :model="editForm"
        :disabled="editSubmitting"
        :rules="rules"
        label-width="120px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="editForm.courseName" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程编号" prop="courseNumber">
              <el-input v-model="editForm.courseNumber" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开课日期">
              <el-date-picker
                v-model="editForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程时长">
              <el-input v-model="editForm.duration" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="授课教师">
          <el-input v-model="editForm.teachingTeachers" clearable />
        </el-form-item>

        <el-form-item label="授课班级">
          <el-input v-model="editForm.teachingClasses" clearable />
        </el-form-item>

        <el-form-item label="目标人群">
          <el-input v-model="editForm.targetAudience" clearable />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="上课地点">
              <el-input v-model="editForm.classAddress" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程价格">
              <el-input-number v-model="editForm.coursePrice" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程状态">
              <el-select v-model="editForm.courseStatus" clearable>
                <el-option label="未开始" value="未开始" />
                <el-option label="进行中" value="进行中" />
                <el-option label="已结束" value="已结束" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程标签">
              <el-input v-model="editForm.courseTags" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="课程图片">
          <el-input v-model="editForm.courseImage" clearable />
        </el-form-item>

        <el-form-item label="课程简介">
          <el-input v-model="editForm.courseIntroduction" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item label="教学目标">
          <el-input v-model="editForm.teachingObjectives" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item label="课程大纲">
          <el-input v-model="editForm.courseOutline" type="textarea" :rows="4" />
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
