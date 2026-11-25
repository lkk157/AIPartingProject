import axios from 'axios'

/** AI 服务（FastAPI: 8000），由 vite 代理 /ai 转发 */
const ai = axios.create({ baseURL: '/ai', timeout: 10000 })

export interface PredictPoint {
  time: string
  predictedRate: number
  predictedOccupied: number
  freeSpaces: number
}

export interface PredictResult {
  lotId: number
  lotName: string
  capacity: number
  model: string
  generatedAt: string
  points: PredictPoint[]
}

export interface ModelMetric {
  mae: number
  rmse: number
  r2: number
}

export interface ModelLot {
  lotId: number
  name: string
  capacity: number
}

export interface ModelInfo {
  ready: boolean
  model: string | null
  trainedAt: string | null
  samples: number | null
  features: string[]
  metrics: Record<string, ModelMetric>
  baselineMae: number | null
  lots: ModelLot[]
}

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export async function fetchModelInfo(): Promise<ModelInfo> {
  const response = await ai.get<ApiEnvelope<ModelInfo>>('/predict/model/info')
  return response.data.data
}

export async function predictOccupancy(lotId: number, hours: number): Promise<PredictResult> {
  const response = await ai.post<ApiEnvelope<PredictResult>>('/predict/occupancy', { lotId, hours })
  return response.data.data
}
