<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import DateText from '@/components/DateText.vue'
import PronunciationButton from '@/components/PronunciationButton.vue'
import { quizApi } from '@/api/quiz'
import type { QuizRecord } from '@/types/api'

const loading = ref(false)
const records = ref<QuizRecord[]>([])

async function load() {
  loading.value = true
  try {
    records.value = await quizApi.history(100)
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
        <h1 class="page-title">考察历史</h1>
        <p class="page-subtitle">保留最近的抽查提交记录，便于回看题型、评分和记忆状态。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="panel">
      <div class="panel-body">
        <el-table v-loading="loading" :data="records" size="large">
          <el-table-column label="单词" min-width="160">
            <template #default="{ row }">
              <span class="word-with-pronunciation">
                <span>{{ row.word }}</span>
                <PronunciationButton :text="row.word" />
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="translation" label="释义" min-width="240" show-overflow-tooltip />
          <el-table-column prop="quality" label="评分" width="90" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.remembered ? 'success' : 'danger'">
                {{ row.remembered ? '记住' : '未记住' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">
              <DateText :value="row.createdAt" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.word-with-pronunciation {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 8px;
}

.word-with-pronunciation > span {
  min-width: 0;
  overflow-wrap: anywhere;
}
</style>
