<template>
  <view class="page">
    <view class="aurora" />
    <view class="content">
      <scroll-view class="messages" scroll-y :scroll-top="scrollTop" scroll-with-animation>
        <view class="date-tip">AI 助手 · 基于实时车位与规则库</view>

        <view v-for="(item, index) in messages" :key="index" :id="`msg-${index}`">
          <view v-if="item.role === 'user'" class="row user">
            <view class="bubble user-bubble">{{ item.content }}</view>
          </view>
          <view v-else class="row assistant">
            <view class="avatar">AI</view>
            <view class="assistant-main">
              <view class="bubble assistant-bubble">
                <text v-if="item.loading && !item.content" class="typing">正在查询…</text>
                <text v-else>{{ item.content }}</text>
              </view>

              <view v-if="item.sources?.length" class="sources">
                <view v-for="source in item.sources" :key="source.title" class="source">
                  <text class="source-title">{{ source.title }}</text>
                  <text class="source-score">{{ (source.score * 100).toFixed(0) }}%</text>
                </view>
              </view>

              <view v-if="item.toolCalls?.length" class="tools">
                <text v-for="tool in item.toolCalls" :key="tool.name" class="tool" :class="{ fail: !tool.ok }">
                  {{ toolLabel(tool.name) }}{{ tool.ok ? '' : '（失败）' }}
                </text>
              </view>
            </view>
          </view>
        </view>

        <view class="spacer" />
      </scroll-view>

      <view class="quick-row">
        <view v-for="chip in suggestions" :key="chip" class="chip" @click="send(chip)">{{ chip }}</view>
      </view>

      <view class="input-bar glass">
        <input
          v-model="draft"
          class="input"
          placeholder="问车位、预约、收费或订单…"
          confirm-type="send"
          :disabled="sending"
          @confirm="send()"
        />
        <view class="send" :class="{ disabled: !draft.trim() || sending }" @click="send()">发送</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { chatStream, type ChatReply, type Source, type ToolCall } from '@/api/agent'

interface Message {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  sources?: Source[]
  toolCalls?: ToolCall[]
}

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: '你好，我是智泊停车助手。可以帮你查空闲车位、看预约与订单，或解答收费规则。',
  },
])
const suggestions = ref(['附近哪里有空位', '我的预约', '停车怎么收费'])
const draft = ref('')
const sending = ref(false)
const scrollTop = ref(0)

const LABELS: Record<string, string> = {
  list_lots: '查询停车场',
  my_reservations: '查询预约',
  my_orders: '查询订单',
  current_parking: '查询停车中',
  forecast: '占用率预测',
  search_knowledge: '检索知识库',
}

function toolLabel(name: string): string {
  return LABELS[name] ?? name
}

async function scrollToBottom() {
  await nextTick()
  // 递增 scroll-top 才会触发滚动；一次给足，直接贴底
  scrollTop.value += 100000
}

async function send(text?: string) {
  const content = (text ?? draft.value).trim()
  if (!content || sending.value) return

  draft.value = ''
  sending.value = true
  messages.value.push({ role: 'user', content })
  const replyIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true })
  await scrollToBottom()

  try {
    const reply: ChatReply = await chatStream(content, {
      onMeta: (meta) => {
        const target = messages.value[replyIndex]
        target.sources = meta.sources
        target.toolCalls = meta.toolCalls
      },
      onDelta: (delta) => {
        const target = messages.value[replyIndex]
        target.loading = false
        target.content += delta
        void scrollToBottom()
      },
    })
    const target = messages.value[replyIndex]
    target.loading = false
    target.content = reply.answer || target.content || '暂时没有答案，换个问法试试？'
    target.sources = reply.sources
    target.toolCalls = reply.toolCalls
    if (reply.suggestions?.length) {
      suggestions.value = reply.suggestions
    }
  } catch (error) {
    const target = messages.value[replyIndex]
    target.loading = false
    target.content = `AI 服务暂时不可用：${error instanceof Error ? error.message : '请稍后再试'}`
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.page {
  /* H5 下顶部导航由 uni-app 渲染，用 window-top 变量扣掉，避免输入栏被顶出屏幕 */
  height: calc(100vh - var(--window-top, 0px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20rpx 28rpx 0;
  min-height: 0;
}
.messages {
  flex: 1;
  min-height: 0;
}
.date-tip {
  text-align: center;
  font-size: 20rpx;
  color: $text-tertiary;
  padding: 12rpx 0 20rpx;
}
.row {
  display: flex;
  margin-bottom: 22rpx;
}
.row.user {
  justify-content: flex-end;
}
.row.assistant {
  align-items: flex-start;
  gap: 14rpx;
}
.avatar {
  width: 62rpx;
  height: 62rpx;
  border-radius: 20rpx;
  background: $brand-gradient;
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 24rpx rgba(79, 108, 255, 0.3);
  flex-shrink: 0;
}
.assistant-main {
  max-width: 78%;
}
.bubble {
  padding: 22rpx 26rpx;
  border-radius: 26rpx;
  font-size: 27rpx;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
.user-bubble {
  background: $brand-gradient;
  color: #fff;
  border-bottom-right-radius: 10rpx;
  box-shadow: 0 12rpx 26rpx rgba(79, 108, 255, 0.28);
}
.assistant-bubble {
  @include glass;
  color: $text-primary;
  border-bottom-left-radius: 10rpx;
}
.typing {
  color: $text-tertiary;
}
.sources,
.tools {
  margin-top: 12rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}
.source {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border-radius: $radius-pill;
  background: rgba(47, 107, 255, 0.1);
}
.source-title {
  font-size: 20rpx;
  color: $brand-primary;
}
.source-score {
  font-size: 18rpx;
  color: $text-tertiary;
}
.tool {
  font-size: 20rpx;
  padding: 6rpx 14rpx;
  border-radius: $radius-pill;
  background: rgba(18, 183, 106, 0.12);
  color: $success-green;
}
.tool.fail {
  background: rgba(240, 68, 56, 0.1);
  color: $danger-red;
}
.spacer {
  height: 20rpx;
}
.quick-row {
  display: flex;
  gap: 12rpx;
  padding: 12rpx 0;
  overflow-x: auto;
  white-space: nowrap;
}
.chip {
  flex-shrink: 0;
  padding: 12rpx 24rpx;
  border-radius: $radius-pill;
  background: rgba(255, 255, 255, 0.7);
  border: 1rpx solid rgba(255, 255, 255, 0.9);
  font-size: 22rpx;
  color: $text-secondary;
}
.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 14rpx 18rpx;
  margin-bottom: 16rpx;
  border-radius: $radius-pill;
}
.input {
  flex: 1;
  font-size: 27rpx;
  color: $text-primary;
  padding-left: 12rpx;
}
.send {
  padding: 14rpx 30rpx;
  border-radius: $radius-pill;
  background: $brand-gradient;
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
}
.send.disabled {
  opacity: 0.45;
}
</style>
