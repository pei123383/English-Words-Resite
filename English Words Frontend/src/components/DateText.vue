<script setup lang="ts">
const props = withDefaults(defineProps<{
  value?: string | null
  format?: 'short' | 'long'
}>(), {
  format: 'short'
})

function parseDate(value?: string | null) {
  if (!value) return '暂无'
  const normalized = value.replace(/(\.\d{3})\d+/, '$1')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return null
  return date
}

function formatTime(date: Date) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function dayDiff(date: Date) {
  const today = new Date()
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime()
  const dateStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  return Math.round((dateStart - todayStart) / 86_400_000)
}

function formatDate(value?: string | null) {
  const date = parseDate(value)
  if (date === '暂无') return date
  if (!date) return '时间格式异常'

  const diff = dayDiff(date)
  if (diff === 0) return `今天 ${formatTime(date)}`
  if (diff === 1) return `明天 ${formatTime(date)}`
  if (diff === -1) return `昨天 ${formatTime(date)}`

  const now = new Date()
  const showYear = props.format === 'long' || date.getFullYear() !== now.getFullYear()
  return new Intl.DateTimeFormat('zh-CN', {
    ...(showYear ? { year: 'numeric' as const } : {}),
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function fullDate(value?: string | null) {
  const date = parseDate(value)
  if (date === '暂无') return date
  if (!date) return '时间格式异常'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}
</script>

<template>
  <span :title="fullDate(value)">{{ formatDate(value) }}</span>
</template>
