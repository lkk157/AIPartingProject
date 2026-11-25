import { describe, expect, it } from 'vitest'
import { orderStatusColor, reservationStatusColor, spaceColor } from './status'
import { toQuery } from './request'

describe('status colors', () => {
  it('should_return_space_color_by_status', () => {
    expect(spaceColor(0)).toBe('#22c55e')
    expect(spaceColor(2)).toBe('#2563eb')
    expect(spaceColor(99)).toBe('#cbd5e1')
  })

  it('should_return_order_color_by_status', () => {
    expect(orderStatusColor(0)).toBe('#f59e0b')
    expect(orderStatusColor(1)).toBe('#16a34a')
    expect(orderStatusColor(2)).toBe('#94a3b8')
  })

  it('should_return_reservation_color_by_status', () => {
    expect(reservationStatusColor(0)).toBe('#2563eb')
    expect(reservationStatusColor(3)).toBe('#f59e0b')
  })
})

describe('toQuery', () => {
  it('should_skip_empty_values', () => {
    expect(toQuery({ page: 1, keyword: '', status: undefined, size: 10 })).toBe('page=1&size=10')
  })

  it('should_encode_values', () => {
    expect(toQuery({ keyword: '科技园' })).toBe('keyword=%E7%A7%91%E6%8A%80%E5%9B%AD')
  })
})
