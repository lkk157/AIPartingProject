<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <!-- 顶部：问候 + 头像 -->
      <view class="topbar">
        <view class="hello">
          <text class="hello-sub">{{ greetingSub }}</text>
          <text class="hello-name">{{ greetingName }}</text>
        </view>
        <view class="avatar" @click="goProfile">{{ avatarText }}</view>
      </view>

      <!-- 搜索 -->
      <view class="search glass">
        <text class="search-icon">搜</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索停车场名称或地址"
          placeholder-class="search-placeholder"
          confirm-type="search"
          @confirm="applyFilter"
        />
        <text v-if="keyword" class="search-clear" @click="clearKeyword">×</text>
      </view>

      <!-- 停车中卡片 -->
      <view v-if="current" class="parking-now glass" @click="goCurrent">
        <view class="pn-head">
          <view class="pn-tag">停车中</view>
          <text class="pn-timer">{{ elapsedText }}</text>
        </view>
        <view class="pn-title">{{ current.lotName }} · {{ current.spaceNo }}</view>
        <view class="pn-meta">车牌 {{ current.plateNo }} · 入场 {{ timeOf(current.entryTime) }}</view>
        <view class="pn-foot">
          <view>
            <text class="pn-label">预估费用</text>
            <text class="pn-amount">{{ current.estimatedCents === null ? '待计费' : formatMoney(current.estimatedCents) }}</text>
          </view>
          <view class="pn-btn">去结算</view>
        </view>
      </view>

      <!-- 待使用预约提示 -->
      <view v-if="pendingReservation" class="pending glass" @click="goOrders(0)">
        <view class="pending-left">
          <text class="pending-title">你有 1 条待使用预约</text>
          <text class="pending-sub">{{ pendingReservation.lotName || '停车场' }} · {{ pendingReservation.spaceNo || '' }} · {{ shortTime(pendingReservation.startTime) }}</text>
        </view>
        <text class="pending-arrow">查看 ›</text>
      </view>

      <!-- 快捷入口 -->
      <view class="quick">
        <view class="quick-item glass" @click="scrollToList">
          <view class="qi-dot blue" />
          <text class="qi-text">找车位</text>
        </view>
        <view class="quick-item glass" @click="goOrders(0)">
          <view class="qi-dot violet" />
          <text class="qi-text">我的预约</text>
        </view>
        <view class="quick-item glass" @click="goOrders(1)">
          <view class="qi-dot orange" />
          <text class="qi-text">我的订单</text>
        </view>
        <view class="quick-item glass" @click="goProfile">
          <view class="qi-dot green" />
          <text class="qi-text">个人中心</text>
        </view>
      </view>

      <!-- 区域筛选 -->
      <scroll-view class="chips" scroll-x :show-scrollbar="false">
        <view class="chip" :class="{ active: area === '' }" @click="selectArea('')">全部</view>
        <view
          v-for="item in areas"
          :key="item"
          class="chip"
          :class="{ active: area === item }"
          @click="selectArea(item)"
        >{{ item }}</view>
      </scroll-view>

      <!-- 列表 -->
      <view class="section-head">
        <text class="section-title">附近停车场</text>
        <text class="section-count">{{ lots.length }} 个</text>
      </view>

      <view v-if="!loggedIn" class="glass state">
        <text class="state-title">登录后查看车位与价格</text>
        <text class="state-desc">预约车位、无感支付、停车记录一站式管理</text>
        <view class="state-btn" @click="goLogin">立即登录</view>
      </view>

      <view v-else-if="loading" class="glass state">
        <text class="state-title">正在加载…</text>
      </view>

      <view v-else-if="lots.length === 0" class="glass state">
        <text class="state-title">没有找到停车场</text>
        <text class="state-desc">换个关键词或区域试试</text>
      </view>

      <view
        v-for="lot in lots"
        :key="lot.id"
        class="lot glass"
        @click="openLot(lot.id)"
      >
        <view class="lot-cover">
          <text class="lot-cover-text">P</text>
        </view>
        <view class="lot-main">
          <view class="lot-title">{{ lot.name }}</view>
          <view class="lot-addr">{{ lot.area }} · {{ lot.address }}</view>
          <view class="lot-tags">
            <text class="tag free" :class="{ tight: (lot.freeCount ?? 0) === 0 }">
              {{ (lot.freeCount ?? 0) > 0 ? `空闲 ${lot.freeCount}` : '已满' }}
            </text>
            <text class="tag">共 {{ lot.capacity }} 位</text>
            <text v-if="lot.priceFromCents" class="tag price">{{ formatMoney(lot.priceFromCents) }}/时起</text>
          </view>
        </view>
        <text class="lot-arrow">›</text>
      </view>

      <view class="footer">数据来自实时接口 · 智泊停车</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { currentParking, listLots, listReservations, type CurrentParking, type Lot, type Reservation } from '@/api'
