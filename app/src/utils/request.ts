/** H5 走 vite 代理（/api）；小程序需通过 VITE_API_BASE 指定后端地址 */
const BASE_URL = import.meta.env.VITE_API_BASE ?? ''

const TOKEN_KEY = 'parking_token'
const USER_KEY = 'parking_user'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  status: number
  roles: string[]
}

export function getToken(): string {
  return (uni.getStorageSync(TOKEN_KEY) as string) || ''
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getStoredUser(): UserInfo | null {
  const raw = uni.getStorageSync(USER_KEY) as string
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export function setStoredUser(user: UserInfo): void {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}

export function clearAuth(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface RequestOptions {
  url: string
  method?: Method
  data?: Record<string, unknown>
}

export function request<T>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (token) {
      header.Authorization = `Bearer ${token}`
    }

    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method ?? 'GET',
      data: options.data,
      header,
      success: (res) => {
        const body = res.data as ApiResponse<T> | undefined
        if (res.statusCode >= 200 && res.statusCode < 300 && body?.code === 0) {
          resolve(body.data)
          return
        }
        if (res.statusCode === 401) {
          clearAuth()
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => uni.navigateTo({ url: '/pages/user/login' }), 600)
        } else {
          uni.showToast({ title: body?.message ?? '请求失败', icon: 'none' })
        }
        reject(new Error(body?.message ?? '请求失败'))
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(new Error(err.errMsg ?? 'network error'))
      },
    })
  })
}

export const http = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    request<T>({ url: params ? `${url}?${toQuery(params)}` : url }),
  post: <T>(url: string, data?: Record<string, unknown>) => request<T>({ url, method: 'POST', data }),
  patch: <T>(url: string, data?: Record<string, unknown>) => request<T>({ url, method: 'PATCH', data }),
  del: <T>(url: string) => request<T>({ url, method: 'DELETE' }),
}

export function toQuery(params: Record<string, unknown>): string {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
}
