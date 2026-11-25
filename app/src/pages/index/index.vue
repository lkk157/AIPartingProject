<template>
  <view class="page">
    <!-- 渐变头部 -->
    <view class="hero">
      <view class="hero-title">智泊停车</view>
      <view class="hero-sub">快速找位 · 预约停车 · 无感支付</view>
      <view class="hero-stats">
        <view class="stat">
          <view class="stat-value">128</view>
          <view class="stat-label">空闲车位</view>
        </view>
        <view class="stat-divider" />
        <view class="stat">
          <view class="stat-value">36</view>
          <view class="stat-label">今日预约</view>
        </view>
        <view class="stat-divider" />
        <view class="stat">
          <view class="stat-value">24</view>
          <view class="stat-label">实时缴费</view>
        </view>
      </view>
    </view>

    <!-- 功能宫格 -->
    <view class="card grid">
      <view v-for="item in entries" :key="item.label" class="grid-item">
        <view class="grid-icon" :style="{ background: item.bg, color: item.color }">
          {{ item.icon }}
        </view>
        <text class="grid-label">{{ item.label }}</text>
      </view>
    </view>

    <!-- 附近停车场 -->
    <view class="section-title">附近停车场</view>
    <view v-for="lot in lots" :key="lot.id" class="card lot">
      <view class="lot-icon">P</view>
      <view class="lot-main">
        <view class="lot-name">{{ lot.name }}</view>
        <view class="lot-meta">
          剩余 <text class="lot-free">{{ lot.free }}</text> 位 · {{ lot.address }}
        </view>
      </view>
      <view class="lot-right">
        <text class="lot-distance">{{ lot.distance }}</text>
        <text class="lot-arrow">›</text>
      </view>
    </view>

    <view class="footer">P0 骨架 · 数据将在 P4 接入真实接口</view>
  </view>
</template>

<script setup lang="ts">
import { formatDistance } from '@/utils/format'

const entries = [
  { label: '预约车位', icon: '预', bg: '#E8EFFD', color: '#2563EB' },
  { label: '导航入场', icon: '导', bg: '#E7F7EE', color: '#16A34A' },
  { label: '停车记录', icon: '记', bg: '#E8EFFD', color: '#2563EB' },
  { label: '优惠券', icon: '券', bg: '#FFF2E5', color: '#FF7A00' },
]

const lots = [
  { id: 1, name: '科技园停车场', free: 42, address: 'B2 层 · 西区', distance: formatDistance(800) },
  { id: 2, name: '市民中心停车场', free: 18, address: 'B1 层 · 东区', distance: formatDistance(1250) },
  { id: 3, name: '万象城地下停车场', free: 6, address: 'B3 层 · 北区', distance: formatDistance(2100) },
]
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
  padding: 96rpx 32rpx 80rpx;
  border-radius: 0 0 40rpx 40rpx;
  color: #fff;
}
.hero-title {
  font-size: 60rpx;
  font-weight: 700;
}
.hero-sub {
  margin-top: 8rpx;
  font-size: 26rpx;
  opacity: 0.8;
}
.hero-stats {
  margin-top: 40rpx;
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
  font-size: 48rpx;
  font-weight: 700;
}
.stat-label {
  margin-top: 4rpx;
  font-size: 24rpx;
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

.grid {
  display: flex;
  margin-top: -40rpx;
}
.grid-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.grid-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 600;
}
.grid-label {
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
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.lot-free {
  color: $brand-primary;
  font-weight: 600;
}
.lot-right {
  display: flex;
  align-items: center;
}
.lot-distance {
  font-size: 24rpx;
  color: $text-tertiary;
}
.lot-arrow {
  margin-left: 8rpx;
  color: $text-tertiary;
  font-size: 32rpx;
}

.footer {
  margin-top: 40rpx;
  text-align: center;
  font-size: 22rpx;
  color: $text-tertiary;
}
</style>