import { formatMoney } from '@/utils/format'
import { getStoredUser, getToken } from '@/utils/request'

const lots = ref<Lot[]>([])
const areas = ref<string[]>([])
const area = ref('')
const keyword = ref('')
const loading = ref(false)
const loggedIn = ref(false)
const nickname = ref('')
const current = ref<CurrentParking | null>(null)
const pendingReservation = ref<Reservation | null>(null)
const nowTick = ref(Date.now())

const avatarText = computed(() => (nickname.value ? nickname.value.slice(0, 1).toUpperCase() : '登'))
const greetingSub = computed(() => (loggedIn.value ? '欢迎回来' : '你好，车主'))
const greetingName = computed(() => (loggedIn.value ? nickname.value || '车主' : '点击登录开始停车'))

const elapsedText = computed(() => {
  if (!current.value) return '00:00'
  const start = new Date(current.value.entryTime).getTime()
  const diff = Math.max(Math.floor((nowTick.value - start) / 1000), 0)
  const h = String(Math.floor(diff / 3600)).padStart(2, '0')
  const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0')
  const s = String(diff % 60).padStart(2, '0')
  return `${h}:${m}:${s}`
})

let timer: ReturnType<typeof setInterval> | undefined

function timeOf(iso: string): string {
  return iso.replace('T', ' ').slice(11, 16)
}

async function loadLots() {
  loading.value = true
  try {
    const result = await listLots({ keyword: keyword.value, area: area.value })
    lots.value = result.records
  } catch {
    lots.value = []
  } finally {
    loading.value = false
  }
}

async function loadAreas() {
  try {
    const result = await listLots()
    areas.value = Array.from(new Set(result.records.map((lot) => lot.area))).slice(0, 8)
  } catch {
    areas.value = []
  }
}

async function loadCurrent() {
  try {
    current.value = await currentParking()
  } catch {
    current.value = null
  }
}

async function loadPendingReservation() {
  try {
    const list = await listReservations()
    pendingReservation.value = list.find((item) => item.status === 0) ?? null
  } catch {
    pendingReservation.value = null
  }
}

function shortTime(iso: string): string {
  return iso.replace('T', ' ').slice(5, 16)
}

async function load() {
  loggedIn.value = Boolean(getToken())
  nickname.value = getStoredUser()?.nickname ?? ''
  if (!loggedIn.value) {
    lots.value = []
    current.value = null
    pendingReservation.value = null
    return
  }
  await Promise.all([loadLots(), loadAreas(), loadCurrent(), loadPendingReservation()])
}

function applyFilter() {
  void loadLots()
}

function clearKeyword() {
  keyword.value = ''
  void loadLots()
}

function selectArea(value: string) {
  area.value = value
  void loadLots()
}

function goLogin() {
  uni.navigateTo({ url: '/pages/user/login' })
}

function goProfile() {
  if (!loggedIn.value) {
    goLogin()
    return
  }
  uni.navigateTo({ url: '/pages/user/profile' })
}

function goOrders(tab: number) {
  if (!loggedIn.value) {
    goLogin()
    return
  }
  uni.navigateTo({ url: `/pages/order/list?tab=${tab}` })
}

function goCurrent() {
  uni.navigateTo({ url: '/pages/parking/current' })
}

function scrollToList() {
  uni.pageScrollTo({ scrollTop: 480, duration: 260 })
}

function openLot(id: number) {
  if (!loggedIn.value) {
    goLogin()
    return
  }
  uni.navigateTo({ url: `/pages/lot/detail?id=${id}` })
}

