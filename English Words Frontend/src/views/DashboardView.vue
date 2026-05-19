<script setup lang="ts">
import { Collection, DataLine, Finished, Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import DateText from '@/components/DateText.vue'
import MasteryTag from '@/components/MasteryTag.vue'
import { progressApi } from '@/api/progress'
import { quizApi } from '@/api/quiz'
import { wordBookApi } from '@/api/wordBooks'
import type { Progress, ProgressOverview, QuizRecord, WordBook } from '@/types/api'

const loading = ref(false)
const overview = ref<ProgressOverview>({
  totalWords: 0,
  dueWords: 0,
  masteredWords: 0,
  totalReviews: 0
})
const dueWords = ref<Progress[]>([])
const history = ref<QuizRecord[]>([])
const books = ref<WordBook[]>([])

const masteryRate = computed(() => {
  if (!overview.value.totalWords) return 0
  return Math.round((overview.value.masteredWords / overview.value.totalWords) * 100)
})

async function load() {
  loading.value = true
  try {
    const [overviewData, dueData, historyData, bookData] = await Promise.all([
      progressApi.overview(),
      progressApi.due(6),
      quizApi.history(6),
      wordBookApi.list()
    ])
    overview.value = overviewData
    dueWords.value = dueData
    history.value = historyData
    books.value = bookData
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">学习总览</h1>
        <p class="page-subtitle">从待复习词、词库规模和最近抽查里判断今天该从哪里开始。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <el-icon><Collection /></el-icon>
        <span>总单词</span>
        <strong>{{ overview.totalWords }}</strong>
      </div>
      <div class="stat-card">
        <el-icon><DataLine /></el-icon>
        <span>待复习</span>
        <strong>{{ overview.dueWords }}</strong>
      </div>
      <div class="stat-card">
        <el-icon><Finished /></el-icon>
        <span>已掌握</span>
        <strong>{{ overview.masteredWords }}</strong>
      </div>
      <div class="stat-card">
        <el-progress type="dashboard" :percentage="masteryRate" :width="76" />
        <span>掌握率</span>
        <strong>{{ masteryRate }}%</strong>
        <small>{{ overview.masteredWords }} / {{ overview.totalWords }} 词，熟练度 ≥ 4/5</small>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-body">
          <div class="section-head">
            <h2>待复习</h2>
            <div class="section-actions">
              <RouterLink to="/review">开始复习</RouterLink>
              <RouterLink to="/progress">查看全部</RouterLink>
            </div>
          </div>
          <el-table v-if="dueWords.length" :data="dueWords" size="large">
            <el-table-column prop="word" label="单词" min-width="120" />
            <el-table-column prop="translation" label="释义" min-width="160" show-overflow-tooltip />
            <el-table-column label="熟练度" width="120">
              <template #default="{ row }">
                <MasteryTag :level="row.masteryLevel" />
              </template>
            </el-table-column>
            <el-table-column label="下次复习" width="130">
              <template #default="{ row }">
                <DateText :value="row.nextReviewAt" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无待复习单词" />
        </div>
      </section>

      <section class="panel">
        <div class="panel-body">
          <div class="section-head">
            <h2>词库</h2>
            <RouterLink to="/books">管理词库</RouterLink>
          </div>
          <div v-if="books.length" class="book-list">
            <div v-for="book in books.slice(0, 5)" :key="book.id" class="book-row">
              <div>
                <strong>{{ book.name }}</strong>
                <span>{{ book.description || '暂无描述' }}</span>
              </div>
              <em>{{ book.wordCount }} 词</em>
            </div>
          </div>
          <el-empty v-else description="还没有词库" />
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-body">
        <div class="section-head">
          <h2>最近抽查</h2>
          <RouterLink to="/history">查看历史</RouterLink>
        </div>
        <el-table v-if="history.length" :data="history" size="large">
          <el-table-column prop="word" label="单词" min-width="140" />
          <el-table-column prop="translation" label="释义" min-width="220" show-overflow-tooltip />
          <el-table-column prop="quality" label="评分" width="90" />
          <el-table-column label="时间" width="150">
            <template #default="{ row }">
              <DateText :value="row.createdAt" />
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无抽查记录" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: grid;
  min-height: 132px;
  align-content: center;
  gap: 8px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.stat-card .el-icon {
  color: #2f8f83;
  font-size: 24px;
}

.stat-card span {
  color: #6b7280;
}

.stat-card strong {
  font-size: 28px;
  color: #111827;
}

.stat-card small {
  color: #6b7280;
  font-size: 13px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(300px, 0.8fr);
  gap: 18px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
}

.section-head a {
  color: #2f8f83;
  text-decoration: none;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.book-list {
  display: grid;
  gap: 10px;
}

.book-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
}

.book-row div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.book-row span {
  overflow: hidden;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-row em {
  flex: 0 0 auto;
  color: #2f8f83;
  font-style: normal;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .stats-grid,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stats-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
