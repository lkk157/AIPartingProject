import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

export const TOKEN_KEY = 'parking_admin_token'
export const USER_KEY = 'parking_admin_user'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

let unauthorizedHandler: () => void = () => {
  window.location.href = '/login'
}

export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}

export function extractErrorMessage(error: unknown): string {
  const axiosError = error as AxiosError<ApiResponse<unknown>>
  return axiosError.response?.data?.message ?? axiosError.message ?? '网络异常，请稍后重试'
}

export const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    if (status === 401) {
      clearToken()
      unauthorizedHandler()
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(extractErrorMessage(error))
    }
    return Promise.reject(error)
  },
)

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}

export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  return unwrap<T>(request.get(url, { params, ...config }))
}

export function post<T>(url: string, body?: unknown): Promise<T> {
  return unwrap<T>(request.post(url, body))
}

export function put<T>(url: string, body?: unknown): Promise<T> {
  return unwrap<T>(request.put(url, body))
}

export function patch<T>(url: string, body?: unknown): Promise<T> {
  return unwrap<T>(request.patch(url, body))
}

export function del<T>(url: string): Promise<T> {
  return unwrap<T>(request.delete(url))
}
