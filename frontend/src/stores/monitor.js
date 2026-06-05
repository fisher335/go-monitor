import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useMonitorStore = defineStore('monitor', () => {
  // 状态
  const servers = ref([])
  const updated = ref(0)

  // 更新所有服务器数据
  function updateServers(data) {
    if (data && Array.isArray(data.servers)) {
      servers.value = data.servers
      updated.value = data.updated || Date.now()
    }
  }

  // 获取单台服务器
  function getServer(name) {
    return servers.value.find(s => s.name === name)
  }

  // 服务器数量
  const serverCount = computed(() => servers.value.length)

  // 在线服务器数
  const onlineCount = computed(() =>
    servers.value.filter(s => s.online).length
  )

  return {
    servers,
    updated,
    updateServers,
    getServer,
    serverCount,
    onlineCount
  }
})
