<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 登录态由 utils/request 的本地存储恢复，这里无需额外处理
})
</script>

<style lang="scss">
page {
  background: #eef3ff;
  color: #10162b;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 全局背景光晕：每个页面首层放 <view class="aurora" /> */
.aurora {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(52% 42% at 14% 6%, rgba(88, 128, 255, 0.45) 0%, rgba(88, 128, 255, 0) 100%),
    radial-gradient(46% 38% at 88% 14%, rgba(139, 92, 246, 0.38) 0%, rgba(139, 92, 246, 0) 100%),
    radial-gradient(48% 40% at 76% 82%, rgba(45, 212, 191, 0.28) 0%, rgba(45, 212, 191, 0) 100%),
    linear-gradient(168deg, #eef3ff 0%, #f8f5ff 46%, #edf8fb 100%);
}

/* 页面内容容器：抬到光晕之上 */
.content {
  position: relative;
  z-index: 1;
}

/* 通用玻璃卡片 */
.glass {
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px) saturate(150%);
  -webkit-backdrop-filter: blur(18px) saturate(150%);
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 32rpx;
  box-shadow: 0 16rpx 46rpx rgba(28, 45, 90, 0.1);
}
</style>
