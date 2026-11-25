import { del, get, patch, post, put } from './request'
import type { FeeRule, Lot, LotDetail, PageResult, Space } from './types'

export interface LotQuery {
  page?: number
  size?: number
  keyword?: string
  area?: string
  status?: number
}

export interface LotPayload {
  name: string
  address: string
  area: string
  capacity?: number
  status?: number
  description?: string
}

export function listLots(query: LotQuery): Promise<PageResult<Lot>> {
  return get<PageResult<Lot>>('/admin/lots', { ...query })
}

export function getLot(id: number): Promise<LotDetail> {
  return get<LotDetail>(`/admin/lots/${id}`)
}

export function createLot(payload: LotPayload): Promise<Lot> {
  return post<Lot>('/admin/lots', payload)
}

export function updateLot(id: number, payload: LotPayload): Promise<Lot> {
  return put<Lot>(`/admin/lots/${id}`, payload)
}

export function removeLot(id: number): Promise<void> {
  return del<void>(`/admin/lots/${id}`)
}

export function batchCreateSpaces(lotId: number, payload: { count: number; prefix: string; zone?: string; type?: string }): Promise<{ created: number }> {
  return post<{ created: number }>(`/admin/lots/${lotId}/spaces/batch`, payload)
}

export function listSpaces(lotId: number, query: { page?: number; size?: number; status?: number }): Promise<PageResult<Space>> {
  return get<PageResult<Space>>(`/admin/lots/${lotId}/spaces`, { ...query })
}

export function changeSpaceStatus(spaceId: number, status: number, reason?: string): Promise<Space> {
  return patch<Space>(`/admin/spaces/${spaceId}/status`, { status, reason })
}

export function listFeeRules(lotId: number): Promise<FeeRule[]> {
  return get<FeeRule[]>(`/admin/lots/${lotId}/fee-rules`)
}

export function createFeeRule(lotId: number, payload: Omit<FeeRule, 'id' | 'lotId' | 'enabled'>): Promise<FeeRule> {
  return post<FeeRule>(`/admin/lots/${lotId}/fee-rules`, payload)
}

export function updateFeeRule(ruleId: number, payload: Omit<FeeRule, 'id' | 'lotId' | 'enabled'>): Promise<FeeRule> {
  return put<FeeRule>(`/admin/fee-rules/${ruleId}`, payload)
}

export function removeFeeRule(ruleId: number): Promise<void> {
  return del<void>(`/admin/fee-rules/${ruleId}`)
}
