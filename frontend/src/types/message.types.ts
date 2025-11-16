export type MessageSender = 'USER' | 'AI'

export interface Message {
  id: string
  sessionId: string
  sender: MessageSender
  content: string
  context?: Record<string, unknown> | null
  messageOrder: number
  createdAt: string
  updatedAt?: string
}

export interface CreateMessageRequest {
  sender: MessageSender
  content: string
  context?: Record<string, unknown>
}
