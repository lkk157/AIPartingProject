<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <view class="profile glass">
        <view class="avatar">{{ avatarText }}</view>
        <view class="who">
          <text class="name">{{ user?.nickname || user?.username || '未登录' }}</text>
          <text class="account">@{{ user?.username || '-' }}</text>
          <view class="role-row">
            <text v-for="role in user?.roles ?? []" :key="role" class="role">{{ roleLabel(role) }}</text>
          </view>
        </view>
      </view>

      <view class="glass block">
        <view class="block-head">
          <text class="block-title">我的车辆</text>
          <text class="block-action" @click="savePlate">保存</text>
        </view>
        <input v-model="plateNo" class="plate-input" placeholder="输入车牌号，如 粤B12345" maxlength="10" />
        <text class="block-tip">入场时默认使用该车牌，可在车位详情页临时修改</text>
      </view>

      <view class="glass menu">
        <view class="menu-item" @click="go('/pages/parking/current')">
          <text class="mi-text">停车中</text>
          <text class="mi-arrow">›</text>
        </view>
        <view class="menu-item" @click="go('/pages/order/list?tab=0')">
          <text class="mi-text">我的预约</text>
          <text class="mi-arrow">›</text>
        </view>
        <view class="menu-item" @click="go('/pages/order/list?tab=1')">
          <text class="mi-text">我的订单</text>
          <text class="mi-arrow">›</text>
        </view>
        <view class="menu-item" @click="about">
          <text class="mi-text">关于智泊停车</text>
          <text class="mi-arrow">›</text>
        </view>
      </view>

      <view class="logout" @click="logout">退出登录</view>
      <view class="version">v0.5.1 · 玻璃拟态主题</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { clearAuth, getStoredUser, getToken } from '@/utils/request'
import type { UserInfo } from '@/utils/request'

const PLATE_KEY = 'parking_plate'

const user = ref<UserInfo | null>(null)
const plateNo = ref('')

const avatarText = computed(() => {
  const name = user.value?.nickname || user.value?.username || '车'
  return name.slice(0, 1).toUpperCase()
})

const roleLabels: Record<string, string> = {
  USER: '车主',
  PARKING_ADMIN: '停车场管理员',
  SYS_ADMIN: '系统管理员',
}

function roleLabel(role: string): string {
  return roleLabels[role] ?? role
}

function go(url: string) {
  uni.navigateTo({ url })
}

function savePlate() {
  uni.setStorageSync(PLATE_KEY, plateNo.value.trim())
  uni.showToast({ title: '已保存', icon: 'success' })
}

function about() {
  uni.showModal({
    title: '智泊停车',
    content: '智能停车场管理系统\nSpring Boot + Vue3 + uni-app\n版本 v0.5.1',
    showCancel: false,
  })
}

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号？',
    success: (res) => {
      if (!res.confirm) return
      clearAuth()
      uni.reLaunch({ url: '/pages/index/index' })
    },
  })
}

onShow(() => {
  if (!getToken()) {
    uni.redirectTo({ url: '/pages/user/login' })
    return
  }
  user.value = getStoredUser()
  plateNo.value = (uni.getStorageSync(PLATE_KEY) as string) || ''
})
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  padding: 32rpx;
}
.profile {
  margin-top: 16rpx;
  padding: 34rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 26rpx;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 36rpx;
  background: $brand-gradient;
  color: #fff;
  font-size: 46rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16rpx 36rpx rgba(79, 108, 255, 0.32);
}
.who {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.name {
  font-size: 34rpx;
  font-weight: 700;
  color: $text-primary;
}
.account {
  font-size: 24rpx;
  color: $text-tertiary;
}
.role-row {
  margin-top: 8rpx;
  display: flex;
  gap: 10rpx;
}
.role {
  font-size: 20rpx;
  padding: 4rpx 16rpx;
  border-radius: $radius-pill;
  background: rgba(47, 107, 255, 0.1);
  color: $brand-primary;
}

.block {
  margin-top: 22rpx;
  padding: 28rpx 30rpx;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.block-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.block-action {
  font-size: 24rpx;
  color: $brand-primary;
}
.plate-input {
  margin-top: 18rpx;
  height: 84rpx;
  padding: 0 24rpx;
  border-radius: 20rpx;
  background: rgba(47, 107, 255, 0.06);
  font-size: 28rpx;
  color: $text-primary;
}
.block-tip {
  display: block;
  margin-top: 14rpx;
  font-size: 20rpx;
  color: $text-tertiary;
}

.menu {
  margin-top: 22rpx;
  padding: 6rpx 30rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $divider;
}
.menu-item:last-child {
  border-bottom: none;
}
.mi-text {
  font-size: 28rpx;
  color: $text-primary;
}
.mi-arrow {
  font-size: 34rpx;
  color: $text-tertiary;
}

.logout {
  margin-top: 40rpx;
  text-align: center;
  padding: 26rpx 0;
  border-radius: $radius-pill;
  background: rgba(255, 255, 255, 0.7);
  border: 1rpx solid rgba(240, 68, 56, 0.24);
  color: $danger-red;
  font-size: 28rpx;
  font-weight: 600;
}
.version {
  margin-top: 24rpx;
  text-align: center;
  font-size: 20rpx;
  color: $text-tertiary;
}
</style>
