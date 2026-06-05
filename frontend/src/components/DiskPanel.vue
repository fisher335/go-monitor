<template>
  <div class="bg-gray-750 rounded-lg p-4 border border-gray-700">
    <div class="flex items-center justify-between mb-3">
      <span class="text-sm text-gray-400 font-medium">磁盘</span>
    </div>

    <div v-if="!disks || disks.length === 0" class="text-xs text-gray-500 text-center py-2">
      无磁盘数据
    </div>

    <div v-for="disk in displayDisks" :key="disk.mount_point" class="mb-2 last:mb-0">
      <div class="flex justify-between text-xs mb-1">
        <span class="text-gray-300 truncate max-w-[120px]" :title="disk.mount_point">{{ disk.mount_point }}</span>
        <span :class="getPercentClass(disk.used_percent)">
          {{ disk.used_percent?.toFixed(1) || 0 }}%
        </span>
      </div>
      <div class="w-full bg-gray-700 rounded-full h-2 overflow-hidden">
        <div class="progress-bar h-full rounded-full"
             :style="{ width: Math.min(disk.used_percent || 0, 100) + '%' }"
             :class="getBarClass(disk.used_percent)">
        </div>
      </div>
      <div class="text-xs text-gray-600 mt-0.5">
        {{ disk.used_gb?.toFixed(1) }} GB / {{ disk.total_gb?.toFixed(1) }} GB
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  disks: { type: Array, default: () => [] }
})

const displayDisks = computed(() => {
  if (!props.disks) return []
  return props.disks.slice(0, 4)
})

function getPercentClass(p) {
  if (!p) return 'text-green-400'
  if (p >= 90) return 'text-red-400'
  if (p >= 80) return 'text-yellow-400'
  return 'text-green-400'
}

function getBarClass(p) {
  if (!p) return 'bg-green-500'
  if (p >= 90) return 'bg-red-500'
  if (p >= 80) return 'bg-yellow-500'
  return 'bg-green-500'
}
</script>
