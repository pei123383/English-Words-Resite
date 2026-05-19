<script setup lang="ts">
import { Refresh, RefreshLeft, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import DateText from '@/components/DateText.vue'
import MasteryTag from '@/components/MasteryTag.vue'
import { progressApi } from '@/api/progress'
import type { Progress, ProgressOverview } from '@/types/api'

const loading = ref(false)
const overview = ref<ProgressOverview>({ totalWords: 0, dueWords: 0, masteredWords: 0, totalReviews: 0 })
const dueWords = ref<Progress[]>([])

async function load() {
  loading.value = true
  try {
    const [overviewData, dueData] = await Promise.all([progressApi.overview(), progressApi.due(100)])
    overview.value = overviewData
    dueWords.value = dueData
  } finally {
    loading.value = false
  }
}

async function reset(row: Progress) {
  await ElMessageBox.confirm(`确认重置「${row.word}」的学习进度？`, '重置进度', {
    confirmButtonText: '重置',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await progressApi.reset(row.wordId)
  ElMessage.success('进度已重置')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">复习进度</h1>
        <p class="page-subtitle">查看当前到期单词和整体掌握情况，对异常词条可以单独重置进度。</p>
      </div>
      <div class="header-actions">
        <RouterLink to="/review" custom v-slot="{ navigate }">
          <el-button type="primary" :icon="VideoPlay" @click="navigate">开始复习</el-button>
        </RouterLink>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="mini-stats">
      <div><span>总单词</span><strong>{{ overview.totalWords }}</strong></div>
      <div><span>待复习</span><strong>{{ overview.dueWords }}</strong></div>
      <div><span>已掌握</span><strong>{{ overview.masteredWords }}</strong></div>
      <div><span>总复习</span><strong>{{ overview.totalReviews }}</strong></div>
    </div>

    <section class="panel">
      <div class="panel-body">
        <el-table v-loading="loading" :data="dueWords" size="large">
          <el-table-column prop="word" label="单词" min-width="140" />
          <el-table-column prop="translation" label="释义" min-width="240" show-overflow-tooltip />
          <el-table-column label="熟练度" width="130">
            <template #default="{ row }">
              <MasteryTag :level="row.masteryLevel" />
            </template>
          </el-table-column>
          <el-table-column prop="reviewCount" label="复习" width="80" />
          <el-table-column prop="correctCount" label="正确" width="80" />
          <el-table-column prop="wrongCount" label="错误" width="80" />
          <el-table-column label="下次复习" width="150">
            <template #default="{ row }">
              <DateText :value="row.nextReviewAt" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button :icon="RefreshLeft" circle @click="reset(row)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.mini-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.mini-stats div {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.mini-stats span {
  color: #6b7280;
}

.mini-stats strong {
  font-size: 26px;
  color: #111827;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 820px) {
  .mini-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
