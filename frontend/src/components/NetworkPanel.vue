<template>
  <div class="bg-gray-750 rounded-lg p-4 border border-gray-700">
    <div class="flex items-center justify-between mb-3">
      <span class="text-sm text-gray-400 font-medium">网络流量</span>
    </div>

    <div v-if="!network || network.length === 0" class="text-xs text-gray-500 text-center py-2">
      无网络数据
    </div>

    <div v-for="iface in network" :key="iface.interface" class="mb-2 last:mb-0">
      <div class="text-xs text-gray-400 mb-1 font-medium">{{ iface.interface }}</div>
      <div class="grid grid-cols-2 gap-2 text-xs">
        <div class="bg-gray-800 rounded p-2">
          <div class="text-gray-500">⬇ 下载</div>
          <div class="font-mono text-sky-400">{{ formatBytes(iface.rx_bytes) }}</div>
        </div>
        <div class="bg-gray-800 rounded p-2">
          <div class="text-gray-500">⬆ 上传</div>
          <div class="font-mono text-orange-400">{{ formatBytes(iface.tx_bytes) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  network: { type: Array, default: () => [] }
})

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let val = bytes
  while (val >= 1024 && i < units.length - 1) {
    val /= 1024
    i++
  }
  return val.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}
</script>
