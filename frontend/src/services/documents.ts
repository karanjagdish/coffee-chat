import { get, post, del } from './api'
import type { SessionDocument } from '../types/document.types'

const BASE_PATH = '/chat/api/sessions'

export async function fetchSessionDocuments(sessionId: string) {
  return await get<SessionDocument[]>(`${BASE_PATH}/${sessionId}/documents`)
}

export async function uploadSessionDocument(sessionId: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return await post<SessionDocument, FormData>(
    `${BASE_PATH}/${sessionId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  )
}

export async function deleteSessionDocument(sessionId: string, documentId: string) {
  return await del<void>(`${BASE_PATH}/${sessionId}/documents/${documentId}`)
}
