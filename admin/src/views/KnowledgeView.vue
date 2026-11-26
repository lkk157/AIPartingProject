<template>
  <div class="page">
    <div class="toolbar glass-panel">
      <div class="title">知识库文档</div>
      <span class="hint">内置 {{ seedCount }} 篇 · 自定义 {{ customCount }} 篇</span>
      <div class="spacer" />
      <el-button type="primary" @click="openCreate">新增文档</el-button>
    </div>

    <div v-if="error" class="glass-panel error-card">
      <div class="error-title">AI 服务不可用</div>
      <div class="error-desc">{{ error }}</div>
      <div class="error-tip">请先启动：<code>cd ai && .venv/Scripts/python -m uvicorn app.main:app --port 8000</code></div>
      <el-button size="small" @click="reload">重试</el-button>
    </div>

    <template v-else>
      <div class="glass-panel table-panel">
        <el-table v-loading="loading" :data="documents" size="default">
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <el-tag :type="row.source === 'seed' ? 'info' : 'success'" size="small">
                {{ row.source === 'seed' ? '内置' : '自定义' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chars" label="字数" width="90" />
          <el-table-column prop="createdAt" label="更新时间" width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.source !== 'seed'" link type="danger" @click="onDelete(row)">删除</el-button>
              <span v-else class="muted">只读</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="glass-panel search-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">检索测试</div>
            <div class="panel-sub">验证用户提问能命中哪篇文档（字符向量召回 + 领域词重排）</div>
          </div>
          <div class="search-box">
            <el-input v-model="query" placeholder="例如：停车怎么收费" style="width: 260px" @keyup.enter="onSearch" />
            <el-button type="primary" :loading="searching" @click="onSearch">检索</el-button>
          </div>
        </div>

        <div v-if="hits.length" class="hits">
          <div v-for="hit in hits" :key="hit.id" class="hit">
            <div class="hit-head">
              <span class="hit-title">{{ hit.title }}</span>
              <span class="hit-source">{{ hit.source === 'seed' ? '内置' : '自定义' }}</span>
              <span class="hit-score">{{ (hit.score * 100).toFixed(1) }}%</span>
            </div>
            <div class="hit-snippet">{{ hit.snippet }}</div>
          </div>
        </div>
        <div v-else-if="searched" class="empty">没有命中的文档，换个问法试试</div>
      </div>
    </template>

    <el-dialog v-model="dialogVisible" title="新增知识库文档" width="560px">
      <el-form label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="如：夜间包月套餐" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="支持 Markdown，建议写清规则、价格与例外情况"
            maxlength="20000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canSave" @click="onCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDocument,
  deleteDocument,
  listDocuments,
  searchKnowledge,
  type KnowledgeDoc,
  type KnowledgeHit,
} from '@/api/knowledge'

const loading = ref(false)
const saving = ref(false)
const searching = ref(false)
const documents = ref<KnowledgeDoc[]>([])
const hits = ref<KnowledgeHit[]>([])
const error = ref('')
const query = ref('停车怎么收费')
const searched = ref(false)
const dialogVisible = ref(false)
const form = reactive({ title: '', content: '' })

const seedCount = computed(() => documents.value.filter((doc) => doc.source === 'seed').length)
const customCount = computed(() => documents.value.filter((doc) => doc.source !== 'seed').length)
const canSave = computed(() => form.title.trim().length >= 2 && form.content.trim().length >= 10)

async function reload() {
  loading.value = true
  error.value = ''
  try {
    documents.value = await listDocuments()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '请求失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.title = ''
  form.content = ''
  dialogVisible.value = true
}

async function onCreate() {
  saving.value = true
  try {
    await createDocument(form.title.trim(), form.content.trim())
    ElMessage.success('已加入知识库，立即可被检索')
    dialogVisible.value = false
    await reload()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(row: KnowledgeDoc) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '删除确认', { type: 'warning' }).catch(() =>
    Promise.reject(new Error('cancel')),
  )
  try {
    await deleteDocument(row.id)
    ElMessage.success('已删除')
    await reload()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function onSearch() {
  if (!query.value.trim()) return
  searching.value = true
  try {
    hits.value = await searchKnowledge(query.value.trim(), 5)
    searched.value = true
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '检索失败')
  } finally {
    searching.value = false
  }
}

onMounted(async () => {
  await reload()
  if (!error.value) {
    await onSearch()
  }
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}
.title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.hint {
  font-size: 12px;
  color: #8290a8;
}
.spacer {
  flex: 1;
}
.table-panel {
  padding: 8px 16px 16px;
}
.search-panel {
  padding: 18px 20px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.panel-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.search-box {
  display: flex;
  gap: 10px;
}
.hits {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hit {
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(47, 107, 255, 0.05);
  border: 1px solid rgba(47, 107, 255, 0.12);
}
.hit-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hit-title {
  font-weight: 600;
  color: #0f172a;
}
.hit-source {
  font-size: 11px;
  color: #8290a8;
  background: #eef2f9;
  padding: 2px 8px;
  border-radius: 999px;
}
.hit-score {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: #2f6bff;
}
.hit-snippet {
  margin-top: 8px;
  font-size: 13px;
  color: #5b6478;
  line-height: 1.6;
}
.empty {
  margin-top: 18px;
  font-size: 13px;
  color: #94a3b8;
}
.muted {
  font-size: 12px;
  color: #b6bfd0;
}
.error-card {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}
.error-title {
  font-size: 16px;
  font-weight: 600;
  color: #b42318;
}
.error-desc {
  font-size: 13px;
  color: #64748b;
}
.error-tip {
  font-size: 12px;
  color: #94a3b8;
}
code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
