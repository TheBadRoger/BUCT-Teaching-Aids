<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addCourse } from '@/api/course'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const form = ref({
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

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseNumber: [{ required: true, message: '请输入课程编号', trigger: 'blur' }],
  teachingTeachers: [{ required: true, message: '请输入授课教师', trigger: 'blur' }],
  courseStatus: [{ required: true, message: '请选择课程状态', trigger: 'change' }]
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (submitting.value) return

    try {
      submitting.value = true
      await addCourse({
        ...form.value,
        courseName: form.value.courseName.trim(),
        courseNumber: form.value.courseNumber.trim(),
        teachingTeachers: form.value.teachingTeachers.trim()
      })
      ElMessage.success('新增成功')
      router.push('/course/list')
    } catch (err) {
      console.log(err)
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  formRef.value.resetFields()
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">新增课程</span>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="form-box"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="课程名称" prop="courseName">
            <el-input v-model="form.courseName" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="课程编号" prop="courseNumber">
            <el-input v-model="form.courseNumber" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="开课日期">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="课程时长">
            <el-input v-model="form.duration" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="授课教师" prop="teachingTeachers">
        <el-input v-model="form.teachingTeachers" clearable />
      </el-form-item>

      <el-form-item label="授课班级">
        <el-input v-model="form.teachingClasses" clearable />
      </el-form-item>

      <el-form-item label="目标人群">
        <el-input v-model="form.targetAudience" clearable />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="上课地点">
            <el-input v-model="form.classAddress" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="课程价格">
            <el-input-number v-model="form.coursePrice" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="课程状态" prop="courseStatus">
            <el-select v-model="form.courseStatus" clearable>
              <el-option label="未开始" value="未开始" />
              <el-option label="进行中" value="进行中" />
              <el-option label="已结束" value="已结束" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="课程标签">
            <el-input v-model="form.courseTags" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="课程图片">
        <el-input v-model="form.courseImage" clearable />
      </el-form-item>

      <el-form-item label="课程简介">
        <el-input v-model="form.courseIntroduction" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="教学目标">
        <el-input v-model="form.teachingObjectives" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="课程大纲">
        <el-input v-model="form.courseOutline" type="textarea" :rows="5" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
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

.form-box {
  max-width: 900px;
  margin: 0 auto;
}
</style>
