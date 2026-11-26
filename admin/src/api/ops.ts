import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

/** 运营分析接口（AI 服务），需要管理员 Token 透传 */
const ai = axios.create({ baseURL: '/ai', timeout: 15000 })

export interface OpsStats {
  lotCount: number
  spaceCount: number
  freeSpaces: number
  occupiedSpaces: number
  todayOrders: number
  todayRevenueCents: number
  pendingReservations: number
}

export interface OpsOverview {
  stats: OpsStats
  forecast: {
    lotName: string
    peakTime: string
    peakRate: number
    peakFree: number
    model: string
  } | null
  summary: string
  generatedAt: string
}

export async function fetchOpsOverview(): Promise<OpsOverview> {
  const auth = useAuthStore()
  const response = await ai.get<{ code: number; message: string; data: OpsOverview }>('/agent/ops/overview', {
    headers: auth.token ? { Authorization: `Bearer ${auth.token}` } : {},
  })
  return response.data.data
}
