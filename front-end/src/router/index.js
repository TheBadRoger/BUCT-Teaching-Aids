import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/register',
    component: () => import('@/views/register/index.vue')
  },
  {
    path: '/',
    redirect: '/teacher/list',
    component: () => import('@/layout/index.vue'), 
    children: [
      {
      path: 'teacher/list',
      component: () => import('@/views/teacher/list.vue')
      },
      {
        path: 'teacher/add',
        component: () => import('@/views/teacher/add.vue')
      },
      {
        path: 'teacher/detail',
        component: () => import('@/views/teacher/detail.vue')
      },
      {
      path: 'student/list',
      component: () => import('@/views/student/list.vue')
      },
      {
        path: 'student/add',
        component: () => import('@/views/student/add.vue')
      },
      {
        path: 'student/detail',
        component: () => import('@/views/student/detail.vue')
      },
      {
      path: 'course/list',
      component: () => import('@/views/course/list.vue')
      },
      {
        path: 'course/add',
        component: () => import('@/views/course/add.vue')
      },
      {
        path: 'course/detail',
        component: () => import('@/views/course/detail.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

//路由守卫
router.beforeEach((to, from) => {
  const isLogin = localStorage.getItem('isLogin')

  // 放行登录页和注册页
  if (to.path === '/login' || to.path === '/register') {
    return true
  }

  // 未登录跳转到登录页
  if (!isLogin) {
    return '/login'
  } 
})

export default router