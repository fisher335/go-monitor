<template>
  <div class="bg-gray-750 rounded-lg p-4 border border-gray-700">
    <div class="flex items-center justify-between mb-3">
      <span class="text-sm text-gray-400 font-medium">CPU</span>
      <span class="text-lg font-bold" :class="colorClass">{{ cpu?.percent?.toFixed(1) || '0.0' }}%</span>
    </div>

    <!-- 进度条 -->
    <div class="w-full bg-gray-700 rounded-full h-3 mb-3 overflow-hidden">
      <div class="progress-bar h-full rounded-full"
           :style="{ width: Math.min(cpu?.percent || 0, 100) + '%' }"
           :class="barColorClass">
      </div>
    </div>

    <div class="grid grid-cols-3 gap-2 text-xs text-gray-500">
      <div class="text-center">
        <div class="font-mono">{{ load?.load_1?.toFixed(2) || '-' }}</div>
        <div>1分钟</div>
      </div>
      <div class="text-center">
        <div class="font-mono">{{ load?.load_5?.toFixed(2) || '-' }}</div>
        <div>5分钟</div>
      </div>
      <div class="text-center">
        <div class="font-mono">{{ load?.load_15?.toFixed(2) || '-' }}</div>
        <div>15分钟</div>
      </div>
    </div>

    <div v-if="cpu?.cores" class="mt-2 text-xs text-gray-600 text-center">
      核心数: {{ cpu.cores }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  cpu: { type: Object, default: () => ({}) },
  load: { type: Object, default: () => ({}) }
})

const percent = computed(() => props.cpu?.percent || 0)

const colorClass = computed(() => {
  if (percent.value >= 90) return 'text-red-400'
  if (percent.value >= 70) return 'text-yellow-400'
  return 'text-green-400'
})

const barColorClass = computed(() => {
  if (percent.value >= 90) return 'bg-red-500'
  if (percent.value >= 70) return 'bg-yellow-500'
  return 'bg-green-500'
})
</script>
