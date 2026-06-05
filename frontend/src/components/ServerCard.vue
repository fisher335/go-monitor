<template>
  <div class="bg-gray-800 rounded-lg border border-gray-700 overflow-hidden">
    <!-- 服务器标题 -->
    <div class="flex items-center justify-between px-5 py-3 bg-gray-750 border-b border-gray-700"
         :class="{'bg-gray-800': true}">
      <div class="flex items-center gap-3">
        <span class="w-3 h-3 rounded-full" :class="server.online ? 'bg-green-500 online-dot' : 'bg-red-500'"></span>
        <span class="font-semibold text-lg">{{ server.name }}</span>
        <span class="text-xs text-gray-500">({{ server.host }})</span>
      </div>
      <div class="flex items-center gap-4 text-sm">
        <span v-if="server.hardware" class="text-gray-400">
          负载: {{ server.hardware.load?.load_1?.toFixed(2) }}
        </span>
        <span v-if="server.docker" class="text-gray-400 cursor-help"
              :title="`运行中: ${server.docker.info.containers_running} / 总计: ${server.docker.info.containers_total}`">
          Docker: {{ server.docker.info.containers_running }}/{{ server.docker.info.containers_total }}
        </span>
        <span v-if="server.error" class="text-red-400 text-xs" :title="server.error">
          ⚠ 异常
        </span>
      </div>
    </div>

    <!-- 离线状态 -->
    <div v-if="!server.online" class="p-8 text-center text-gray-500">
      <div class="text-4xl mb-2">🔌</div>
      <p>服务器离线</p>
      <p class="text-xs mt-1" v-if="server.error">{{ server.error }}</p>
    </div>

    <!-- 在线：指标面板 -->
    <div v-if="server.online && server.hardware" class="p-5">
      <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-4 gap-4 mb-4">
        <!-- CPU -->
        <CpuPanel :cpu="server.hardware.cpu" :load="server.hardware.load" />
        <!-- 内存 -->
        <MemoryPanel :memory="server.hardware.memory" />
        <!-- 磁盘 -->
        <DiskPanel :disks="server.hardware.disks" />
        <!-- 网络 -->
        <NetworkPanel :network="server.hardware.network" />
      </div>

      <!-- 运行时间 -->
      <div v-if="server.hardware.uptime" class="text-xs text-gray-500 mb-3">
        🟢 已运行 {{ server.hardware.uptime }}
      </div>

      <!-- Docker 面板 -->
      <DockerPanel v-if="server.docker" :docker="server.docker" />
    </div>
  </div>
</template>

<script setup>
import CpuPanel from './CpuPanel.vue'
import MemoryPanel from './MemoryPanel.vue'
import DiskPanel from './DiskPanel.vue'
import NetworkPanel from './NetworkPanel.vue'
import DockerPanel from './DockerPanel.vue'

defineProps({
  server: {
    type: Object,
    required: true
  }
})
</script>

<style>
.bg-gray-750 {
  background-color: #2d3748;
}
</style>
