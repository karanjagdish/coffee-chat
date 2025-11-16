import { useCallback, useEffect, useState } from 'react'
import type { Message } from '../types/message.types'
import { fetchMessages, sendMessage } from '../services/messages'

interface UseMessagesResult {
  messages: Message[]
  loading: boolean
  error: string | null
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
  loadMore: () => Promise<void>
  refresh: () => Promise<void>
  send: (content: string) => Promise<void>
}

export function useMessages(sessionId: string | null): UseMessagesResult {
  const [messages, setMessages] = useState<Message[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [page, setPage] = useState(0)
  const [size] = useState(5)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [hasPrevious, setHasPrevious] = useState(false)

  const load = useCallback(
    async (targetPage = 0, append = false) => {
      if (!sessionId) {
        setMessages([])
        setPage(0)
        setTotalElements(0)
        setTotalPages(0)
        setHasNext(false)
        setHasPrevious(false)
        return
      }

      setLoading(true)
      setError(null)
      try {
        const response = await fetchMessages(sessionId, targetPage, size)
        if (response.success && response.data) {
          const data = response.data
          const pageMessages = data.content.slice().reverse()
          setPage(data.page)
          setTotalElements(data.totalElements)
          setTotalPages(data.totalPages)
          setHasNext(data.hasNext)
          setHasPrevious(data.hasPrevious)
          setMessages((prev) => (append ? [...pageMessages, ...prev] : pageMessages))
        } else if (response.error) {
          setError(response.error.message)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load messages')
      } finally {
        setLoading(false)
      }
    },
    [sessionId, size],
  )

  useEffect(() => {
    void load(0, false)
  }, [load])

  const loadMore = useCallback(async () => {
    if (!sessionId || !hasNext || loading) return
    await load(page + 1, true)
  }, [sessionId, hasNext, loading, load, page])

  const refresh = useCallback(async () => {
    if (!sessionId) return
    await load(0, false)
  }, [sessionId, load])

  const send = async (content: string) => {
    if (!sessionId || !content.trim()) return
    setError(null)
    try {
      const response = await sendMessage(sessionId, { sender: 'USER', content })
      if (response.success && response.data) {
        setMessages((prev) => [...prev, response.data!])
        setTotalElements((prev) => prev + 1)
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send message')
    }
  }

  return {
    messages,
    loading,
    error,
    page,
    size,
    totalElements,
    totalPages,
    hasNext,
    hasPrevious,
    loadMore,
    refresh,
    send,
  }
}
