import { get, patch } from './request'
import type { OrderItem, PageResult, ReservationItem, Stats, UserInfo } from './types'

export function listUsers(query: { page?: number; size?: number; keyword?: string }): Promise<PageResult<UserInfo>> {
  return get<PageResult<UserInfo>>('/admin/users', { ...query })
}

export function updateUserStatus(id: number, status: number): Promise<void> {
  return patch<void>(`/admin/users/${id}/status`, { status })
}

export function listOrders(query: { page?: number; size?: number; status?: number; keyword?: string }): Promise<PageResult<OrderItem>> {
  return get<PageResult<OrderItem>>('/admin/orders', { ...query })
}

export function listReservations(query: { page?: number; size?: number; status?: number }): Promise<PageResult<ReservationItem>> {
  return get<PageResult<ReservationItem>>('/admin/reservations', { ...query })
}

export function fetchStats(): Promise<Stats> {
  return get<Stats>('/admin/stats/overview')
}
