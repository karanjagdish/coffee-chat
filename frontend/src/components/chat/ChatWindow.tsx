import { useEffect, useRef, useState, type FormEvent, type ChangeEvent } from 'react'
import { useMessages } from '../../hooks/useMessages'
import { useSessionDocuments } from '../../hooks/useSessionDocuments'

interface ChatWindowProps {
  sessionId: string | null
  sessionName: string
}

export function ChatWindow({ sessionId, sessionName }: ChatWindowProps) {
  const { messages, loading, error, send, loadMore, hasNext, sending } = useMessages(sessionId)
  const [draft, setDraft] = useState('')

  const containerRef = useRef<HTMLDivElement | null>(null)
  const isLoadingMoreRef = useRef(false)
  const initialLoadRef = useRef(true)
  const shouldScrollToBottomRef = useRef(false)

  const {
    documents,
    loading: docsLoading,
    error: docsError,
    uploading,
    upload,
    remove,
  } = useSessionDocuments(sessionId)

  const fileInputRef = useRef<HTMLInputElement | null>(null)

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
    if (shouldScrollToBottomRef.current) {
      container.scrollTop = container.scrollHeight
      shouldScrollToBottomRef.current = false
      return
    }
    if (initialLoadRef.current && messages.length > 0) {
      container.scrollTop = container.scrollHeight
      initialLoadRef.current = false
    }
  }, [messages.length])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!draft.trim()) return
    shouldScrollToBottomRef.current = true
    await send(draft.trim())
    setDraft('')
  }

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    await upload(file)
    event.target.value = ''
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
      <div className="border-b border-slate-800 px-4 py-2 text-xs text-slate-400 flex items-center justify-between gap-2">
        <div>
          <span className="font-medium">Documents</span>{' '}
          {docsLoading
            ? 'Loading…'
            : documents.length === 0
            ? 'None attached'
            : `${documents.length} attached`}
        </div>
        <div className="flex items-center gap-2">
          {uploading && <span className="text-xs text-slate-500">Uploading…</span>}
          <label className="cursor-pointer rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-xs hover:bg-slate-800">
            Attach
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={handleFileChange}
              disabled={!sessionId || uploading}
            />
          </label>
        </div>
      </div>
      {docsError && (
        <div className="border-b border-slate-800 px-4 py-1 text-xs text-red-400">{docsError}</div>
      )}
      {documents.length > 0 && (
        <div className="border-b border-slate-800 px-4 py-2 text-xs text-slate-400 flex flex-wrap gap-2">
          {documents.map((doc) => (
            <button
              key={doc.id}
              type="button"
              onClick={() => remove(doc.id)}
              className="flex items-center gap-1 rounded-full border border-slate-700 px-2 py-0.5 hover:bg-slate-800"
            >
              <span className="max-w-[160px] truncate">{doc.filename}</span>
              <span className="text-[10px] uppercase text-slate-500">{doc.status.toLowerCase()}</span>
              <span className="text-slate-500">×</span>
            </button>
          ))}
        </div>
      )}
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
        {sending && (
          <p className="text-xs text-slate-500">Waiting for AI response…</p>
        )}
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
            disabled={loading || sending || !sessionId}
          />
          <button
            type="submit"
            disabled={loading || sending || !sessionId || !draft.trim()}
            className="rounded-md bg-emerald-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400 disabled:opacity-60"
          >
            Send
          </button>
        </form>
      </div>
    </div>
  )
}
