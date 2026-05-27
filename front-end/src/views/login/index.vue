<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi } from '@/api/admin'

import logo from '@/assets/images/BUCT-logo-blue.png'

const router = useRouter()

const username = ref('')
const password = ref('')
const errorMsg = ref('')
const time = ref('')

const updateTime = () => {
  time.value = new Date().toLocaleString()
}

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
})

const handleLogin = async () => {
  errorMsg.value = '' 
  if (!username.value || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }

  try {
    const res = await loginApi({
      username: username.value,
      password: password.value
    })
    localStorage.setItem('isLogin', '1')
    localStorage.setItem('user', JSON.stringify(res))
    router.push('/')
  } catch (err) {
    errorMsg.value = '用户名或密码错误'
  }
}

const goRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-page">

    <header class="top-bar">
      <div class="left">
        <img :src="logo" class="logo" />
        <span class="title">教学辅助系统后台</span>
      </div>
      <div class="right">
        {{ time }}
      </div>
    </header>

    <div class="main">
      <div class="login-card">

        <h2>管理员登录</h2>

        <el-input v-model="username" placeholder="学号/工号" size="large" />

        <el-input v-model="password" type="password" placeholder="密码" size="large" />

        <el-button type="primary" class="login-btn" @click="handleLogin">
          登录
        </el-button>

        <div class="actions">
          <span class="link">忘记/修改密码</span>
          <span class="link" @click="goRegister">注册</span>
        </div>

        <div class="error">{{ errorMsg }}</div>

      </div>
    </div>

    <footer class="foot-bar">
      <div class="links">
        <span>关于平台</span>
        <span>|</span>
        <span>服务协议</span>
        <span>|</span>
        <span>联系邮箱</span>
        <span>|</span>
        <span>侵权投诉</span>
      </div>

      <div class="copyright">
        © 2025-2026 北京化工大学信息学院计算机专业实践教学团队. All Rights Reserved.
      </div>
    </footer>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: url('@/assets/images/main-background.jpg') no-repeat center/cover;
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
  justify-content: flex-end;
  align-items: center;
  padding-right: 120px;
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
  justify-content: space-between;
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

.foot-bar {
  height: 80px;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px 40px;
}

.links {
  font-size: 16px;
  margin: 0 8px;
  color: #333;
}

.links span {
  margin: 0 6px;
}

.copyright {
  font-size: 12px;
  color: #666;
  margin-top: 1px;
}
</style>