import { describe, expect, it } from 'vitest'
import {
  ORDER_STATUS_META,
  RESERVATION_STATUS_META,
  SPACE_STATUS_META,
  orderStatusType,
  reservationStatusType,
  spaceStatusMeta,
  spaceStatusType,
} from './status'

describe('spaceStatusMeta', () => {
  it('should_return_meta_when_status_known', () => {
    expect(spaceStatusMeta(0).label).toBe('空闲')
    expect(spaceStatusMeta(2).label).toBe('预约锁定')
    expect(spaceStatusMeta(3).color).toBe('#94a3b8')
  })

  it('should_return_fallback_when_status_unknown', () => {
    expect(spaceStatusMeta(99).label).toBe('未知')
  })

  it('should_cover_all_four_statuses', () => {
    expect(Object.keys(SPACE_STATUS_META)).toHaveLength(4)
  })
})

describe('status tag types', () => {
  it('should_map_order_status', () => {
    expect(orderStatusType(1)).toBe('success')
    expect(orderStatusType(0)).toBe('warning')
    expect(orderStatusType(2)).toBe('info')
    expect(Object.keys(ORDER_STATUS_META)).toHaveLength(3)
  })

  it('should_map_reservation_status', () => {
    expect(reservationStatusType(0)).toBe('primary')
    expect(reservationStatusType(1)).toBe('success')
    expect(reservationStatusType(3)).toBe('warning')
    expect(Object.keys(RESERVATION_STATUS_META)).toHaveLength(4)
  })

  it('should_map_space_status', () => {
    expect(spaceStatusType(0)).toBe('success')
    expect(spaceStatusType(1)).toBe('danger')
    expect(spaceStatusType(2)).toBe('primary')
    expect(spaceStatusType(3)).toBe('info')
  })
})
