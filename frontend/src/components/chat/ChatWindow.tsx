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
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const container = containerRef.current
    const sentinel = sentinelRef.current
    if (!container || !sentinel) return

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry.isIntersecting && hasNext && !loading) {
          void loadMore()
        }
      },
      { root: container, threshold: 1.0 },
    )

    observer.observe(sentinel)

    return () => {
      observer.disconnect()
    }
  }, [hasNext, loading, loadMore])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!draft.trim()) return
    await send(draft.trim())
    setDraft('')
  }

  if (!sessionId) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-slate-500">
        Select or create a session to start chatting.
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-slate-800 px-4 py-2 text-sm font-medium text-slate-200">
        {sessionName || 'Chat'}
      </div>
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto p-4 text-sm text-slate-100 space-y-2"
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
        {/* Sentinel for infinite scroll */}
        <div ref={sentinelRef} className="h-1 w-full" />
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
