export interface Session {
  id: string
  userId: string
  sessionName: string
  isFavorite: boolean
  createdAt: string
  updatedAt?: string
}

export interface CreateSessionRequest {
  sessionName: string
}

export interface RenameSessionRequest {
  name: string
}
