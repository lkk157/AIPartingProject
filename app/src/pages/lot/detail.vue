<template>
  <view class="page">
    <view v-if="detail" class="card summary">
      <view class="summary-top">
        <view>
          <view class="lot-name">{{ detail.lot.name }}</view>
          <view class="lot-meta">{{ detail.lot.area }} · {{ detail.lot.address }}</view>
        </view>
        <view class="free-badge">空闲 {{ detail.freeCount }}</view>
      </view>
      <view class="summary-stats">总车位 {{ detail.spaceCount }} · 计费：免费 15 分钟，首小时 6 元</view>
    </view>

    <view class="section-title">选择车位</view>
    <view class="grid">
      <view
        v-for="space in spaces"
        :key="space.id"
        class="space"
        :class="{ active: selected?.id === space.id, disabled: space.status !== 0 }"
        :style="{ borderColor: spaceColor(space.status) }"
        @click="selectSpace(space)"
      >
        <view class="space-no">{{ space.spaceNo }}</view>
        <view class="space-status" :style="{ color: spaceColor(space.status) }">{{ space.statusText }}</view>
      </view>
    </view>

    <view class="card form">
      <view class="form-row">
        <text class="label">预约时长</text>
        <picker :range="hourOptions" @change="onHoursChange">
          <view class="picker">{{ hours }} 小时</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">开始时间</text>
        <picker :range="slotLabels" @change="onSlotChange">
          <view class="picker">{{ slotLabels[slotIndex] }}</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">车牌号</text>
        <input v-model="plateNo" class="input" placeholder="如：粤B12345" />
      </view>

      <view class="actions">
        <button class="btn ghost" :disabled="!selected" @click="submitEntry">立即入场</button>
        <button class="btn primary" :disabled="!selected" @click="submitReserve">预约车位</button>
      </view>
      <view class="tip">提示：预约需整点开始；入场可提前 60 分钟。</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createReservation, entryParking, getLotDetail, listSpaces, type Lot, type Space } from '@/api'
import { spaceColor } from '@/utils/status'

const lotId = ref(0)
const detail = ref<{ lot: Lot; freeCount: number; spaceCount: number } | null>(null)
const spaces = ref<Space[]>([])
const selected = ref<Space | null>(null)
const hours = ref(2)
const hourOptions = ['1 小时', '2 小时', '3 小时', '4 小时']
const plateNo = ref('粤B12345')

const slots = computed(() => {
  const now = new Date()
  const base = new Date(now.getTime())
  base.setMinutes(0, 0, 0)
  base.setHours(base.getHours() + 1)
  return Array.from({ length: 12 }, (_, index) => {
    const time = new Date(base.getTime() + index * 3600 * 1000)
    return { label: `${pad(time.getMonth() + 1)}-${pad(time.getDate())} ${pad(time.getHours())}:00`, iso: toIso(time) }
  })
})
const slotLabels = computed(() => slots.value.map((slot) => slot.label))
const slotIndex = ref(0)

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function toIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:00:00`
}

async function load() {
  if (!lotId.value) return
  detail.value = await getLotDetail(lotId.value)
  spaces.value = await listSpaces(lotId.value)
}

function selectSpace(space: Space) {
  if (space.status !== 0) {
    uni.showToast({ title: `该车位${space.statusText}`, icon: 'none' })
    return
  }
  selected.value = space
}

function onHoursChange(event: { detail: { value: number | string } }) {
  hours.value = Number(event.detail.value) + 1
}

function onSlotChange(event: { detail: { value: number | string } }) {
  slotIndex.value = Number(event.detail.value)
}

async function submitReserve() {
  if (!selected.value) return
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
  if (!selected.value) return
  try {
    await entryParking({
      lotId: lotId.value,
      spaceId: selected.value.id,
      plateNo: plateNo.value,
    })
    uni.showToast({ title: '入场成功', icon: 'success' })
    uni.navigateTo({ url: '/pages/order/list' })
  } catch {
    // 错误提示由请求层统一处理
  }
}

onLoad((query) => {
  lotId.value = Number(query?.id ?? 0)
  void load()
})
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  background: $page-bg;
  padding-bottom: 40rpx;
}
.card {
  background: $card-bg;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  margin: 24rpx 32rpx 0;
  padding: 32rpx;
}
.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.lot-name {
  font-size: 34rpx;
  font-weight: 700;
  color: $text-primary;
}
.lot-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.free-badge {
  background: #e7f7ee;
  color: $success-green;
  font-size: 24rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
}
.summary-stats {
  margin-top: 18rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.section-title {
  margin: 36rpx 32rpx 0;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin: 20rpx 32rpx 0;
}
.space {
  border: 2rpx solid $status-free;
  border-radius: 16rpx;
  background: #fff;
  padding: 16rpx 8rpx;
  text-align: center;
}
.space.active {
  background: #e8effd;
  box-shadow: 0 0 0 4rpx rgba(37, 99, 235, 0.25);
}
.space.disabled {
  opacity: 0.55;
}
.space-no {
  font-size: 24rpx;
  font-weight: 600;
  color: $text-primary;
}
.space-status {
  margin-top: 6rpx;
  font-size: 20rpx;
}
.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 0;
  border-bottom: 1rpx solid $divider;
}
.label {
  font-size: 26rpx;
  color: $text-secondary;
}
.picker {
  font-size: 26rpx;
  color: $text-primary;
  min-width: 220rpx;
  text-align: right;
}
.input {
  font-size: 26rpx;
  text-align: right;
  flex: 1;
  margin-left: 24rpx;
}
.actions {
  display: flex;
  gap: 20rpx;
  margin-top: 28rpx;
}
.btn {
  flex: 1;
  border-radius: 999rpx;
  font-size: 28rpx;
}
.btn.primary {
  background: $brand-primary;
  color: #fff;
}
.btn.ghost {
  background: #e8effd;
  color: $brand-primary;
}
.btn[disabled] {
  opacity: 0.5;
}
.tip {
  margin-top: 16rpx;
  font-size: 22rpx;
  color: $text-tertiary;
  text-align: center;
}
</style>
