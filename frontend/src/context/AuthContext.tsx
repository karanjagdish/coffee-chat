import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { AuthPayload, LoginRequest, SignupRequest, User } from '../types/auth.types'
import type { ApiResponse } from '../types/api.types'
import { login as loginApi, signup as signupApi, validateToken } from '../services/auth'

interface AuthContextValue {
  user: User | null
  token: string | null
  refreshToken: string | null
  loading: boolean
  login: (payload: LoginRequest) => Promise<void>
  signup: (payload: SignupRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const AUTH_TOKEN_KEY = 'auth_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(AUTH_TOKEN_KEY))
  const [refreshToken, setRefreshToken] = useState<string | null>(() => localStorage.getItem(REFRESH_TOKEN_KEY))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function bootstrap() {
      if (!token) {
        setLoading(false)
        return
      }

      try {
        const response = await validateToken()
        if (response.success && response.data) {
          setUser(response.data)
        } else {
          clearAuth()
        }
      } catch {
        clearAuth()
      } finally {
        setLoading(false)
      }
    }

    void bootstrap()
    // we intentionally ignore refreshToken here; validation is based on access token
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  function persistAuth(payload: AuthPayload) {
    setUser(payload.user)
    setToken(payload.token)
    setRefreshToken(payload.refreshToken)
    localStorage.setItem(AUTH_TOKEN_KEY, payload.token)
    localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken)
  }

  function clearAuth() {
    setUser(null)
    setToken(null)
    setRefreshToken(null)
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  async function login(payload: LoginRequest) {
    const response: ApiResponse<AuthPayload> = await loginApi(payload)
    if (response.success && response.data) {
      persistAuth(response.data)
    } else {
      throw new Error(response.error?.message ?? 'Login failed')
    }
  }

  async function signup(payload: SignupRequest) {
    const response: ApiResponse<AuthPayload> = await signupApi(payload)
    if (response.success && response.data) {
      persistAuth(response.data)
    } else {
      throw new Error(response.error?.message ?? 'Signup failed')
    }
  }

  function logout() {
    clearAuth()
  }

  const value: AuthContextValue = {
    user,
    token,
    refreshToken,
    loading,
    login,
    signup,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
