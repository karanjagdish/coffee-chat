export interface ApiErrorDetails {
  code: string
  message: string
  timestamp: string
  path?: string
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: ApiErrorDetails
  timestamp?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}
