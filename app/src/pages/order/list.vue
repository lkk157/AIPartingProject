<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <view class="segment glass">
        <view class="seg-item" :class="{ active: tab === 0 }" @click="tab = 0">
          预约 <text class="seg-count">{{ reservations.length }}</text>
        </view>
        <view class="seg-item" :class="{ active: tab === 1 }" @click="tab = 1">
          订单 <text class="seg-count">{{ orders.length }}</text>
        </view>
      </view>

      <block v-if="tab === 0">
        <view v-if="reservations.length === 0" class="glass state">
          <text class="state-title">还没有预约</text>
          <text class="state-desc">去首页找个车位，提前锁定位置</text>
          <view class="state-btn" @click="goHome">去找车位</view>
        </view>

        <view v-for="item in reservations" :key="item.id" class="card glass">
          <view class="card-head">
            <text class="card-title">{{ item.lotName || '停车场' }}</text>
            <text class="badge" :style="{ color: reservationStatusColor(item.status) }">{{ item.statusText }}</text>
          </view>
          <view class="card-sub">车位 {{ item.spaceNo || '-' }}</view>
          <view class="time-row">
            <view class="time-block">
              <text class="t-label">开始</text>
              <text class="t-value">{{ readable(item.startTime) }}</text>
            </view>
            <text class="t-arrow">→</text>
            <view class="time-block">
              <text class="t-label">结束</text>
              <text class="t-value">{{ timeOnly(item.endTime) }}</text>
            </view>
          </view>
          <view v-if="item.status === 0" class="card-actions">
            <view class="mini ghost" @click="onCancel(item.id)">取消预约</view>
          </view>
        </view>
      </block>

      <block v-else>
        <view v-if="orders.length === 0" class="glass state">
          <text class="state-title">还没有订单</text>
          <text class="state-desc">停车离场后会自动生成账单</text>
        </view>

        <view v-for="order in orders" :key="order.id" class="card glass">
          <view class="card-head">
            <text class="card-title">{{ order.lotName || '停车场' }}</text>
            <text class="badge" :style="{ color: orderStatusColor(order.status) }">{{ order.statusText }}</text>
          </view>
          <view class="card-sub">车位 {{ order.spaceNo || '-' }} · 停车 {{ formatDuration(order.billedMinutes) }}</view>
          <view class="amount-row">
            <text class="amount">{{ formatMoney(order.amountCents) }}</text>
            <text v-if="order.status === 0" class="amount-tip">待支付</text>
            <text v-else-if="order.status === 1" class="amount-tip paid">已完成</text>
          </view>
          <view class="card-actions">
            <view v-if="order.status === 0" class="mini primary" @click="onPay(order.id)">立即支付</view>
          </view>
          <view class="order-no">订单号 {{ order.orderNo }}</view>
        </view>
      </block>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  cancelReservation,
  listOrders,
  listReservations,
  payOrder,
  type Order,
  type Reservation,
} from '@/api'
import { formatDuration, formatMoney } from '@/utils/format'
import { orderStatusColor, reservationStatusColor } from '@/utils/status'

const tab = ref(0)
const reservations = ref<Reservation[]>([])
const orders = ref<Order[]>([])

function readable(iso: string): string {
  return iso.replace('T', ' ').slice(5, 16)
}

function timeOnly(iso: string): string {
  return iso.replace('T', ' ').slice(11, 16)
}

function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}

async function load() {
  try {
    reservations.value = await listReservations()
    const page = await listOrders()
    orders.value = page.records
  } catch {
    reservations.value = []
    orders.value = []
  }
}

async function onCancel(id: number) {
  uni.showModal({
    title: '取消预约',
    content: '确认取消该预约？车位会释放给其他车主。',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelReservation(id)
        uni.showToast({ title: '已取消', icon: 'success' })
        await load()
      } catch {
        // 统一提示
      }
    },
  })
}

async function onPay(id: number) {
  try {
    await payOrder(id)
    uni.showToast({ title: '支付成功', icon: 'success' })
    await load()
  } catch {
    // 统一提示
  }
}

onLoad((query) => {
  const initial = Number(query?.tab ?? 0)
  tab.value = initial === 1 ? 1 : 0
})

onShow(load)
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 60rpx;
}

/* 分段控件 */
.segment {
  display: flex;
  padding: 8rpx;
  border-radius: $radius-pill;
}
.seg-item {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  border-radius: $radius-pill;
  font-size: 28rpx;
  color: $text-secondary;
}
.seg-item.active {
  background: $brand-gradient;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 10rpx 24rpx rgba(79, 108, 255, 0.28);
}
.seg-count {
  font-size: 20rpx;
  margin-left: 6rpx;
  opacity: 0.8;
}

/* 卡片 */
.card {
  margin-top: 20rpx;
  padding: 28rpx 30rpx;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.badge {
  font-size: 24rpx;
  font-weight: 600;
}
.card-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.time-row {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(47, 107, 255, 0.06);
  border-radius: 20rpx;
  padding: 18rpx 24rpx;
}
.time-block {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.t-label {
  font-size: 20rpx;
  color: $text-tertiary;
}
.t-value {
  font-size: 26rpx;
  color: $text-primary;
  font-weight: 500;
}
.t-arrow {
  color: $brand-primary;
  font-size: 26rpx;
}
.amount-row {
  margin-top: 18rpx;
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.amount {
  font-size: 40rpx;
  font-weight: 700;
  color: $amount-orange;
}
.amount-tip {
  font-size: 22rpx;
  color: $warning-amber;
}
.amount-tip.paid {
  color: $success-green;
}
.card-actions {
  margin-top: 18rpx;
  display: flex;
  justify-content: flex-end;
  gap: 14rpx;
}
.mini {
  padding: 12rpx 30rpx;
  border-radius: $radius-pill;
  font-size: 24rpx;
  font-weight: 600;
}
.mini.primary {
  background: $brand-gradient;
  color: #fff;
}
.mini.ghost {
  background: rgba(255, 255, 255, 0.7);
  color: $text-secondary;
  border: 1rpx solid rgba(16, 22, 43, 0.08);
}
.order-no {
  margin-top: 16rpx;
  font-size: 20rpx;
  color: $text-tertiary;
}

/* 空态 */
.state {
  margin-top: 24rpx;
  padding: 70rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.state-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.state-desc {
  font-size: 24rpx;
  color: $text-secondary;
}
.state-btn {
  margin-top: 18rpx;
  padding: 16rpx 46rpx;
  border-radius: $radius-pill;
  background: $brand-gradient;
  color: #fff;
  font-size: 26rpx;
}
</style>
