<template>
  <div class="page">
    <div class="toolbar glass-panel">
      <el-select v-model="lotId" placeholder="选择场景停车场" style="width: 240px" @change="run">
        <el-option
          v-for="lot in lots"
          :key="lot.id"
          :label="`${lot.name}（${lot.capacity} 位）`"
          :value="lot.id"
        />
      </el-select>
      <el-select v-model="hours" style="width: 168px" @change="run">
        <el-option label="未来 12 小时" :value="12" />
        <el-option label="未来 24 小时" :value="24" />
        <el-option label="未来 48 小时" :value="48" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="run">生成预测</el-button>
      <div class="spacer" />
      <span class="hint">模型：{{ modelInfo?.model?.toUpperCase() || '-' }}</span>
    </div>

    <div class="scope-tip">
      预测基于模型内置场景数据集（办公 / 商业 / 住宅三类典型停车场）；接入真实停车记录后自动切换为实际停车场。
    </div>

    <div v-if="error" class="glass-panel error-card">
      <div class="error-title">AI 服务不可用</div>
      <div class="error-desc">{{ error }}</div>
      <div class="error-tip">请先启动 AI 服务：<code>cd ai && .venv/Scripts/python -m uvicorn app.main:app --port 8000</code></div>
      <el-button size="small" @click="run">重试</el-button>
    </div>

    <template v-else>
      <div class="metrics">
        <div class="metric glass-panel">
          <div class="m-label">最佳模型</div>
          <div class="m-value">{{ modelInfo?.model?.toUpperCase() || '-' }}</div>
          <div class="m-extra">训练样本 {{ modelInfo?.samples ?? '-' }}</div>
        </div>
        <div class="metric glass-panel">
          <div class="m-label">MAE</div>
          <div class="m-value">{{ formatRate(activeMetric?.mae) }}</div>
          <div class="m-extra">平均绝对误差</div>
        </div>
        <div class="metric glass-panel">
          <div class="m-label">RMSE</div>
          <div class="m-value">{{ formatRate(activeMetric?.rmse) }}</div>
          <div class="m-extra">均方根误差</div>
        </div>
        <div class="metric glass-panel">
          <div class="m-label">R²</div>
          <div class="m-value">{{ activeMetric?.r2?.toFixed(4) ?? '-' }}</div>
          <div class="m-extra">基线 MAE {{ formatRate(modelInfo?.baselineMae ?? undefined) }}</div>
        </div>
      </div>

      <div class="glass-panel chart-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">占用率预测</div>
            <div class="panel-sub">
              {{ result ? `${result.lotName} · 容量 ${result.capacity} 位` : '选择停车场后生成预测' }}
            </div>
          </div>
          <div class="legend">
            <span class="dot high" /> ≥80% 紧张
            <span class="dot mid" /> 50-80%
            <span class="dot low" /> &lt;50%
          </div>
        </div>
        <div ref="chartRef" class="chart" />
      </div>

      <div v-if="tableRows.length" class="glass-panel table-panel">
        <div class="panel-title">逐小时明细</div>
        <el-table :data="tableRows" size="small" max-height="280">
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="rate" label="预测占用率" width="130" />
          <el-table-column prop="occupied" label="预计占用车位" width="130" />
          <el-table-column prop="free" label="预计空闲车位" />
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { fetchModelInfo, predictOccupancy, type ModelInfo, type PredictResult } from '@/api/ai'

const lots = ref<{ id: number; name: string; capacity: number }[]>([])
const lotId = ref<number>()
const hours = ref(24)
const loading = ref(false)
const error = ref('')
const result = ref<PredictResult | null>(null)
const modelInfo = ref<ModelInfo | null>(null)

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

const activeMetric = computed(() => {
  const model = modelInfo.value?.model
  if (!model || !modelInfo.value) return undefined
  return modelInfo.value.metrics[model]
})

const tableRows = computed(() =>
  (result.value?.points ?? []).map((point) => ({
    time: point.time.replace('T', ' ').slice(0, 16),
    rate: `${(point.predictedRate * 100).toFixed(1)}%`,
    occupied: point.predictedOccupied,
    free: point.freeSpaces,
  })),
)

function formatRate(value?: number | null): string {
  if (value === undefined || value === null) return '-'
  return `${(value * 100).toFixed(2)}%`
}

async function loadLots() {
  const info = await fetchModelInfo()
  lots.value = (info.lots ?? []).map((lot) => ({ id: lot.lotId, name: lot.name, capacity: lot.capacity }))
  if (!lotId.value && lots.value.length) {
    lotId.value = lots.value[0].id
  }
}

async function run() {
  if (!lotId.value) return
  loading.value = true
  error.value = ''
  try {
    modelInfo.value = await fetchModelInfo()
    result.value = await predictOccupancy(lotId.value, hours.value)
    renderChart()
  } catch (err) {
    result.value = null
    error.value = err instanceof Error ? err.message : '请求失败'
    chart?.clear()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || !result.value) return
  chart = chart ?? echarts.init(chartRef.value)
  const points = result.value.points
  chart.setOption({
    grid: { left: 48, right: 24, top: 24, bottom: 40 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0]
        const index = item.dataIndex
        const point = points[index]
        return `${point.time.replace('T', ' ').slice(5, 16)}<br/>占用率 ${(point.predictedRate * 100).toFixed(1)}%<br/>空闲 ${point.freeSpaces} 位`
      },
    },
    xAxis: {
      type: 'category',
      data: points.map((point) => point.time.replace('T', ' ').slice(5, 16)),
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#64748b' },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: points.map((point) => Number((point.predictedRate * 100).toFixed(1))),
        lineStyle: { width: 3, color: '#4b7dff' },
        itemStyle: { color: '#4b7dff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(75,125,255,0.35)' },
            { offset: 1, color: 'rgba(75,125,255,0.02)' },
          ]),
        },
        markLine: {
          silent: true,
          symbol: 'none',
          label: { formatter: '80% 预警线', color: '#f04438', fontSize: 11 },
          lineStyle: { color: 'rgba(240,68,56,0.6)', type: 'dashed' },
          data: [{ yAxis: 80 }],
        },
      },
    ],
  })
}

function onResize() {
  chart?.resize()
}

onMounted(async () => {
  await loadLots()
  await run()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
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
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 16px;
}
.spacer {
  flex: 1;
}
.scope-tip {
  font-size: 12px;
  color: #8290a8;
  padding: 0 4px;
  line-height: 1.6;
}
.hint {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}
.metric {
  padding: 18px 20px;
}
.m-label {
  color: #64748b;
  font-size: 13px;
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
.chart-panel {
  padding: 18px 20px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
.legend {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  margin-left: 6px;
}
.dot.high { background: #f04438; }
.dot.mid { background: #f59e0b; }
.dot.low { background: #12b76a; }
.chart {
  height: 340px;
  margin-top: 12px;
}
.table-panel {
  padding: 18px 20px;
}
.error-card {
  padding: 32px;
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
  color: #64748b;
  font-size: 13px;
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
