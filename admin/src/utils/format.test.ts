import { describe, expect, it } from 'vitest'
import { formatDuration, formatMoney, formatPercent } from './format'

describe('formatMoney', () => {
  it('should_format_cents_to_yuan_when_normal_value', () => {
    expect(formatMoney(3600)).toBe('¥36.00')
    expect(formatMoney(5)).toBe('¥0.05')
    expect(formatMoney(0)).toBe('¥0.00')
  })

  it('should_keep_minus_sign_when_negative', () => {
    expect(formatMoney(-600)).toBe('-¥6.00')
  })

  it('should_return_zero_when_not_finite', () => {
    expect(formatMoney(Number.NaN)).toBe('¥0.00')
  })
})

describe('formatDuration', () => {
  it('should_render_hours_and_minutes_when_both_present', () => {
    expect(formatDuration(375)).toBe('6 小时 15 分')
  })

  it('should_render_only_minutes_when_less_than_hour', () => {
    expect(formatDuration(45)).toBe('45 分')
  })

  it('should_render_only_hours_when_whole_hour', () => {
    expect(formatDuration(120)).toBe('2 小时')
  })
})

describe('formatPercent', () => {
  it('should_format_ratio_to_percent', () => {
    expect(formatPercent(0.856)).toBe('86%')
    expect(formatPercent(0.856, 1)).toBe('85.6%')
  })
})
