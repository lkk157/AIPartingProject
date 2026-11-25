<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <view class="brand">
        <view class="brand-orb">P</view>
        <text class="brand-name">智泊停车</text>
        <text class="brand-slogan">找位 · 预约 · 无感支付</text>
      </view>

      <view class="card glass">
        <view class="tabs">
          <view class="tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</view>
          <view class="tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</view>
        </view>

        <view class="field">
          <text class="label">用户名</text>
          <input v-model="username" class="input" placeholder="4-20 位字母/数字/下划线" />
        </view>
        <view class="field">
          <text class="label">密码</text>
          <input v-model="password" class="input" password placeholder="6-32 位" />
        </view>

        <view class="submit" :class="{ loading }" @click="submit">
          {{ loading ? '处理中…' : mode === 'login' ? '登录' : '注册并登录' }}
        </view>

        <view class="switch" @click="mode = mode === 'login' ? 'register' : 'login'">
          {{ mode === 'login' ? '没有账号？立即注册' : '已有账号？返回登录' }}
        </view>
      </view>

      <view class="hint">演示账号 admin / admin123 · 或注册普通车主账号</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login, register } from '@/api'

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    if (mode.value === 'register') {
      await register(username.value, password.value)
    }
    await login(username.value, password.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  padding: 120rpx 40rpx 60rpx;
}
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}
.brand-orb {
  width: 132rpx;
  height: 132rpx;
  border-radius: 40rpx;
  background: $brand-gradient;
  color: #fff;
  font-size: 60rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20rpx 48rpx rgba(79, 108, 255, 0.38);
}
.brand-name {
  margin-top: 16rpx;
  font-size: 42rpx;
  font-weight: 700;
  color: $text-primary;
}
.brand-slogan {
  font-size: 24rpx;
  color: $text-secondary;
}

.card {
  margin-top: 56rpx;
  padding: 34rpx 34rpx 30rpx;
}
.tabs {
  display: flex;
  background: rgba(47, 107, 255, 0.07);
  border-radius: $radius-pill;
  padding: 6rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: $radius-pill;
  font-size: 28rpx;
  color: $text-secondary;
}
.tab.active {
  background: #fff;
  color: $brand-primary;
  font-weight: 600;
  box-shadow: 0 8rpx 20rpx rgba(28, 45, 90, 0.08);
}
.field {
  margin-top: 30rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid $divider;
}
.label {
  font-size: 22rpx;
  color: $text-tertiary;
}
.input {
  margin-top: 12rpx;
  font-size: 30rpx;
  color: $text-primary;
}
.submit {
  margin-top: 44rpx;
  text-align: center;
  padding: 26rpx 0;
  border-radius: $radius-pill;
  background: $brand-gradient;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 16rpx 36rpx rgba(79, 108, 255, 0.32);
}
.submit.loading {
  opacity: 0.7;
}
.switch {
  margin-top: 26rpx;
  text-align: center;
  font-size: 24rpx;
  color: $brand-primary;
}
.hint {
  margin-top: 48rpx;
  text-align: center;
  font-size: 22rpx;
  color: $text-tertiary;
}
</style>
