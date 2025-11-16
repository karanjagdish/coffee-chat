import { get, post, put } from './api'
import type { ApiResponse } from '../types/api.types'
import type { User } from '../types/auth.types'
import type { ApiKeyResponse, UpdateUserRequest } from '../types/user.types'

const BASE_PATH = '/user/api/users/me'

export async function getCurrentUser() {
  const response = await get<User>(BASE_PATH)
  return response as ApiResponse<User>
}

export async function updateCurrentUser(payload: UpdateUserRequest) {
  const response = await put<User, UpdateUserRequest>(BASE_PATH, payload)
  return response as ApiResponse<User>
}

export async function getApiKey() {
  const response = await get<ApiKeyResponse>(`${BASE_PATH}/api-key`)
  return response as ApiResponse<ApiKeyResponse>
}

export async function regenerateApiKey() {
  const response = await post<ApiKeyResponse, void>(`${BASE_PATH}/regenerate-api-key`, undefined as never)
  return response as ApiResponse<ApiKeyResponse>
}
