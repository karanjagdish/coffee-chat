import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { LoginForm } from './components/auth/LoginForm'
import { SignupForm } from './components/auth/SignupForm'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { SessionList } from './components/sessions/SessionList'
import { ChatWindow } from './components/chat/ChatWindow'
import { useAuth } from './context/AuthContext'
import { useSessions } from './hooks/useSessions'
import { ProfileView } from './components/profile/ProfileView'

function AppShell() {
  const { user, logout } = useAuth()
  const {
    sessions,
    loading,
    error,
    selectedSessionId,
    selectSession,
    createNewSession,
    renameSession,
    toggleFavoriteSession,
    deleteSession,
  } = useSessions()
  const location = useLocation()
  const navigate = useNavigate()

  const selectedSession = sessions.find((s) => s.id === selectedSessionId) ?? null
  const isProfile = location.pathname.startsWith('/app/profile')

  return (
    <div className="flex h-screen flex-col bg-slate-950 text-slate-100 overflow-hidden">
      <header className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
        <div className="flex items-center gap-4 text-sm">
          <div className="font-semibold tracking-tight text-slate-100">Coffee Chat</div>
          <nav className="flex gap-2 text-xs">
            <button
              type="button"
              onClick={() => navigate('/app')}
              className={`rounded-md border px-2 py-1 ${
                !isProfile
                  ? 'border-emerald-500 bg-emerald-500 text-slate-950'
                  : 'border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800'
              }`}
            >
              Chat
            </button>
            <button
              type="button"
              onClick={() => navigate('/app/profile')}
              className={`rounded-md border px-2 py-1 ${
                isProfile
                  ? 'border-emerald-500 bg-emerald-500 text-slate-950'
                  : 'border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800'
              }`}
            >
              Profile
            </button>
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm text-slate-300">
          {user && <span className="text-slate-400">{user.username}</span>}
          <button
            type="button"
            onClick={logout}
            className="rounded-md border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-800"
          >
            Logout
          </button>
        </div>
      </header>
      <main className="flex flex-1 min-h-0">
        {isProfile ? (
          <section className="flex-1 min-h-0">
            <ProfileView />
          </section>
        ) : (
          <>
            <aside className="w-72 border-r border-slate-800">
              <SessionList
                sessions={sessions}
                loading={loading}
                error={error}
                selectedSessionId={selectedSessionId}
                onSelectSession={selectSession}
                onCreateSession={createNewSession}
                onRenameSession={renameSession}
                onToggleFavorite={toggleFavoriteSession}
                onDeleteSession={deleteSession}
              />
            </aside>
            <section className="flex-1 min-h-0 flex">
              <ChatWindow sessionId={selectedSession?.id ?? null} sessionName={selectedSession?.sessionName ?? ''} />
            </section>
          </>
        )}
      </main>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginForm />} />
      <Route path="/signup" element={<SignupForm />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AppShell />} />
        <Route path="/app/profile" element={<AppShell />} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}

export default App
