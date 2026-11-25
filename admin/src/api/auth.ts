import { get, post } from './request'
import type { LoginResponse, UserInfo } from './types'

export function loginApi(username: string, password: string): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/login', { username, password })
}

export function meApi(): Promise<UserInfo> {
  return get<UserInfo>('/auth/me')
}
