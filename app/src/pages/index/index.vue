<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-title">智泊停车</view>
        <view class="hero-user" @click="onUserTap">{{ greeting }}</view>
      </view>
      <view class="hero-sub">快速找位 · 在线预约 · 无感支付</view>
      <view class="hero-stats">
        <view class="stat">
          <view class="stat-value">{{ lots.length }}</view>
          <view class="stat-label">在营停车场</view>
        </view>
        <view class="stat-divider" />
        <view class="stat">
          <view class="stat-value">{{ totalSpaces }}</view>
          <view class="stat-label">车位总数</view>
        </view>
        <view class="stat-divider" />
        <view class="stat">
          <view class="stat-value">{{ loggedIn ? '已登录' : '未登录' }}</view>
          <view class="stat-label">当前状态</view>
        </view>
      </view>
    </view>

    <view class="card shortcuts">
      <view class="shortcut" @click="goOrders">
        <view class="shortcut-icon blue">约</view>
        <text class="shortcut-label">我的预约</text>
      </view>
      <view class="shortcut" @click="goOrders">
        <view class="shortcut-icon orange">单</view>
        <text class="shortcut-label">我的订单</text>
      </view>
      <view class="shortcut" @click="goLogin">
        <view class="shortcut-icon green">登</view>
        <text class="shortcut-label">{{ loggedIn ? '切换账号' : '登录' }}</text>
      </view>
    </view>

    <view class="section-title">附近停车场</view>

    <view v-if="!loggedIn" class="card empty">
      <view class="empty-text">登录后查看附近停车场并预约车位</view>
      <button class="primary-btn" size="mini" @click="goLogin">去登录</button>
    </view>

    <view v-else-if="lots.length === 0" class="card empty">
      <view class="empty-text">暂无在营停车场，请稍后再试</view>
    </view>

    <view v-for="lot in lots" :key="lot.id" class="card lot" @click="openLot(lot.id)">
      <view class="lot-icon">P</view>
      <view class="lot-main">
        <view class="lot-name">{{ lot.name }}</view>
        <view class="lot-meta">{{ lot.area }} · {{ lot.address }}</view>
        <view class="lot-meta">车位 {{ lot.capacity }} 个</view>
      </view>
      <text class="lot-arrow">›</text>
    </view>

    <view class="footer">P4 已接入真实接口 · 后端 Spring Boot + MySQL</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listLots, type Lot } from '@/api'
import { clearAuth, getStoredUser, getToken } from '@/utils/request'

const lots = ref<Lot[]>([])
const loggedIn = ref(false)
const nickname = ref('')

const greeting = computed(() => (loggedIn.value ? nickname.value || '已登录' : '点击登录'))
const totalSpaces = computed(() => lots.value.reduce((sum, lot) => sum + (lot.capacity || 0), 0))

async function load() {
  loggedIn.value = Boolean(getToken())
  if (!loggedIn.value) {
    lots.value = []
    return
  }
  nickname.value = getStoredUser()?.nickname ?? ''
  try {
    const result = await listLots()
    lots.value = result.records
  } catch {
    lots.value = []
  }
}

function goLogin() {
  uni.navigateTo({ url: '/pages/user/login' })
}

function goOrders() {
  if (!getToken()) {
    goLogin()
    return
  }
  uni.navigateTo({ url: '/pages/order/list' })
}

function openLot(id: number) {
  if (!getToken()) {
    goLogin()
    return
  }
  uni.navigateTo({ url: `/pages/lot/detail?id=${id}` })
}

function onUserTap() {
  if (loggedIn.value) {
    uni.showModal({
      title: '账号',
      content: `当前账号：${nickname.value || '已登录'}，是否退出登录？`,
      success: (res) => {
        if (res.confirm) {
          clearAuth()
          void load()
        }
      },
    })
  } else {
    goLogin()
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
.hero {
  background: $brand-gradient;
  padding: 96rpx 32rpx 72rpx;
  border-radius: 0 0 40rpx 40rpx;
  color: #fff;
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.hero-title {
  font-size: 56rpx;
  font-weight: 700;
}
.hero-user {
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
}
.hero-sub {
  margin-top: 8rpx;
  font-size: 26rpx;
  opacity: 0.85;
}
.hero-stats {
  margin-top: 36rpx;
  background: rgba(255, 255, 255, 0.14);
  border-radius: 24rpx;
  padding: 24rpx 0;
  display: flex;
  align-items: center;
}
.stat {
  flex: 1;
  text-align: center;
}
.stat-value {
  font-size: 40rpx;
  font-weight: 700;
}
.stat-label {
  margin-top: 4rpx;
  font-size: 22rpx;
  opacity: 0.8;
}
.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: rgba(255, 255, 255, 0.3);
}
.card {
  background: $card-bg;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  margin: 24rpx 32rpx 0;
  padding: 32rpx;
}
.shortcuts {
  display: flex;
  margin-top: -40rpx;
}
.shortcut {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.shortcut-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 600;
}
.blue {
  background: #e8effd;
  color: $brand-primary;
}
.orange {
  background: #fff2e5;
  color: $amount-orange;
}
.green {
  background: #e7f7ee;
  color: $success-green;
}
.shortcut-label {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.section-title {
  margin: 40rpx 32rpx 0;
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
}
.lot {
  display: flex;
  align-items: center;
}
.lot-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: #e8effd;
  color: $brand-primary;
  font-size: 40rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lot-main {
  flex: 1;
  margin-left: 24rpx;
}
.lot-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.lot-meta {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.lot-arrow {
  color: $text-tertiary;
  font-size: 36rpx;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}
.empty-text {
  font-size: 26rpx;
  color: $text-secondary;
}
.primary-btn {
  background: $brand-primary;
  color: #fff;
  border-radius: 999rpx;
}
.footer {
  margin-top: 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $text-tertiary;
}
</style>
