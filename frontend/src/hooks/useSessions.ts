import { useCallback, useEffect, useState } from 'react'
import type { Session } from '../types/session.types'
import { createSession, deleteSession, fetchSessions, renameSession, toggleFavorite } from '../services/sessions'

interface UseSessionsResult {
  sessions: Session[]
  loading: boolean
  error: string | null
  selectedSessionId: string | null
  selectSession: (id: string) => void
  createNewSession: (name: string) => Promise<void>
  renameSession: (id: string, newName: string) => Promise<void>
  toggleFavoriteSession: (id: string) => Promise<void>
  deleteSession: (id: string) => Promise<void>
}

export function useSessions(): UseSessionsResult {
  const [sessions, setSessions] = useState<Session[]>([])
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await fetchSessions()
      if (response.success && response.data) {
        setSessions(response.data)
        if (!selectedSessionId && response.data.length > 0) {
          setSelectedSessionId(response.data[0].id)
        }
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load sessions')
    } finally {
      setLoading(false)
    }
  }, [selectedSessionId])

  useEffect(() => {
    void load()
  }, [load])

  const selectSession = (id: string) => {
    setSelectedSessionId(id)
  }

  const createNewSession = async (name: string) => {
    setError(null)
    try {
      const response = await createSession({ sessionName: name })
      if (response.success && response.data) {
        setSessions((prev) => [response.data!, ...prev])
        setSelectedSessionId(response.data.id)
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create session')
    }
  }

  const renameSessionAction = async (id: string, newName: string) => {
    setError(null)
    try {
      const response = await renameSession(id, { name: newName })
      if (response.success && response.data) {
        setSessions((prev) => prev.map((s) => (s.id === id ? response.data! : s)))
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to rename session')
    }
  }

  const toggleFavoriteSession = async (id: string) => {
    setError(null)
    try {
      const response = await toggleFavorite(id)
      if (response.success && response.data) {
        setSessions((prev) => prev.map((s) => (s.id === id ? response.data! : s)))
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update favorite status')
    }
  }

  const deleteSessionAction = async (id: string) => {
    setError(null)
    try {
      const response = await deleteSession(id)
      if (response.success) {
        setSessions((prev) => {
          const updated = prev.filter((s) => s.id !== id)
          if (selectedSessionId === id) {
            const nextSelected = updated[0]?.id ?? null
            setSelectedSessionId(nextSelected)
          }
          return updated
        })
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete session')
    }
  }

  return {
    sessions,
    loading,
    error,
    selectedSessionId,
    selectSession,
    createNewSession,
    renameSession: renameSessionAction,
    toggleFavoriteSession,
    deleteSession: deleteSessionAction,
  }
}
