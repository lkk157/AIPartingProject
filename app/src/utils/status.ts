/** 车位状态颜色，与设计 Token 保持一致 */
export const SPACE_COLORS: Record<number, string> = {
  0: '#22c55e',
  1: '#ef4444',
  2: '#2563eb',
  3: '#94a3b8',
}

export function spaceColor(status: number): string {
  return SPACE_COLORS[status] ?? '#cbd5e1'
}

export function orderStatusColor(status: number): string {
  if (status === 1) return '#16a34a'
  if (status === 0) return '#f59e0b'
  return '#94a3b8'
}

export function reservationStatusColor(status: number): string {
  if (status === 0) return '#2563eb'
  if (status === 1) return '#16a34a'
  if (status === 3) return '#f59e0b'
  return '#94a3b8'
}
