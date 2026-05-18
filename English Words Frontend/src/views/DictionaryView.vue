<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { dictionaryApi } from '@/api/dictionary'
import { wordBookApi } from '@/api/wordBooks'
import { wordsApi } from '@/api/words'
import type { DictionaryEntry, DictionaryQuery, PageResponse, WordBook, WordPayload } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const addVisible = ref(false)
const selectedEntry = ref<DictionaryEntry | null>(null)
const books = ref<WordBook[]>([])
const pageData = ref<PageResponse<DictionaryEntry>>({ items: [], total: 0, page: 0, size: 20, totalPages: 0 })

const query = reactive<DictionaryQuery>({
  keyword: '',
  page: 0,
  size: 20
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
}

async function search() {
  loading.value = true
  try {
    pageData.value = await dictionaryApi.search(query)
  } finally {
    loading.value = false
  }
}

function submitSearch() {
  query.page = 0
  search()
}

function resetSearch() {
  query.keyword = ''
  query.page = 0
  search()
}

function openAdd(entry: DictionaryEntry) {
  selectedEntry.value = entry
  Object.assign(form, {
    bookId: form.bookId || books.value[0]?.id || 0,
    word: entry.word,
    translation: entry.translation,
    phonetic: '',
    example: '',
    tags: ''
  })
  addVisible.value = true
}

async function saveWord() {
  if (!form.bookId) {
    ElMessage.warning('请选择词库')
    return
  }
  saving.value = true
  try {
    await wordsApi.create(form)
    ElMessage.success('已添加到词库')
    addVisible.value = false
  } finally {
    saving.value = false
  }
}

function handlePageChange(page: number) {
  query.page = page - 1
  search()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 0
  search()
}

onMounted(async () => {
  await loadBooks()
  await search()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">英语词典</h1>
        <p class="page-subtitle">搜索内置英语词典，查到合适的释义后可以直接加入自己的词库。</p>
      </div>
    </div>

    <section class="panel">
      <div class="panel-body">
        <div class="dictionary-search">
          <el-input
            v-model.trim="query.keyword"
            clearable
            placeholder="输入英文单词或中文释义"
            :prefix-icon="Search"
            @keyup.enter="submitSearch"
          />
          <el-button type="primary" :icon="Search" :loading="loading" @click="submitSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-body">
        <el-table v-loading="loading" :data="pageData.items" size="large">
          <el-table-column prop="word" label="单词" min-width="160" />
          <el-table-column prop="translation" label="释义" min-width="360" show-overflow-tooltip />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" :icon="Plus" plain @click="openAdd(row)">加入词库</el-button>
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

    <el-dialog v-model="addVisible" title="加入词库" width="620px">
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
          <el-input v-model="form.translation" type="textarea" :rows="4" maxlength="1000" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="例如：词典添加, 高频" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveWord">加入词库</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.dictionary-search {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto auto;
  gap: 12px;
  align-items: center;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 720px) {
  .dictionary-search {
    grid-template-columns: 1fr;
  }
}
</style>
