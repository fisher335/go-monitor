<template>
  <div class="bg-gray-750 rounded-lg p-4 border border-gray-700">
    <div class="flex items-center justify-between mb-3">
      <span class="text-sm text-gray-400 font-medium">内存</span>
      <span class="text-lg font-bold" :class="colorClass">{{ memory?.used_percent?.toFixed(1) || '0.0' }}%</span>
    </div>

    <!-- 进度条 -->
    <div class="w-full bg-gray-700 rounded-full h-3 mb-3 overflow-hidden">
      <div class="progress-bar h-full rounded-full"
           :style="{ width: Math.min(memory?.used_percent || 0, 100) + '%' }"
           :class="barColorClass">
      </div>
    </div>

    <div class="grid grid-cols-2 gap-2 text-xs">
      <div>
        <span class="text-gray-500">已用</span>
        <div class="font-mono text-gray-200">{{ memory?.used_gb?.toFixed(1) || '-' }} GB</div>
      </div>
      <div>
        <span class="text-gray-500">总计</span>
        <div class="font-mono text-gray-200">{{ memory?.total_gb?.toFixed(1) || '-' }} GB</div>
      </div>
      <div>
        <span class="text-gray-500">空闲</span>
        <div class="font-mono text-gray-200">{{ memory?.free_gb?.toFixed(1) || '-' }} GB</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  memory: { type: Object, default: () => ({}) }
})

const percent = computed(() => props.memory?.used_percent || 0)

const colorClass = computed(() => {
  if (percent.value >= 90) return 'text-red-400'
  if (percent.value >= 80) return 'text-yellow-400'
  return 'text-blue-400'
})

const barColorClass = computed(() => {
  if (percent.value >= 90) return 'bg-red-500'
  if (percent.value >= 80) return 'bg-yellow-500'
  return 'bg-blue-500'
})
</script>
