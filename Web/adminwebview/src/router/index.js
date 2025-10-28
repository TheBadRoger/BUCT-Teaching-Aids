// 此文件专门负责项目的路由

import VueRouter from "vue-router"

// 引入组件
import Login from '../view/LoginView.vue'
import RegisterView from '../view/RegisterView.vue'
import Home from '../view/HomePage.vue'

// 创建并暴露一个路由器
export default new VueRouter({
    mode: 'history',    // 路由模式，该模式不会在地址中显示井号#
    routes: [
        {
            path: '/',          // 路径
            redirect: '/login'  // 重定向
        },
        {
            path: '/login',     // 路径
            component: Login    // 跳转到的组件
        },
        {
            path: '/register',
            component: RegisterView
        },
        {
            path: '/home',
            component: Home
        }
    ]
})