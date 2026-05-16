import { http, unwrap } from '@/api/http'
import type { AuthResponse, User } from '@/types/api'

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export const authApi = {
  register: (payload: RegisterPayload) => unwrap<AuthResponse>(http.post('/auth/register', payload)),
  login: (payload: LoginPayload) => unwrap<AuthResponse>(http.post('/auth/login', payload)),
  me: () => unwrap<User>(http.get('/auth/me'))
}
