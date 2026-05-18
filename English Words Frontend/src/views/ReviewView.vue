<script setup lang="ts">
import { Check, Close, DataLine, Refresh, Right, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import DateText from '@/components/DateText.vue'
import MasteryTag from '@/components/MasteryTag.vue'
import { progressApi } from '@/api/progress'
import { quizApi } from '@/api/quiz'
import type { ProgressOverview, QuizMode, QuizQuestion, QuizSubmitResult } from '@/types/api'

const loading = ref(false)
const submitting = ref(false)
const questions = ref<QuizQuestion[]>([])
const currentIndex = ref(0)
const selectedAnswer = ref('')
const submitResult = ref<QuizSubmitResult | null>(null)
const questionStartedAt = ref<number | null>(null)
const lastResult = ref<QuizSubmitResult | null>(null)
const emptyMessage = ref('点击开始复习，进入本轮已学习单词练习')
const overview = ref<ProgressOverview>({
  totalWords: 0,
  dueWords: 0,
  masteredWords: 0,
  totalReviews: 0
})

const currentQuestion = computed(() => questions.value[currentIndex.value])
const finished = computed(() => questions.value.length > 0 && currentIndex.value >= questions.value.length)
const canAnswer = computed(() => Boolean(currentQuestion.value && !submitResult.value && !submitting.value))
const canStartReview = computed(() => overview.value.dueWords > 0 && !loading.value)
const hasActiveReview = computed(() => Boolean(currentQuestion.value && !finished.value))

function modeLabel(mode: QuizMode) {
  if (mode === 'EN_TO_CN') return '英译中'
  if (mode === 'CN_TO_EN') return '中译英'
  return '混合'
}

function formattedSeconds(ms: number) {
  return `${(ms / 1000).toFixed(1)} 秒`
}

function optionClass(option: string) {
  if (!submitResult.value) return ''
  if (option === submitResult.value.correctAnswer) return 'is-correct'
  if (option === submitResult.value.selectedAnswer) return 'is-wrong'
  return 'is-muted'
}

async function loadOverview() {
  overview.value = await progressApi.overview()
}

async function refreshOverview() {
  loading.value = true
  try {
    await loadOverview()
  } finally {
    loading.value = false
  }
}

async function startReview() {
  loading.value = true
  lastResult.value = null
  submitResult.value = null
  selectedAnswer.value = ''
  emptyMessage.value = '点击开始复习，进入本轮已学习单词练习'
  try {
    await loadOverview()
    if (overview.value.dueWords <= 0) {
      questions.value = []
      currentIndex.value = 0
      questionStartedAt.value = null
      return
    }

    questions.value = await quizApi.random({
      count: Math.min(Math.max(overview.value.dueWords, 1), 20),
      mode: 'MIXED',
      onlyDue: true
    })
    currentIndex.value = 0
    questionStartedAt.value = questions.value.length ? performance.now() : null
    if (!questions.value.length) {
      emptyMessage.value = '暂无可复习题目'
      ElMessage.info('当前已学习单词不足以组成选择题，可以先去随机抽查练习。')
    }
  } finally {
    loading.value = false
  }
}

async function submitAnswer(option: string) {
  if (!canAnswer.value || !currentQuestion.value) return
  selectedAnswer.value = option
  submitting.value = true
  const elapsed = Math.max(0, Math.round(performance.now() - (questionStartedAt.value ?? performance.now())))
  try {
    submitResult.value = await quizApi.submit({
      wordId: currentQuestion.value.wordId,
      mode: currentQuestion.value.mode,
      selectedAnswer: option,
      responseTimeMs: elapsed
    })
    lastResult.value = submitResult.value
    await loadOverview()
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  currentIndex.value += 1
  selectedAnswer.value = ''
  submitResult.value = null
  questionStartedAt.value = finished.value ? null : performance.now()
}

watch(currentQuestion, async (question) => {
  if (!question || submitResult.value) return
  await nextTick()
  questionStartedAt.value = performance.now()
})

onMounted(loadOverview)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">复习</h1>
        <p class="page-subtitle">默认练习已经学习过的单词，未到下次复习时间的单词也会显示在这里。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="refreshOverview">刷新</el-button>
        <el-button type="primary" :icon="VideoPlay" :disabled="!canStartReview" :loading="loading" @click="startReview">
          开始复习
        </el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-body review-summary">
        <div class="due-stat">
          <el-icon><DataLine /></el-icon>
          <div>
            <span>待复习单词</span>
            <strong>{{ overview.dueWords }}</strong>
          </div>
        </div>
        <div class="summary-copy">
          <strong>只抽已学习单词</strong>
          <span>本页固定使用待复习筛选，不会混入还没有学习过的单词。</span>
        </div>
        <el-button type="primary" :icon="VideoPlay" :disabled="!canStartReview" :loading="loading" @click="startReview">
          开始复习
        </el-button>
      </div>
    </section>

    <section v-if="hasActiveReview" class="quiz-card panel">
      <div class="panel-body">
        <div class="quiz-topline">
          <span>第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
          <MasteryTag :level="submitResult?.progress.masteryLevel ?? currentQuestion.masteryLevel" />
        </div>

        <div class="quiz-mode">{{ modeLabel(currentQuestion.mode) }}</div>
        <h2 class="quiz-prompt">{{ currentQuestion.prompt }}</h2>

        <div class="quiz-meta">
          <span v-if="currentQuestion.phonetic">{{ currentQuestion.phonetic }}</span>
          <span v-if="currentQuestion.tags">{{ currentQuestion.tags }}</span>
        </div>

        <div class="option-grid">
          <button
            v-for="option in currentQuestion.options"
            :key="option"
            class="option-button"
            :class="optionClass(option)"
            :disabled="!canAnswer"
            @click="submitAnswer(option)"
          >
            <span>{{ option }}</span>
            <el-icon v-if="submitResult && option === submitResult.correctAnswer"><Check /></el-icon>
            <el-icon v-else-if="submitResult && option === submitResult.selectedAnswer"><Close /></el-icon>
          </button>
        </div>

        <div v-if="submitResult" class="answer-box" :class="{ success: submitResult.correct, danger: !submitResult.correct }">
          <div class="result-title">
            <el-icon><Check v-if="submitResult.correct" /><Close v-else /></el-icon>
            <strong>{{ submitResult.correct ? '回答正确' : '回答错误' }}</strong>
          </div>
          <p>
            正确答案：<strong>{{ submitResult.correctAnswer }}</strong>
          </p>
          <p v-if="currentQuestion.example">{{ currentQuestion.example }}</p>
          <div class="result-stats">
            <span>用时 {{ formattedSeconds(submitResult.responseTimeMs) }}</span>
            <span>系统评分 {{ submitResult.quality }}/5</span>
            <span>熟练度 {{ submitResult.progress.masteryLevel }}/5</span>
            <span>下次复习 <DateText :value="submitResult.progress.nextReviewAt" /></span>
          </div>
          <el-button type="primary" :icon="Right" @click="nextQuestion">
            {{ currentIndex + 1 >= questions.length ? '完成本轮' : '下一题' }}
          </el-button>
        </div>
      </div>
    </section>

    <section v-else-if="finished" class="panel">
      <div class="empty-panel">
        <el-result icon="success" title="本轮复习完成">
          <template #sub-title>
            <span v-if="lastResult">
              最后一个单词熟练度 {{ lastResult.progress.masteryLevel }}/5，下次复习
              <DateText :value="lastResult.progress.nextReviewAt" />
            </span>
          </template>
          <template #extra>
            <el-button type="primary" :disabled="overview.dueWords <= 0" @click="startReview">继续复习</el-button>
            <RouterLink to="/quiz" custom v-slot="{ navigate }">
              <el-button @click="navigate">随机抽查</el-button>
            </RouterLink>
          </template>
        </el-result>
      </div>
    </section>

    <section v-else-if="overview.dueWords <= 0" class="panel">
      <div class="empty-panel">
        <el-empty description="暂无已学习单词">
          <template #default>
            <RouterLink to="/quiz" custom v-slot="{ navigate }">
              <el-button @click="navigate">随机抽查</el-button>
            </RouterLink>
          </template>
        </el-empty>
      </div>
    </section>

    <section v-else class="panel">
      <div class="empty-panel">
        <el-empty :description="emptyMessage">
          <template #default>
            <el-button type="primary" :icon="VideoPlay" :loading="loading" @click="startReview">开始复习</el-button>
            <RouterLink v-if="emptyMessage === '暂无可复习题目'" to="/quiz" custom v-slot="{ navigate }">
              <el-button @click="navigate">随机抽查</el-button>
            </RouterLink>
          </template>
        </el-empty>
      </div>
    </section>
  </div>
</template>

<style scoped>
.header-actions,
.review-summary,
.summary-copy,
.due-stat,
.result-title,
.result-stats {
  display: flex;
  align-items: center;
}

.header-actions {
  flex-wrap: wrap;
  gap: 10px;
}

.review-summary {
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
}

.due-stat {
  gap: 14px;
}

.due-stat .el-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 8px;
  background: #eefaf6;
  color: #2f8f83;
  font-size: 24px;
}

.due-stat div,
.summary-copy {
  display: grid;
  gap: 4px;
}

.due-stat span,
.summary-copy span {
  color: #6b7280;
}

.due-stat strong {
  color: #111827;
  font-size: 30px;
  line-height: 1;
}

.summary-copy {
  flex: 1 1 260px;
}

.summary-copy strong {
  color: #111827;
}

.quiz-card {
  min-height: 440px;
}

.quiz-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #6b7280;
}

