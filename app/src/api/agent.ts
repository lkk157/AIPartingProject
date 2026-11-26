import { getToken } from '@/utils/request'

/** H5 走 vite 代理 /ai；小程序端用 VITE_AI_BASE 指向 AI 服务地址 */
const AI_BASE = `${import.meta.env.VITE_AI_BASE ?? ''}/ai`

export interface ToolCall {
  name: string
  ok: boolean
  summary: string
}

export interface Source {
  title: string
  score: number
  snippet: string
}

export interface ChatReply {
  answer: string
  intent: string
  toolCalls: ToolCall[]
  sources: Source[]
  suggestions: string[]
}

interface StreamHandlers {
  onMeta?: (meta: Pick<ChatReply, 'intent' | 'toolCalls' | 'sources' | 'suggestions'>) => void
  onDelta?: (text: string) => void
}

function authHeader(): Record<string, string> {
  const token = getToken()
  const header: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) header.Authorization = `Bearer ${token}`
  return header
}

/** 非流式（小程序端或流式不可用时） */
export async function chatOnce(message: string): Promise<ChatReply> {
  const response = await new Promise<UniApp.RequestSuccessCallbackResult>((resolve, reject) => {
    uni.request({
      url: `${AI_BASE}/agent/chat`,
      method: 'POST',
      data: { message },
      header: authHeader(),
      timeout: 20000,
      success: resolve,
      fail: reject,
    })
  })
  const body = response.data as { code: number; message: string; data: ChatReply }
  if (response.statusCode >= 400 || body?.code !== 0) {
    throw new Error(body?.message || 'AI 服务暂时不可用')
  }
  return body.data
}

/** 流式：H5 用 fetch 读取 SSE；失败时自动降级为非流式 */
export async function chatStream(message: string, handlers: StreamHandlers): Promise<ChatReply> {
  if (typeof fetch !== 'function') {
    const reply = await chatOnce(message)
    handlers.onMeta?.(reply)
    handlers.onDelta?.(reply.answer)
    return reply
  }

  let response: Response
  try {
    response = await fetch(`${AI_BASE}/agent/chat/stream`, {
      method: 'POST',
      headers: authHeader(),
      body: JSON.stringify({ message }),
    })
  } catch {
    return chatStreamFallback(message, handlers)
  }
  if (!response.ok || !response.body) {
    return chatStreamFallback(message, handlers)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let answer = ''
  const reply: ChatReply = { answer: '', intent: '', toolCalls: [], sources: [], suggestions: [] }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let splitIndex = buffer.indexOf('\n\n')
    while (splitIndex >= 0) {
      const rawEvent = buffer.slice(0, splitIndex)
      buffer = buffer.slice(splitIndex + 2)
      splitIndex = buffer.indexOf('\n\n')

      const event = parseEvent(rawEvent)
      if (!event) continue
      if (event.name === 'meta') {
        const meta = event.data as Pick<ChatReply, 'intent' | 'toolCalls' | 'sources' | 'suggestions'>
        reply.intent = meta.intent
        reply.toolCalls = meta.toolCalls ?? []
        reply.sources = meta.sources ?? []
        reply.suggestions = meta.suggestions ?? []
        handlers.onMeta?.(reply)
      } else if (event.name === 'delta') {
        const text = String((event.data as { text?: string }).text ?? '')
        answer += text
        handlers.onDelta?.(text)
      } else if (event.name === 'done') {
        const final = (event.data as { answer?: string }).answer
        if (final) answer = final
      }
    }
  }

  reply.answer = answer
  return reply
}

async function chatStreamFallback(message: string, handlers: StreamHandlers): Promise<ChatReply> {
  const reply = await chatOnce(message)
  handlers.onMeta?.(reply)
  handlers.onDelta?.(reply.answer)
  return reply
}

function parseEvent(raw: string): { name: string; data: unknown } | null {
  const lines = raw.split('\n')
  const nameLine = lines.find((line) => line.startsWith('event:'))
  const dataLine = lines.find((line) => line.startsWith('data:'))
  if (!nameLine || !dataLine) return null
  try {
    return { name: nameLine.slice(6).trim(), data: JSON.parse(dataLine.slice(5).trim()) }
  } catch {
    return null
  }
}
