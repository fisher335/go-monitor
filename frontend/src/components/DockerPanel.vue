<template>
  <div class="bg-gray-750 rounded-lg border border-gray-700 overflow-hidden">
    <!-- Docker 头部 -->
    <div class="flex items-center justify-between px-4 py-3 bg-gray-800 border-b border-gray-700">
      <div class="flex items-center gap-2">
        <span class="text-lg">🐳</span>
        <span class="font-medium text-sm">Docker</span>
        <span class="text-xs text-gray-500">v{{ docker.info?.server_version || '-' }}</span>
      </div>
      <div class="flex items-center gap-3 text-xs">
        <span class="text-green-400">▶ {{ docker.info?.containers_running || 0 }}</span>
        <span class="text-red-400">⏹ {{ docker.info?.containers_stopped || 0 }}</span>
        <span class="text-gray-500">总计 {{ docker.info?.containers_total || 0 }}</span>
      </div>
    </div>

    <!-- 容器列表 -->
    <div class="p-4">
      <div v-if="containers.length === 0" class="text-xs text-gray-500 text-center py-3">
        无运行中的容器
      </div>

      <!-- 容器表格 -->
      <div v-if="containers.length > 0" class="overflow-x-auto">
        <table class="w-full text-xs">
          <thead>
            <tr class="text-gray-500 border-b border-gray-700">
              <th class="text-left py-2 pr-2">状态</th>
              <th class="text-left py-2 pr-2">名称</th>
              <th class="text-left py-2 pr-2">镜像</th>
              <th class="text-right py-2 pr-2">CPU</th>
              <th class="text-right py-2 pr-2">内存</th>
              <th class="text-right py-2">端口</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in containers" :key="c.id"
                class="border-b border-gray-700/50 hover:bg-gray-700/30">
              <td class="py-2 pr-2">
                <span class="w-2 h-2 inline-block rounded-full"
                      :class="c.state === 'running' ? 'bg-green-500' : 'bg-red-500'">
                </span>
              </td>
              <td class="py-2 pr-2 font-mono text-gray-200 max-w-[120px] truncate" :title="c.name">
                {{ c.name }}
              </td>
              <td class="py-2 pr-2 text-gray-400 max-w-[120px] truncate" :title="c.image">
                {{ c.image }}
              </td>
              <td class="py-2 pr-2 text-right font-mono">
                {{ c.cpu_percent?.toFixed(1) || '-' }}%
              </td>
              <td class="py-2 pr-2 text-right">
                <div class="font-mono">{{ c.mem_percent?.toFixed(1) || '-' }}%</div>
                <div class="text-gray-600">{{ c.mem_usage || '' }}</div>
              </td>
              <td class="py-2 text-right text-gray-400 max-w-[100px] truncate" :title="c.ports">
                {{ c.ports || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 镜像列表（折叠） -->
      <details class="mt-3">
        <summary class="text-xs text-gray-500 cursor-pointer hover:text-gray-300 select-none">
          镜像 ({{ docker.images?.length || 0 }})
        </summary>
        <div class="mt-2 flex flex-wrap gap-2">
          <span v-for="img in docker.images" :key="img.id"
                class="text-xs bg-gray-700 px-2 py-1 rounded text-gray-400"
                :title="`ID: ${img.id}\n创建: ${img.created_at}\n大小: ${img.size}`">
            {{ img.repo_tag }}
          </span>
        </div>
      </details>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  docker: { type: Object, required: true }
})

const containers = computed(() => {
  if (!props.docker?.containers) return []
  return props.docker.containers
})
</script>
