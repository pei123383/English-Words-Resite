<script setup lang="ts">
defineProps<{
  level: number
}>()

const labels = ['未学习', '初识', '熟悉', '稳定', '掌握', '精通']
const types = ['', 'info', 'warning', 'success', 'success', 'primary'] as const
</script>

<template>
  <el-tag :type="types[Math.min(level, 5)]" effect="light">
    {{ labels[Math.min(level, 5)] }} · {{ level }}/5
  </el-tag>
</template>
