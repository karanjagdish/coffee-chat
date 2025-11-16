import { get, post, put, del } from './api'
import type { ApiResponse } from '../types/api.types'
import type { Session, CreateSessionRequest, RenameSessionRequest } from '../types/session.types'

const BASE_PATH = '/chat/api/sessions'

export async function fetchSessions() {
  const response = await get<Session[]>(BASE_PATH)
  return response as ApiResponse<Session[]>
}

export async function createSession(payload: CreateSessionRequest) {
  const response = await post<Session, CreateSessionRequest>(BASE_PATH, payload)
  return response as ApiResponse<Session>
}

export async function renameSession(id: string, payload: RenameSessionRequest) {
  const response = await put<Session, RenameSessionRequest>(`${BASE_PATH}/${id}/rename`, payload)
  return response as ApiResponse<Session>
}

export async function toggleFavorite(id: string) {
  const response = await put<Session, void>(`${BASE_PATH}/${id}/favorite`)
  return response as ApiResponse<Session>
}

export async function deleteSession(id: string) {
  const response = await del<void>(`${BASE_PATH}/${id}`)
  return response as ApiResponse<void>
}
