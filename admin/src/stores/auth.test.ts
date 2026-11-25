import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { hasRoleAccess, useAuthStore } from './auth'
import { loginApi } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/api/request'
import type { LoginResponse } from '@/api/types'

vi.mock('@/api/auth', () => ({
  loginApi: vi.fn(),
  meApi: vi.fn(),
}))

const loginResponse: LoginResponse = {
  token: 'jwt-token',
  user: {
    id: 1,
    username: 'admin',
    nickname: '系统管理员',
    status: 1,
    roles: ['SYS_ADMIN'],
  },
}

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(loginApi).mockReset()
  })

  it('should_start_logged_out', () => {
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.roles).toEqual([])
    expect(auth.displayName).toBe('未登录')
  })

  it('should_persist_token_and_user_when_login', async () => {
    vi.mocked(loginApi).mockResolvedValue(loginResponse)

    const auth = useAuthStore()
    await auth.login('admin', 'admin123')

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.roles).toEqual(['SYS_ADMIN'])
    expect(auth.displayName).toBe('系统管理员')
    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-token')
    expect(JSON.parse(localStorage.getItem(USER_KEY) ?? '{}').username).toBe('admin')
  })

  it('should_clear_state_when_logout', async () => {
    vi.mocked(loginApi).mockResolvedValue(loginResponse)

    const auth = useAuthStore()
    await auth.login('admin', 'admin123')
    auth.logout()

    expect(auth.isLoggedIn).toBe(false)
    expect(auth.user).toBeNull()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })
})

describe('hasRoleAccess', () => {
  it('should_allow_when_no_role_required', () => {
    expect(hasRoleAccess([], undefined)).toBe(true)
    expect(hasRoleAccess(['USER'], [])).toBe(true)
  })

  it('should_allow_when_any_role_matched', () => {
    expect(hasRoleAccess(['USER', 'SYS_ADMIN'], ['SYS_ADMIN'])).toBe(true)
  })

  it('should_deny_when_role_missing', () => {
    expect(hasRoleAccess(['USER'], ['SYS_ADMIN'])).toBe(false)
  })
})
