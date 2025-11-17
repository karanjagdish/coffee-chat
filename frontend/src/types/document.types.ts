export type SessionDocumentStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED'

export interface SessionDocument {
  id: string
  filename: string
  contentType: string
  sizeBytes: number
  status: SessionDocumentStatus
  errorMessage?: string | null
  createdAt: string
  updatedAt?: string | null
}
