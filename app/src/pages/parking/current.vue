<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <view v-if="!parking" class="glass empty">
        <view class="empty-orb">P</view>
        <text class="empty-title">当前没有进行中的停车</text>
        <text class="empty-desc">去首页挑一个空闲车位，到场即可开始计费</text>
        <view class="empty-btn" @click="goHome">去找车位</view>
      </view>

      <block v-else>
        <view class="timer-card">
          <text class="tc-label">已停车</text>
          <text class="tc-time">{{ elapsedText }}</text>
          <text class="tc-entry">入场时间 {{ readableTime(parking.entryTime) }}</text>

          <view class="tc-foot">
            <view class="tc-fee">
              <text class="fee-label">预估费用</text>
              <text class="fee-value">{{ parking.estimatedCents === null ? '待计费' : formatMoney(parking.estimatedCents) }}</text>
            </view>
            <view class="fee-tip" v-if="parking.freeApplied">免费时段内</view>
            <view class="fee-tip warn" v-else-if="parking.ruleMissing">未配置规则</view>
          </view>
        </view>

        <view class="glass info">
          <view class="row">
            <text class="row-label">停车场</text>
            <text class="row-value">{{ parking.lotName }}</text>
          </view>
          <view class="row">
            <text class="row-label">车位</text>
            <text class="row-value">{{ parking.spaceNo }}</text>
          </view>
          <view class="row">
            <text class="row-label">车牌</text>
            <text class="row-value">{{ parking.plateNo }}</text>
          </view>
          <view class="row" v-if="rule">
            <text class="row-label">计费规则</text>
            <text class="row-value">免费 {{ rule.freeMinutes }} 分 · 首 {{ formatMoney(rule.firstHourCents) }} · 后 {{ formatMoney(rule.extraPerHourCents) }}/时</text>
          </view>
        </view>

        <view class="actions">
          <view class="btn primary" @click="onExit">离场结算</view>
          <view class="btn ghost" @click="refresh">刷新费用</view>
        </view>

        <view class="tips">费用按实际离场时间计算，若在免费时段内离场则不产生费用。</view>
      </block>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { currentParking, exitParking, getLotDetail, payOrder, type CurrentParking, type FeeRule } from '@/api'
import { formatMoney } from '@/utils/format'

const parking = ref<CurrentParking | null>(null)
const rule = ref<FeeRule | null>(null)
const nowTick = ref(Date.now())
let timer: ReturnType<typeof setInterval> | undefined

const elapsedText = computed(() => {
  if (!parking.value) return '00:00:00'
  const start = new Date(parking.value.entryTime).getTime()
  const diff = Math.max(Math.floor((nowTick.value - start) / 1000), 0)
  const h = String(Math.floor(diff / 3600)).padStart(2, '0')
  const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0')
  const s = String(diff % 60).padStart(2, '0')
  return `${h}:${m}:${s}`
})

function readableTime(iso: string): string {
  return iso.replace('T', ' ').slice(5, 16)
}

async function refresh() {
  try {
    parking.value = await currentParking()
    if (parking.value) {
      const detail = await getLotDetail(parking.value.lotId)
      rule.value = detail.enabledRule
    } else {
      rule.value = null
    }
  } catch {
    parking.value = null
  }
}

function goHome() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/index/index' }),
  })
}

async function onExit() {
  if (!parking.value) return
  const recordId = parking.value.recordId
  uni.showModal({
    title: '确认离场',
    content: '离场后将按实际停车时长结算，是否继续？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const order = await exitParking(recordId)
        const amountText = formatMoney(order.amountCents)
        uni.showModal({
          title: '结算完成',
          content: `停车 ${formatDuration(order.billedMinutes)}，应付 ${amountText}`,
          confirmText: order.status === 0 ? '立即支付' : '查看订单',
          success: async (result) => {
            if (order.status === 0 && result.confirm) {
              await payOrder(order.id)
              uni.showToast({ title: '支付成功', icon: 'success' })
            }
            setTimeout(() => uni.redirectTo({ url: '/pages/order/list?tab=1' }), 500)
          },
        })
      } catch {
        // 统一提示
      }
    },
  })
}

function formatDuration(minutes: number): string {
  if (minutes <= 0) return '不足 1 分钟'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m} 分钟`
  if (m === 0) return `${h} 小时`
  return `${h} 小时 ${m} 分`
}

onShow(() => {
  void refresh()
  timer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  padding: 32rpx;
}
/* 计时卡 */
.timer-card {
  margin-top: 24rpx;
  padding: 40rpx 36rpx;
  border-radius: 40rpx;
  background: $brand-gradient;
  color: #fff;
  box-shadow: 0 24rpx 60rpx rgba(79, 108, 255, 0.35);
  position: relative;
  overflow: hidden;
}
.timer-card::after {
  content: '';
  position: absolute;
  right: -60rpx;
  top: -60rpx;
  width: 220rpx;
  height: 220rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
}
.tc-label {
  font-size: 24rpx;
  opacity: 0.85;
}
.tc-time {
  display: block;
  margin-top: 12rpx;
  font-size: 78rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
  line-height: 1.1;
}
.tc-entry {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  opacity: 0.8;
}
.tc-foot {
  margin-top: 30rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.28);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fee-label {
  font-size: 22rpx;
  opacity: 0.85;
}
.fee-value {
  margin-left: 12rpx;
  font-size: 36rpx;
  font-weight: 700;
}
.fee-tip {
  font-size: 20rpx;
  padding: 6rpx 16rpx;
  border-radius: $radius-pill;
  background: rgba(255, 255, 255, 0.22);
}
.fee-tip.warn {
  background: rgba(255, 255, 255, 0.3);
}

/* 信息卡 */
.info {
  margin-top: 24rpx;
  padding: 10rpx 30rpx;
}
.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 1rpx solid $divider;
}
.row:last-child {
  border-bottom: none;
}
.row-label {
  font-size: 26rpx;
  color: $text-secondary;
}
.row-value {
  flex: 1;
  margin-left: 30rpx;
  text-align: right;
  font-size: 26rpx;
  color: $text-primary;
  font-weight: 500;
}

/* 操作 */
.actions {
  margin-top: 32rpx;
  display: flex;
  gap: 18rpx;
}
.btn {
  flex: 1;
  text-align: center;
  padding: 26rpx 0;
  border-radius: $radius-pill;
  font-size: 30rpx;
  font-weight: 600;
}
.btn.primary {
  background: $brand-gradient;
  color: #fff;
  box-shadow: 0 16rpx 36rpx rgba(79, 108, 255, 0.32);
}
.btn.ghost {
  background: rgba(255, 255, 255, 0.66);
  color: $text-secondary;
  border: 1rpx solid rgba(255, 255, 255, 0.8);
}
.tips {
  margin-top: 22rpx;
  text-align: center;
  font-size: 22rpx;
  color: $text-tertiary;
}

/* 空态 */
.empty {
  margin-top: 120rpx;
  padding: 70rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}
.empty-orb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 36rpx;
  background: $brand-gradient-soft;
  color: $brand-primary;
  font-size: 52rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}
.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.empty-desc {
  font-size: 24rpx;
  color: $text-secondary;
  text-align: center;
}
.empty-btn {
  margin-top: 20rpx;
  padding: 18rpx 52rpx;
  border-radius: $radius-pill;
  background: $brand-gradient;
  color: #fff;
  font-size: 28rpx;
  box-shadow: 0 14rpx 30rpx rgba(79, 108, 255, 0.3);
}
</style>
