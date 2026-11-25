/** 车位 / 订单 / 预约状态展示元数据，与后端状态机保持一致 */

export interface StatusMeta {
  label: string
  color: string
}

export const SPACE_STATUS_META: Record<number, StatusMeta> = {
  0: { label: '空闲', color: '#22c55e' },
  1: { label: '占用', color: '#ef4444' },
  2: { label: '预约锁定', color: '#2563eb' },
  3: { label: '维护', color: '#94a3b8' },
}

export const ORDER_STATUS_META: Record<number, StatusMeta> = {
  0: { label: '待支付', color: '#f59e0b' },
  1: { label: '已支付', color: '#16a34a' },
  2: { label: '已取消', color: '#94a3b8' },
}

export const RESERVATION_STATUS_META: Record<number, StatusMeta> = {
  0: { label: '待使用', color: '#2563eb' },
  1: { label: '已完成', color: '#16a34a' },
  2: { label: '已取消', color: '#94a3b8' },
  3: { label: '已过期', color: '#f59e0b' },
}

export function spaceStatusMeta(status: number): StatusMeta {
  return SPACE_STATUS_META[status] ?? { label: '未知', color: '#cbd5e1' }
}

export type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

export function orderStatusType(status: number): TagType {
  if (status === 1) return 'success'
  if (status === 0) return 'warning'
  return 'info'
}

export function reservationStatusType(status: number): TagType {
  if (status === 1) return 'success'
  if (status === 0) return 'primary'
  if (status === 3) return 'warning'
  return 'info'
}

export function spaceStatusType(status: number): TagType {
  if (status === 0) return 'success'
  if (status === 1) return 'danger'
  if (status === 2) return 'primary'
  return 'info'
}
