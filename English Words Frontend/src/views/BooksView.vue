<script setup lang="ts">
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import DateText from '@/components/DateText.vue'
import { wordBookApi, type WordBookPayload } from '@/api/wordBooks'
import type { WordBook } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const books = ref<WordBook[]>([])

const form = reactive<WordBookPayload>({
  name: '',
  description: ''
})

async function load() {
  loading.value = true
  try {
    books.value = await wordBookApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

function openEdit(book: WordBook) {
  editingId.value = book.id
  form.name = book.name
  form.description = book.description || ''
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入词库名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await wordBookApi.update(editingId.value, form)
      ElMessage.success('词库已更新')
    } else {
      await wordBookApi.create(form)
      ElMessage.success('词库已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(book: WordBook) {
  await ElMessageBox.confirm(`确认删除词库「${book.name}」及其单词和记录？`, '删除词库', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await wordBookApi.remove(book.id)
  ElMessage.success('词库已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">词库管理</h1>
        <p class="page-subtitle">按考试、课程或主题拆分词库，后续抽查和导入都可以指定范围。</p>
      </div>
      <div class="toolbar">
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建词库</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-body">
        <el-table v-loading="loading" :data="books" size="large">
          <el-table-column prop="name" label="词库名称" min-width="180" />
          <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
          </el-table-column>
          <el-table-column prop="wordCount" label="单词数" width="110" />
          <el-table-column label="更新时间" width="150">
            <template #default="{ row }">
              <DateText :value="row.updatedAt" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button :icon="Edit" circle @click="openEdit(row)" />
              <el-button :icon="Delete" circle type="danger" @click="remove(row)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑词库' : '新建词库'" width="460px">
      <el-form label-position="top">
        <el-form-item label="词库名称" required>
          <el-input v-model.trim="form.name" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
