import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { TOKEN_KEY, USER_KEY, clearToken, extractErrorMessage, getToken, request, setToken, setUnauthorizedHandler } from './request'

describe('token storage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should_persist_and_read_token', () => {
    setToken('abc123')
    expect(getToken()).toBe('abc123')
    expect(localStorage.getItem(TOKEN_KEY)).toBe('abc123')
  })

  it('should_clear_token_and_user', () => {
    setToken('abc123')
    localStorage.setItem(USER_KEY, '{}')
    clearToken()
    expect(getToken()).toBe('')
    expect(localStorage.getItem(USER_KEY)).toBeNull()
  })

  it('should_return_empty_when_no_token', () => {
    expect(getToken()).toBe('')
  })
})

describe('extractErrorMessage', () => {
  it('should_prefer_backend_message', () => {
    const error = { response: { data: { message: '用户名或密码错误' } }, message: 'Request failed' }
    expect(extractErrorMessage(error)).toBe('用户名或密码错误')
  })

  it('should_fallback_to_axios_message', () => {
    const error = { message: 'Network Error' }
    expect(extractErrorMessage(error)).toBe('Network Error')
  })

  it('should_fallback_to_default_when_empty', () => {
    expect(extractErrorMessage({})).toBe('网络异常，请稍后重试')
  })
})

describe('response interceptor', () => {
  const originalAdapter = request.defaults.adapter

  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    request.defaults.adapter = originalAdapter
    vi.restoreAllMocks()
  })

  it('should_clear_token_and_call_handler_when_401', async () => {
    setToken('expired-token')
    const handler = vi.fn()
    setUnauthorizedHandler(handler)

    request.defaults.adapter = () =>
      Promise.reject({ isAxiosError: true, response: { status: 401, data: { message: '未登录' } } })

    await expect(request.get('/auth/me')).rejects.toBeTruthy()
    expect(getToken()).toBe('')
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('should_not_call_handler_when_400', async () => {
    setToken('valid-token')
    const handler = vi.fn()
    setUnauthorizedHandler(handler)

    request.defaults.adapter = () =>
      Promise.reject({ isAxiosError: true, response: { status: 400, data: { message: '参数错误' } } })

    await expect(request.get('/lots')).rejects.toBeTruthy()
    expect(getToken()).toBe('valid-token')
    expect(handler).not.toHaveBeenCalled()
  })
})