onShow(() => {
  void load()
  timer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  min-height: 100vh;
  padding-bottom: 60rpx;
}
.content {
  padding: 96rpx 32rpx 0;
}

/* 顶部 */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hello {
  display: flex;
  flex-direction: column;
}
.hello-sub {
  font-size: 24rpx;
  color: $text-secondary;
}
.hello-name {
  margin-top: 6rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: $text-primary;
}
.avatar {
  width: 84rpx;
  height: 84rpx;
  border-radius: 28rpx;
  background: $brand-gradient;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 30rpx rgba(79, 108, 255, 0.35);
}

/* 搜索 */
.search {
  margin-top: 32rpx;
  display: flex;
  align-items: center;
  padding: 0 26rpx;
  height: 96rpx;
  border-radius: $radius-pill;
}
.search-icon {
  width: 40rpx;
  font-size: 24rpx;
  color: $brand-primary;
}
.search-input {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
}
.search-placeholder {
  color: $text-tertiary;
}
.search-clear {
  font-size: 36rpx;
  color: $text-tertiary;
  padding-left: 16rpx;
}

/* 停车中 */
.parking-now {
  margin-top: 28rpx;
  padding: 30rpx 32rpx;
  background: $brand-gradient;
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
  box-shadow: 0 20rpx 50rpx rgba(79, 108, 255, 0.35);
}
.pn-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pn-tag {
  font-size: 22rpx;
  padding: 6rpx 18rpx;
  border-radius: $radius-pill;
  background: rgba(255, 255, 255, 0.22);
}
.pn-timer {
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}
.pn-title {
  margin-top: 18rpx;
  font-size: 32rpx;
  font-weight: 600;
}
.pn-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  opacity: 0.85;
}
.pn-foot {
  margin-top: 24rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.pn-label {
  font-size: 22rpx;
  opacity: 0.85;
}
.pn-amount {
  margin-left: 12rpx;
  font-size: 34rpx;
  font-weight: 700;
}
.pn-btn {
  padding: 12rpx 30rpx;
  border-radius: $radius-pill;
  background: #fff;
  color: $brand-primary-deep;
  font-size: 26rpx;
  font-weight: 600;
}

/* 待使用预约 */
.pending {
  margin-top: 24rpx;
  padding: 22rpx 26rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-left: 6rpx solid $brand-primary;
}
.pending-left {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.pending-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $text-primary;
}
.pending-sub {
  font-size: 22rpx;
  color: $text-secondary;
}
.pending-arrow {
  font-size: 24rpx;
  color: $brand-primary;
}

/* 快捷入口 */
.quick {
  margin-top: 28rpx;
  display: flex;
  gap: 18rpx;
}
.quick-item {
  flex: 1;
  padding: 24rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  border-radius: 26rpx;
}
.qi-dot {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
}
.blue { background: linear-gradient(135deg, #5b8cff, #3b6bff); }
.violet { background: linear-gradient(135deg, #a78bfa, #7c5cff); }
.orange { background: linear-gradient(135deg, #ffb266, #ff7a2f); }
.green { background: linear-gradient(135deg, #34d399, #12b76a); }
.qi-text {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 区域 chips */
.chips {
  margin-top: 32rpx;
  white-space: nowrap;
}
.chip {
  display: inline-block;
  margin-right: 16rpx;
  @include pill-chip(false);
}
.chip.active {
  @include pill-chip(true);
}

/* 列表标题 */
.section-head {
  margin-top: 36rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $text-primary;
}
.section-count {
  font-size: 24rpx;
  color: $text-tertiary;
}

/* 停车场卡片 */
.lot {
  margin-top: 20rpx;
  padding: 26rpx;
  display: flex;
  align-items: center;
}
.lot-cover {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  background: $brand-gradient-soft;
  border: 1rpx solid rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}
.lot-cover-text {
  font-size: 40rpx;
  font-weight: 800;
  background: $brand-gradient;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.lot-main {
  flex: 1;
  margin-left: 22rpx;
  min-width: 0;
}
.lot-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.lot-addr {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lot-tags {
  margin-top: 12rpx;
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}
.tag {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: $radius-pill;
  background: rgba(47, 107, 255, 0.08);
  color: $brand-primary;
}
.tag.free {
  background: rgba(18, 183, 106, 0.12);
  color: $success-green;
}
.tag.free.tight {
  background: rgba(240, 68, 56, 0.1);
  color: $danger-red;
}
.tag.price {
  background: rgba(255, 122, 47, 0.12);
  color: $amount-orange;
}
.lot-arrow {
  margin-left: 12rpx;
  font-size: 40rpx;
  color: $text-tertiary;
}

/* 空/加载态 */
.state {
  margin-top: 20rpx;
  padding: 56rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.state-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.state-desc {
  font-size: 24rpx;
  color: $text-secondary;
}
.state-btn {
  margin-top: 16rpx;
  padding: 16rpx 44rpx;
  border-radius: $radius-pill;
  background: $brand-gradient;
  color: #fff;
  font-size: 26rpx;
  box-shadow: 0 12rpx 28rpx rgba(79, 108, 255, 0.3);
}
.footer {
  margin-top: 44rpx;
  text-align: center;
  font-size: 22rpx;
  color: $text-tertiary;
}
</style>
