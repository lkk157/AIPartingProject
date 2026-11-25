<template>
  <view class="page">
    <view class="card">
      <view class="title">{{ mode === 'login' ? '登录智泊停车' : '注册新账号' }}</view>
      <view class="subtitle">登录后即可预约车位、查看订单</view>

      <view class="field">
        <text class="label">用户名</text>
        <input v-model="username" class="input" placeholder="4-20 位字母/数字/下划线" />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input v-model="password" class="input" password placeholder="6-32 位" />
      </view>

      <button class="primary-btn" :loading="loading" @click="submit">
        {{ mode === 'login' ? '登录' : '注册并登录' }}
      </button>
      <view class="switch" @click="toggleMode">
        {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
      </view>
    </view>

    <view class="hint">开发环境可用 admin / admin123，或自行注册普通车主账号。</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register, login } from '@/api'

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const loading = ref(false)

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

async function submit() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
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
  background: $page-bg;
  padding: 60rpx 32rpx;
}
.card {
  background: #fff;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  padding: 48rpx 40rpx;
}
.title {
  font-size: 40rpx;
  font-weight: 700;
  color: $text-primary;
}
.subtitle {
  margin-top: 10rpx;
  margin-bottom: 30rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.field {
  padding: 20rpx 0;
  border-bottom: 1rpx solid $divider;
}
.label {
  font-size: 24rpx;
  color: $text-secondary;
}
.input {
  margin-top: 12rpx;
  font-size: 30rpx;
  color: $text-primary;
}
.primary-btn {
  margin-top: 40rpx;
  background: $brand-primary;
  color: #fff;
  border-radius: 999rpx;
  font-size: 30rpx;
}
.switch {
  margin-top: 26rpx;
  text-align: center;
  color: $brand-primary;
  font-size: 26rpx;
}
.hint {
  margin-top: 26rpx;
  text-align: center;
  font-size: 22rpx;
  color: $text-tertiary;
}
</style>
