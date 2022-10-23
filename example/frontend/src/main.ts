import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { setupGraphql } from 'src/setupGraphql'

const app = createApp(App)

setupGraphql(app)

// provideClient(client)
app.mount('#app')
