<template>
  <div class="min-h-screen bg-gray-900 text-gray-100">
    <!-- 顶部导航 -->
    <header class="bg-gray-800 border-b border-gray-700 px-6 py-3">
      <div class="max-w-7xl mx-auto flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="text-2xl">📊</span>
          <h1 class="text-xl font-bold">Go-Monitor</h1>
          <span class="text-xs bg-gray-700 px-2 py-0.5 rounded text-gray-400">v1.0</span>
        </div>
        <div class="flex items-center gap-4 text-sm text-gray-400">
          <span class="flex items-center gap-1">
            <span class="w-2 h-2 rounded-full" :class="connected ? 'bg-green-500 online-dot' : 'bg-red-500'"></span>
            {{ connected ? '已连接' : '连接断开' }}
          </span>
          <span>服务器: {{ serverCount }} 台</span>
        </div>
      </div>
    </header>

    <!-- 主体 -->
    <main class="max-w-7xl mx-auto p-6">
      <!-- 上次更新时间 -->
      <div class="text-xs text-gray-500 mb-4 text-right">
        数据更新时间: {{ lastUpdateTime }}
      </div>

      <!-- 无服务器提示 -->
      <div v-if="serverCount === 0 && !loading" class="text-center py-20 text-gray-500">
        <div class="text-6xl mb-4">📡</div>
        <p class="text-xl">暂无服务器数据</p>
        <p class="mt-2 text-sm">请检查 config.yaml 中的服务器配置和后端连接</p>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="text-center py-20 text-gray-500">
        <div class="animate-spin text-4xl mb-4">⏳</div>
        <p>正在连接 WebSocket...</p>
      </div>

      <!-- 服务器仪表盘 -->
      <div v-if="!loading" class="grid gap-6">
        <div v-for="server in servers" :key="server.name">
          <ServerCard :server="server" />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useMonitorStore } from './stores/monitor'
import ServerCard from './components/ServerCard.vue'

const store = useMonitorStore()
const loading = ref(true)
const connected = ref(false)

const servers = computed(() => store.servers)
const serverCount = computed(() => store.servers.length)

const lastUpdateTime = computed(() => {
  if (!store.updated) return '等待数据...'
  const d = new Date(store.updated)
  return d.toLocaleTimeString('zh-CN', { hour12: false })
})

let ws = null
let reconnectTimer = null

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${location.host}/api/ws`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    connected.value = true
    loading.value = false
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      store.updateServers(data)
      loading.value = false
    } catch (e) {
      console.error('解析数据失败:', e)
    }
  }

  ws.onclose = () => {
    connected.value = false
    // 自动重连
    reconnectTimer = setTimeout(connectWebSocket, 3000)
  }

  ws.onerror = () => {
    connected.value = false
  }
}

onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  if (ws) ws.close()
  if (reconnectTimer) clearTimeout(reconnectTimer)
})
</script>