.quiz-mode {
  margin-top: 26px;
  color: #2f8f83;
  font-weight: 700;
}

.quiz-prompt {
  margin: 10px 0 0;
  color: #111827;
  font-size: 44px;
  letter-spacing: 0;
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.quiz-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 14px 0 24px;
  color: #6b7280;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  max-width: 900px;
}

.option-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.option-button:hover:not(:disabled) {
  border-color: #2f8f83;
  box-shadow: 0 8px 18px rgb(47 143 131 / 12%);
}

.option-button:disabled {
  cursor: default;
}

.option-button span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.option-button .el-icon {
  flex: 0 0 auto;
  font-size: 20px;
}

.option-button.is-correct {
  border-color: #2f8f83;
  background: #eefaf6;
  color: #146356;
}

.option-button.is-wrong {
  border-color: #e57474;
  background: #fff1f1;
  color: #9f2525;
}

.option-button.is-muted {
  color: #8a94a3;
  background: #f7f8fa;
}

.answer-box {
  display: grid;
  max-width: 900px;
  gap: 12px;
  margin-top: 18px;
  padding: 18px;
  border: 1px solid #d6ebe7;
  border-radius: 8px;
  background: #f5fbfa;
}


.answer-box.danger {
  border-color: #f2c2c2;
  background: #fff7f7;
}

.result-title,
.result-stats {
  flex-wrap: wrap;
  gap: 12px;
}

.result-title {
  color: #146356;
  font-size: 18px;
}

.answer-box.danger .result-title {
  color: #9f2525;
}

.answer-box p {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
}

.result-stats span {
  padding: 6px 10px;
  border-radius: 8px;
  background: rgb(255 255 255 / 70%);
  color: #4b5563;
}

@media (max-width: 680px) {
  .review-summary {
    align-items: stretch;
  }

  .review-summary > .el-button {
    width: 100%;
  }

  .quiz-prompt {
    font-size: 34px;
  }

  .option-grid {
    grid-template-columns: 1fr;
  }
}
</style>
