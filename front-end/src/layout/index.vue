<script setup>
import { useRouter } from 'vue-router'
import BUCTLogo from '@/assets/images/BUCT-logo.png'
import { ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const go = (path) => {
    router.push(path)
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('isLogin')
    localStorage.removeItem('user')
    router.push('/login')
  })
}
</script>

<template>
    <el-container style="height: 100vh;">
        <!-- 左侧菜单 -->
        <el-aside width="200px" style="background: #325a7d;">
            <div style="height:60px; display:flex; align-items:center; justify-content:center;">
                <img :src="BUCTLogo" alt="BUCTLogo" style="width:200px;object-fit:contain;">
            </div>
            <el-menu default-active="/teacher/list" background-color="#325a7d" text-color="#fff"
                active-text-color="#409EFF" style="border-right: none;" router>
                <!-- 教师管理 -->
                <el-sub-menu index="teacher">
                    <template #title>
                        <span>教师管理</span>
                    </template>

                    <el-menu-item index="/teacher/list">
                        教师列表
                    </el-menu-item>

                    <el-menu-item index="/teacher/add">
                        新增教师
                    </el-menu-item>

                    <el-menu-item index="/teacher/detail">
                        教师详情
                    </el-menu-item>
                </el-sub-menu>

                <!-- 学生管理 -->
                <el-sub-menu index="student">
                    <template #title>
                        <span>学生管理</span>
                    </template>

                    <el-menu-item index="/student/list">
                        学生列表
                    </el-menu-item>

                    <el-menu-item index="/student/add">
                        新增学生
                    </el-menu-item>

                    <el-menu-item index="/student/detail">
                        学生详情
                    </el-menu-item>
                </el-sub-menu>

                <!-- 课程管理 -->
                <el-sub-menu index="course">
                    <template #title>
                        <span>课程管理</span>
                    </template>

                    <el-menu-item index="/course/list">
                        课程列表
                    </el-menu-item>

                    <el-menu-item index="/course/add">
                        新增课程
                    </el-menu-item>

                    <el-menu-item index="/course/detail">
                        课程详情
                    </el-menu-item>
                </el-sub-menu>

            </el-menu>
        </el-aside>

        <!-- 右侧 -->
        <el-container>
            <!-- 顶部 -->
            <el-header class="header" style="background: #325a7d; color: white;">
                <span class="welcome">欢迎您，{{ user.username }}！</span>
                <el-dropdown >
                    <span class="avatar-wrapper" style="border: none !important; outline: none !important;">
                        <el-avatar size="small" :icon="UserFilled" />
                    </span>

                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item @click="handleLogout">
                                退出登录
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </el-header>

            <!-- 内容 -->
            <el-main style="background: #fff; padding: 0px;">
                <router-view />
            </el-main>
        </el-container>
    </el-container>
</template>

<style scoped>
:deep(.el-sub-menu .el-menu) {
    background-color: #2b4a65 !important;
}


:deep(.el-menu-item) {
    background-color: #2b4a65 !important;
}

:deep(.el-menu-item:hover) {
    background-color: #1f3a52 !important;
}

:deep(.el-menu-item.is-active) {
    background-color: #1a2f45 !important;
}

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  background: #325a7d;
  color: white;
  padding: 0 60px;
}

.welcome {
  margin-right: 10px;
}

.avatar-wrapper {
  cursor: pointer;
  display: flex;
  align-items: center;
}
</style>