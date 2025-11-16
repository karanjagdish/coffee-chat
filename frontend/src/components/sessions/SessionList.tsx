import { useState, type FormEvent } from 'react'
import type { Session } from '../../types/session.types'

interface SessionListProps {
  sessions: Session[]
  loading: boolean
  error: string | null
  selectedSessionId: string | null
  onSelectSession: (id: string) => void
  onCreateSession: (name: string) => Promise<void>
  onRenameSession: (id: string, newName: string) => Promise<void>
  onToggleFavorite: (id: string) => Promise<void>
  onDeleteSession: (id: string) => Promise<void>
}

export function SessionList({
  sessions,
  loading,
  error,
  selectedSessionId,
  onSelectSession,
  onCreateSession,
  onRenameSession,
  onToggleFavorite,
  onDeleteSession,
}: SessionListProps) {
  const [newName, setNewName] = useState('')
  const [creating, setCreating] = useState(false)

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    if (!newName.trim()) return
    setCreating(true)
    try {
      await onCreateSession(newName.trim())
      setNewName('')
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="flex h-full flex-col border-r border-slate-800 bg-slate-950/60">
      <div className="px-4 py-3 text-sm font-semibold text-slate-200 border-b border-slate-800 flex items-center justify-between">
        <span>Sessions</span>
      </div>
      <form onSubmit={handleCreate} className="border-b border-slate-800 px-3 py-2 flex gap-2">
        <input
          type="text"
          placeholder="New session…"
          className="flex-1 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-50 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
        />
        <button
          type="submit"
          disabled={creating || !newName.trim()}
          className="rounded-md bg-emerald-500 px-2 py-1 text-xs font-medium text-slate-950 hover:bg-emerald-400 disabled:opacity-60"
        >
          New
        </button>
      </form>
      <div className="flex-1 overflow-y-auto p-2 text-sm text-slate-400">
        {loading && <p className="px-2 py-1 text-xs text-slate-500">Loading sessions…</p>}
        {error && <p className="px-2 py-1 text-xs text-red-400">{error}</p>}
        {!loading && sessions.length === 0 && !error && (
          <p className="px-2 py-1 text-xs text-slate-500">No sessions yet. Create one above.</p>
        )}
        <ul className="space-y-1">
          {sessions.map((session) => {
            const isSelected = session.id === selectedSessionId
            return (
              <li key={session.id}>
                <div
                  className={`flex w-full items-center justify-between rounded-md px-2 py-1.5 text-xs transition ${
                    isSelected
                      ? 'bg-slate-800 text-slate-50'
                      : 'bg-transparent text-slate-300 hover:bg-slate-900'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => onSelectSession(session.id)}
                    className="flex-1 truncate text-left"
                  >
                    {session.sessionName}
                  </button>
                  <div className="ml-2 flex items-center gap-1">
                    <button
                      type="button"
                      title={session.isFavorite ? 'Unfavorite' : 'Favorite'}
                      onClick={() => onToggleFavorite(session.id)}
                      className={`rounded px-1 hover:bg-slate-800 ${
                        session.isFavorite ? 'text-amber-400' : 'text-slate-500'
                      }`}
                    >
                      {session.isFavorite ? '★' : '☆'}
                    </button>
                    <button
                      type="button"
                      title="Rename session"
                      onClick={async () => {
                        const current = session.sessionName
                        const next = window.prompt('Rename session', current)
                        if (!next || next.trim() === '' || next === current) return
                        await onRenameSession(session.id, next.trim())
                      }}
                      className="rounded px-1 text-slate-400 hover:bg-slate-800"
                    >
                      ✎
                    </button>
                    <button
                      type="button"
                      title="Delete session"
                      onClick={async () => {
                        const confirmed = window.confirm('Delete this session? This action cannot be undone.')
                        if (!confirmed) return
                        await onDeleteSession(session.id)
                      }}
                      className="rounded px-1 text-red-400 hover:bg-slate-800"
                    >
                      ×
                    </button>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}
