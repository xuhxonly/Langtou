import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/variables.scss'
import 'element-plus/theme-chalk/dark/css-vars.css'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus 全局组件图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as any)
}

// 安装 Pinia（状态管理）
app.use(createPinia())
// 安装 Vue Router
app.use(router)
// 安装 Element Plus
app.use(ElementPlus, {
  locale: undefined
})

// 挂载应用
app.mount('#app')
