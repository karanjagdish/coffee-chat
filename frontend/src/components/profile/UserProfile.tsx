import { useAuth } from '../../context/AuthContext'

export function UserProfile() {
  const { user } = useAuth()

  if (!user) {
    return null
  }

  const created = new Date(user.createdAt)

  return (
    <section className="space-y-2 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
      <h2 className="text-sm font-semibold text-slate-100">Profile</h2>
      <dl className="space-y-1 text-xs text-slate-300">
        <div className="flex justify-between gap-4">
          <dt className="text-slate-400">Username</dt>
          <dd className="font-medium text-slate-100">{user.username}</dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-slate-400">Email</dt>
          <dd className="font-medium text-slate-100">{user.email}</dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-slate-400">Joined</dt>
          <dd className="text-slate-400">{created.toLocaleString()}</dd>
        </div>
      </dl>
    </section>
  )
}
