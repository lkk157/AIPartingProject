/** 金额格式化：分 -> ¥x.xx */
export function formatMoney(cents: number): string {
  if (!Number.isFinite(cents)) return '¥0.00'
  const sign = cents < 0 ? '-' : ''
  const abs = Math.abs(Math.round(cents))
  const yuan = Math.floor(abs / 100)
  const fen = abs % 100
  return `${sign}¥${yuan}.${String(fen).padStart(2, '0')}`
}

/** 分钟 -> x 小时 y 分 */
export function formatDuration(minutes: number): string {
  if (!Number.isFinite(minutes) || minutes < 0) return '0 分'
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  if (h === 0) return `${m} 分`
  if (m === 0) return `${h} 小时`
  return `${h} 小时 ${m} 分`
}

/** 占用率 0~1 -> 百分数 */
export function formatPercent(ratio: number, digits = 0): string {
  if (!Number.isFinite(ratio)) return '0%'
  return `${(ratio * 100).toFixed(digits)}%`
}
