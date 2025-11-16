import { post, get } from './api'
import type {
  AuthPayload,
  LoginRequest,
  SignupRequest,
  RefreshTokenRequest,
  User,
} from '../types/auth.types'
import type { ApiResponse } from '../types/api.types'

const BASE_PATH = '/user/api/auth'

export async function signup(request: SignupRequest) {
  const response = await post<AuthPayload, SignupRequest>(`${BASE_PATH}/signup`, request)
  return response as ApiResponse<AuthPayload>
}

export async function login(request: LoginRequest) {
  const response = await post<AuthPayload, LoginRequest>(`${BASE_PATH}/login`, request)
  return response as ApiResponse<AuthPayload>
}

export async function refreshToken(request: RefreshTokenRequest) {
  const response = await post<AuthPayload, RefreshTokenRequest>(`${BASE_PATH}/refresh`, request)
  return response as ApiResponse<AuthPayload>
}

export async function validateToken() {
  const response = await get<User>(`${BASE_PATH}/validate-token`)
  return response as ApiResponse<User>
}
