import Vue from "vue"
import App from "./App.vue"
import ElementUI from "element-ui"
import VueRouter from "vue-router"
import Axios from "axios"
import VueAxios from "vue-axios"
import router from "./router"
import "element-ui/lib/theme-chalk/index.css"

Vue.config.productionTip = false

Vue.use(ElementUI);
Vue.use(VueRouter);
Vue.use(VueAxios,Axios)

new Vue({
  render: h => h(App),
  router
}).$mount('#app')
