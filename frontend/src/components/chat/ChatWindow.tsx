import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMessages } from '../../hooks/useMessages'

interface ChatWindowProps {
  sessionId: string | null
  sessionName: string
}

export function ChatWindow({ sessionId, sessionName }: ChatWindowProps) {
  const { messages, loading, error, send, loadMore, hasNext } = useMessages(sessionId)
  const [draft, setDraft] = useState('')

  const containerRef = useRef<HTMLDivElement | null>(null)
  const isLoadingMoreRef = useRef(false)
  const initialLoadRef = useRef(true)

  const handleScroll = () => {
    const container = containerRef.current
    if (!container) return
    if (!hasNext || loading || isLoadingMoreRef.current) return

    // When the user scrolls near the top, load older messages
    if (container.scrollTop <= 16) {
      isLoadingMoreRef.current = true
      const previousScrollHeight = container.scrollHeight
      const previousScrollTop = container.scrollTop

      void loadMore()
        .then(() => {
          const updatedContainer = containerRef.current
          if (!updatedContainer) return
          const newScrollHeight = updatedContainer.scrollHeight
          const addedHeight = newScrollHeight - previousScrollHeight
          // Preserve the user's viewport position: shift down by the height of the newly prepended content
          updatedContainer.scrollTop = previousScrollTop + addedHeight
        })
        .finally(() => {
          isLoadingMoreRef.current = false
        })
    }
  }

  // On initial load, scroll to the bottom so the latest messages are visible
  useEffect(() => {
    const container = containerRef.current
    if (!container) return
    if (initialLoadRef.current && messages.length > 0) {
      container.scrollTop = container.scrollHeight
      initialLoadRef.current = false
    }
  }, [messages.length])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!draft.trim()) return
    await send(draft.trim())
    setDraft('')

    const container = containerRef.current
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  }

  if (!sessionId) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-slate-500">
        Select or create a session to start chatting.
      </div>
    )
  }

  return (
    <div className="flex flex-1 flex-col min-h-0">
      <div className="border-b border-slate-800 px-4 py-2 text-sm font-medium text-slate-200">
        {sessionName || 'Chat'}
      </div>
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto p-4 text-sm text-slate-100 space-y-2"
        onScroll={handleScroll}
      >
        {loading && messages.length === 0 && (
          <p className="text-xs text-slate-500">Loading messages…</p>
        )}
        {loading && messages.length > 0 && (
          <p className="text-xs text-slate-500">Loading more messages…</p>
        )}
        {error && <p className="text-xs text-red-400">{error}</p>}
        {messages.map((m) => (
          <div
            key={m.id}
            className={`max-w-xl rounded-lg px-3 py-2 text-xs ${
              m.sender === 'USER'
                ? 'ml-auto bg-emerald-600 text-slate-950'
                : 'mr-auto bg-slate-800 text-slate-100'
            }`}
          >
            <p className="whitespace-pre-wrap break-words">{m.content}</p>
          </div>
        ))}
        {!loading && messages.length === 0 && !error && (
          <p className="text-xs text-slate-500">No messages yet. Say hi!</p>
        )}
      </div>
      <div className="border-t border-slate-800 p-3">
        <form className="flex gap-2" onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Type your message…"
            className="flex-1 rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-50 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            disabled={loading || !sessionId}
          />
          <button
            type="submit"
            disabled={loading || !sessionId || !draft.trim()}
            className="rounded-md bg-emerald-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400 disabled:opacity-60"
          >
            Send
          </button>
        </form>
      </div>
    </div>
  )
}
