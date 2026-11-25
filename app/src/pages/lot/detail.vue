<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <!-- 头部信息 -->
      <view class="hero glass">
        <view class="hero-top">
          <view class="hero-main">
            <text class="hero-name">{{ detail?.lot.name || '停车场' }}</text>
            <text class="hero-addr">{{ detail?.lot.area }} · {{ detail?.lot.address }}</text>
          </view>
          <view class="hero-badge">
            <text class="hb-num">{{ detail?.freeCount ?? 0 }}</text>
            <text class="hb-label">空闲</text>
          </view>
        </view>
        <view class="hero-metrics">
          <view class="metric">
            <text class="m-value">{{ detail?.spaceCount ?? 0 }}</text>
            <text class="m-label">总车位</text>
          </view>
          <view class="m-divider" />
          <view class="metric">
            <text class="m-value">{{ detail?.occupiedCount ?? 0 }}</text>
            <text class="m-label">已占用</text>
          </view>
          <view class="m-divider" />
          <view class="metric">
            <text class="m-value">{{ detail?.reservedCount ?? 0 }}</text>
            <text class="m-label">预约锁定</text>
          </view>
        </view>
      </view>

      <!-- 价格卡 -->
      <view v-if="detail?.enabledRule" class="price glass">
        <view class="price-head">
          <text class="price-title">收费标准</text>
          <text class="price-rule">{{ detail.enabledRule.ruleName }}</text>
        </view>
        <view class="price-grid">
          <view class="p-item">
            <text class="p-value">{{ detail.enabledRule.freeMinutes }}<text class="p-unit">分</text></text>
            <text class="p-label">免费时长</text>
          </view>
          <view class="p-item">
            <text class="p-value">{{ formatMoney(detail.enabledRule.firstHourCents) }}</text>
            <text class="p-label">首小时</text>
          </view>
          <view class="p-item">
            <text class="p-value">{{ formatMoney(detail.enabledRule.extraPerHourCents) }}</text>
            <text class="p-label">每小时</text>
          </view>
          <view class="p-item">
            <text class="p-value">{{ formatMoney(detail.enabledRule.dailyCapCents) }}</text>
            <text class="p-label">24h 封顶</text>
          </view>
        </view>
      </view>

      <!-- 车位 -->
      <view class="section-head">
        <text class="section-title">选择车位</text>
        <text class="section-tip">点击空闲车位</text>
      </view>

      <scroll-view class="chips" scroll-x :show-scrollbar="false">
        <view class="chip" :class="{ active: filter === 'free' }" @click="setFilter('free')">空闲</view>
        <view class="chip" :class="{ active: filter === 'all' }" @click="setFilter('all')">全部</view>
        <view class="chip" :class="{ active: filter === 'charging' }" @click="setFilter('charging')">充电桩</view>
      </scroll-view>

      <view class="grid">
        <view
          v-for="space in visibleSpaces"
          :key="space.id"
          class="space"
          :class="{ active: selected?.id === space.id, disabled: space.status !== 0 }"
          @click="selectSpace(space)"
        >
          <view class="space-dot" :style="{ background: spaceColor(space.status) }" />
          <text class="space-no">{{ shortNo(space.spaceNo) }}</text>
          <text class="space-type">{{ space.type === 'CHARGING' ? '充电' : space.statusText }}</text>
        </view>
      </view>

      <view v-if="visibleSpaces.length === 0" class="glass state">
        <text class="state-title">该筛选下暂无车位</text>
      </view>

      <view class="legend">
        <view class="lg-item"><view class="lg-dot" style="background:#22c55e" />空闲</view>
        <view class="lg-item"><view class="lg-dot" style="background:#f04438" />占用</view>
        <view class="lg-item"><view class="lg-dot" style="background:#2f6bff" />预约</view>
        <view class="lg-item"><view class="lg-dot" style="background:#94a3b8" />维护</view>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="bar-info" @click="sheetVisible = true">
        <text class="bar-space">{{ selected ? shortNo(selected.spaceNo) : '未选车位' }}</text>
        <text class="bar-time">{{ hours }} 小时 · {{ slotLabels[slotIndex] }}</text>
      </view>
      <view class="bar-actions">
        <view class="btn ghost" :class="{ disabled: !selected }" @click="submitEntry">立即停车</view>
        <view class="btn primary" :class="{ disabled: !selected }" @click="submitReserve">预约</view>
      </view>
    </view>

    <!-- 时间/时长选择 -->
    <view v-if="sheetVisible" class="mask" @click="sheetVisible = false">
      <view class="sheet" @click.stop>
        <view class="sheet-head">
          <text class="sheet-title">预约时间</text>
          <text class="sheet-close" @click="sheetVisible = false">完成</text>
        </view>

        <text class="sheet-label">停车时长</text>
        <view class="hour-row">
          <view
            v-for="item in hourOptions"
            :key="item.value"
            class="hour"
            :class="{ active: hours === item.value }"
            @click="setHours(item.value)"
          >{{ item.label }}</view>
        </view>

        <text class="sheet-label">开始时间</text>
        <scroll-view class="slot-list" scroll-y>
          <view
            v-for="(slot, index) in slots"
            :key="slot.iso"
            class="slot"
            :class="{ active: slotIndex === index }"
            @click="setSlot(index)"
          >
            <text class="slot-label">{{ slot.label }}</text>
            <text class="slot-hint">{{ index === 0 ? '最近可约' : '' }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { createReservation, entryParking, getLotDetail, listSpaces, type LotDetail, type Space } from '@/api'
import { formatMoney } from '@/utils/format'
import { spaceColor } from '@/utils/status'

type Filter = 'free' | 'all' | 'charging'

const lotId = ref(0)
const detail = ref<LotDetail | null>(null)
const spaces = ref<Space[]>([])
const selected = ref<Space | null>(null)
const filter = ref<Filter>('free')
const hours = ref(2)
const slotIndex = ref(0)
const sheetVisible = ref(false)
const plateNo = ref('粤B12345')

const hourOptions = [
  { label: '1h', value: 1 },
  { label: '2h', value: 2 },
  { label: '3h', value: 3 },
  { label: '4h', value: 4 },
]

const slots = computed(() => {
  const base = new Date()
  base.setMinutes(0, 0, 0)
  base.setHours(base.getHours() + 1)
  return Array.from({ length: 12 }, (_, index) => {
    const time = new Date(base.getTime() + index * 3600 * 1000)
    return {
      iso: toIso(time),
      label: `${pad(time.getMonth() + 1)}-${pad(time.getDate())} ${pad(time.getHours())}:00`,
    }
  })
})
const slotLabels = computed(() => slots.value.map((slot) => slot.label))

const visibleSpaces = computed(() => {
  if (filter.value === 'all') return spaces.value
  if (filter.value === 'charging') return spaces.value.filter((space) => space.type === 'CHARGING')
  return spaces.value.filter((space) => space.status === 0)
})

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function toIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:00:00`
}

function shortNo(spaceNo: string): string {
  const parts = spaceNo.split('-')
  return parts.length > 1 ? parts[parts.length - 1] : spaceNo
}

async function load() {
  if (!lotId.value) return
  detail.value = await getLotDetail(lotId.value)
  spaces.value = await listSpaces(lotId.value)
}

function setFilter(value: Filter) {
  filter.value = value
}

function setHours(value: number) {
  hours.value = value
}

function setSlot(index: number) {
  slotIndex.value = index
}

function selectSpace(space: Space) {
  if (space.status !== 0) {
    uni.showToast({ title: `该车位${space.statusText}`, icon: 'none' })
    return
  }
  selected.value = space
}

async function submitReserve() {
  if (!selected.value) {
    uni.showToast({ title: '请先选择车位', icon: 'none' })
    return
  }
  try {
    await createReservation({
      lotId: lotId.value,
      spaceId: selected.value.id,
      startTime: slots.value[slotIndex.value].iso,
      hours: hours.value,
    })
    uni.showToast({ title: '预约成功', icon: 'success' })
    selected.value = null
    await load()
  } catch {
    // 错误提示由请求层统一处理
  }
}

async function submitEntry() {
  if (!selected.value) {
    uni.showToast({ title: '请先选择车位', icon: 'none' })
    return
  }
  try {
    await entryParking({ lotId: lotId.value, spaceId: selected.value.id, plateNo: plateNo.value })
    uni.showToast({ title: '入场成功', icon: 'success' })
    setTimeout(() => uni.navigateTo({ url: '/pages/parking/current' }), 400)
  } catch {
    // 错误提示由请求层统一处理
  }
}

onLoad((query) => {
  lotId.value = Number(query?.id ?? 0)
})

onShow(() => {
  void load()
})
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 240rpx;
}
.hero {
  padding: 32rpx;
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.hero-name {
  font-size: 36rpx;
  font-weight: 700;
  color: $text-primary;
}
.hero-addr {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.hero-badge {
  width: 120rpx;
  padding: 14rpx 0;
  border-radius: 26rpx;
  background: $brand-gradient;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
  box-shadow: 0 12rpx 28rpx rgba(79, 108, 255, 0.3);
}
.hb-num {
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1;
}
.hb-label {
  margin-top: 6rpx;
  font-size: 20rpx;
  opacity: 0.85;
}
.hero-metrics {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  padding-top: 22rpx;
  border-top: 1rpx solid $divider;
}
.metric {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.m-value {
  font-size: 30rpx;
  font-weight: 700;
  color: $text-primary;
}
.m-label {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: $text-tertiary;
}
.m-divider {
  width: 1rpx;
  height: 44rpx;
  background: $divider;
}

/* 价格 */
.price {
  margin-top: 20rpx;
  padding: 26rpx 30rpx;
}
.price-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.price-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.price-rule {
  font-size: 22rpx;
  color: $text-tertiary;
}
.price-grid {
  margin-top: 20rpx;
  display: flex;
}
.p-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.p-value {
  font-size: 28rpx;
  font-weight: 700;
  color: $amount-orange;
}
.p-unit {
  font-size: 20rpx;
  font-weight: 400;
}
.p-label {
  margin-top: 6rpx;
  font-size: 20rpx;
  color: $text-tertiary;
}

/* 车位 */
.section-head {
  margin-top: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $text-primary;
}
.section-tip {
  font-size: 22rpx;
  color: $text-tertiary;
}
.chips {
  margin-top: 18rpx;
  white-space: nowrap;
}
.chip {
  display: inline-block;
  margin-right: 14rpx;
  @include pill-chip(false);
}
.chip.active {
  @include pill-chip(true);
}
.grid {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}
.space {
  padding: 18rpx 8rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.66);
  border: 1rpx solid rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  box-shadow: 0 8rpx 20rpx rgba(28, 45, 90, 0.06);
}
.space.active {
  border-color: $brand-primary;
  background: rgba(47, 107, 255, 0.12);
  box-shadow: 0 0 0 4rpx rgba(47, 107, 255, 0.18);
}
.space.disabled {
  opacity: 0.5;
}
.space-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.space-no {
  font-size: 24rpx;
  font-weight: 600;
  color: $text-primary;
}
.space-type {
  font-size: 18rpx;
  color: $text-tertiary;
}
.legend {
  margin-top: 24rpx;
  display: flex;
  gap: 24rpx;
  justify-content: center;
}
.lg-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 20rpx;
  color: $text-tertiary;
}
.lg-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.state {
  margin-top: 20rpx;
  padding: 40rpx;
  text-align: center;
}
.state-title {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 底部栏 */
.bottom-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  z-index: 20;
  padding: 20rpx 24rpx;
  @include glass-strong(34rpx);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bar-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.bar-space {
  font-size: 28rpx;
  font-weight: 700;
  color: $text-primary;
}
.bar-time {
  font-size: 22rpx;
  color: $text-secondary;
}
.bar-actions {
  display: flex;
  gap: 14rpx;
}
.btn {
  padding: 18rpx 30rpx;
  border-radius: $radius-pill;
  font-size: 26rpx;
  font-weight: 600;
}
.btn.primary {
  background: $brand-gradient;
  color: #fff;
  box-shadow: 0 12rpx 28rpx rgba(79, 108, 255, 0.3);
}
.btn.ghost {
  background: rgba(47, 107, 255, 0.1);
  color: $brand-primary;
}
.btn.disabled {
  opacity: 0.45;
}

/* 时间选择弹层 */
.mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  background: rgba(16, 22, 43, 0.28);
  display: flex;
  align-items: flex-end;
}
.sheet {
  width: 100%;
  padding: 32rpx 32rpx 48rpx;
  border-radius: 40rpx 40rpx 0 0;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(20px);
}
.sheet-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sheet-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $text-primary;
}
.sheet-close {
  font-size: 26rpx;
  color: $brand-primary;
}
.sheet-label {
  display: block;
  margin: 28rpx 0 14rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.hour-row {
  display: flex;
  gap: 14rpx;
}
.hour {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 18rpx;
  background: #f2f5fb;
  font-size: 26rpx;
  color: $text-secondary;
}
.hour.active {
  background: $brand-gradient;
  color: #fff;
  font-weight: 600;
}
.slot-list {
  max-height: 320rpx;
}
.slot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  margin-bottom: 12rpx;
  border-radius: 18rpx;
  background: #f6f8fd;
}
.slot.active {
  background: rgba(47, 107, 255, 0.12);
  border: 1rpx solid rgba(47, 107, 255, 0.4);
}
.slot-label {
  font-size: 26rpx;
  color: $text-primary;
}
.slot-hint {
  font-size: 20rpx;
  color: $brand-primary;
}
</style>
