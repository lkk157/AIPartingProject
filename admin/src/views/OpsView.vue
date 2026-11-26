<template>
  <div class="page">
    <div class="toolbar glass-panel">
      <div class="title">AI 运营分析</div>
      <span class="hint">预定义统计口径 + 占用率模型，不生成 SQL、不编造数据</span>
      <div class="spacer" />
      <span v-if="data" class="stamp">生成于 {{ data.generatedAt.replace('T', ' ') }}</span>
      <el-button type="primary" :loading="loading" @click="run">生成分析</el-button>
    </div>

    <div v-if="error" class="glass-panel error-card">
      <div class="error-title">分析暂不可用</div>
      <div class="error-desc">{{ error }}</div>
      <div class="error-tip">需要同时启动业务后端（8080）与 AI 服务（8000），并使用管理员账号登录。</div>
      <el-button size="small" @click="run">重试</el-button>
    </div>

    <template v-else-if="data">
      <div class="metrics">
        <div class="metric glass-panel">
          <div class="m-label">今日订单</div>
          <div class="m-value">{{ data.stats.todayOrders }}</div>
          <div class="m-extra">待用预约 {{ data.stats.pendingReservations }}</div>
        </div>
        <div class="metric glass-panel">
          <div class="m-label">今日营收</div>
          <div class="m-value">{{ money(data.stats.todayRevenueCents) }}</div>
          <div class="m-extra">已支付订单合计</div>
        </div>
        <div class="metric glass-panel">
          <div class="m-label">空闲车位</div>
          <div class="m-value">{{ data.stats.freeSpaces }}</div>
          <div class="m-extra">占用 {{ data.stats.occupiedSpaces }} / 总 {{ data.stats.spaceCount }}</div>
        </div>
        <div class="metric glass-panel">
          <div class="m-label">预测高峰</div>
          <div class="m-value">{{ data.forecast ? data.forecast.peakTime : '-' }}</div>
          <div class="m-extra">
            {{ data.forecast ? `占用 ${(data.forecast.peakRate * 100).toFixed(0)}% · 空闲 ${data.forecast.peakFree} 位` : '预测服务不可用' }}
          </div>
        </div>
      </div>

      <div class="glass-panel summary-panel">
        <div class="panel-head">
          <div class="panel-title">分析结论</div>
          <span class="panel-sub">{{ data.forecast ? `模型 ${data.forecast.model.toUpperCase()} · ${data.forecast.lotName}` : '未包含预测' }}</span>
        </div>
        <pre class="summary">{{ data.summary }}</pre>
      </div>
    </template>

    <div v-else class="glass-panel placeholder">
      点击右上角「生成分析」，汇总今日运营数据与未来 12 小时高峰预测
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchOpsOverview, type OpsOverview } from '@/api/ops'

const loading = ref(false)
const error = ref('')
const data = ref<OpsOverview | null>(null)

function money(cents: number): string {
  return `¥${((cents ?? 0) / 100).toFixed(2)}`
}

async function run() {
  loading.value = true
  error.value = ''
  try {
    data.value = await fetchOpsOverview()
  } catch (err: any) {
    data.value = null
    const status = err?.response?.status
    if (status === 401) {
      error.value = '登录状态已失效，请重新登录'
    } else if (status === 403) {
      error.value = '当前账号没有运营数据权限（需系统管理员）'
    } else if (status === 502) {
      error.value = '业务后端不可用，请确认 server 已启动'
    } else {
      error.value = err instanceof Error ? err.message : '请求失败'
    }
  } finally {
    loading.value = false
  }
}

onMounted(run)
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
  flex-wrap: wrap;
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
.stamp {
  font-size: 12px;
  color: #94a3b8;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 16px;
}
.metric {
  padding: 18px 20px;
}
.m-label {
  font-size: 13px;
  color: #64748b;
}
.m-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}
.m-extra {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}
.summary-panel {
  padding: 18px 20px;
}
.panel-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.panel-sub {
  font-size: 12px;
  color: #94a3b8;
}
.summary {
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.9;
  color: #334155;
  white-space: pre-wrap;
  font-family: inherit;
}
.placeholder {
  padding: 48px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
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
</style>
