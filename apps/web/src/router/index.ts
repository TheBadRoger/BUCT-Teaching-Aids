import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { pinia } from '@/stores'
import { useSessionStore } from '@/stores/session'

const routes: RouteRecordRaw[] = [
  {
    path: '/learn',
    component: () => import('@/views/learning/layout.vue'),
    meta: { public: true },
    children: [
      { path: '', component: () => import('@/views/learning/home.vue') },
      { path: 'courses', component: () => import('@/views/learning/courses.vue') },
      { path: 'course', component: () => import('@/views/learning/course.vue') },
      { path: 'play', component: () => import('@/views/learning/play.vue') },
    ],
  },
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
    redirect: '/learn',
    component: () => import('@/layout/index.vue'), 
    children: [
      { path: 'news/list', component: () => import('@/views/news/list.vue') },
      { path: 'news/add', component: () => import('@/views/news/add.vue') },
      { path: 'news/detail', component: () => import('@/views/news/detail.vue') },
      { path: 'review/list', component: () => import('@/views/review/list.vue') },
      { path: 'review/detail', component: () => import('@/views/review/detail.vue') },
      ...['knowledge', 'lesson-plan'].map(tool => ({
        path: `ai/${tool}`,
        component: () => import('@/views/teaching/index.vue')
      })),
      ...['list', 'add', 'detail'].map(mode => ({
        path: `organization/${mode}`,
        component: () => import('@/views/organization/index.vue')
      })),
      ...['list', 'add'].map(mode => ({
        path: `admin/${mode}`,
        component: () => import('@/views/admin/index.vue')
      })),
      ...['widgets', 'email', 'calendar', 'live/add', 'live/room'].map(mode => ({
        path: `application/${mode}`,
        component: () => import('@/views/application/index.vue')
      })),
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
  history: createWebHashHistory(`${import.meta.env.BASE_URL}index.html`),
  routes
})

//路由守卫
router.beforeEach((to) => {
  const session = useSessionStore(pinia)

  // 放行登录页和注册页
  if (to.meta.public || to.path === '/login' || to.path === '/register') {
    return true
  }

  // 未登录跳转到登录页
  if (!session.authenticated) {
    return '/login'
  } 
})

export default router
