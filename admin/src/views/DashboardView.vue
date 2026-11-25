<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">停车场</div>
        <div class="stat-value">{{ stats.lotCount }}</div>
        <div class="stat-extra">共 {{ stats.spaceCount }} 个车位</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">空闲车位</div>
        <div class="stat-value free">{{ stats.freeSpaces }}</div>
        <div class="stat-extra">占用 {{ stats.occupiedSpaces }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日订单</div>
        <div class="stat-value">{{ stats.todayOrders }}</div>
        <div class="stat-extra">待用预约 {{ stats.pendingReservations }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日营收</div>
        <div class="stat-value amount">{{ formatMoney(stats.todayRevenueCents) }}</div>
        <div class="stat-extra">已支付合计</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">车位状态分布</div>
      <div ref="chartRef" class="chart" />
    </div>

    <div class="panel">
      <div class="panel-title">快捷操作</div>
      <div class="quick">
        <el-button type="primary" @click="router.push('/lots')">停车场管理</el-button>
        <el-button @click="router.push('/reservations')">预约管理</el-button>
        <el-button @click="router.push('/orders')">订单管理</el-button>
        <el-button @click="router.push('/users')">用户管理</el-button>
      </div>
      <div class="tips">AI 运营分析与问答将在 P7 接入，本页数据全部来自真实接口。</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { fetchStats } from '@/api/admin'
import type { Stats } from '@/api/types'
import { formatMoney } from '@/utils/format'

const router = useRouter()
const stats = ref<Stats>({
  lotCount: 0,
  spaceCount: 0,
  freeSpaces: 0,
  occupiedSpaces: 0,
  todayOrders: 0,
  todayRevenueCents: 0,
  pendingReservations: 0,
})

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

function renderChart() {
  if (!chartRef.value) return
  chart = chart ?? echarts.init(chartRef.value)
  const rest = Math.max(stats.value.spaceCount - stats.value.freeSpaces - stats.value.occupiedSpaces, 0)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        data: [
          { value: stats.value.freeSpaces, name: '空闲', itemStyle: { color: '#22c55e' } },
          { value: stats.value.occupiedSpaces, name: '占用', itemStyle: { color: '#ef4444' } },
          { value: rest, name: '预约/维护', itemStyle: { color: '#94a3b8' } },
        ],
      },
    ],
  })
}

function onResize() {
  chart?.resize()
}

onMounted(async () => {
  try {
    stats.value = await fetchStats()
  } finally {
    renderChart()
  }
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}
.stat-card {
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 12px 32px rgba(28, 45, 90, 0.09);
}
.stat-label {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}
.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}
.stat-value.free {
  color: #16a34a;
}
.stat-value.amount {
  color: #ff7a00;
}
.stat-extra {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}
.panel {
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 12px 32px rgba(28, 45, 90, 0.09);
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
}
.chart {
  height: 280px;
}
.quick {
  display: flex;
  gap: 10px;
}
.tips {
  margin-top: 14px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
