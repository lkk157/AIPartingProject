<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="名称 / 地址" clearable style="width: 200px" @keyup.enter="reload" />
      <el-input v-model="query.area" placeholder="区域" clearable style="width: 140px" @keyup.enter="reload" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="营业中" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer" />
      <el-button type="primary" @click="openCreate">新建停车场</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" class="table" @row-click="(row: Lot) => router.push(`/lots/${row.id}`)">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="area" label="区域" width="110" />
      <el-table-column prop="address" label="地址" min-width="180" />
      <el-table-column prop="capacity" label="车位" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '营业中' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="router.push(`/lots/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click.stop="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="confirmRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑停车场' : '新建停车场'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：科技园停车场" />
        </el-form-item>
        <el-form-item label="区域" prop="area">
          <el-input v-model="form.area" placeholder="如：南山区" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="车位容量">
          <el-input-number v-model="form.capacity" :min="0" :max="5000" />
          <span class="hint">批量创建车位后会自动同步</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="营业" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createLot, listLots, removeLot, updateLot, type LotPayload } from '@/api/lots'
import type { Lot } from '@/api/types'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const rows = ref<Lot[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const editing = ref<Lot | null>(null)
const formRef = ref<FormInstance>()

const query = reactive<{ page: number; size: number; keyword: string; area: string; status?: number }>({
  page: 1,
  size: 10,
  keyword: '',
  area: '',
  status: undefined,
})

const form = reactive<LotPayload>({
  name: '',
  address: '',
  area: '',
  capacity: 0,
  status: 1,
  description: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  area: [{ required: true, message: '请输入区域', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const result = await listLots(query)
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  void load()
}

function reset() {
  query.keyword = ''
  query.area = ''
  query.status = undefined
  reload()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', address: '', area: '', capacity: 0, status: 1, description: '' })
  dialogVisible.value = true
}

function openEdit(row: Lot) {
  editing.value = row
  Object.assign(form, {
    name: row.name,
    address: row.address,
    area: row.area,
    capacity: row.capacity,
    status: row.status,
    description: row.description ?? '',
  })
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editing.value) {
      await updateLot(editing.value.id, { ...form })
      ElMessage.success('已保存')
    } else {
      await createLot({ ...form })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function confirmRemove(row: Lot) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？其车位、规则与预约会被一并删除。`, '删除确认', {
    type: 'warning',
  }).catch(() => Promise.reject(new Error('cancel')))
  await removeLot(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  padding: 14px 16px;
  border-radius: 12px;
}
.spacer {
  flex: 1;
}
.table {
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
}
.pager {
  justify-content: flex-end;
}
.hint {
  margin-left: 10px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
