import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi, type LoginPayload, type RegisterPayload } from '@/api/auth'
import { tokenKey } from '@/api/http'
import type { User } from '@/types/api'

const userKey = 'english_words_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(tokenKey) || '')
  const user = ref<User | null>(loadStoredUser())
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(payload: LoginPayload) {
    const result = await authApi.login(payload)
    setSession(result.token, result.user)
  }

  async function register(payload: RegisterPayload) {
    const result = await authApi.register(payload)
    setSession(result.token, result.user)
  }

  async function fetchMe() {
    if (!token.value) return
    user.value = await authApi.me()
    localStorage.setItem(userKey, JSON.stringify(user.value))
  }

  function setSession(nextToken: string, nextUser: User) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(tokenKey, nextToken)
    localStorage.setItem(userKey, JSON.stringify(nextUser))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(tokenKey)
    localStorage.removeItem(userKey)
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    register,
    fetchMe,
    logout
  }
})

function loadStoredUser() {
  const raw = localStorage.getItem(userKey)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}
