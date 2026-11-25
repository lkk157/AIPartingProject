import { describe, expect, it } from 'vitest'
import { formatDistance, formatDuration, formatMoney } from './format'

describe('formatMoney', () => {
  it('should_format_cents_to_yuan_when_normal_value', () => {
    expect(formatMoney(3600)).toBe('¥36.00')
    expect(formatMoney(5)).toBe('¥0.05')
  })

  it('should_keep_minus_sign_when_negative', () => {
    expect(formatMoney(-600)).toBe('-¥6.00')
  })
})

describe('formatDuration', () => {
  it('should_render_hours_and_minutes_when_both_present', () => {
    expect(formatDuration(375)).toBe('6 小时 15 分')
  })

  it('should_render_only_minutes_when_less_than_hour', () => {
    expect(formatDuration(45)).toBe('45 分')
  })
})

describe('formatDistance', () => {
  it('should_render_meter_when_under_1km', () => {
    expect(formatDistance(800)).toBe('800m')
  })

  it('should_render_km_when_over_1km', () => {
    expect(formatDistance(1250)).toBe('1.3km')
  })
})
