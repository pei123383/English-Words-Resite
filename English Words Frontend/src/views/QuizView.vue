<script setup lang="ts">
import { Check, Close, Refresh, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import DateText from '@/components/DateText.vue'
import MasteryTag from '@/components/MasteryTag.vue'
import PronunciationButton from '@/components/PronunciationButton.vue'
import { quizApi } from '@/api/quiz'
import { wordBookApi } from '@/api/wordBooks'
import type { QuizMode, QuizQuestion, QuizSubmitResult, WordBook } from '@/types/api'

const loading = ref(false)
const submitting = ref(false)
const books = ref<WordBook[]>([])
const questions = ref<QuizQuestion[]>([])
const currentIndex = ref(0)
const selectedAnswer = ref('')
const submitResult = ref<QuizSubmitResult | null>(null)
const questionStartedAt = ref<number | null>(null)
const lastResult = ref<QuizSubmitResult | null>(null)

const form = reactive({
  bookId: undefined as number | undefined,
  count: 10,
  mode: 'MIXED' as QuizMode,
  onlyDue: false
})

const currentQuestion = computed(() => questions.value[currentIndex.value])
const finished = computed(() => questions.value.length > 0 && currentIndex.value >= questions.value.length)
const canAnswer = computed(() => Boolean(currentQuestion.value && !submitResult.value && !submitting.value))
const promptPronunciationText = computed(() =>
  currentQuestion.value?.mode === 'EN_TO_CN' ? currentQuestion.value.word : ''
)

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

async function loadBooks() {
  books.value = await wordBookApi.list()
}

async function startQuiz() {
  loading.value = true
  lastResult.value = null
  submitResult.value = null
  selectedAnswer.value = ''
  try {
    questions.value = await quizApi.random({
      bookId: form.bookId,
      count: form.count,
      mode: form.mode,
      onlyDue: form.onlyDue
    })
    currentIndex.value = 0
    questionStartedAt.value = questions.value.length ? performance.now() : null
    if (!questions.value.length) {
      ElMessage.info('当前条件下没有足够的可抽查单词，请至少准备 2 个可用单词')
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

onMounted(() => {
  loadBooks()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">随机抽查</h1>
        <p class="page-subtitle">选择正确答案，系统会根据准确度和作答时间自动更新熟练度。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="startQuiz">开始抽查</el-button>
    </div>

    <section class="panel">
      <div class="panel-body">
        <div class="form-grid">
          <el-select v-model="form.bookId" clearable placeholder="全部词库">
            <el-option v-for="book in books" :key="book.id" :label="book.name" :value="book.id" />
          </el-select>
          <el-input-number v-model="form.count" :min="1" :max="100" controls-position="right" />
          <el-select v-model="form.mode" placeholder="题型">
            <el-option label="混合" value="MIXED" />
            <el-option label="英译中" value="EN_TO_CN" />
            <el-option label="中译英" value="CN_TO_EN" />
          </el-select>
          <el-checkbox v-model="form.onlyDue">只抽待复习</el-checkbox>
        </div>
      </div>
    </section>

    <section v-if="currentQuestion && !finished" class="quiz-card panel">
      <div class="panel-body">
        <div class="quiz-topline">
          <span>第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
          <MasteryTag :level="submitResult?.progress.masteryLevel ?? currentQuestion.masteryLevel" />
        </div>

        <div class="quiz-mode">{{ modeLabel(currentQuestion.mode) }}</div>
        <div class="quiz-prompt-row">
          <h2 class="quiz-prompt">{{ currentQuestion.prompt }}</h2>
          <PronunciationButton v-if="promptPronunciationText" :text="promptPronunciationText" size="default" />
        </div>

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
            <PronunciationButton v-if="currentQuestion.mode === 'CN_TO_EN'" :text="currentQuestion.word" />
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
        <el-result icon="success" title="本轮抽查完成">
          <template #sub-title>
            <span v-if="lastResult">
              最后一个单词熟练度 {{ lastResult.progress.masteryLevel }}/5，下一次复习
              <DateText :value="lastResult.progress.nextReviewAt" />
            </span>
          </template>
          <template #extra>
            <el-button type="primary" @click="startQuiz">再来一轮</el-button>
          </template>
        </el-result>
      </div>
    </section>

    <section v-else class="panel">
      <div class="empty-panel">
        <el-empty description="选择条件后开始抽查" />
      </div>
    </section>
  </div>
</template>

<style scoped>
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

.quiz-prompt-row {
  display: flex;
  max-width: 900px;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
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
  .quiz-prompt {
    font-size: 34px;
  }

  .quiz-prompt-row {
    align-items: flex-start;
  }

  .option-grid {
    grid-template-columns: 1fr;
  }
}
</style>
