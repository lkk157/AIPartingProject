<template>
  <view class="page">
    <view class="tabs">
      <view class="tab" :class="{ active: tab === 0 }" @click="tab = 0">我的预约</view>
      <view class="tab" :class="{ active: tab === 1 }" @click="tab = 1">我的订单</view>
    </view>

    <block v-if="tab === 0">
      <view v-if="reservations.length === 0" class="card empty">暂无预约记录</view>
      <view v-for="item in reservations" :key="item.id" class="card item">
        <view class="item-top">
          <view class="item-title">车位 {{ item.spaceNo || item.spaceId }}</view>
          <text class="status" :style="{ color: reservationStatusColor(item.status) }">{{ item.statusText }}</text>
        </view>
        <view class="item-meta">{{ formatRange(item.startTime, item.endTime) }}</view>
        <view class="item-actions">
          <button v-if="item.status === 0" class="mini ghost" size="mini" @click="onCancel(item.id)">取消预约</button>
        </view>
      </view>
    </block>

    <block v-else>
      <view v-if="orders.length === 0" class="card empty">暂无订单</view>
      <view v-for="order in orders" :key="order.id" class="card item">
        <view class="item-top">
          <view class="item-title">{{ order.orderNo }}</view>
          <text class="status" :style="{ color: orderStatusColor(order.status) }">{{ order.statusText }}</text>
        </view>
        <view class="item-meta">金额 {{ formatMoney(order.amountCents) }} · 计费 {{ formatDuration(order.billedMinutes) }}</view>
        <view class="item-actions">
          <button v-if="order.status === 0" class="mini primary" size="mini" @click="onPay(order.id)">立即支付</button>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
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

function formatRange(start: string, end: string): string {
  const format = (value: string, from: number, to: number) => value.replace('T', ' ').slice(from, to)
  return `${format(start, 5, 16)} ~ ${format(end, 11, 16)}`
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
  try {
    await cancelReservation(id)
    uni.showToast({ title: '已取消', icon: 'success' })
    await load()
  } catch {
    // 已统一提示
  }
}

async function onPay(id: number) {
  try {
    await payOrder(id)
    uni.showToast({ title: '支付成功', icon: 'success' })
    await load()
  } catch {
    // 已统一提示
  }
}

onShow(load)
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  background: $page-bg;
  padding-bottom: 40rpx;
}
.tabs {
  display: flex;
  background: #fff;
  padding: 12rpx 32rpx;
  gap: 20rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: $text-secondary;
  border-radius: 16rpx;
}
.tab.active {
  background: #e8effd;
  color: $brand-primary;
  font-weight: 600;
}
.card {
  background: $card-bg;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  margin: 24rpx 32rpx 0;
  padding: 28rpx 32rpx;
}
.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.item-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.status {
  font-size: 24rpx;
  font-weight: 600;
}
.item-meta {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.item-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
}
.mini {
  border-radius: 999rpx;
  font-size: 24rpx;
}
.mini.primary {
  background: $brand-primary;
  color: #fff;
}
.mini.ghost {
  background: #f1f5f9;
  color: $text-secondary;
}
.empty {
  text-align: center;
  color: $text-tertiary;
  font-size: 26rpx;
}
</style>
