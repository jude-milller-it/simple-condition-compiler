import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

// 创建Vue实例并挂载
const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')
