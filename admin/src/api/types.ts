/** 与后端 ApiResponse 对应 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  phone?: string
  status: number
  roles: string[]
  createdAt?: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface Lot {
  id: number
  name: string
  address: string
  area: string
  capacity: number
  status: number
  description?: string
  createdAt?: string
}

export interface FeeRule {
  id: number
  lotId: number
  ruleName: string
  freeMinutes: number
  firstHourCents: number
  extraPerHourCents: number
  dailyCapCents: number
  enabled: number
}

export interface LotDetail {
  lot: Lot
  spaceCount: number
  freeCount: number
  occupiedCount: number
  reservedCount: number
  maintenanceCount: number
  enabledRule: FeeRule | null
  adminIds: number[]
}

export interface Space {
  id: number
  lotId: number
  spaceNo: string
  zone?: string
  type: string
  status: number
  statusText: string
}

export interface OrderItem {
  id: number
  orderNo: string
  username?: string
  lotName?: string
  amountCents: number
  billedMinutes: number
  freeApplied?: number
  status: number
  statusText: string
  paidAt?: string
  createdAt?: string
}

export interface ReservationItem {
  id: number
  username?: string
  lotName?: string
  spaceNo?: string
  startTime: string
  endTime: string
  status: number
  statusText: string
}

export interface Stats {
  lotCount: number
  spaceCount: number
  freeSpaces: number
  occupiedSpaces: number
  todayOrders: number
  todayRevenueCents: number
  pendingReservations: number
}
