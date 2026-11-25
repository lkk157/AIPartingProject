<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">智泊停车</div>
      <el-menu :default-active="'1'" class="menu">
        <el-menu-item index="1">运营看板</el-menu-item>
        <el-menu-item index="2">停车场管理</el-menu-item>
        <el-menu-item index="3">车位管理</el-menu-item>
        <el-menu-item index="4">预约与订单</el-menu-item>
        <el-menu-item index="5">知识库管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="title">运营看板</span>
        <span class="subtitle">P0 骨架 · 数据接入见后续阶段</span>
      </el-header>
      <el-main>
        <el-row :gutter="16">
          <el-col v-for="card in cards" :key="card.label" :span="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="panel">
          <template #header>实时占用率（占位）</template>
          <div ref="chartRef" class="chart"></div>
        </el-card>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { formatMoney, formatPercent } from '@/utils/format'

const cards = [
  { label: '停车场总数', value: '12' },
  { label: '空闲车位', value: '128' },
  { label: '今日订单', value: '36' },
  { label: '今日收入', value: formatMoney(21600) },
]

const chartRef = ref<HTMLDivElement>()
const occupancy = [0.42, 0.56, 0.71, 0.83, 0.64, 0.48]

onMounted(() => {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00'] },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => formatPercent(v) }, max: 1 },
    series: [
      {
        type: 'line',
        smooth: true,
        data: occupancy,
        areaStyle: { opacity: 0.12 },
        itemStyle: { color: '#2563EB' },
      },
    ],
  })
})
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid var(--divider);
}
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  font-size: 17px;
  color: var(--brand-primary);
}
.menu {
  border-right: none;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--divider);
}
.title {
  font-weight: 600;
}
.subtitle {
  color: var(--text-tertiary);
  font-size: 12px;
}
.stat-card {
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}
.stat-label {
  color: var(--text-secondary);
  font-size: 13px;
}
.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
}
.panel {
  margin-top: 16px;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}
.chart {
  height: 280px;
}
</style>
