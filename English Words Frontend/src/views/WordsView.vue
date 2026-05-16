<script setup lang="ts">
import { Delete, Edit, Plus, Refresh, UploadFilled } from '@element-plus/icons-vue'
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import DateText from '@/components/DateText.vue'
import MasteryTag from '@/components/MasteryTag.vue'
import { wordBookApi } from '@/api/wordBooks'
import { wordsApi } from '@/api/words'
import type { PageResponse, Word, WordBook, WordImportResult, WordPayload, WordQuery } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const importVisible = ref(false)
const editingId = ref<number | null>(null)
const books = ref<WordBook[]>([])
const pageData = ref<PageResponse<Word>>({ items: [], total: 0, page: 0, size: 20, totalPages: 0 })
const importResult = ref<WordImportResult | null>(null)
const importBookId = ref<number | undefined>()

const query = reactive<WordQuery>({
  page: 0,
  size: 20,
  onlyDue: false
})

const form = reactive<WordPayload>({
  bookId: 0,
  word: '',
  translation: '',
  phonetic: '',
  example: '',
  tags: ''
})

const bookOptions = computed(() => books.value.map((book) => ({ label: book.name, value: book.id })))

async function loadBooks() {
  books.value = await wordBookApi.list()
  if (!form.bookId && books.value[0]) form.bookId = books.value[0].id
  if (!importBookId.value && books.value[0]) importBookId.value = books.value[0].id
}

async function loadWords() {
  loading.value = true
  try {
    pageData.value = await wordsApi.list(query)
  } finally {
    loading.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadBooks(), loadWords()])
}

function search() {
  query.page = 0
  loadWords()
}

function resetFilters() {
  query.bookId = undefined
  query.keyword = ''
  query.tag = ''
  query.masteryLevel = undefined
  query.onlyDue = false
  query.page = 0
  loadWords()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    bookId: books.value[0]?.id || 0,
    word: '',
    translation: '',
    phonetic: '',
    example: '',
    tags: ''
  })
  dialogVisible.value = true
}

function openEdit(word: Word) {
  editingId.value = word.id
  Object.assign(form, {
    bookId: word.bookId,
    word: word.word,
    translation: word.translation,
    phonetic: word.phonetic || '',
    example: word.example || '',
    tags: word.tags || ''
  })
  dialogVisible.value = true
}

async function saveWord() {
  if (!form.bookId || !form.word.trim() || !form.translation.trim()) {
    ElMessage.warning('词库、单词和释义不能为空')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await wordsApi.update(editingId.value, form)
      ElMessage.success('单词已更新')
    } else {
      await wordsApi.create(form)
      ElMessage.success('单词已添加')
    }
    dialogVisible.value = false
    await reloadAll()
  } finally {
    saving.value = false
  }
}

async function removeWord(word: Word) {
  await ElMessageBox.confirm(`确认删除「${word.word}」？`, '删除单词', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await wordsApi.remove(word.id)
  ElMessage.success('单词已删除')
  await reloadAll()
}

async function uploadFile(options: UploadRequestOptions) {
  if (!importBookId.value) {
    ElMessage.warning('请选择导入词库')
    return
  }
  importResult.value = await wordsApi.import(importBookId.value, options.file)
  ElMessage.success('导入完成')
  await reloadAll()
}

function handlePageChange(page: number) {
  query.page = page - 1
  loadWords()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 0
  loadWords()
}

onMounted(reloadAll)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">单词管理</h1>
        <p class="page-subtitle">新增、维护和批量导入单词，并按熟练度或到期状态快速筛选。</p>
      </div>
      <div class="toolbar">
        <el-button :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
        <el-button :icon="UploadFilled" @click="importVisible = true">导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增单词</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-body">
        <div class="form-grid">
          <el-select v-model="query.bookId" clearable placeholder="全部词库">
            <el-option v-for="book in bookOptions" :key="book.value" :label="book.label" :value="book.value" />
          </el-select>
          <el-input v-model.trim="query.keyword" clearable placeholder="搜索单词或释义" />
          <el-input v-model.trim="query.tag" clearable placeholder="标签" />
          <el-select v-model="query.masteryLevel" clearable placeholder="熟练度">
            <el-option v-for="level in 6" :key="level - 1" :label="`${level - 1}/5`" :value="level - 1" />
          </el-select>
          <el-checkbox v-model="query.onlyDue">只看待复习</el-checkbox>
          <div class="toolbar">
            <el-button type="primary" @click="search">筛选</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-body">
        <el-table v-loading="loading" :data="pageData.items" size="large">
          <el-table-column prop="word" label="单词" min-width="130" />
          <el-table-column prop="translation" label="释义" min-width="220" show-overflow-tooltip />
          <el-table-column prop="bookName" label="词库" min-width="130" />
          <el-table-column label="熟练度" width="130">
            <template #default="{ row }">
              <MasteryTag :level="row.masteryLevel" />
            </template>
          </el-table-column>
          <el-table-column prop="reviewCount" label="复习" width="80" />
          <el-table-column label="下次复习" width="140">
            <template #default="{ row }">
              <DateText :value="row.nextReviewAt" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button :icon="Edit" circle @click="openEdit(row)" />
              <el-button :icon="Delete" circle type="danger" @click="removeWord(row)" />
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :current-page="pageData.page + 1"
            :page-size="pageData.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pageData.total"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑单词' : '新增单词'" width="620px">
      <el-form label-position="top">
        <el-form-item label="词库" required>
          <el-select v-model="form.bookId" placeholder="请选择词库">
            <el-option v-for="book in bookOptions" :key="book.value" :label="book.label" :value="book.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单词" required>
          <el-input v-model.trim="form.word" maxlength="160" />
        </el-form-item>
        <el-form-item label="释义" required>
          <el-input v-model="form.translation" type="textarea" :rows="3" maxlength="1000" />
        </el-form-item>
        <el-form-item label="音标">
          <el-input v-model="form.phonetic" maxlength="160" />
        </el-form-item>
        <el-form-item label="例句">
          <el-input v-model="form.example" type="textarea" :rows="3" maxlength="1000" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="例如：四级, 高频, 动词" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveWord">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入" width="620px">
      <el-form label-position="top">
        <el-form-item label="导入到词库" required>
          <el-select v-model="importBookId" placeholder="请选择词库">
            <el-option v-for="book in bookOptions" :key="book.value" :label="book.label" :value="book.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-upload drag :http-request="uploadFile" :show-file-list="false" accept=".csv,.xlsx,.xls">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到这里，或点击选择 CSV/Excel</div>
        <template #tip>
          <div class="el-upload__tip">表头固定为 word, translation, phonetic, example, tags</div>
        </template>
      </el-upload>

      <el-alert v-if="importResult" class="import-result" type="success" show-icon :closable="false">
        <template #title>
          共 {{ importResult.totalRows }} 行，新增 {{ importResult.createdCount }}，更新
          {{ importResult.updatedCount }}，跳过 {{ importResult.skippedCount }}
        </template>
      </el-alert>
      <el-alert
        v-for="error in importResult?.errors || []"
        :key="error"
        class="import-error"
        type="warning"
        :title="error"
        :closable="false"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.import-result {
  margin-top: 16px;
}

.import-error {
  margin-top: 8px;
}
</style>
