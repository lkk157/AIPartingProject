import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginApi, meApi } from '@/api/auth'
import { clearToken, getToken, setToken, TOKEN_KEY, USER_KEY } from '@/api/request'
import type { UserInfo } from '@/api/types'

function readUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<UserInfo | null>(readUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const roles = computed(() => user.value?.roles ?? [])
  const displayName = computed(() => user.value?.nickname || user.value?.username || '未登录')

  function persist(nextToken: string, nextUser: UserInfo) {
    token.value = nextToken
    user.value = nextUser
    setToken(nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  async function login(username: string, password: string) {
    const data = await loginApi(username, password)
    persist(data.token, data.user)
    return data
  }

  async function refreshProfile() {
    const profile = await meApi()
    if (token.value) {
      persist(token.value, profile)
    }
    return profile
  }

  function logout() {
    token.value = ''
    user.value = null
    clearToken()
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, user, isLoggedIn, roles, displayName, login, refreshProfile, logout }
})

export function hasRoleAccess(roles: string[], required?: string[]): boolean {
  if (!required || required.length === 0) return true
  return required.some((role) => roles.includes(role))
}
