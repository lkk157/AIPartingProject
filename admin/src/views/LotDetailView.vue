<template>
  <div v-loading="loading" class="page">
    <el-page-header class="back" @back="router.push('/lots')" />

    <div v-if="detail" class="summary">
      <div class="summary-main">
        <div class="lot-name">{{ detail.lot.name }}</div>
        <div class="lot-meta">{{ detail.lot.area }} · {{ detail.lot.address }}</div>
        <el-tag :type="detail.lot.status === 1 ? 'success' : 'info'" size="small">
          {{ detail.lot.status === 1 ? '营业中' : '停用' }}
        </el-tag>
      </div>
      <div class="summary-stats">
        <div class="mini"><span class="num">{{ detail.spaceCount }}</span><span class="lbl">总车位</span></div>
        <div class="mini"><span class="num free">{{ detail.freeCount }}</span><span class="lbl">空闲</span></div>
        <div class="mini"><span class="num occupied">{{ detail.occupiedCount }}</span><span class="lbl">占用</span></div>
        <div class="mini"><span class="num reserved">{{ detail.reservedCount }}</span><span class="lbl">预约锁定</span></div>
        <div class="mini"><span class="num maintenance">{{ detail.maintenanceCount }}</span><span class="lbl">维护</span></div>
      </div>
    </div>

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane label="车位" name="spaces">
        <div class="toolbar">
          <el-radio-group v-model="spaceStatus" @change="loadSpaces">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">空闲</el-radio-button>
            <el-radio-button :value="1">占用</el-radio-button>
            <el-radio-button :value="2">预约锁定</el-radio-button>
            <el-radio-button :value="3">维护</el-radio-button>
          </el-radio-group>
          <div class="spacer" />
          <el-button type="primary" @click="batchVisible = true">批量创建车位</el-button>
        </div>

        <div v-if="spaces.length === 0" class="empty">暂无车位，先批量创建一批吧</div>
        <div v-else class="grid">
          <div
            v-for="space in spaces"
            :key="space.id"
            class="space"
            :style="{ borderColor: spaceStatusMeta(space.status).color }"
            @click="openStatusDialog(space)"
          >
            <div class="space-no">{{ space.spaceNo }}</div>
            <div class="space-status" :style="{ color: spaceStatusMeta(space.status).color }">
              {{ space.statusText }}
            </div>
            <div class="space-zone">{{ space.zone || '未分区' }} · {{ space.type }}</div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="收费规则" name="rules">
        <div class="toolbar">
          <div class="spacer" />
          <el-button type="primary" @click="openRuleDialog()">新建规则</el-button>
        </div>
        <el-table :data="rules" class="table">
          <el-table-column prop="ruleName" label="规则名称" min-width="120" />
          <el-table-column label="免费时长" width="100">
            <template #default="{ row }">{{ row.freeMinutes }} 分钟</template>
          </el-table-column>
          <el-table-column label="首小时" width="100">
            <template #default="{ row }">{{ formatMoney(row.firstHourCents) }}</template>
          </el-table-column>
          <el-table-column label="超出/小时" width="110">
            <template #default="{ row }">{{ formatMoney(row.extraPerHourCents) }}</template>
          </el-table-column>
          <el-table-column label="24h 封顶" width="110">
            <template #default="{ row }">{{ formatMoney(row.dailyCapCents) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
                {{ row.enabled === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRuleDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="confirmRemoveRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="batchVisible" title="批量创建车位" width="440px">
      <el-form :model="batchForm" label-width="90px">
        <el-form-item label="数量">
          <el-input-number v-model="batchForm.count" :min="1" :max="200" />
        </el-form-item>
        <el-form-item label="编号前缀">
          <el-input v-model="batchForm.prefix" placeholder="如：B2" />
        </el-form-item>
        <el-form-item label="分区">
          <el-input v-model="batchForm.zone" placeholder="如：西区" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="batchForm.type">
            <el-option label="普通" value="NORMAL" />
            <el-option label="充电桩" value="CHARGING" />
            <el-option label="无障碍" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBatch">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="变更车位状态" width="420px">
      <div v-if="activeSpace" class="status-dialog">
        <div class="status-space">车位 {{ activeSpace.spaceNo }}（当前：{{ activeSpace.statusText }}）</div>
        <el-form label-width="90px">
          <el-form-item label="目标状态">
            <el-select v-model="statusForm.status">
              <el-option v-for="s in allowedTargets" :key="s" :label="spaceStatusMeta(s).label" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="原因">
            <el-input v-model="statusForm.reason" placeholder="可选，如：设备检修" />
          </el-form-item>
        </el-form>
        <div class="status-tip">状态流转受后端状态机限制，非法流转会被拒绝。</div>
      </div>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitStatus">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ruleVisible" :title="ruleEditing ? '编辑规则' : '新建规则'" width="480px">
      <el-form :model="ruleForm" label-width="110px">
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.ruleName" />
        </el-form-item>
        <el-form-item label="免费时长(分)">
          <el-input-number v-model="ruleForm.freeMinutes" :min="0" :max="120" />
        </el-form-item>
        <el-form-item label="首小时(元)">
          <el-input-number v-model="ruleForm.firstHour" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="超出/小时(元)">
          <el-input-number v-model="ruleForm.extraPerHour" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="24h 封顶(元)">
          <el-input-number v-model="ruleForm.dailyCap" :min="0" :precision="2" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchCreateSpaces,
  changeSpaceStatus,
  createFeeRule,
  getLot,
  listFeeRules,
  listSpaces,
  removeFeeRule,
  updateFeeRule,
} from '@/api/lots'
import type { FeeRule, LotDetail, Space } from '@/api/types'
import { formatMoney } from '@/utils/format'
import { spaceStatusMeta } from '@/utils/status'

const route = useRoute()
const router = useRouter()
const lotId = Number(route.params.id)

const loading = ref(false)
const saving = ref(false)
const tab = ref('spaces')
const detail = ref<LotDetail | null>(null)
const spaces = ref<Space[]>([])
const rules = ref<FeeRule[]>([])
const spaceStatus = ref(-1)

const batchVisible = ref(false)
const batchForm = reactive({ count: 10, prefix: 'A', zone: '', type: 'NORMAL' })

const statusVisible = ref(false)
const activeSpace = ref<Space | null>(null)
const statusForm = reactive<{ status: number; reason: string }>({ status: 0, reason: '' })

const ruleVisible = ref(false)
const ruleEditing = ref<FeeRule | null>(null)
const ruleForm = reactive({ ruleName: '标准规则', freeMinutes: 15, firstHour: 6, extraPerHour: 4, dailyCap: 40 })

const transitions: Record<number, number[]> = {
  0: [1, 2, 3],
  1: [0],
  2: [0, 1],
  3: [0],
}

const allowedTargets = computed(() => (activeSpace.value ? transitions[activeSpace.value.status] ?? [] : []))

async function loadDetail() {
  detail.value = await getLot(lotId)
}

async function loadSpaces() {
  const result = await listSpaces(lotId, { page: 1, size: 200, status: spaceStatus.value >= 0 ? spaceStatus.value : undefined })
  spaces.value = result.records
}

async function loadRules() {
  rules.value = await listFeeRules(lotId)
}

async function reloadAll() {
  loading.value = true
  try {
    await Promise.all([loadDetail(), loadSpaces(), loadRules()])
  } finally {
    loading.value = false
  }
}

function openStatusDialog(space: Space) {
  if ((transitions[space.status] ?? []).length === 0) return
  activeSpace.value = space
  statusForm.status = transitions[space.status][0]
  statusForm.reason = ''
  statusVisible.value = true
}

async function submitStatus() {
  if (!activeSpace.value) return
  saving.value = true
  try {
    await changeSpaceStatus(activeSpace.value.id, statusForm.status, statusForm.reason)
    ElMessage.success('状态已更新')
    statusVisible.value = false
    await Promise.all([loadSpaces(), loadDetail()])
  } finally {
    saving.value = false
  }
}

async function submitBatch() {
  saving.value = true
  try {
    const result = await batchCreateSpaces(lotId, { ...batchForm })
    ElMessage.success(`已创建 ${result.created} 个车位`)
    batchVisible.value = false
    await Promise.all([loadSpaces(), loadDetail()])
  } finally {
    saving.value = false
  }
}

function openRuleDialog(rule?: FeeRule) {
  ruleEditing.value = rule ?? null
  if (rule) {
    Object.assign(ruleForm, {
      ruleName: rule.ruleName,
      freeMinutes: rule.freeMinutes,
      firstHour: rule.firstHourCents / 100,
      extraPerHour: rule.extraPerHourCents / 100,
      dailyCap: rule.dailyCapCents / 100,
    })
  } else {
    Object.assign(ruleForm, { ruleName: '标准规则', freeMinutes: 15, firstHour: 6, extraPerHour: 4, dailyCap: 40 })
  }
  ruleVisible.value = true
}

async function submitRule() {
  saving.value = true
  const payload = {
    ruleName: ruleForm.ruleName,
    freeMinutes: ruleForm.freeMinutes,
    firstHourCents: Math.round(ruleForm.firstHour * 100),
    extraPerHourCents: Math.round(ruleForm.extraPerHour * 100),
    dailyCapCents: Math.round(ruleForm.dailyCap * 100),
  }
  try {
    if (ruleEditing.value) {
      await updateFeeRule(ruleEditing.value.id, payload)
    } else {
      await createFeeRule(lotId, payload)
    }
    ElMessage.success('规则已保存')
    ruleVisible.value = false
    await Promise.all([loadRules(), loadDetail()])
  } finally {
    saving.value = false
  }
}

async function confirmRemoveRule(rule: FeeRule) {
  await ElMessageBox.confirm(`确认删除规则「${rule.ruleName}」？`, '删除确认', { type: 'warning' }).catch(() =>
    Promise.reject(new Error('cancel')),
  )
  await removeFeeRule(rule.id)
  ElMessage.success('已删除')
  await Promise.all([loadRules(), loadDetail()])
}

onMounted(reloadAll)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.back {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
}
.summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: 0 8px 24px rgba(31, 74, 168, 0.06);
}
.lot-name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}
.lot-meta {
  margin: 6px 0 10px;
  color: #64748b;
  font-size: 13px;
}
.summary-stats {
  display: flex;
  gap: 26px;
}
.mini {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.mini .num {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.mini .num.free {
  color: #16a34a;
}
.mini .num.occupied {
  color: #ef4444;
}
.mini .num.reserved {
  color: #2563eb;
}
.mini .num.maintenance {
  color: #94a3b8;
}
.mini .lbl {
  font-size: 12px;
  color: #94a3b8;
}
.tabs {
  background: #fff;
  border-radius: 14px;
  padding: 8px 18px 18px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.spacer {
  flex: 1;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 12px;
}
.space {
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  background: #fff;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.space:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}
.space-no {
  font-weight: 700;
  color: #0f172a;
}
.space-status {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 600;
}
.space-zone {
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}
.empty {
  color: #94a3b8;
  padding: 30px 0;
  text-align: center;
}
.table {
  width: 100%;
}
.status-space {
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
}
.status-tip {
  font-size: 12px;
  color: #94a3b8;
}
</style>
