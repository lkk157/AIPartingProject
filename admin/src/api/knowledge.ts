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
  source: 'seed' | 'custom' | string
  chars: number
  createdAt: string
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

export async function searchKnowledge(query: string, topK = 5): Promise<KnowledgeHit[]> {
  const response = await ai.post<ApiEnvelope<KnowledgeHit[]>>('/knowledge/search', { query, topK })
  return response.data.data
}
