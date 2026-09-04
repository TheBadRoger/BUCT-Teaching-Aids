<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi } from '@/api/admin' 
import { ElMessage } from 'element-plus' 

import logo from '@/assets/images/BUCT-logo-blue.png'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')
const time = ref('')

const updateTime = () => {
  time.value = new Date().toLocaleString()
}

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
})

const handleRegister = async () => {
  errorMsg.value = ''
  // 前端基础校验
  if (!username.value || !password.value || !confirmPassword.value) {
    errorMsg.value = '请填写完整信息'
    return
  }
  if (password.value !== confirmPassword.value) {
    errorMsg.value = '两次密码输入不一致'
    return
  }

  try {
    const res = await registerApi({
      username: username.value,
      password: password.value
    })

    ElMessage.success({
      message: '注册成功！即将跳转到登录页',
    })

    setTimeout(() => {
      router.push('/login')
    }, 1500)

  } catch (err) {
    errorMsg.value = '注册失败，用户名已存在'
  }
}

const goLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="login-page">

    <header class="top-bar">
      <div class="left">
        <img :src="logo" class="logo" />
        <span class="title">注册</span>
      </div>
      <div class="right">
        {{ time }}
      </div>
    </header>

    <div class="main">
      <div class="login-card">

        <h2>管理员注册</h2>

        <el-input v-model="username" placeholder="用户名" size="large" />

        <el-input v-model="password" type="password" placeholder="密码" size="large" />

        <el-input v-model="confirmPassword" type="password" placeholder="确认密码" size="large" />

        <el-button type="primary" class="login-btn" @click="handleRegister">
          注册
        </el-button>

        <div class="actions">
          <span class="link" @click="goLogin">返回登录</span>
        </div>

        <div class="error">{{ errorMsg }}</div>

      </div>
    </div>

  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-bar {
  height: 70px;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 40px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.left {
  display: flex;
  align-items: center;
}

.logo {
  height: 50px;
}

.title {
  margin-left: 40px;
  font-size: 18px;
}

.right {
  color: #333;
}

.main {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-card h2 {
  color: #222;
  font-weight: 600;
  margin-bottom: 10px;
}

.login-card {
  width: 360px;
  padding: 20px 30px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(6px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.login-card :deep(.el-input) {
  margin-bottom: 15px;
}

.login-btn {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  background-color: #1b2682;
  border: none;
}

.actions {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}

.error {
  margin-top: 15px;
  color: #e74c3c;
  font-size: 14px;
}

.link {
  cursor: pointer;
  color: #1b2682;
}
.link:hover {
  text-decoration: underline;
}
</style>