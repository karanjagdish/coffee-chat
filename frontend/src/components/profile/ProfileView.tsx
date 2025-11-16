import { UserProfile } from './UserProfile'
import { ApiKeyManager } from './ApiKeyManager'

export function ProfileView() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-4 px-4 py-6">
      <UserProfile />
      <ApiKeyManager />
    </div>
  )
}
