import { get, post } from './api'
import type { PageResponse } from '../types/api.types'
import type { Message, CreateMessageRequest } from '../types/message.types'

const BASE_PATH = '/chat/api/sessions'

export async function fetchMessages(sessionId: string, page = 0, size = 50) {
  return await get<PageResponse<Message>>(
    `${BASE_PATH}/${sessionId}/messages?page=${page}&size=${size}`,
  )
}

export async function sendMessage(sessionId: string, payload: CreateMessageRequest) {
  return await post<Message, CreateMessageRequest>(
    `${BASE_PATH}/${sessionId}/messages`,
    payload,
  )
}
