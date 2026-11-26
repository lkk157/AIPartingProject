import axios from 'axios'

/** AI 服务知识库接口，vite 代理 /ai → 8000 */
const ai = axios.create({ baseURL: '/ai', timeout: 10000 })

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface KnowledgeDoc {
  id: string
  title: string
  source: 'seed' | 'custom' | 'upload' | string
  chars: number
  chunks: number
  createdAt: string
}

export interface KnowledgeStats {
  provider: string
  dim: number
  documents: number
  chunks: number
  chunkSize: number
  overlap: number
}

export interface KnowledgeHit {
  id: string
  title: string
  source: string
  score: number
  snippet: string
}

export async function listDocuments(): Promise<KnowledgeDoc[]> {
  const response = await ai.get<ApiEnvelope<KnowledgeDoc[]>>('/knowledge/documents')
  return response.data.data
}

export async function createDocument(title: string, content: string): Promise<void> {
  await ai.post('/knowledge/documents', { title, content })
}

export async function deleteDocument(id: string): Promise<void> {
  await ai.delete(`/knowledge/documents/${id}`)
}

export async function searchKnowledge(query: string, topK = 5, useMmr = true): Promise<KnowledgeHit[]> {
  const response = await ai.post<ApiEnvelope<KnowledgeHit[]>>('/knowledge/search', { query, topK, useMmr })
  return response.data.data
}

export async function fetchKnowledgeStats(): Promise<KnowledgeStats> {
  const response = await ai.get<ApiEnvelope<KnowledgeStats>>('/knowledge/stats')
  return response.data.data
}

/** 以 base64 上传 md/txt/pdf，避免额外 multipart 依赖 */
export async function uploadKnowledgeFile(filename: string, base64: string): Promise<void> {
  await ai.post('/knowledge/upload', { filename, contentBase64: base64 })
}
