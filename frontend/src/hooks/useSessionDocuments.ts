import { useCallback, useEffect, useState } from 'react'
import type { SessionDocument } from '../types/document.types'
import {
  fetchSessionDocuments,
  uploadSessionDocument,
  deleteSessionDocument,
} from '../services/documents'

interface UseSessionDocumentsResult {
  documents: SessionDocument[]
  loading: boolean
  error: string | null
  uploading: boolean
  upload: (file: File) => Promise<void>
  remove: (id: string) => Promise<void>
}

export function useSessionDocuments(sessionId: string | null): UseSessionDocumentsResult {
  const [documents, setDocuments] = useState<SessionDocument[]>([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!sessionId) {
      setDocuments([])
      return
    }
    setLoading(true)
    setError(null)
    try {
      const response = await fetchSessionDocuments(sessionId)
      if (response.success && response.data) {
        setDocuments(response.data)
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents')
    } finally {
      setLoading(false)
    }
  }, [sessionId])

  useEffect(() => {
    void load()
  }, [load])

  const upload = useCallback(
    async (file: File) => {
      if (!sessionId) return
      setError(null)
      setUploading(true)
      try {
        const response = await uploadSessionDocument(sessionId, file)
        if (response.success) {
          await load()
        } else if (response.error) {
          setError(response.error.message)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to upload document')
      } finally {
        setUploading(false)
      }
    },
    [sessionId, load],
  )

  const remove = useCallback(
    async (id: string) => {
      if (!sessionId) return
      setError(null)
      try {
        const response = await deleteSessionDocument(sessionId, id)
        if (response.success) {
          setDocuments((prev) => prev.filter((d) => d.id !== id))
        } else if (response.error) {
          setError(response.error.message)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to delete document')
      }
    },
    [sessionId],
  )

  return {
    documents,
    loading,
    error,
    uploading,
    upload,
    remove,
  }
}
