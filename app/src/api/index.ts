import { http, setStoredUser, setToken, toQuery, type UserInfo } from '@/utils/request'

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface Lot {
  id: number
  name: string
  address: string
  area: string
  capacity: number
  status: number
  freeCount?: number
  priceFromCents?: number | null
}

export interface FeeRule {
  id: number
  ruleName: string
  freeMinutes: number
  firstHourCents: number
  extraPerHourCents: number
  dailyCapCents: number
}

export interface LotDetail {
  lot: Lot
  spaceCount: number
  freeCount: number
  occupiedCount: number
  reservedCount: number
  maintenanceCount: number
  enabledRule: FeeRule | null
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

export interface Reservation {
  id: number
  lotId: number
  lotName?: string
  spaceId: number
  spaceNo?: string
  startTime: string
  endTime: string
  status: number
  statusText: string
}

export interface Order {
  id: number
  orderNo: string
  lotId: number
  lotName?: string
  spaceNo?: string
  amountCents: number
  billedMinutes: number
  freeApplied?: number
  status: number
  statusText: string
  paidAt?: string
  createdAt: string
}

export interface CurrentParking {
  recordId: number
  lotId: number
  lotName: string
  spaceId: number
  spaceNo: string
  plateNo: string
  entryTime: string
  elapsedMinutes: number
  estimatedCents: number | null
  freeApplied: boolean
  ruleMissing: boolean
}

export async function login(username: string, password: string): Promise<UserInfo> {
  const data = await http.post<{ token: string; user: UserInfo }>('/api/auth/login', { username, password })
  setToken(data.token)
  setStoredUser(data.user)
  return data.user
}

export async function register(username: string, password: string): Promise<void> {
  await http.post<UserInfo>('/api/auth/register', { username, password })
}

export function listLots(params: { keyword?: string; area?: string } = {}): Promise<PageResult<Lot>> {
  const query = toQuery({ page: 1, size: 50, keyword: params.keyword, area: params.area })
  return http.get<PageResult<Lot>>(`/api/lots?${query}`)
}

export function getLotDetail(lotId: number): Promise<LotDetail> {
  return http.get<LotDetail>(`/api/lots/${lotId}`)
}

export function listSpaces(lotId: number, status?: number): Promise<Space[]> {
  const query = status === undefined ? '' : `?${toQuery({ status })}`
  return http.get<Space[]>(`/api/lots/${lotId}/spaces${query}`)
}

export function createReservation(payload: {
  lotId: number
  spaceId: number
  startTime: string
  hours: number
}): Promise<Reservation> {
  return http.post<Reservation>('/api/reservations', { ...payload })
}

export function listReservations(): Promise<Reservation[]> {
  return http.get<Reservation[]>('/api/reservations')
}

export function cancelReservation(id: number): Promise<void> {
  return http.del<void>(`/api/reservations/${id}`)
}

export function entryParking(payload: {
  lotId: number
  spaceId: number
  plateNo: string
  reservationId?: number
}): Promise<{ id: number; statusText: string }> {
  return http.post('/api/parking/entry', { ...payload })
}

export function currentParking(): Promise<CurrentParking | null> {
  return http.get<CurrentParking | null>('/api/parking/current')
}

export function exitParking(recordId: number): Promise<Order> {
  return http.post<Order>(`/api/parking/records/${recordId}/exit`)
}

export function listOrders(): Promise<PageResult<Order>> {
  return http.get<PageResult<Order>>('/api/orders', { page: 1, size: 50 })
}

export function payOrder(orderId: number): Promise<{ orderId: number; status: number }> {
  return http.post(`/api/orders/${orderId}/pay`)
}
